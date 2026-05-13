package br.com.cremepe.jeton.repositorio;

import br.com.cremepe.jeton.dominio.GestaoConselheiro;
import br.com.cremepe.jeton.dominio.GestaoConselheiroId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GestaoConselheiroRepository extends JpaRepository<GestaoConselheiro, GestaoConselheiroId> {

    // Lista todos os conselheiros vinculados a uma determinada gestão
    // O spring entende que 'id' é a chave embutida (GestaoConselheiroId) e 'idGestao' é um atributo dela
    List<GestaoConselheiro> findByIdIdGestao(Integer idGestao);

    // Opcionalmente, encontrar todas as gestões das quais um conselheiro fez parte
    List<GestaoConselheiro> findByIdIdPessoa(Integer idPessoa);
    
    // Lista vínculos filtrando por situação (ex: 'A' para Ativos na Gestão)
    List<GestaoConselheiro> findByIdIdGestaoAndInSituacao(Integer idGestao, String inSituacao);
}