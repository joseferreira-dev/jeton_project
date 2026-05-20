package br.com.cremepe.jeton.dominio;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
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

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "idRegraConjunta")
    private Integer idRegraConjunta;

    @Column(name = "nomeRegra", length = 50, nullable = false)
    private String nomeRegra;

    @Column(name = "inTipoLimite", length = 1, nullable = false)
    private String inTipoLimite;

    @Column(name = "pontosLimite", nullable = false)
    private Integer pontosLimite;

    /**
     * Relacionamento Muitos-para-Muitos com a tabela 'regras'.
     * O JoinTable define a tabela de ligação 'regra_conjunta_agrupada' mapeada no
     * banco.
     */
    @ManyToMany
    @JoinTable(name = "regra_conjunta_agrupada", joinColumns = @JoinColumn(name = "idRegraConjunta"), inverseJoinColumns = @JoinColumn(name = "idRegra"))
    private List<Regras> regrasAgrupadas = new ArrayList<>();

    public RegrasConjuntas() {
    }

    // ==========================================
    // GETTERS E SETTERS
    // ==========================================

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
}