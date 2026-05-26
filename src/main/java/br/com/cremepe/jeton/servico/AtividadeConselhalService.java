package br.com.cremepe.jeton.servico;

import br.com.cremepe.jeton.dominio.AtividadeConselhal;
import br.com.cremepe.jeton.dominio.Comprovante;
import br.com.cremepe.jeton.dominio.Gestao;
import br.com.cremepe.jeton.dominio.TipoAnexo;
import br.com.cremepe.jeton.repositorio.AtividadeConselhalRepository;
import br.com.cremepe.jeton.repositorio.ComprovanteRepository;
import br.com.cremepe.jeton.repositorio.GestaoConselheiroRepository;
import br.com.cremepe.jeton.repositorio.GestaoRepository;
import br.com.cremepe.jeton.repositorio.TipoAnexoRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

@Service
public class AtividadeConselhalService {

    // =========================================================================
    // DEPENDÊNCIAS
    // =========================================================================
    @Autowired
    private AtividadeConselhalRepository atividadeRepository;
    @Autowired
    private GestaoRepository gestaoRepository;
    @Autowired
    private GestaoConselheiroRepository gestaoConselheiroRepository;
    @Autowired
    private ComprovanteRepository comprovanteRepository;
    @Autowired
    private TipoAnexoRepository tipoAnexoRepository;
    @Autowired
    private FileStorageService fileStorageService;

    // =========================================================================
    // OPERAÇÕES DE ESCRITA (CRUD)
    // =========================================================================

    @Transactional
    public AtividadeConselhal salvarAtividade(AtividadeConselhal atividade) {
        validarAtividadeNaoFechada(atividade.getIdAtividade());
        Gestao gestao = validarGestaoEvinculo(atividade);
        validarDataDentroDoMandato(atividade.getDataHoraAtividade().toLocalDate(), gestao);

        if (atividade.getIdAtividade() == null) {
            atividade.setDataHoraRegistro(LocalDateTime.now());
            if (atividade.getInSituacao() == null || atividade.getInSituacao().isEmpty()) {
                atividade.setInSituacao(AtividadeConselhal.SITUACAO_PENDENTE);
            }
        }

        if (atividade.getQtdAtividade() == null || atividade.getQtdAtividade() <= 0) {
            atividade.setQtdAtividade(1);
        }

        return atividadeRepository.save(atividade);
    }

    @Transactional
    public void salvarAtividadeComComprovante(AtividadeConselhal atividade,
            MultipartFile file,
            Integer idTipoAnexo,
            String nomeComprovanteUsuario,
            Integer idComprovanteAntigo) {

        // 1. Se há um comprovante antigo, desvincula da atividade (mas não apaga ainda)
        if (idComprovanteAntigo != null) {
            atividadeRepository.desvincularComprovante(atividade.getIdAtividade());
        }

        // 2. Se um novo arquivo foi enviado, cria e salva o novo comprovante
        Comprovante novoComprovante = null;
        if (file != null && !file.isEmpty()) {
            novoComprovante = criarComprovante(file, idTipoAnexo, nomeComprovanteUsuario);
            atividade.setComprovante(novoComprovante);
        } else if (atividade.getIdAtividade() != null && idComprovanteAntigo != null) {
            // Mantém o antigo, mas atualiza nome se necessário
            Comprovante antigo = comprovanteRepository.findById(idComprovanteAntigo).orElse(null);
            if (antigo != null && nomeComprovanteUsuario != null
                    && !nomeComprovanteUsuario.equals(antigo.getNomeComprovante())) {
                antigo.setNomeComprovante(nomeComprovanteUsuario);
                comprovanteRepository.save(antigo);
            }
            atividade.setComprovante(antigo);
        }

        // 3. Salva a atividade (com o comprovante já gerenciado)
        salvarAtividade(atividade);

        // 4. Após salvar, exclui o comprovante antigo se não for mais usado
        if (idComprovanteAntigo != null && file != null && !file.isEmpty()) {
            long outrasAtividades = atividadeRepository.countByComprovanteIdComprovante(idComprovanteAntigo);
            if (outrasAtividades == 0) {
                comprovanteRepository.findById(idComprovanteAntigo).ifPresent(comp -> {
                    fileStorageService.deleteFile(comp.getNomeArquivo(), comp.getAno(), comp.getMes());
                    comprovanteRepository.delete(comp);
                });
            }
        }
    }

    private Comprovante criarComprovante(MultipartFile file, Integer idTipoAnexo, String nomeComprovanteUsuario) {
        LocalDate hoje = LocalDate.now();
        String nomeArquivo = fileStorageService.storeFileToFtp(file, hoje.getYear(), hoje.getMonthValue());
        TipoAnexo tipo = tipoAnexoRepository.findById(idTipoAnexo)
                .orElseThrow(() -> new RuntimeException("Tipo de anexo inválido"));
        Comprovante comp = new Comprovante();
        comp.setTipoAnexo(tipo);
        comp.setNomeComprovante(nomeComprovanteUsuario);
        comp.setNomeArquivo(nomeArquivo);
        comp.setContentType(file.getContentType());
        comp.setMes(hoje.getMonthValue());
        comp.setAno(hoje.getYear());
        return comprovanteRepository.save(comp);
    }

    @Transactional
    public void validarAtividade(Integer id) {
        AtividadeConselhal atividade = buscarAtividadeOuLancarExcecao(id);
        if (AtividadeConselhal.SITUACAO_FECHADA.equals(atividade.getInSituacao())) {
            throw new RuntimeException("Operação negada: Esta atividade está fechada em folha.");
        }
        atividade.setInSituacao(AtividadeConselhal.SITUACAO_VALIDADA);
        atividadeRepository.save(atividade);
    }

