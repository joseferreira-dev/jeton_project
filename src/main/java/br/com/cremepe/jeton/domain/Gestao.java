package br.com.cremepe.jeton.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.Objects;

/**
 * Entidade que representa uma gestão (mandato) dentro do sistema.
 * Define o período de vigência e o nome da gestão.
 */
@Entity
@Table(name = "gestao")
public class Gestao implements Serializable {

    private static final long serialVersionUID = 1L;

    // =========================================================================
    // CONSTANTES PARA ORDENAÇÃO (usadas nos controllers)
    // =========================================================================
    public static final String SORT_DT_INICIO = "dtInicio";
    public static final String SORT_DT_FIM = "dtFim";
    public static final String SORT_NOME = "nomeGestao";

    // =========================================================================
    // CAMPOS DA ENTIDADE
    // =========================================================================
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "idGestao")
    private Integer idGestao;

    @NotBlank(message = "O nome da gestão é obrigatório")
    @Size(max = 70)
    @Column(name = "nomeGestao", length = 70, nullable = false, unique = true)
    private String nomeGestao;

    @NotNull(message = "A data de início é obrigatória")
    @PastOrPresent(message = "A data de início não pode ser futura")
    @Column(name = "dtInicio", nullable = false)
    private LocalDate dtInicio;

    @NotNull(message = "A data de fim é obrigatória")
    @Column(name = "dtFim", nullable = false)
    private LocalDate dtFim;

    // =========================================================================
    // CONSTRUTORES
    // =========================================================================
    public Gestao() {
    }

    // =========================================================================
    // MÉTODOS DE CONVENIÊNCIA
    // =========================================================================
    public boolean isPeriodoValido() {
        return dtInicio != null && dtFim != null && dtFim.isAfter(dtInicio);
    }

    public boolean isPeriodoAtual() {
        LocalDate hoje = LocalDate.now();
        return dtInicio != null && dtFim != null && !hoje.isBefore(dtInicio) && !hoje.isAfter(dtFim);
    }

    // =========================================================================
    // JPA LIFECYCLE – NORMALIZAÇÃO
    // =========================================================================
    @PrePersist
    @PreUpdate
    protected void normalize() {
        if (nomeGestao != null)
            nomeGestao = nomeGestao.trim();
    }

    // =========================================================================
    // GETTERS E SETTERS
    // =========================================================================
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

    public LocalDate getDtInicio() {
        return dtInicio;
    }

    public void setDtInicio(LocalDate dtInicio) {
        this.dtInicio = dtInicio;
    }

    public LocalDate getDtFim() {
        return dtFim;
    }

    public void setDtFim(LocalDate dtFim) {
        this.dtFim = dtFim;
    }

    // =========================================================================
    // EQUALS & HASHCODE (baseado no ID)
    // =========================================================================
    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        Gestao gestao = (Gestao) o;
        return Objects.equals(idGestao, gestao.idGestao);
    }

    @Override
    public int hashCode() {
        return Objects.hash(idGestao);
    }

    // =========================================================================
    // TO_STRING (para logs)
    // =========================================================================
    @Override
    public String toString() {
        return "Gestao{" +
                "id=" + idGestao +
                ", nome='" + nomeGestao + '\'' +
                ", inicio=" + dtInicio +
                ", fim=" + dtFim +
                '}';
    }
}