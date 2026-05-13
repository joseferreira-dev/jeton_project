package br.com.cremepe.jeton.servico;

import br.com.cremepe.jeton.dominio.AtividadeConselhal;
import br.com.cremepe.jeton.dominio.Regras;
import br.com.cremepe.jeton.repositorio.AtividadeConselhalRepository;
import br.com.cremepe.jeton.repositorio.RegrasRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Serviço que substitui a FachadaAtividadeConselhal.
 * Aqui residem as regras de negócio antes da persistência.
 */
@Service
public class AtividadeConselhalService {

    @Autowired
    private AtividadeConselhalRepository atividadeRepository;

    @Autowired
    private RegrasRepository regrasRepository;

    @Transactional(readOnly = true)
    public List<AtividadeConselhal> listarPorConselheiro(Integer idConselheiro) {
        return atividadeRepository.findByConselheiroIdPessoaOrderByDataHoraAtividadeDesc(idConselheiro);
    }

    @Transactional
    public AtividadeConselhal salvarAtividade(AtividadeConselhal atividade) {
        // REGRA DE NEGÓCIO EXEMPLO: 
        // Verificar se a regra utilizada está revogada antes de permitir o registro
        if ("S".equals(atividade.getRegra().getInRevogado())) {
            throw new RuntimeException("Não é possível registrar atividades para uma regra revogada.");
        }
        
        // Aqui você pode reaproveitar suas classes de validação como ValidarCPF
        // que agora podem ser injetadas como componentes se necessário.

        return atividadeRepository.save(atividade);
    }

    @Transactional
    public void excluirAtividade(Integer id) {
        atividadeRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public List<Regras> listarRegrasAtivas() {
        return regrasRepository.findByInRevogado("N");
    }

    @Transactional(readOnly = true)
    public List<AtividadeConselhal> listarTodos() {
        return atividadeRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Optional<AtividadeConselhal> buscarPorId(Integer id) {
        return atividadeRepository.findById(id);
    }
}