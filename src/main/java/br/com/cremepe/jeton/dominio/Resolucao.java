package br.com.cremepe.jeton.dominio;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Objects;

/**
 * Entidade JPA que representa a tabela 'resolucao'.
 * Define limites de pontos e valores financeiros (Jetons).
 */
@Entity
@Table(name = "resolucao")
public class Resolucao implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "idResolucao")
    private Integer idResolucao;

    @Column(name = "numero", nullable = false)
    private Integer numero;

    @Column(name = "ano", nullable = false)
    private Integer ano;

    @Column(name = "linkPublicado", length = 100)
    private String linkPublicado;

    @Column(name = "inRevogado", length = 1, nullable = false)
    private String inRevogado;

    // Mapeamento específico para suportar textos longos do MySQL
    @Column(name = "ementa", columnDefinition = "text")
    private String ementa;

    @Column(name = "pontosPorJeton", nullable = false)
    private Integer pontosPorJeton;

    @Column(name = "maxJetonsDia", nullable = false)
    private Integer maxJetonsDia;

    @Column(name = "maxJetonsPeriodo", nullable = false)
    private Integer maxJetonsPeriodo;

    @Column(name = "maxJetonsMes", nullable = false)
    private Integer maxJetonsMes;

    // Uso rigoroso de BigDecimal para dados monetários (decimal(10,2))
    @Column(name = "valorJeton", precision = 10, scale = 2, nullable = false)
    private BigDecimal valorJeton;

    public Resolucao() {
    }

    // ==========================================
    // GETTERS E SETTERS
    // ==========================================

    public Integer getIdResolucao() {
        return idResolucao;
    }

    public void setIdResolucao(Integer idResolucao) {
        this.idResolucao = idResolucao;
    }

    public Integer getNumero() {
        return numero;
    }

    public void setNumero(Integer numero) {
        this.numero = numero;
    }

    public Integer getAno() {
        return ano;
    }

    public void setAno(Integer ano) {
        this.ano = ano;
    }

    public String getLinkPublicado() {
        return linkPublicado;
    }

    public void setLinkPublicado(String linkPublicado) {
        this.linkPublicado = linkPublicado;
    }

    public String getInRevogado() {
        return inRevogado;
    }

    public void setInRevogado(String inRevogado) {
        this.inRevogado = inRevogado;
    }

    public String getEmenta() {
        return ementa;
    }

    public void setEmenta(String ementa) {
        this.ementa = ementa;
    }

    public Integer getPontosPorJeton() {
        return pontosPorJeton;
    }

    public void setPontosPorJeton(Integer pontosPorJeton) {
        this.pontosPorJeton = pontosPorJeton;
    }

    public Integer getMaxJetonsDia() {
        return maxJetonsDia;
    }

    public void setMaxJetonsDia(Integer maxJetonsDia) {
        this.maxJetonsDia = maxJetonsDia;
    }

    public Integer getMaxJetonsPeriodo() {
        return maxJetonsPeriodo;
    }

    public void setMaxJetonsPeriodo(Integer maxJetonsPeriodo) {
        this.maxJetonsPeriodo = maxJetonsPeriodo;
    }

    public Integer getMaxJetonsMes() {
        return maxJetonsMes;
    }

    public void setMaxJetonsMes(Integer maxJetonsMes) {
        this.maxJetonsMes = maxJetonsMes;
    }

    public BigDecimal getValorJeton() {
        return valorJeton;
    }

    public void setValorJeton(BigDecimal valorJeton) {
        this.valorJeton = valorJeton;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Resolucao resolucao = (Resolucao) o;
        return Objects.equals(idResolucao, resolucao.idResolucao);
    }

    @Override
    public int hashCode() {
        return Objects.hash(idResolucao);
    }
}