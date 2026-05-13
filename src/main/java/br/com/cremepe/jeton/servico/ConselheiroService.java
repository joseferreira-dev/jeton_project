package br.com.cremepe.jeton.servico;

import br.com.cremepe.jeton.dominio.Conselheiro;
import br.com.cremepe.jeton.dominio.GestaoConselheiro;
import br.com.cremepe.jeton.dominio.GestaoConselheiroId;
import br.com.cremepe.jeton.repositorio.ConselheiroRepository;
import br.com.cremepe.jeton.repositorio.GestaoConselheiroRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Serviço responsável pela gestão de conselheiros e seus vínculos com as gestões.
 * Consolida as regras de negócio de conselheiros e mandatos.
 */
@Service
public class ConselheiroService {

    @Autowired
    private ConselheiroRepository conselheiroRepository;

    @Autowired
    private GestaoConselheiroRepository gestaoConselheiroRepository;

    @Transactional(readOnly = true)
    public List<Conselheiro> listarTodos() {
        return conselheiroRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<Conselheiro> listarAtivos() {
        return conselheiroRepository.findByInSituacao("A");
    }

    @Transactional(readOnly = true)
    public Optional<Conselheiro> buscarPorCrm(Integer crm) {
        return conselheiroRepository.findByCrm(crm);
    }

    @Transactional(readOnly = true)
    public List<Conselheiro> buscarPorNome(String nome) {
        return conselheiroRepository.findByPessoaNomeContainingIgnoreCase(nome);
    }

    /**
     * Recupera todos os vínculos (mandatos) de uma gestão específica.
     */
    @Transactional(readOnly = true)
    public List<GestaoConselheiro> listarVinculosPorGestao(Integer idGestao) {
        return gestaoConselheiroRepository.findByIdIdGestao(idGestao);
    }

    /**
     * Salva ou atualiza um conselheiro.
     * Devido ao @MapsId, a entidade Pessoa associada é gerenciada em cascata.
     */
    @Transactional
    public Conselheiro salvar(Conselheiro conselheiro) {
        return conselheiroRepository.save(conselheiro);
    }

    /**
     * Vincula um conselheiro a uma gestão (Criação de Mandato).
     */
    @Transactional
    public GestaoConselheiro vincularAGestao(Integer idConselheiro, Integer idGestao) {
        GestaoConselheiroId idComposto = new GestaoConselheiroId(idConselheiro, idGestao);
        
        // Verifica se o vínculo já existe
        return gestaoConselheiroRepository.findById(idComposto).orElseGet(() -> {
            GestaoConselheiro novoVinculo = new GestaoConselheiro();
            novoVinculo.setId(idComposto);
            novoVinculo.setInSituacao("A"); // Ativo por padrão no vínculo
            return gestaoConselheiroRepository.save(novoVinculo);
        });
    }

    @Transactional
    public void removerVinculo(Integer idConselheiro, Integer idGestao) {
        gestaoConselheiroRepository.deleteById(new GestaoConselheiroId(idConselheiro, idGestao));
    }

    public Optional<Conselheiro> buscarPorId(Integer id) {
        return conselheiroRepository.findById(id);
    }

    @Transactional
    public void excluir(Integer id) {
        conselheiroRepository.deleteById(id);
    }
}