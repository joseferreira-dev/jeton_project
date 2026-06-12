package br.com.cremepe.jeton.dto;

import java.math.BigDecimal;

public class JetonAgrupadoDTO {

    private Integer idConselheiro;
    private String nomeConselheiro;
    private Integer idGestao;
    private String nomeGestao;
    private Integer mes;
    private Integer ano;
    private Integer totalJeton;
    private BigDecimal valor;
    private String situacao;

    // Construtor padrão
    public JetonAgrupadoDTO() {
        this.totalJeton = 0;
        this.valor = BigDecimal.ZERO;
    }

    // Construtor completo
    public JetonAgrupadoDTO(Integer idConselheiro, String nomeConselheiro,
            Integer idGestao, String nomeGestao,
            Integer mes, Integer ano,
            Integer totalJeton, BigDecimal valor, String situacao) {
        this.idConselheiro = idConselheiro;
        this.nomeConselheiro = nomeConselheiro;
        this.idGestao = idGestao;
        this.nomeGestao = nomeGestao;
        this.mes = mes;
        this.ano = ano;
        this.totalJeton = totalJeton != null ? totalJeton : 0;
        this.valor = valor != null ? valor : BigDecimal.ZERO;
        this.situacao = situacao;
    }

    // Método utilitário para adicionar dados de outro Jeton (usado no agrupamento)
    public void adicionarJeton(Integer quantidade, BigDecimal valorJeton, String novaSituacao) {
        this.totalJeton += quantidade;
        this.valor = this.valor.add(valorJeton);
        // Se qualquer um dos jetons agrupados for 'E', considera-se homologado
        if ("E".equals(novaSituacao)) {
            this.situacao = "E";
        } else if (this.situacao == null) {
            this.situacao = novaSituacao;
        }
    }

    // Getters e Setters
    public Integer getIdConselheiro() {
        return idConselheiro;
    }

    public void setIdConselheiro(Integer idConselheiro) {
        this.idConselheiro = idConselheiro;
    }

    public String getNomeConselheiro() {
        return nomeConselheiro;
    }

    public void setNomeConselheiro(String nomeConselheiro) {
        this.nomeConselheiro = nomeConselheiro;
    }

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

    public Integer getTotalJeton() {
        return totalJeton;
    }

    public void setTotalJeton(Integer totalJeton) {
        this.totalJeton = totalJeton;
    }

    public BigDecimal getValor() {
        return valor;
    }

    public void setValor(BigDecimal valor) {
        this.valor = valor;
    }

    public String getSituacao() {
        return situacao;
    }

    public void setSituacao(String situacao) {
        this.situacao = situacao;
    }
}