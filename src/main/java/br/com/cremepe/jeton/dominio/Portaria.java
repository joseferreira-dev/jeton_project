package br.com.cremepe.jeton.dominio;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.io.Serializable;
import java.time.LocalDate;
import java.util.Objects;

@Entity
@Table(name = "portaria")
public class Portaria implements Serializable {

    private static final long serialVersionUID = 1L;

    // =========================================================================
    // CONSTANTES PARA REVOGAÇÃO
    // =========================================================================
    public static final String REVOGADO_SIM = "S";
    public static final String REVOGADO_NAO = "N";

    // =========================================================================
    // CAMPOS
    // =========================================================================
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "idPortaria")
    private Integer idPortaria;

    @NotNull(message = "O número da portaria é obrigatório")
    @Positive(message = "O número deve ser positivo")
    @Column(name = "numero", nullable = false)
    private Integer numero;

    @NotNull(message = "O ano da portaria é obrigatório")
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

    // =========================================================================
    // CONSTRUTORES
    // =========================================================================
    public Portaria() {
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
    public Integer getIdPortaria() {
        return idPortaria;
    }

    public void setIdPortaria(Integer idPortaria) {
        this.idPortaria = idPortaria;
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

    // =========================================================================
    // EQUALS & HASHCODE
    // =========================================================================
    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        Portaria portaria = (Portaria) o;
        return Objects.equals(idPortaria, portaria.idPortaria);
    }

    @Override
    public int hashCode() {
        return Objects.hash(idPortaria);
    }

    @Override
    public String toString() {
        return "Portaria{" +
                "id=" + idPortaria +
                ", numero=" + numero +
                ", ano=" + ano +
                ", vigencia=" + dtInicioVigencia + " até " + dtFimVigencia +
                ", revogado=" + inRevogado +
                '}';
    }
}