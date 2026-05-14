package br.com.cremepe.jeton.repositorio;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import br.com.cremepe.jeton.dominio.Gestao;

@Repository
public interface GestaoRepository extends JpaRepository<Gestao, Integer> {

    Optional<Gestao> findByNomeGestao(String nomeGestao);

    @Query("SELECT g FROM Gestao g WHERE LOWER(g.nomeGestao) LIKE LOWER(CONCAT('%', :termo, '%'))")
    Page<Gestao> pesquisarPaginado(@Param("termo") String termo, Pageable pageable);
}