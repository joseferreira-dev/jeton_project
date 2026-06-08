package br.com.cremepe.jeton.repository;

import br.com.cremepe.jeton.dominio.NivelAcesso;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface NivelAcessoRepository extends JpaRepository<NivelAcesso, String> {

    Optional<NivelAcesso> findByNomeNivel(String nomeNivel);
}