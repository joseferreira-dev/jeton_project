package br.com.cremepe.jeton.repositorio;

import br.com.cremepe.jeton.dominio.Comprovante;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ComprovanteRepository extends JpaRepository<Comprovante, Integer> {

    // Busca um comprovante pelo nome físico do ficheiro
    Optional<Comprovante> findByNomeArquivo(String nomeArquivo);

    // Lista todos os comprovantes de um determinado mês e ano (ótimo para filtros
    // de relatório)
    List<Comprovante> findByMesAndAno(Integer mes, Integer ano);

    // Navegação de relacionamento: Busca comprovantes pelo ID do Tipo de Anexo
    List<Comprovante> findByTipoAnexoIdTipo(Integer idTipo);
}