package br.com.cremepe.jeton.repositorio;

import br.com.cremepe.jeton.dominio.Jeton;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface JetonRepository extends JpaRepository<Jeton, Integer> {

    // Encontra o pagamento específico de um Conselheiro num mês/ano para validação
    Optional<Jeton> findByConselheiroIdPessoaAndMesAndAno(Integer idPessoa, Integer mes, Integer ano);

    // Lista o histórico financeiro de um Conselheiro num determinado ano, ordenado do mais recente para o mais antigo
    List<Jeton> findByConselheiroIdPessoaAndAnoOrderByMesDesc(Integer idPessoa, Integer ano);
    
    // Lista todos os Jetons processados numa determinada gestão
    List<Jeton> findByGestaoIdGestao(Integer idGestao);
}