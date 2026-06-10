package br.com.cremepe.jeton.dto;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;

public class AtividadeAgrupadaRelatorioDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String gestao;
    private String conselheiro;
    private Map<String, Integer> regras = new LinkedHashMap<>();
    private int totalPontos;

    public AtividadeAgrupadaRelatorioDTO() {
    }

    public AtividadeAgrupadaRelatorioDTO(String gestao, String conselheiro) {
        this.gestao = gestao;
        this.conselheiro = conselheiro;
    }

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

    public void adicionarRegra(String nomeRegra, Integer valor) {
        this.regras.put(nomeRegra, valor);
    }

    public void setTotalPontos(int totalPontos) {
        this.totalPontos = totalPontos;
    }

    public int getTotalPontos() {
        return totalPontos;
    }
}