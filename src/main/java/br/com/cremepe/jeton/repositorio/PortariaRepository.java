package br.com.cremepe.jeton.repositorio;

import br.com.cremepe.jeton.dominio.Portaria;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

@Repository
public interface PortariaRepository extends JpaRepository<Portaria, Integer> {

    // Encontra uma portaria combinando número e ano
    Optional<Portaria> findByNumeroAndAno(Integer numero, Integer ano);

    // Lista portarias ativas (inRevogado = 'N')
    List<Portaria> findByInRevogado(String inRevogado);

    @Query("SELECT p FROM Portaria p WHERE " +
           "(:termo IS NULL OR :termo = '' OR CAST(p.numero AS string) LIKE CONCAT('%', :termo, '%') OR CAST(p.ano AS string) LIKE CONCAT('%', :termo, '%')) AND " +
           "(:situacao IS NULL OR :situacao = '' OR p.inRevogado = :situacao)")
    Page<Portaria> pesquisarPaginado(@Param("termo") String termo, @Param("situacao") String situacao, Pageable pageable);
}