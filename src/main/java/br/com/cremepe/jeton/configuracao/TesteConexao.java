package br.com.cremepe.jeton.configuracao;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * ARQUIVO: TesteConexao.java
 * OBJETIVO: Validar a recuperação de dados reais do banco legado antes do Passo 5.
 */
@Component
public class TesteConexao implements CommandLineRunner {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) throws Exception {
        System.out.println("\n=== TESTE DE RECUPERAÇÃO DE DADOS (JDBC) ===");
        
        try {
            // Teste simples: Contar quantos registros existem na tabela pessoa
            Integer totalPessoas = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM pessoa", Integer.class);
            System.out.println("-> Conexão ativa! Total de pessoas no banco: " + totalPessoas);

            // Teste de busca: Listar os primeiros 5 registros para conferir a codificação (UTF-8)
            String sql = "SELECT nome, cpf, email FROM pessoa LIMIT 5";
            List<Map<String, Object>> resultados = jdbcTemplate.queryForList(sql);

            System.out.println("-> Amostra de dados encontrados:");
            System.out.println("-------------------------------------------------------------------------");
            for (Map<String, Object> linha : resultados) {
                System.out.printf("NOME: %-30s | CPF: %-11s | EMAIL: %s%n", 
                    linha.get("nome"), 
                    linha.get("cpf"), 
                    linha.get("email"));
            }
            System.out.println("-------------------------------------------------------------------------");
            System.out.println("✅ SUCESSO: O Spring Boot consegue ler o banco legado perfeitamente.");
            
        } catch (Exception e) {
            System.err.println("❌ ERRO ao buscar dados: " + e.getMessage());
            System.err.println("DICA: Verifique se o MySQL está rodando e se as credenciais no application.properties estão corretas.");
        }
        System.out.println("==========================================\n");
    }
}