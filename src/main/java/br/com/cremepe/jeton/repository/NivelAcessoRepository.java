package br.com.cremepe.jeton.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import br.com.cremepe.jeton.domain.NivelAcesso;

import java.util.Optional;

public interface NivelAcessoRepository extends JpaRepository<NivelAcesso, String> {

    Optional<NivelAcesso> findByNomeNivel(String nomeNivel);
}