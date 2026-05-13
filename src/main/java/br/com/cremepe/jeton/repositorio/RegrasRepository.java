package br.com.cremepe.jeton.repositorio;

import br.com.cremepe.jeton.dominio.Regras;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RegrasRepository extends JpaRepository<Regras, Integer> {

    // Procura todas as regras não revogadas que pertencem a uma Resolução específica
    List<Regras> findByResolucaoIdResolucaoAndInRevogado(Integer idResolucao, String inRevogado);

    // Procura regras pelo identificador se são atividades judicantes ou não
    List<Regras> findByInJudicante(String inJudicante);
}