package br.com.cremepe.jeton.dominio;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
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

    @EmbeddedId
    private JetonSaldoId id = new JetonSaldoId();

    // @MapsId faz com que o JPA use o ID do Jeton dentro da chave composta
    @ManyToOne
    @MapsId("idJeton")
    @JoinColumn(name = "idJeton")
    private Jeton jeton;

    // @MapsId faz com que o JPA use o ID do PontosSaldo dentro da chave composta
    @ManyToOne
    @MapsId("idPontosSaldo")
    @JoinColumn(name = "idPontosSaldo")
    private PontosSaldo pontosSaldo;

    public JetonSaldo() {
    }

    // ==========================================
    // GETTERS E SETTERS
    // ==========================================

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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        JetonSaldo that = (JetonSaldo) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}