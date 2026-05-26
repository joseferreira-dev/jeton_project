package br.com.cremepe.jeton.servico;

import br.com.cremepe.jeton.dominio.Comprovante;
import br.com.cremepe.jeton.dominio.TipoAnexo;
import br.com.cremepe.jeton.repositorio.ComprovanteRepository;
import br.com.cremepe.jeton.repositorio.TipoAnexoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.Optional;

@Service
public class ComprovanteService {

    @Autowired
    private ComprovanteRepository comprovanteRepository;
    @Autowired
    private TipoAnexoRepository tipoAnexoRepository;
    @Autowired
    private FileStorageService fileStorageService;

    @Transactional
    public Comprovante guardarComprovante(MultipartFile file, Integer idTipoAnexo, String descricaoUsuario) {

        // Pega o mês e ano atuais para organizar as pastas
        LocalDate hoje = LocalDate.now();
        int ano = hoje.getYear();
        int mes = hoje.getMonthValue();

        // 1. Envia o ficheiro DIRETAMENTE para a Locaweb e recupera o nome único gerado
        String nomeArquivoGerado = fileStorageService.storeFileToFtp(file, ano, mes);

        // 2. Procura o Tipo de Anexo
        TipoAnexo tipo = tipoAnexoRepository.findById(idTipoAnexo)
                .orElseThrow(() -> new RuntimeException("Tipo de anexo inválido."));

        // 3. Monta a entidade Comprovante
        Comprovante comprovante = new Comprovante();
        comprovante.setTipoAnexo(tipo);
        comprovante.setNomeComprovante(descricaoUsuario);
        comprovante.setNomeArquivo(nomeArquivoGerado);
        comprovante.setContentType(file.getContentType());
        comprovante.setMes(mes);
        comprovante.setAno(ano);

        // 4. Salva os metadados na base de dados
        return comprovanteRepository.save(comprovante);
    }

    @Transactional(readOnly = true)
    public Optional<Comprovante> buscarPorId(Integer id) {
        return comprovanteRepository.findById(id);
    }

    @Transactional
    public void excluirComprovante(Integer id) {
        comprovanteRepository.findById(id).ifPresent(comp -> {
            fileStorageService.deleteFile(comp.getNomeArquivo(), comp.getAno(), comp.getMes());
            comprovanteRepository.delete(comp);
        });
    }

    @Transactional
    public Comprovante atualizar(Comprovante comprovante) {
        return comprovanteRepository.save(comprovante);
    }
}