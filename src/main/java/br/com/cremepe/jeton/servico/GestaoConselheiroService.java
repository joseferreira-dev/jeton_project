package br.com.cremepe.jeton.servico;

import br.com.cremepe.jeton.dominio.Conselheiro;
import br.com.cremepe.jeton.dominio.Gestao;
import br.com.cremepe.jeton.dominio.GestaoConselheiro;
import br.com.cremepe.jeton.dominio.GestaoConselheiroId;
import br.com.cremepe.jeton.repositorio.AtividadeConselhalRepository;
import br.com.cremepe.jeton.repositorio.ConselheiroRepository;
import br.com.cremepe.jeton.repositorio.GestaoConselheiroRepository;
import br.com.cremepe.jeton.repositorio.GestaoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class GestaoConselheiroService {

    @Autowired private GestaoConselheiroRepository repository;
    @Autowired private GestaoRepository gestaoRepository;
    @Autowired private ConselheiroRepository conselheiroRepository;
    @Autowired private AtividadeConselhalRepository atividadeRepository;

    @Transactional
    public GestaoConselheiro salvar(GestaoConselheiro vinculo) {
        if (vinculo.getId() == null) {
            vinculo.setId(new GestaoConselheiroId());
        }
        vinculo.getId().setIdGestao(vinculo.getGestao().getIdGestao());
        vinculo.getId().setIdPessoa(vinculo.getConselheiro().getIdPessoa());

        return repository.save(vinculo);
    }

    @Transactional
    public void atualizarVinculosEmMassa(Integer idGestao, List<Integer> idsPessoasSelecionadas) {
        if (idsPessoasSelecionadas == null) idsPessoasSelecionadas = new java.util.ArrayList<>();

        // 1. Pegar quem está vinculado atualmente
        List<GestaoConselheiro> atuais = repository.findByIdIdGestao(idGestao);
        List<Integer> idsAtuais = atuais.stream().map(v -> v.getId().getIdPessoa()).collect(Collectors.toList());

        // 2. PROCESSAR REMOÇÕES (Quem estava vinculado mas foi DESMARCADO)
        for (GestaoConselheiro vinculo : atuais) {
            Integer idPessoaLink = vinculo.getId().getIdPessoa();
            
            if (!idsPessoasSelecionadas.contains(idPessoaLink)) {
                // Antes de apagar, verificamos se ele tem atividades nesta gestão
                long qtdAtividades = atividadeRepository.countByGestaoIdGestaoAndConselheiroIdPessoa(idGestao, idPessoaLink);
                
                if (qtdAtividades == 0) {
                    repository.delete(vinculo);
                }
                // Se tiver atividades, o sistema simplesmente não apaga (ignora o desmarcar)
                // Isso evita o erro de Constraint no banco de dados.
            }
        }

        // 3. PROCESSAR ADIÇÕES (Quem foi MARCADO mas ainda não está vinculado)
        if (!idsPessoasSelecionadas.isEmpty()) {
            Gestao gestao = gestaoRepository.findById(idGestao).orElseThrow();
            
            for (Integer idNovo : idsPessoasSelecionadas) {
                if (!idsAtuais.contains(idNovo)) {
                    Conselheiro c = conselheiroRepository.findById(idNovo).orElseThrow();
                    GestaoConselheiro novoVinculo = new GestaoConselheiro();
                    novoVinculo.setId(new GestaoConselheiroId(idGestao, idNovo));
                    novoVinculo.setGestao(gestao);
                    novoVinculo.setConselheiro(c);
                    novoVinculo.setInSituacao("A");
                    repository.save(novoVinculo);
                }
            }
        }
    }

    @Transactional(readOnly = true)
    public Page<GestaoConselheiro> listarComPaginacaoEPesquisa(String termo, String situacao, int page, int size, String sortField, String sortDir) {
        Sort sort = sortDir.equalsIgnoreCase("desc") ? Sort.by(sortField).descending() : Sort.by(sortField).ascending();
        Pageable pageable = (size == 0) ? Pageable.unpaged(sort) : PageRequest.of(page, size, sort);
        return repository.pesquisarPaginado(termo, situacao, pageable);
    }

    @Transactional(readOnly = true)
    public List<GestaoConselheiro> listarTodos() {
        return repository.findAll();
    }

    @Transactional(readOnly = true)
    public Optional<GestaoConselheiro> buscarPorId(Integer idGestao, Integer idPessoa) {
        return repository.findById(new GestaoConselheiroId(idGestao, idPessoa));
    }

    @Transactional
    public void excluir(Integer idGestao, Integer idPessoa) {
        repository.deleteById(new GestaoConselheiroId(idGestao, idPessoa));
    }
}