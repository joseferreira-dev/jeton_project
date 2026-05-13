package br.com.cremepe.jeton.repositorio;

import br.com.cremepe.jeton.dominio.Gestao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface GestaoRepository extends JpaRepository<Gestao, Integer> {

    // Método gerado automaticamente para procurar uma gestão pelo nome exato
    Optional<Gestao> findByNomeGestao(String nomeGestao);

    // Query JPQL que verifica qual gestão está ativa no dia de hoje
    @Query("SELECT g FROM Gestao g WHERE CURRENT_DATE BETWEEN g.dtInicio AND g.dtFim")
    Optional<Gestao> buscarGestaoVigente();
}