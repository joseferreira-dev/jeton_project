package br.com.cremepe.jeton.servico;

import br.com.cremepe.jeton.dominio.AtividadeConselhal;
import br.com.cremepe.jeton.repositorio.AtividadeConselhalRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class AtividadeConselhalService {

    @Autowired
    private AtividadeConselhalRepository atividadeRepository;

    @Transactional
    public AtividadeConselhal salvarAtividade(AtividadeConselhal atividade) {
        
        // Regras para novas atividades
        if (atividade.getIdAtividade() == null) {
            // Regista o momento exato em que a atividade foi submetida
            atividade.setDataHoraRegistro(LocalDateTime.now());
            
            // Se o estado não foi definido, assume Pendente
            if (atividade.getInSituacao() == null || atividade.getInSituacao().isEmpty()) {
                atividade.setInSituacao("P"); 
            }
        }

        // Validação básica de bloqueio
        if (atividade.getRegra() != null && "S".equals(atividade.getRegra().getInRevogado())) {
            throw new RuntimeException("Não é possível registrar atividades utilizando uma regra revogada.");
        }

        // A qtdAtividade no modelo legado costuma ser 1 por padrão para a maioria dos casos
        if (atividade.getQtdAtividade() == null || atividade.getQtdAtividade() <= 0) {
            atividade.setQtdAtividade(1);
        }

        return atividadeRepository.save(atividade);
    }

    @Transactional(readOnly = true)
    public Page<AtividadeConselhal> listarComPaginacaoEPesquisa(String termo, String situacao, String turno, int page, int size, String sortField, String sortDir) {
        Sort sort = sortDir.equalsIgnoreCase("desc") ? Sort.by(sortField).descending() : Sort.by(sortField).ascending();
        Pageable pageable = (size == 0) ? Pageable.unpaged(sort) : PageRequest.of(page, size, sort);
        return atividadeRepository.pesquisarPaginado(termo, situacao, turno, pageable);
    }

    @Transactional
    public void excluirAtividade(Integer id) {
        atividadeRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public Optional<AtividadeConselhal> buscarPorId(Integer id) {
        return atividadeRepository.findById(id);
    }
}