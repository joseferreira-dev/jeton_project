package br.com.cremepe.jeton.dto;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * DTO (Data Transfer Object) utilizado para a extração de relatórios em formato
 * Pivot.
 * Agrupa as atividades de um Conselheiro dentro de uma Gestão, transformando as
 * Regras
 * em colunas dinâmicas (Regra -> Total de Pontos/Atividades).
 * NOTA: Esta classe não possui mapeamento físico (@Entity) no banco de dados.
 */
public class RelatorioAtividadeConselhalAgrupadoDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String gestao;
    private String conselheiro;

    // Utilização de LinkedHashMap para garantir que a ordem das colunas dinâmicas
    // inseridas pela query ou stored procedure seja mantida na exibição (UI).
    private Map<String, Integer> regras = new LinkedHashMap<>();

    public RelatorioAtividadeConselhalAgrupadoDTO() {
    }

    public RelatorioAtividadeConselhalAgrupadoDTO(String gestao, String conselheiro) {
        this.gestao = gestao;
        this.conselheiro = conselheiro;
    }

    // ==========================================
    // GETTERS E SETTERS
    // ==========================================

    public String getGestao() {
        return gestao;
    }

    public void setGestao(String gestao) {
        this.gestao = gestao;
    }

    public String getConselheiro() {
        return conselheiro;
    }

    public void setConselheiro(String conselheiro) {
        this.conselheiro = conselheiro;
    }

    public Map<String, Integer> getRegras() {
        return regras;
    }

    public void setRegras(Map<String, Integer> regras) {
        this.regras = regras;
    }

    // Método utilitário recomendado para adicionar regras dinâmicas com facilidade
    public void adicionarRegra(String nomeRegra, Integer valor) {
        this.regras.put(nomeRegra, valor);
    }
}