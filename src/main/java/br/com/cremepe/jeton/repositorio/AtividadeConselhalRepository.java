package br.com.cremepe.jeton.repositorio;

import br.com.cremepe.jeton.dominio.AtividadeConselhal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AtividadeConselhalRepository extends JpaRepository<AtividadeConselhal, Integer> {

    // Procura todas as atividades de um conselheiro específico, ordenadas da mais recente para a mais antiga
    List<AtividadeConselhal> findByConselheiroIdPessoaOrderByDataHoraAtividadeDesc(Integer idConselheiro);

    // Permite filtrar atividades dentro de um mês ou turno específico utilizando os novos recursos de datas do Java 8+
    List<AtividadeConselhal> findByDataHoraAtividadeBetween(LocalDateTime inicio, LocalDateTime fim);
}