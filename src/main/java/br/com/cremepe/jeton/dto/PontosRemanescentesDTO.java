package br.com.cremepe.jeton.dto;

import java.io.Serializable;
import java.math.BigDecimal;

public class PontosRemanescentesDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Integer idPessoa;
    private String nome;
    private Long somaPontos;
    private String somaPontosFormatado;
    private BigDecimal somaJetons;
    private String somaJetonsFormatado;

    public PontosRemanescentesDTO() {
    }

    public PontosRemanescentesDTO(Integer idPessoa, String nome, Long somaPontos, BigDecimal somaJetons) {
        this.idPessoa = idPessoa;
        this.nome = nome;
        this.somaPontos = somaPontos;
        this.somaJetons = somaJetons;
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

    public Long getSomaPontos() {
        return somaPontos;
    }

    public void setSomaPontos(Long somaPontos) {
        this.somaPontos = somaPontos;
    }

    public String getSomaPontosFormatado() {
        return somaPontosFormatado;
    }

    public void setSomaPontosFormatado(String somaPontosFormatado) {
        this.somaPontosFormatado = somaPontosFormatado;
    }

    public BigDecimal getSomaJetons() {
        return somaJetons;
    }

    public void setSomaJetons(BigDecimal somaJetons) {
        this.somaJetons = somaJetons;
    }

    public String getSomaJetonsFormatado() {
        return somaJetonsFormatado;
    }

    public void setSomaJetonsFormatado(String somaJetonsFormatado) {
        this.somaJetonsFormatado = somaJetonsFormatado;
    }
}