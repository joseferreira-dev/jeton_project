package br.com.cremepe.jeton.repositorio;

import br.com.cremepe.jeton.dominio.Portaria;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PortariaRepository extends JpaRepository<Portaria, Integer> {

    // Encontra uma portaria combinando número e ano
    Optional<Portaria> findByNumeroAndAno(Integer numero, Integer ano);

    // Lista portarias ativas (inRevogado = 'N')
    List<Portaria> findByInRevogado(String inRevogado);
}