package br.com.cremepe.jeton.dominio;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

@Entity
@Table(name = "resolucao")
public class Resolucao implements Serializable {

    private static final long serialVersionUID = 1L;

    // =========================================================================
    // CONSTANTES PARA REVOGAÇÃO
    // =========================================================================
    public static final String REVOGADO_SIM = "S";
    public static final String REVOGADO_NAO = "N";

    // =========================================================================
    // CAMPOS DA ENTIDADE
    // =========================================================================
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "idResolucao")
    private Integer idResolucao;

    @NotNull(message = "O número da resolução é obrigatório")
    @Positive(message = "O número deve ser positivo")
    @Column(name = "numero", nullable = false)
    private Integer numero;

    @NotNull(message = "O ano da resolução é obrigatório")
    @Positive(message = "O ano deve ser positivo")
    @Column(name = "ano", nullable = false)
    private Integer ano;

    @Column(name = "dtInicioVigencia")
    private LocalDate dtInicioVigencia;

    @Column(name = "dtFimVigencia")
    private LocalDate dtFimVigencia;

    @Size(max = 100)
    @Column(name = "linkPublicado", length = 100)
    private String linkPublicado;

    @NotNull
    @Pattern(regexp = "[SN]", message = "inRevogado deve ser S ou N")
    @Column(name = "inRevogado", nullable = false, length = 1)
    private String inRevogado = REVOGADO_NAO;

    @Column(name = "ementa", columnDefinition = "TEXT")
    private String ementa;

    @NotNull(message = "Pontos por Jeton é obrigatório")
    @Positive(message = "Pontos por Jeton deve ser maior que zero")
    @Column(name = "pontosPorJeton", nullable = false)
    private Integer pontosPorJeton;

    @NotNull(message = "Máximo de Jetons por dia é obrigatório")
    @Positive(message = "Máximo de Jetons por dia deve ser maior que zero")
    @Column(name = "maxJetonsDia", nullable = false)
    private Integer maxJetonsDia;

    @NotNull(message = "Máximo de Jetons por período é obrigatório")
    @Positive(message = "Máximo de Jetons por período deve ser maior que zero")
    @Column(name = "maxJetonsPeriodo", nullable = false)
    private Integer maxJetonsPeriodo;

    @NotNull(message = "Máximo de Jetons por mês é obrigatório")
    @Positive(message = "Máximo de Jetons por mês deve ser maior que zero")
    @Column(name = "maxJetonsMes", nullable = false)
    private Integer maxJetonsMes;

    @NotNull(message = "Valor do Jeton é obrigatório")
    @Positive(message = "Valor do Jeton deve ser maior que zero")
    @Column(name = "valorJeton", nullable = false, precision = 10, scale = 2)
    private BigDecimal valorJeton;

    // =========================================================================
    // CONSTRUTORES
    // =========================================================================
    public Resolucao() {
    }

    // =========================================================================
    // MÉTODOS DE CONVENIÊNCIA
    // =========================================================================
    public boolean isRevogado() {
        return REVOGADO_SIM.equals(inRevogado);
    }

    public boolean isEmVigor() {
        return REVOGADO_NAO.equals(inRevogado);
    }

    // =========================================================================
    // JPA LIFECYCLE – NORMALIZAÇÃO
    // =========================================================================
    @PrePersist
    @PreUpdate
    protected void normalize() {
        if (inRevogado != null) {
            inRevogado = inRevogado.toUpperCase();
        }
        if (linkPublicado != null) {
            linkPublicado = linkPublicado.trim();
        }
        // Garante que o valor padrão seja 'N' caso venha inválido
        if (!REVOGADO_SIM.equals(inRevogado) && !REVOGADO_NAO.equals(inRevogado)) {
            inRevogado = REVOGADO_NAO;
        }
    }

    // =========================================================================
    // GETTERS E SETTERS
    // =========================================================================
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

    public LocalDate getDtInicioVigencia() {
        return dtInicioVigencia;
    }

    public void setDtInicioVigencia(LocalDate dtInicioVigencia) {
        this.dtInicioVigencia = dtInicioVigencia;
    }

    public LocalDate getDtFimVigencia() {
        return dtFimVigencia;
    }

    public void setDtFimVigencia(LocalDate dtFimVigencia) {
        this.dtFimVigencia = dtFimVigencia;
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

    // =========================================================================
    // EQUALS & HASHCODE
    // =========================================================================
    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        Resolucao that = (Resolucao) o;
        return Objects.equals(idResolucao, that.idResolucao);
    }

    @Override
    public int hashCode() {
        return Objects.hash(idResolucao);
    }

    @Override
    public String toString() {
        return "Resolucao{" +
                "id=" + idResolucao +
                ", numero=" + numero +
                ", ano=" + ano +
                ", vigencia=" + dtInicioVigencia + " até " + dtFimVigencia +
                ", revogado=" + inRevogado +
                '}';
    }
}