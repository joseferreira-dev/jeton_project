// package br.com.cremepe.jeton.configuracao;

// import br.com.cremepe.jeton.servico.*;
// import org.springframework.beans.factory.annotation.Autowired;
// import org.springframework.boot.CommandLineRunner;
// import org.springframework.stereotype.Component;

// @Component
// public class TesteConexao implements CommandLineRunner {

//     // Injetando os principais serviços para testar o mapeamento de várias tabelas
//     @Autowired private UsuarioService usuarioService;
//     @Autowired private GestaoService gestaoService;
//     @Autowired private ConselheiroService conselheiroService;
//     @Autowired private AtividadeConselhalService atividadeService;
//     @Autowired private JetonService jetonService;
//     @Autowired private ParametrosService parametrosService;

//     @Override
//     public void run(String... args) throws Exception {
//         System.out.println("\n=========================================================");
//         System.out.println(" INICIANDO TESTE EXTENSO DE CONEXÃO E SERVIÇOS (BACKEND) ");
//         System.out.println("=========================================================");

//         try {
//             // // Teste 1: Tabela de Usuários (vw_user_login / usuario / pessoa)
//             // int qtdUsuarios = usuarioService.listarTodos().size();
//             // System.out.println("✅ [OK] Usuários mapeados com sucesso. Total de registros: " + qtdUsuarios);

//             // // Teste 2: Tabela de Gestões
//             // int qtdGestoes = gestaoService.listarTodos().size();
//             // System.out.println("✅ [OK] Gestões mapeadas com sucesso. Total de registros: " + qtdGestoes);

//             // // Teste 3: Tabela de Conselheiros
//             // int qtdConselheiros = conselheiroService.listarTodos().size();
//             // System.out.println("✅ [OK] Conselheiros mapeados com sucesso. Total de registros: " + qtdConselheiros);

//             // // Teste 4: Tabela de Atividades Conselhais
//             // int qtdAtividades = atividadeService.listarTodos().size();
//             // System.out.println("✅ [OK] Atividades Conselhais mapeadas com sucesso. Total: " + qtdAtividades);

//             // // Teste 5: Tabela de Jeton
//             // int qtdJetons = jetonService.listarTodos().size();
//             // System.out.println("✅ [OK] Histórico de Jetons mapeado com sucesso. Total: " + qtdJetons);

//             // // Teste 6: Tabela de Parâmetros
//             // int qtdParametros = parametrosService.listarTodos().size();
//             // System.out.println("✅ [OK] Parâmetros mapeados com sucesso. Total de registros: " + qtdParametros);

//             System.out.println("=========================================================");
//             System.out.println("TODOS OS TESTES PASSARAM! O BACKEND ESTÁ PERFEITO!");
//             System.out.println("=========================================================\n");

//         } catch (Exception e) {
//             System.err.println("\n=========================================================");
//             System.err.println("ERRO DURANTE OS TESTES DE CONEXÃO COM O BANCO: ");
//             System.err.println(" Detalhe do erro: " + e.getMessage());
//             e.printStackTrace();
//             System.err.println("=========================================================\n");
//         }
//     }
// }