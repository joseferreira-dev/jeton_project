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

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class GestaoConselheiroService {

    @Autowired
    private GestaoConselheiroRepository repository;
    @Autowired
    private GestaoRepository gestaoRepository;
    @Autowired
    private ConselheiroRepository conselheiroRepository;
    @Autowired
    private AtividadeConselhalRepository atividadeRepository;

    @Transactional
    public GestaoConselheiro salvar(GestaoConselheiro vinculo) {
        if (vinculo.getId() == null) {
            vinculo.setId(new GestaoConselheiroId());
        }
        vinculo.getId().setIdGestao(vinculo.getGestao().getIdGestao());
        vinculo.getId().setIdPessoa(vinculo.getConselheiro().getIdPessoa());

        // =========================================================================
        // REGRA DE NEGÓCIO: Um conselheiro só pode estar ATIVO em uma Gestão
        // =========================================================================
        if ("A".equals(vinculo.getInSituacao())) {
            inativarOutrosVinculosAtivos(vinculo.getId().getIdGestao(), vinculo.getId().getIdPessoa());
        }

        return repository.save(vinculo);
    }

    /**
     * Método auxiliar que busca e inativa silenciosamente quaisquer outros
     * vínculos ativos do conselheiro em gestões anteriores.
     */
    private void inativarOutrosVinculosAtivos(Integer idGestaoAtual, Integer idPessoa) {
        List<GestaoConselheiro> todosVinculos = repository.findByIdIdPessoa(idPessoa);

        for (GestaoConselheiro vinculoExistente : todosVinculos) {
            // Se o vínculo não for da gestão que estamos a salvar e estiver Ativo ('A')
            if (!vinculoExistente.getId().getIdGestao().equals(idGestaoAtual)
                    && "A".equals(vinculoExistente.getInSituacao())) {
                vinculoExistente.setInSituacao("I");
                repository.save(vinculoExistente);
            }
        }
    }

    @Transactional
    public void atualizarVinculosEmMassa(Integer idGestao, List<Integer> idsPessoasSelecionadas) {
        if (idsPessoasSelecionadas == null)
            idsPessoasSelecionadas = new ArrayList<>();

        // 1. Pegar quem está vinculado atualmente na gestão (independentemente do
        // status)
        List<GestaoConselheiro> atuais = repository.findByIdIdGestao(idGestao);
        List<Integer> idsAtuais = atuais.stream().map(v -> v.getId().getIdPessoa()).collect(Collectors.toList());

        // 2. PROCESSAR REMOÇÕES (Quem estava vinculado mas a caixa foi desmarcada)
        for (GestaoConselheiro vinculo : atuais) {
            Integer idPessoaLink = vinculo.getId().getIdPessoa();

            if (!idsPessoasSelecionadas.contains(idPessoaLink)) {
                // Verifica se tem atividades vinculadas
                long qtdAtividades = atividadeRepository.countByGestaoIdGestaoAndConselheiroIdPessoa(idGestao,
                        idPessoaLink);

                if (qtdAtividades == 0) {
                    repository.delete(vinculo);
                } else {
                    // Se a caixa foi desmarcada, mas ele tem atividades, não podemos apagar.
                    // No entanto, alteramos o status para INATIVO automaticamente!
                    if ("A".equals(vinculo.getInSituacao())) {
                        vinculo.setInSituacao("I");
                        repository.save(vinculo);
                    }
                }
            }
        }

        // 3. PROCESSAR ADIÇÕES (Quem teve a caixa marcada)
        if (!idsPessoasSelecionadas.isEmpty()) {
            Gestao gestao = gestaoRepository.findById(idGestao).orElseThrow();

            for (Integer idNovo : idsPessoasSelecionadas) {
                if (!idsAtuais.contains(idNovo)) {
                    // É um vínculo totalmente novo (nunca esteve nesta gestão)
                    Conselheiro c = conselheiroRepository.findById(idNovo).orElseThrow();
                    GestaoConselheiro novoVinculo = new GestaoConselheiro();
                    novoVinculo.setId(new GestaoConselheiroId(idGestao, idNovo));
                    novoVinculo.setGestao(gestao);
                    novoVinculo.setConselheiro(c);
                    novoVinculo.setInSituacao("A");

                    // Como entrará como ATIVO, aplica-se a regra de inativar as outras gestões
                    inativarOutrosVinculosAtivos(idGestao, idNovo);

                    repository.save(novoVinculo);
                } else {
                    // Já existia na gestão (talvez como inativo), e a caixa foi remarcada
                    GestaoConselheiro vinculoExistente = atuais.stream()
                            .filter(v -> v.getId().getIdPessoa().equals(idNovo))
                            .findFirst().orElse(null);

                    if (vinculoExistente != null && "I".equals(vinculoExistente.getInSituacao())) {
                        vinculoExistente.setInSituacao("A");
                        inativarOutrosVinculosAtivos(idGestao, idNovo);
                        repository.save(vinculoExistente);
                    } else if (vinculoExistente != null && "A".equals(vinculoExistente.getInSituacao())) {
                        // Apenas por precaução, se já estava ativo, varre as outras gestões
                        inativarOutrosVinculosAtivos(idGestao, idNovo);
                    }
                }
            }
        }
    }

    @Transactional(readOnly = true)
    public Page<GestaoConselheiro> listarComPaginacaoEPesquisa(String termo, String situacao, int page, int size,
            String sortField, String sortDir) {
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