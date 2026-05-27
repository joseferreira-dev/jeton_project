package br.com.cremepe.jeton.dto;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * DTO Utilizado para encapsular o resultado de consultas agregadas (SOMAS e
 * GROUP BY) referentes aos saldos dos conselheiros para exibição em tela ou
 * relatórios.
 * NOTA: Esta classe não possui mapeamento físico (@Entity) no banco de dados.
 */
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

    // =========================================================================
    // GETTERS E SETTERS
    // =========================================================================
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