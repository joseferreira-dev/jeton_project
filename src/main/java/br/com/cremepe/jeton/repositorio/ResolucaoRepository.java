package br.com.cremepe.jeton.repositorio;

import br.com.cremepe.jeton.dominio.Resolucao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ResolucaoRepository extends JpaRepository<Resolucao, Integer> {

    // Retorna todas as resoluções ativas ("N" = Não revogado)
    List<Resolucao> findByInRevogado(String inRevogado);
    
    // Encontra uma resolução específica pelo seu número e ano
    Optional<Resolucao> findByNumeroAndAno(Integer numero, Integer ano);
}