package br.com.cremepe.jeton.configuracao;

import br.com.cremepe.jeton.dominio.Resolucao;
import br.com.cremepe.jeton.dominio.ViewUserLogin;
import br.com.cremepe.jeton.servico.ConfiguracaoService;
import br.com.cremepe.jeton.servico.UsuarioService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * TesteConexao.java - Versão de Validação de Serviços (Passo 5)
 */
@Component
public class TesteConexao implements CommandLineRunner {

    @Autowired
    private UsuarioService usuarioService;

    @Autowired
    private ConfiguracaoService configuracaoService;

    @Override
    public void run(String... args) throws Exception {
        System.out.println("\n" + "=".repeat(50));
        System.out.println("🧪 INICIANDO TESTE DE INTEGRAÇÃO DE SERVIÇOS");
        System.out.println("=".repeat(50));

        // --- TESTE 1: USUÁRIO E AUTENTICAÇÃO ---
        System.out.println("\n[Teste 1] Validando busca de usuário por CPF...");
        // Tente usar um CPF que você sabe que existe no seu banco legado
        String cpfTeste = "734.210.574-00"; 
        
        Optional<ViewUserLogin> user = usuarioService.autenticar(cpfTeste, "senha_do_banco");
        
        if (user.isPresent()) {
            System.out.println("✅ Sucesso! Usuário encontrado: " + user.get().getNome());
            System.out.println("   Permissões mapeadas: " + user.get().getPermissoes());
        } else {
            System.out.println("⚠️ Aviso: Usuário não encontrado ou senha incorreta (verifique o CPF no banco).");
        }

        // --- TESTE 2: REGRAS E CONFIGURAÇÕES ---
        System.out.println("\n[Teste 2] Listando Resoluções Ativas via Service...");
        try {
            List<Resolucao> resolucoes = configuracaoService.listarResolucoesAtivas();
            System.out.println("✅ Sucesso! Total de resoluções ativas: " + resolucoes.size());
            resolucoes.stream().limit(3).forEach(r -> 
                System.out.println("   -> Resolução " + r.getNumero() + "/" + r.getAno())
            );
        } catch (Exception e) {
            System.err.println("❌ Erro ao listar resoluções: " + e.getMessage());
        }

        // --- TESTE 3: STATUS DO SISTEMA ---
        System.out.println("\n[Teste 3] Verificando Parâmetros Globais...");
        boolean bloqueado = configuracaoService.isSistemaBloqueado();
        System.out.println("   O sistema está bloqueado para lançamentos? " + (bloqueado ? "SIM 🔒" : "NÃO 🔓"));

        System.out.println("\n" + "=".repeat(50));
        System.out.println("🏁 TESTES CONCLUÍDOS COM SUCESSO!");
        System.out.println("=".repeat(50) + "\n");
    }
}