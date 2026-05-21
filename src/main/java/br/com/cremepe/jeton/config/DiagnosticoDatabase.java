// package br.com.cremepe.jeton.config;

// import org.springframework.beans.factory.annotation.Autowired;
// import org.springframework.boot.CommandLineRunner;
// import org.springframework.stereotype.Component;

// import javax.sql.DataSource;
// import java.sql.*;

// /**
// * Componente de diagnóstico estrutural e auditoria para o banco de dados
// Jeton.
// * Executa automaticamente na inicialização da aplicação para exibir tabelas,
// * views,
// * metadados, dados amostrais (top 5) e código completo dos gatilhos
// (triggers).
// */
// @Component
// public class DiagnosticoDatabase implements CommandLineRunner {

// @Autowired
// private DataSource dataSource;

// @Override
// public void run(String... args) throws Exception {
// System.out.println("\n=========================================================================");
// System.out.println(" INICIANDO AUDITORIA E DIAGNÓSTICO PROFUNDO DO BANCO DE
// DADOS JETON");
// System.out.println("=========================================================================\n");

// try (Connection conn = dataSource.getConnection()) {
// String catalog = conn.getCatalog();
// System.out.println("[INFO] Conexão estabelecida com sucesso!");
// System.out.println("[INFO] Catálogo/Banco de Dados Ativo: " + catalog);
// System.out.println("[INFO] SGBD: " +
// conn.getMetaData().getDatabaseProductVersion());
// System.out.println("-------------------------------------------------------------------------\n");

// // 1. MAPEAMENTO DE TABELAS FÍSICAS E AMOSTRAS
// mapearTabelasERestricoes(conn, catalog);

// // 2. MAPEAMENTO COMPLETO DE VIEWS E SUAS DEFINIÇÕES SQL
// mapearViewsDoSistema(conn, catalog);

// // 3. EXTRAÇÃO E CÓDIGO FONTE COMPLETO DOS GATILHOS (TRIGGERS)
// mapearGatilhosTriggers(conn, catalog);

// } catch (SQLException e) {
// System.err.println("[ERRO CRÍTICO] Falha ao executar diagnóstico no banco de
// dados: " + e.getMessage());
// e.printStackTrace();
// }

// System.out.println("\n=========================================================================");
// System.out.println(" FIM DO DIAGNÓSTICO E MAPEAMENTO DO BANCO DE DADOS");
// System.out.println("=========================================================================\n");
// }

// private void mapearTabelasERestricoes(Connection conn, String catalog) throws
// SQLException {
// DatabaseMetaData metaData = conn.getMetaData();

// try (ResultSet rsTabelas = metaData.getTables(catalog, null, "%", new
// String[] { "TABLE" })) {
// while (rsTabelas.next()) {
// String nomeTabela = rsTabelas.getString("TABLE_NAME");
// System.out.println("#########################################################################");
// System.out.println(" TABELA FÍSICA: " + nomeTabela.toUpperCase());
// System.out.println("#########################################################################");

// // Estrutura de Colunas
// System.out.println("\n[ESTRUTURA DE COLUNAS]");
// System.out.printf("%-25s | %-15s | %-8s | %-10s | %s\n", "CAMPO", "TIPO",
// "NULO", "PADRÃO",
// "PROPRIEDADES EXTRAS");
// System.out.println(
// "----------------------------------------------------------------------------------------------------------------");

// try (ResultSet rsColunas = metaData.getColumns(catalog, null, nomeTabela,
// "%")) {
// while (rsColunas.next()) {
// String coluna = rsColunas.getString("COLUMN_NAME");
// String tipo = rsColunas.getString("TYPE_NAME");
// int tamanho = rsColunas.getInt("COLUMN_SIZE");
// String nulo = rsColunas.getString("IS_NULLABLE");
// String padrao = rsColunas.getString("COLUMN_DEF");
// String autoIncrement = rsColunas.getString("IS_AUTOINCREMENT");

