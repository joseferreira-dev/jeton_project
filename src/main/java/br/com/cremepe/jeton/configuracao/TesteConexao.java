package br.com.cremepe.jeton.configuracao; // Lembre-se de manter o seu pacote correto!

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * ==================================================================
 * ARQUIVO: TesteConexao.java
 * OBJETIVO: Testar a conexão e listar as tabelas do banco de dados.
 * ==================================================================
 */
@Component
public class TesteConexao implements CommandLineRunner {

    // O JdbcTemplate é a ferramenta do Spring para rodar SQL puro de forma fácil
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) throws Exception {
        System.out.println("A iniciar o teste de leitura do banco de dados...");
        
        try {
            // Executamos um comando SQL direto para listar todas as tabelas do seu MySQL
            List<String> tabelas = jdbcTemplate.queryForList("SHOW TABLES", String.class);
            
            System.out.println("=================================================");
            System.out.println("✅ SUCESSO! O Spring Boot conseguiu ler o banco!");
            System.out.println("Foram encontradas " + tabelas.size() + " tabelas:");
            
            // Imprime o nome de cada tabela encontrada no console
            for (String nomeDaTabela : tabelas) {
                System.out.println(" -> " + nomeDaTabela);
            }
            System.out.println("=================================================");
            
        } catch (Exception e) {
            System.err.println("=================================================");
            System.err.println("❌ ERRO ao tentar ler o banco de dados.");
            System.err.println("Motivo: " + e.getMessage());
            System.err.println("=================================================");
        }
    }
}