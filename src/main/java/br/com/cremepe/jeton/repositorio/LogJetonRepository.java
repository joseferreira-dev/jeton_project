package br.com.cremepe.jeton.repositorio;

import br.com.cremepe.jeton.dominio.LogJeton;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface LogJetonRepository extends JpaRepository<LogJeton, Integer> {

    // Extrai a trilha de auditoria de um utilizador específico, do mais recente
    // para o mais antigo
    List<LogJeton> findByUsuarioIdUsuarioPessoaOrderByDataHoraLogDesc(Integer idUsuario);

    // Permite varrer a base de dados em busca de alterações feitas numa tabela
    // específica
    // dentro de um determinado intervalo de datas
    List<LogJeton> findByNomeTabelaAndDataHoraLogBetween(String nomeTabela, LocalDateTime inicio, LocalDateTime fim);
}