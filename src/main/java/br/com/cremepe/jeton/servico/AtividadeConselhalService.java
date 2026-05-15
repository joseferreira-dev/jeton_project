package br.com.cremepe.jeton.servico;

import br.com.cremepe.jeton.dominio.AtividadeConselhal;
import br.com.cremepe.jeton.dominio.Gestao;
import br.com.cremepe.jeton.repositorio.AtividadeConselhalRepository;
import br.com.cremepe.jeton.repositorio.GestaoConselheiroRepository;
import br.com.cremepe.jeton.repositorio.GestaoRepository;
import br.com.cremepe.jeton.repositorio.ComprovanteRepository;
import br.com.cremepe.jeton.servico.FileStorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

@Service
public class AtividadeConselhalService {

    @Autowired private AtividadeConselhalRepository atividadeRepository;
    @Autowired private GestaoRepository gestaoRepository;
    @Autowired private GestaoConselheiroRepository gestaoConselheiroRepository;
    @Autowired private ComprovanteRepository comprovanteRepository;
    @Autowired private FileStorageService fileStorageService;

    @Transactional
    public AtividadeConselhal salvarAtividade(AtividadeConselhal atividade) {
        
        // 1. Validar se a Gestão existe e se a data da atividade está DENTRO do mandato
        Gestao gestao = gestaoRepository.findById(atividade.getGestao().getIdGestao())
                .orElseThrow(() -> new RuntimeException("A gestão informada não foi encontrada no sistema."));
                
        LocalDate dataAtividade = atividade.getDataHoraAtividade().toLocalDate();
        if (dataAtividade.isBefore(gestao.getDtInicio()) || dataAtividade.isAfter(gestao.getDtFim())) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
            throw new RuntimeException("A data da atividade (" + dataAtividade.format(formatter) + 
                    ") não é permitida. Ela deve estar dentro do período da Gestão selecionada (" + 
                    gestao.getDtInicio().format(formatter) + " a " + gestao.getDtFim().format(formatter) + ").");
        }

        // 2. Garantir segurança extra: Verificar se o conselheiro selecionado realmente possui vínculo com esta gestão
        boolean conselheiroVinculado = gestaoConselheiroRepository.findByIdIdGestao(gestao.getIdGestao()).stream()
                .anyMatch(v -> v.getConselheiro().getIdPessoa().equals(atividade.getConselheiro().getIdPessoa()));
        
        if (!conselheiroVinculado) {
            throw new RuntimeException("O médico selecionado não possui vínculo ativo com a Gestão informada.");
        }

        // 3. Regras para Novas Atividades
        if (atividade.getIdAtividade() == null) {
            atividade.setDataHoraRegistro(LocalDateTime.now());
            
            if (atividade.getInSituacao() == null || atividade.getInSituacao().isEmpty()) {
                atividade.setInSituacao("P"); 
            }
        }

        // 4. Validação de Regra Revogada
        if (atividade.getRegra() != null && "S".equals(atividade.getRegra().getInRevogado())) {
            throw new RuntimeException("Atenção: Não é possível registrar atividades utilizando uma regra que já foi revogada.");
        }

        // 5. Normalização de Quantidade
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
        Optional<AtividadeConselhal> optAtividade = atividadeRepository.findById(id);
        
        if (optAtividade.isPresent()) {
            AtividadeConselhal atividade = optAtividade.get();
            // Guarda a referência do comprovante (se existir) antes de apagar a atividade
            br.com.cremepe.jeton.dominio.Comprovante comprovante = atividade.getComprovante();
            
            // 1. Remove a atividade primeiro (para libertar a chave estrangeira)
            atividadeRepository.deleteById(id);
            
            // 2. Se a atividade tinha um anexo, destrói as provas (BD e FTP)
            if (comprovante != null) {
                // Remove da base de dados
                comprovanteRepository.delete(comprovante);
                // Remove o ficheiro físico do disco e da Locaweb
                fileStorageService.deleteFile(comprovante.getNomeArquivo(), comprovante.getAno(), comprovante.getMes());
            }
        }
    }

    @Transactional(readOnly = true)
    public Optional<AtividadeConselhal> buscarPorId(Integer id) {
        return atividadeRepository.findById(id);
    }
}