// String tipoFormatado = tipo + (tamanho > 0 && !tipo.contains("INT") &&
// !tipo.contains("TEXT")
// && !tipo.contains("DATE") && !tipo.contains("TIME") ? "(" + tamanho + ")" :
// "");
// String extras = "YES".equalsIgnoreCase(autoIncrement) ? "AUTO_INCREMENT" :
// "";

// System.out.printf("%-25s | %-15s | %-8s | %-10s | %s\n", coluna,
// tipoFormatado, nulo,
// (padrao != null ? padrao : "NULL"), extras);
// }
// }

// // Chaves Primárias
// System.out.println("\n[CHAVE PRIMÁRIA]");
// try (ResultSet rsPk = metaData.getPrimaryKeys(catalog, null, nomeTabela)) {
// boolean temPk = false;
// while (rsPk.next()) {
// temPk = true;
// System.out.println(" -> Campo Primário: " + rsPk.getString("COLUMN_NAME") + "
// [Constraint: "
// + rsPk.getString("PK_NAME") + "]");
// }
// if (!temPk)
// System.out.println(" -> Nenhuma Chave Primária detectada.");
// }

// // Chaves Estrangeiras
// System.out.println("\n[CHAVES ESTRANGEIRAS / RELACIONAMENTOS]");
// try (ResultSet rsFk = metaData.getImportedKeys(catalog, null, nomeTabela)) {
// boolean temFk = false;
// while (rsFk.next()) {
// temFk = true;
// System.out.println(" -> CONSTRAINT: " + rsFk.getString("FK_NAME") + " |
// Local: "
// + rsFk.getString("FKCOLUMN_NAME") + " ---> Aponta para: "
// + rsFk.getString("PKTABLE_NAME") + "(" + rsFk.getString("PKCOLUMN_NAME") +
// ")");
// }
// if (!temFk)
// System.out.println(" -> Nenhuma Foreign Key importada.");
// }

// // Amostra de Dados
// System.out.println("\n[AMOSTRA DE DADOS (PRIMEIRAS 5 LINHAS)]");
// exibirDadosAmostra(conn, nomeTabela);
// System.out.println("\n\n");
// }
// }
// }

// private void mapearViewsDoSistema(Connection conn, String catalog) throws
// SQLException {
// DatabaseMetaData metaData = conn.getMetaData();

// System.out.println("#########################################################################");
// System.out.println(" MAPEAMENTO DE VISÕES LOGICAS (VIEWS) DO BANCO DE
// DADOS");
// System.out.println("#########################################################################\n");

// try (ResultSet rsViews = metaData.getTables(catalog, null, "%", new String[]
// { "VIEW" })) {
// while (rsViews.next()) {
// String nomeView = rsViews.getString("TABLE_NAME");
// System.out.println("=========================================================================");
// System.out.println(" VIEW: " + nomeView.toUpperCase());
// System.out.println("=========================================================================");

// // Estrutura resultante da View
// System.out.println("[CAMPOS RESULTANTES DA VISÃO]");
// System.out.printf("%-25s | %-15s | %-8s\n", "CAMPO", "TIPO DE DADO", "ACEITA
// NULO");
// System.out.println("-------------------------------------------------------------------------");
// try (ResultSet rsColunas = metaData.getColumns(catalog, null, nomeView, "%"))
// {
// while (rsColunas.next()) {
// System.out.printf("%-25s | %-15s | %-8s\n",
// rsColunas.getString("COLUMN_NAME"),
// rsColunas.getString("TYPE_NAME"),
// rsColunas.getString("IS_NULLABLE"));
// }
// }

