package br.com.cremepe.jeton.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class JetonSaldoId implements Serializable {

    private static final long serialVersionUID = 1L;

    @Column(name = "idJeton")
    private Integer idJeton;

    @Column(name = "idPontosSaldo")
    private Integer idPontosSaldo;

    public JetonSaldoId() {
    }

    public JetonSaldoId(Integer idJeton, Integer idPontosSaldo) {
        this.idJeton = idJeton;
        this.idPontosSaldo = idPontosSaldo;
    }

    public Integer getIdJeton() {
        return idJeton;
    }

    public void setIdJeton(Integer idJeton) {
        this.idJeton = idJeton;
    }

    public Integer getIdPontosSaldo() {
        return idPontosSaldo;
    }

    public void setIdPontosSaldo(Integer idPontosSaldo) {
        this.idPontosSaldo = idPontosSaldo;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        JetonSaldoId that = (JetonSaldoId) o;
        return Objects.equals(idJeton, that.idJeton) &&
                Objects.equals(idPontosSaldo, that.idPontosSaldo);
    }

    @Override
    public int hashCode() {
        return Objects.hash(idJeton, idPontosSaldo);
    }
}