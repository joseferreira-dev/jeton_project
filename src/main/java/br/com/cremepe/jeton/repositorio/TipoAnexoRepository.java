package br.com.cremepe.jeton.repositorio;

import br.com.cremepe.jeton.dominio.TipoAnexo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TipoAnexoRepository extends JpaRepository<TipoAnexo, Integer> {

    Optional<TipoAnexo> findByNome(String nome);

    List<TipoAnexo> findByExigePublicacao(String exigePublicacao);
}