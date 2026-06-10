package br.com.cremepe.jeton.dto;

import java.math.BigDecimal;
import java.util.List;

public class RelatorioGeralDTO {
    private Integer idGestao;
    private String nomeGestao;
    private Integer mes;
    private Integer ano;
    private List<ConselheiroRelatorioDTO> conselheiros;
    private Integer totalGeralJetons;
    private BigDecimal totalGeralValor;

    public Integer getIdGestao() {
        return idGestao;
    }

    public void setIdGestao(Integer idGestao) {
        this.idGestao = idGestao;
    }

    public String getNomeGestao() {
        return nomeGestao;
    }

    public void setNomeGestao(String nomeGestao) {
        this.nomeGestao = nomeGestao;
    }

    public Integer getMes() {
        return mes;
    }

    public void setMes(Integer mes) {
        this.mes = mes;
    }

    public Integer getAno() {
        return ano;
    }

    public void setAno(Integer ano) {
        this.ano = ano;
    }

    public List<ConselheiroRelatorioDTO> getConselheiros() {
        return conselheiros;
    }

    public void setConselheiros(List<ConselheiroRelatorioDTO> conselheiros) {
        this.conselheiros = conselheiros;
    }

    public Integer getTotalGeralJetons() {
        return totalGeralJetons;
    }

    public void setTotalGeralJetons(Integer totalGeralJetons) {
        this.totalGeralJetons = totalGeralJetons;
    }

    public BigDecimal getTotalGeralValor() {
        return totalGeralValor;
    }

    public void setTotalGeralValor(BigDecimal totalGeralValor) {
        this.totalGeralValor = totalGeralValor;
    }
}