package br.com.cremepe.jeton.servico;

import br.com.cremepe.jeton.dominio.Comprovante;
import br.com.cremepe.jeton.dominio.TipoAnexo;
import br.com.cremepe.jeton.repositorio.ComprovanteRepository;
import br.com.cremepe.jeton.repositorio.TipoAnexoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Serviço responsável pela gestão de metadados de comprovativos e anexos.
 * Garante a integridade entre o registro da atividade e o arquivo físico.
 */
@Service
public class ComprovanteService {

    @Autowired
    private ComprovanteRepository comprovanteRepository;

    @Autowired
    private TipoAnexoRepository tipoAnexoRepository;

    @Transactional(readOnly = true)
    public List<Comprovante> listarTodos() {
        return comprovanteRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Optional<Comprovante> buscarPorId(Integer id) {
        return comprovanteRepository.findById(id);
    }

    /**
     * Lista comprovantes de um período específico para auditoria mensal.
     */
    @Transactional(readOnly = true)
    public List<Comprovante> listarPorPeriodo(Integer mes, Integer ano) {
        return comprovanteRepository.findByMesAndAno(mes, ano);
    }

    /**
     * Salva o registro do comprovante no banco de dados.
     * NOTA: A lógica de upload físico do arquivo (MultipartFile) 
     * será tratada no Controller (Passo 6).
     */
    @Transactional
    public Comprovante salvar(Comprovante comprovante) {
        return comprovanteRepository.save(comprovante);
    }

    /**
     * Remove o registro do comprovante. 
     * Em uma implementação completa, este método também dispararia 
     * a exclusão do arquivo físico no servidor.
     */
    @Transactional
    public void excluir(Integer id) {
        comprovanteRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public List<TipoAnexo> listarTiposDisponiveis() {
        return tipoAnexoRepository.findAll();
    }
}