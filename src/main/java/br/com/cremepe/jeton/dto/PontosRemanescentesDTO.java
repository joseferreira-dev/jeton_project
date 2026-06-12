package br.com.cremepe.jeton.dto;

import java.math.BigDecimal;

public class PontosRemanescentesDTO {
    private Integer idPessoa;
    private String nome;
    private Long pontosRemanescentes;
    private BigDecimal somaJetons;
    private String pontosRemanescentesFormatado;
    private String somaJetonsFormatado;

    private Long pontosAtividadesValidadas;
    private Long saldoPontos;
    private String pontosAtividadesValidadasFormatado;
    private String saldoPontosFormatado;

    public PontosRemanescentesDTO(Integer idPessoa, String nome, Long pontosRemanescentes, BigDecimal somaJetons) {
        this.idPessoa = idPessoa;
        this.nome = nome;
        this.pontosRemanescentes = pontosRemanescentes;
        this.somaJetons = somaJetons;
    }

    public PontosRemanescentesDTO() {
    }

    public Integer getIdPessoa() {
        return idPessoa;
    }

    public void setIdPessoa(Integer idPessoa) {
        this.idPessoa = idPessoa;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public Long getPontosRemanescentes() {
        return pontosRemanescentes;
    }

    public void setPontosRemanescentes(Long pontosRemanescentes) {
        this.pontosRemanescentes = pontosRemanescentes;
    }

    public BigDecimal getSomaJetons() {
        return somaJetons;
    }

    public void setSomaJetons(BigDecimal somaJetons) {
        this.somaJetons = somaJetons;
    }

    public String getPontosRemanescentesFormatado() {
        return pontosRemanescentesFormatado;
    }

    public void setPontosRemanescentesFormatado(String pontosRemanescentesFormatado) {
        this.pontosRemanescentesFormatado = pontosRemanescentesFormatado;
    }

    public String getSomaJetonsFormatado() {
        return somaJetonsFormatado;
    }

    public void setSomaJetonsFormatado(String somaJetonsFormatado) {
        this.somaJetonsFormatado = somaJetonsFormatado;
    }

    public Long getPontosAtividadesValidadas() {
        return pontosAtividadesValidadas;
    }

    public void setPontosAtividadesValidadas(Long pontosAtividadesValidadas) {
        this.pontosAtividadesValidadas = pontosAtividadesValidadas;
    }

    public Long getSaldoPontos() {
        return saldoPontos;
    }

    public void setSaldoPontos(Long saldoPontos) {
        this.saldoPontos = saldoPontos;
    }

    public String getPontosAtividadesValidadasFormatado() {
        return pontosAtividadesValidadasFormatado;
    }

    public void setPontosAtividadesValidadasFormatado(String pontosAtividadesValidadasFormatado) {
        this.pontosAtividadesValidadasFormatado = pontosAtividadesValidadasFormatado;
    }

    public String getSaldoPontosFormatado() {
        return saldoPontosFormatado;
    }

    public void setSaldoPontosFormatado(String saldoPontosFormatado) {
        this.saldoPontosFormatado = saldoPontosFormatado;
    }
}