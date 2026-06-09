package br.com.cremepe.jeton.dto;

import br.com.cremepe.jeton.domain.Conselheiro;
import br.com.cremepe.jeton.domain.Gestao;
import br.com.cremepe.jeton.domain.Jeton;

import java.math.BigDecimal;

public class JetonAgrupadoDTO {
    private final Conselheiro conselheiro;
    private final Gestao gestao;
    private final Integer mes;
    private final Integer ano;
    private Integer totalJeton = 0;
    private BigDecimal valor = BigDecimal.ZERO;
    private String inSituacao;

    public JetonAgrupadoDTO(Conselheiro conselheiro, Gestao gestao, Integer mes, Integer ano) {
        this.conselheiro = conselheiro;
        this.gestao = gestao;
        this.mes = mes;
        this.ano = ano;
    }

    public void adicionarJeton(Jeton jeton) {
        this.totalJeton += jeton.getTotalJeton();
        this.valor = this.valor.add(jeton.getValor());
        this.inSituacao = jeton.getInSituacao();
    }

    // Getters
    public Conselheiro getConselheiro() {
        return conselheiro;
    }

    public Gestao getGestao() {
        return gestao;
    }

    public Integer getMes() {
        return mes;
    }

    public Integer getAno() {
        return ano;
    }

    public Integer getTotalJeton() {
        return totalJeton;
    }

    public BigDecimal getValor() {
        return valor;
    }

    public String getInSituacao() {
        return inSituacao;
    }
}