// // Recuperação da query de definição SQL original da View (via
// // information_schema)
// System.out.println("\n[QUERY DE DEFINIÇÃO SQL ORIGINAL (VIEW_DEFINITION)]");
// System.out.println("-------------------------------------------------------------------------");
// String sqlDef = "SELECT VIEW_DEFINITION FROM INFORMATION_SCHEMA.VIEWS WHERE
// TABLE_SCHEMA = ? AND TABLE_NAME = ?";
// try (PreparedStatement psDef = conn.prepareStatement(sqlDef)) {
// psDef.setString(1, catalog);
// psDef.setString(2, nomeView);
// try (ResultSet rsDef = psDef.executeQuery()) {
// if (rsDef.next()) {
// System.out.println(rsDef.getString("VIEW_DEFINITION"));
// } else {
// System.out.println(" -> Não foi possível extrair a query DDL da visão.");
// }
// }
// }

// // Amostra de Dados da View
// System.out.println("\n[AMOSTRA DE DADOS DA VISÃO (PRIMEIRAS 5 LINHAS)]");
// exibirDadosAmostra(conn, nomeView);
// System.out.println("=========================================================================\n\n");
// }
// }
// }

// private void exibirDadosAmostra(Connection conn, String nomeAlvo) {
// String sql = "SELECT * FROM " + nomeAlvo + " LIMIT 5";
// try (Statement stmt = conn.createStatement();
// ResultSet rs = stmt.executeQuery(sql)) {

// ResultSetMetaData rsmd = rs.getMetaData();
// int totalColunas = rsmd.getColumnCount();

// for (int i = 1; i <= totalColunas; i++) {
// System.out.print(rsmd.getColumnName(i) + "\t|");
// }
// System.out.println(
// "\n----------------------------------------------------------------------------------------------------------------");

// int linhasCount = 0;
// while (rs.next()) {
// linhasCount++;
// for (int i = 1; i <= totalColunas; i++) {
// Object valor = rs.getObject(i);
// System.out.print((valor != null ? valor.toString() : "NULL") + "\t|");
// }
// System.out.println();
// }
// if (linhasCount == 0)
// System.out.println(" [Nenhum registro encontrado nesta estrutura]");
// } catch (SQLException e) {
// System.out.println(" [AVISO] Não foi possível extrair amostra de dados: " +
// e.getMessage());
// }
// }

// private void mapearGatilhosTriggers(Connection conn, String catalog) throws
// SQLException {
// System.out.println("#########################################################################");
// System.out.println(" GATILHOS (TRIGGERS) ATIVOS DE VALIDAÇÃO FINANCEIRA");
// System.out.println("#########################################################################\n");

// String sql = "SELECT TRIGGER_NAME, EVENT_MANIPULATION, EVENT_OBJECT_TABLE,
// ACTION_TIMING, ACTION_STATEMENT " +
// "FROM INFORMATION_SCHEMA.TRIGGERS WHERE TRIGGER_SCHEMA = ? ORDER BY
// EVENT_OBJECT_TABLE ASC";

// try (PreparedStatement stmt = conn.prepareStatement(sql)) {
// stmt.setString(1, catalog);
// try (ResultSet rs = stmt.executeQuery()) {
// boolean existemTriggers = false;
// while (rs.next()) {
// existemTriggers = true;
// System.out.println("=========================================================================");
// System.out.println(" TRIGGER: " +
// rs.getString("TRIGGER_NAME").toUpperCase());
// System.out.println("=========================================================================");
// System.out.println(" Tabela Vinculada : " +
// rs.getString("EVENT_OBJECT_TABLE"));
// System.out.println(" Momento Disparo : " + rs.getString("ACTION_TIMING") + "
// "
// + rs.getString("EVENT_MANIPULATION"));
// System.out.println("-------------------------------------------------------------------------");
// System.out.println(" CÓDIGO FONTE:");
// System.out.println("-------------------------------------------------------------------------");
// System.out.println(rs.getString("ACTION_STATEMENT"));
// System.out.println("=========================================================================\n");
// }
// if (!existemTriggers)
// System.out.println(" [INFO] Nenhuma trigger localizada no esquema.");
// }
// }
// }
// }