package br.com.cremepe.jeton.repositorio;

import br.com.cremepe.jeton.dominio.TipoAnexo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TipoAnexoRepository extends JpaRepository<TipoAnexo, Integer> {

    // Encontra um tipo de anexo pelo seu nome exato (que é único na base de dados)
    Optional<TipoAnexo> findByNome(String nome);

    // Lista tipos de anexos filtrando se exigem publicação (ex: 'S' ou 'N')
    List<TipoAnexo> findByExigePublicacao(String exigePublicacao);
}