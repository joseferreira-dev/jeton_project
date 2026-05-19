package br.com.cremepe.jeton.servico;

import br.com.cremepe.jeton.dominio.AtividadeConselhal;
import br.com.cremepe.jeton.dominio.Gestao;
import br.com.cremepe.jeton.dominio.Resolucao;
import br.com.cremepe.jeton.repositorio.AtividadeConselhalRepository;
import br.com.cremepe.jeton.repositorio.GestaoConselheiroRepository;
import br.com.cremepe.jeton.repositorio.GestaoRepository;
import br.com.cremepe.jeton.repositorio.ComprovanteRepository;
import br.com.cremepe.jeton.repositorio.ResolucaoRepository;
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
import java.util.List;
import java.util.Optional;

@Service
public class AtividadeConselhalService {

    @Autowired private AtividadeConselhalRepository atividadeRepository;
    @Autowired private GestaoRepository gestaoRepository;
    @Autowired private GestaoConselheiroRepository gestaoConselheiroRepository;
    @Autowired private ComprovanteRepository comprovanteRepository;
    @Autowired private FileStorageService fileStorageService;
    @Autowired private ResolucaoRepository resolucaoRepository; // Novo repositório injetado

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

        // ==============================================================================
        // NOVA LÓGICA: Descoberta Dinâmica da Resolução e Validação de Teto Financeiro
        // ==============================================================================
        List<Resolucao> resolucoesVigentes = resolucaoRepository.findResoluesVigentesNaData(dataAtividade);
        if (resolucoesVigentes.isEmpty()) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
            throw new IllegalStateException("Não foi encontrada nenhuma Resolução ativa cadastrada para a data desta atividade: " + dataAtividade.format(formatter));
        }
        
        // A query (order by dtInicioVigencia desc) garante que o índice 0 é a norma mais recente válida para aquele dia
        Resolucao resolucaoFinanceiraVigente = resolucoesVigentes.get(0);
        
        // Realiza o bloqueio caso estoure o limite de jetons do turno estabelecido pela resolução encontrada
        validarLimitesTurno(atividade, resolucaoFinanceiraVigente);
        // ==============================================================================

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

    /**
     * Valida se a inclusão da nova atividade vai extrapolar o teto de pontos por turno
     * definido pela Resolução que estava vigente no dia da ocorrência.
     */
    private void validarLimitesTurno(AtividadeConselhal atividade, Resolucao resolucaoVigente) {
        LocalDate dataAtividade = atividade.getDataHoraAtividade().toLocalDate();
        
        // Busca a soma de pontos já lançados no banco para o mesmo conselheiro, no mesmo dia e turno
        // Certifique-se de que o método 'sumPontosPorConselheiroDiaETurno' esteja declarado no seu AtividadeConselhalRepository
        Integer pontosRegistrados = atividadeRepository.sumPontosPorConselheiroDiaETurno(
                atividade.getConselheiro().getIdPessoa(), 
                dataAtividade, 
                atividade.getInTurno()
        );
        
        if (pontosRegistrados == null) {
            pontosRegistrados = 0;
        }

        // Calcula a nova carga (Pontos da regra vs Quantidade executada)
        int pontosDaNovaAtividade = atividade.getRegra().getPontos() * (atividade.getQtdAtividade() != null ? atividade.getQtdAtividade() : 1);
        int somaTotalPontosTurno = pontosRegistrados + pontosDaNovaAtividade;

        // Limite da resolução. Se a soma completar/exceder o teto para o mesmo período, bloqueia.
        if (somaTotalPontosTurno > resolucaoVigente.getPontosPorJeton()) {
            throw new RuntimeException("Inclusão bloqueada: A soma dos pontos neste turno (" 
                    + somaTotalPontosTurno + " pontos) excede o limite estabelecido pela Resolução n. " 
                    + resolucaoVigente.getNumero() + "/" + resolucaoVigente.getAno() 
                    + " (" + resolucaoVigente.getPontosPorJeton() + " pontos por Jeton).");
        }
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