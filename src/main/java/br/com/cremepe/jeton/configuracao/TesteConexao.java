package br.com.cremepe.jeton.configuracao;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * ==================================================================
 * ARQUIVO: TesteConexao.java
 * OBJETIVO: Mapear todas as tabelas e suas respectivas colunas para 
 * auxiliar na criação das Entidades JPA.
 * ==================================================================
 */
@Component
public class TesteConexao implements CommandLineRunner {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) throws Exception {
        System.out.println("Iniciando o mapeamento estrutural do banco de dados legado...");
        
        try {
            // Obtém a lista de todas as tabelas do banco
            List<String> tabelas = jdbcTemplate.queryForList("SHOW TABLES", String.class);
            
            System.out.println("=================================================");
            System.out.println("✅ BANCO DE DADOS LIDO COM SUCESSO!");
            System.out.println("Total de tabelas encontradas: " + tabelas.size());
            System.out.println("=================================================");
            
            for (String nomeDaTabela : tabelas) {
                System.out.println("\n-> TABELA: " + nomeDaTabela);
                System.out.println("-------------------------------------------------------------------------");
                System.out.printf("%-25s | %-15s | %-5s | %s%n", "CAMPO", "TIPO", "NULO", "CHAVE");
                System.out.println("-------------------------------------------------------------------------");
                
                // Executa DESCRIBE para cada tabela para pegar a estrutura interna
                List<Map<String, Object>> colunas = jdbcTemplate.queryForList("DESCRIBE " + nomeDaTabela);
                
                for (Map<String, Object> coluna : colunas) {
                    String field = String.valueOf(coluna.get("Field"));
                    String type = String.valueOf(coluna.get("Type"));
                    String isNull = String.valueOf(coluna.get("Null"));
                    String key = String.valueOf(coluna.get("Key"));
                    
                    System.out.printf("%-25s | %-15s | %-5s | %s%n", field, type, isNull, key);
                }
            }
            System.out.println("\n=================================================");
            System.out.println("FIM DO MAPEAMENTO");
            System.out.println("=================================================");
            
        } catch (Exception e) {
            System.err.println("=================================================");
            System.err.println("❌ ERRO ao tentar ler a estrutura do banco de dados.");
            System.err.println("Motivo: " + e.getMessage());
            e.printStackTrace();
            System.err.println("=================================================");
        }
    }
}