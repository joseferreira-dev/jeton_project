package br.com.cremepe.jeton.dominio;

import jakarta.persistence.*;
import java.io.Serializable;
import java.util.Objects;

/**
 * Entidade JPA que representa a tabela associativa 'jeton_saldo'.
 * Faz a ligação explícita entre um pagamento (Jeton) e os saldos de pontos.
 */
@Entity
@Table(name = "jeton_saldo")
public class JetonSaldo implements Serializable {

    private static final long serialVersionUID = 1L;

    // =========================================================================
    // CAMPOS DA ENTIDADE
    // =========================================================================
    @EmbeddedId
    private JetonSaldoId id = new JetonSaldoId();

    @ManyToOne
    @MapsId("idJeton")
    @JoinColumn(name = "idJeton")
    private Jeton jeton;

    @ManyToOne
    @MapsId("idPontosSaldo")
    @JoinColumn(name = "idPontosSaldo")
    private PontosSaldo pontosSaldo;

    // =========================================================================
    // CONSTRUTORES
    // =========================================================================
    public JetonSaldo() {
    }

    // =========================================================================
    // GETTERS E SETTERS
    // =========================================================================
    public JetonSaldoId getId() {
        return id;
    }

    public void setId(JetonSaldoId id) {
        this.id = id;
    }

    public Jeton getJeton() {
        return jeton;
    }

    public void setJeton(Jeton jeton) {
        this.jeton = jeton;
    }

    public PontosSaldo getPontosSaldo() {
        return pontosSaldo;
    }

    public void setPontosSaldo(PontosSaldo pontosSaldo) {
        this.pontosSaldo = pontosSaldo;
    }

    // =========================================================================
    // EQUALS & HASHCODE (baseado na chave composta)
    // =========================================================================
    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        JetonSaldo that = (JetonSaldo) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}