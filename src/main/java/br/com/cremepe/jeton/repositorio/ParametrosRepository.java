package br.com.cremepe.jeton.repositorio;

import br.com.cremepe.jeton.dominio.Parametros;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ParametrosRepository extends JpaRepository<Parametros, Integer> {

    // Como é uma tabela de configuração de linha única (ID = 1), 
    // podemos criar um método direto para ler o estado de bloqueio do sistema
    @Query("SELECT p.bloqueaSistema FROM Parametros p WHERE p.id = 1")
    Optional<String> obterStatusBloqueioSistema();
}