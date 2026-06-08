package br.com.cremepe.jeton.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Entidade que define grupos de regras que partilham um limite de pontos comum.
 */
@Entity
@Table(name = "regras_conjuntas")
public class RegrasConjuntas implements Serializable {

    private static final long serialVersionUID = 1L;

    // =========================================================================
    // CONSTANTES PARA inTipoLimite
    // =========================================================================
    public static final String TIPO_LIMITE_DIARIO = "D";
    public static final String TIPO_LIMITE_TURNO = "T";
    public static final String TIPO_LIMITE_MENSAL = "M";

    // =========================================================================
    // CAMPOS
    // =========================================================================
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "idRegraConjunta")
    private Integer idRegraConjunta;

    @NotNull(message = "O nome da regra conjunta é obrigatório")
    @Size(min = 3, max = 50, message = "O nome deve ter entre 3 e 50 caracteres")
    @Column(name = "nomeRegra", length = 50, nullable = false)
    private String nomeRegra;

    @NotNull(message = "O tipo de limite é obrigatório")
    @Pattern(regexp = "[DTM]", message = "inTipoLimite deve ser D, T ou M")
    @Column(name = "inTipoLimite", length = 1, nullable = false)
    private String inTipoLimite;

    @NotNull(message = "O limite de pontos é obrigatório")
    @Positive(message = "O limite de pontos deve ser maior que zero")
    @Column(name = "pontosLimite", nullable = false)
    private Integer pontosLimite;

    /**
     * Relacionamento Muitos-para-Muitos com a tabela 'regras'.
     * O JoinTable define a tabela de ligação 'regra_conjunta_agrupada'.
     */
    @ManyToMany
    @JoinTable(name = "regra_conjunta_agrupada", joinColumns = @JoinColumn(name = "idRegraConjunta"), inverseJoinColumns = @JoinColumn(name = "idRegra"))
    private List<Regras> regrasAgrupadas = new ArrayList<>();

    // =========================================================================
    // CONSTRUTORES
    // =========================================================================
    public RegrasConjuntas() {
    }

    // =========================================================================
    // MÉTODOS DE CONVENIÊNCIA
    // =========================================================================
    public boolean isTipoDiario() {
        return TIPO_LIMITE_DIARIO.equals(inTipoLimite);
    }

    public boolean isTipoTurno() {
        return TIPO_LIMITE_TURNO.equals(inTipoLimite);
    }

    public boolean isTipoMensal() {
        return TIPO_LIMITE_MENSAL.equals(inTipoLimite);
    }

    // =========================================================================
    // JPA LIFECYCLE – NORMALIZAÇÃO
    // =========================================================================
    @PrePersist
    @PreUpdate
    protected void normalize() {
        if (nomeRegra != null) {
            nomeRegra = nomeRegra.trim();
        }
        if (inTipoLimite != null) {
            inTipoLimite = inTipoLimite.toUpperCase();
        }
        // Garante valor padrão 'D' caso venha inválido
        if (!TIPO_LIMITE_DIARIO.equals(inTipoLimite) &&
                !TIPO_LIMITE_TURNO.equals(inTipoLimite) &&
                !TIPO_LIMITE_MENSAL.equals(inTipoLimite)) {
            inTipoLimite = TIPO_LIMITE_DIARIO;
        }
        if (pontosLimite == null) {
            pontosLimite = 0;
        }
    }

    // =========================================================================
    // GETTERS E SETTERS
    // =========================================================================
    public Integer getIdRegraConjunta() {
        return idRegraConjunta;
    }

    public void setIdRegraConjunta(Integer idRegraConjunta) {
        this.idRegraConjunta = idRegraConjunta;
    }

    public String getNomeRegra() {
        return nomeRegra;
    }

    public void setNomeRegra(String nomeRegra) {
        this.nomeRegra = nomeRegra;
    }

    public String getInTipoLimite() {
        return inTipoLimite;
    }

    public void setInTipoLimite(String inTipoLimite) {
        this.inTipoLimite = inTipoLimite;
    }

    public Integer getPontosLimite() {
        return pontosLimite;
    }

    public void setPontosLimite(Integer pontosLimite) {
        this.pontosLimite = pontosLimite;
    }

    public List<Regras> getRegrasAgrupadas() {
        return regrasAgrupadas;
    }

    public void setRegrasAgrupadas(List<Regras> regrasAgrupadas) {
        this.regrasAgrupadas = regrasAgrupadas;
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
        RegrasConjuntas that = (RegrasConjuntas) o;
        return Objects.equals(idRegraConjunta, that.idRegraConjunta);
    }

    @Override
    public int hashCode() {
        return Objects.hash(idRegraConjunta);
    }

    // =========================================================================
    // TO_STRING
    // =========================================================================
    @Override
    public String toString() {
        return "RegrasConjuntas{" +
                "id=" + idRegraConjunta +
                ", nome='" + nomeRegra + '\'' +
                ", tipoLimite=" + inTipoLimite +
                ", limite=" + pontosLimite +
                '}';
    }
}