    @Transactional
    public void desvalidarAtividade(Integer id) {
        AtividadeConselhal atividade = buscarAtividadeOuLancarExcecao(id);
        if (AtividadeConselhal.SITUACAO_FECHADA.equals(atividade.getInSituacao())) {
            throw new RuntimeException("Operação negada: Esta atividade está fechada em folha.");
        }
        if (AtividadeConselhal.COMPUTADA_SIM.equals(atividade.getInComputada())) {
            throw new RuntimeException(
                    "Operação negada: Esta atividade já foi computada em um processamento financeiro.");
        }
        atividade.setInSituacao(AtividadeConselhal.SITUACAO_PENDENTE);
        atividadeRepository.save(atividade);
    }

    @Transactional
    public void excluirAtividade(Integer id) {
        AtividadeConselhal atividade = buscarAtividadeOuLancarExcecao(id);
        if (AtividadeConselhal.SITUACAO_FECHADA.equals(atividade.getInSituacao())) {
            throw new RuntimeException(
                    "Operação negada: Esta atividade está vinculada a uma folha de pagamento FECHADA e não pode ser eliminada.");
        }

        Integer idComprovante = null;
        Comprovante comprovanteBackup = null;
        if (atividade.getComprovante() != null) {
            idComprovante = atividade.getComprovante().getIdComprovante();
            comprovanteBackup = atividade.getComprovante();
        }

        // Desvincula o comprovante (apenas na tabela atividade)
        atividadeRepository.desvincularComprovante(id);

        try {
            atividadeRepository.deleteById(id);
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            throw new RuntimeException(
                    "Não é possível remover esta atividade pois ela já possui vínculos com o histórico financeiro (Pagamentos de Jeton).");
        }

        // Remove o comprovante físico e o registro se não for mais usado
        if (idComprovante != null && comprovanteBackup != null) {
            try {
                long outrasAtividades = atividadeRepository.countByComprovanteIdComprovante(idComprovante);
                if (outrasAtividades == 0) {
                    comprovanteRepository.deleteById(idComprovante);
                    fileStorageService.deleteFile(comprovanteBackup.getNomeArquivo(),
                            comprovanteBackup.getAno(),
                            comprovanteBackup.getMes());
                }
            } catch (Exception e) {
                // Falha silenciosa na limpeza do comprovante (não impede a exclusão da
                // atividade)
            }
        }
    }

    @Transactional
    public void desvincularComprovante(Integer idAtividade) {
        atividadeRepository.desvincularComprovante(idAtividade);
    }

    // =========================================================================
    // OPERAÇÕES DE LEITURA (CONSULTAS)
    // =========================================================================

    @Transactional(readOnly = true)
    public Page<AtividadeConselhal> listarComPaginacaoEPesquisa(String termo, String situacao, String turno,
            String comprovanteFiltro, LocalDate dataInicio, LocalDate dataFim,
            int page, int size, String sortField, String sortDir) {
        Sort.Direction direction = sortDir.equalsIgnoreCase("desc") ? Sort.Direction.DESC : Sort.Direction.ASC;
        Sort sort = Sort.by(direction, sortField);
        Pageable pageable = (size == 0) ? PageRequest.of(0, Integer.MAX_VALUE, sort) : PageRequest.of(page, size, sort);
        return atividadeRepository.pesquisarPaginado(termo, situacao, turno, comprovanteFiltro,
                dataInicio, dataFim, pageable);
    }

    @Transactional(readOnly = true)
    public Optional<AtividadeConselhal> buscarPorId(Integer id) {
        return atividadeRepository.findById(id);
    }

    // =========================================================================
    // MÉTODOS PRIVADOS AUXILIARES
    // =========================================================================

    private void validarAtividadeNaoFechada(Integer idAtividade) {
        if (idAtividade == null)
            return;
        AtividadeConselhal existente = atividadeRepository.findById(idAtividade)
                .orElseThrow(() -> new RuntimeException("Atividade não encontrada no sistema."));
        if (AtividadeConselhal.SITUACAO_FECHADA.equals(existente.getInSituacao())) {
            throw new RuntimeException(
                    "Operação negada: Esta atividade já foi processada e FECHADA na folha de pagamento e não pode ser modificada.");
        }
    }

    private Gestao validarGestaoEvinculo(AtividadeConselhal atividade) {
        Gestao gestao = gestaoRepository.findById(atividade.getGestao().getIdGestao())
                .orElseThrow(() -> new RuntimeException("A gestão informada não foi encontrada no sistema."));

        boolean vinculado = gestaoConselheiroRepository.findByIdIdGestao(gestao.getIdGestao()).stream()
                .anyMatch(v -> v.getConselheiro().getIdPessoa().equals(atividade.getConselheiro().getIdPessoa()));
        if (!vinculado) {
            throw new RuntimeException("O médico selecionado não possui vínculo ativo com a Gestão informada.");
        }
        return gestao;
    }

    private void validarDataDentroDoMandato(LocalDate dataAtividade, Gestao gestao) {
        if (dataAtividade.isBefore(gestao.getDtInicio()) || dataAtividade.isAfter(gestao.getDtFim())) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
            throw new RuntimeException("A data da atividade (" + dataAtividade.format(formatter) +
                    ") não é permitida. Ela deve estar dentro do período da Gestão selecionada (" +
                    gestao.getDtInicio().format(formatter) + " a " + gestao.getDtFim().format(formatter) + ").");
        }
    }

    private AtividadeConselhal buscarAtividadeOuLancarExcecao(Integer id) {
        return atividadeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Atividade não encontrada."));
    }
}