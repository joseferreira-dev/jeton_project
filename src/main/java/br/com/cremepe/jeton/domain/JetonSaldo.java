package br.com.cremepe.jeton.domain;

import jakarta.persistence.*;
import java.io.Serializable;
import java.util.Objects;

@Entity
@Table(name = "jeton_saldo")
public class JetonSaldo implements Serializable {

    private static final long serialVersionUID = 1L;

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

    public JetonSaldo() {
    }

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