package br.com.cremepe.jeton.repositorio;

import br.com.cremepe.jeton.dominio.ViewAtividadeConselhal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ViewAtividadeConselhalRepository extends JpaRepository<ViewAtividadeConselhal, Integer> {

    // Retorna todos os dados consolidados (Atividade, Gestão, Regra, etc.) de um conselheiro
    List<ViewAtividadeConselhal> findByIdPessoaOrderByDataHoraAtividadeDesc(Integer idPessoa);
    
    // Excelente para relatórios de uma gestão inteira
    List<ViewAtividadeConselhal> findByIdGestao(Integer idGestao);
}