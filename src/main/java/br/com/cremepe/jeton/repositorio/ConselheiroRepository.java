package br.com.cremepe.jeton.repositorio;

import br.com.cremepe.jeton.dominio.Conselheiro;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ConselheiroRepository extends JpaRepository<Conselheiro, Integer> {

    // Busca um conselheiro pelo seu CRM
    Optional<Conselheiro> findByCrm(Integer crm);

    // Lista conselheiros filtrando pela situação (ex: 'A' para Ativo)
    List<Conselheiro> findByInSituacao(String inSituacao);

    // Um exemplo de navegação profunda: Encontrar um conselheiro pelo nome da Pessoa (usando LIKE dinâmico)
    // O Spring gera: SELECT c.* FROM conselheiro c JOIN pessoa p ON c.idPessoa = p.idPessoa WHERE p.nome LIKE %?%
    List<Conselheiro> findByPessoaNomeContainingIgnoreCase(String nome);
}