package br.com.cremepe.jeton.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class UsuarioAcessoId implements Serializable {

    private static final long serialVersionUID = 1L;

    @Column(name = "idUsuarioPessoa")
    private Integer idUsuarioPessoa;

    @Column(name = "idNivel")
    private String idNivel;

    public UsuarioAcessoId() {
    }

    public UsuarioAcessoId(Integer idUsuarioPessoa, String idNivel) {
        this.idUsuarioPessoa = idUsuarioPessoa;
        this.idNivel = idNivel;
    }

    public Integer getIdUsuarioPessoa() {
        return idUsuarioPessoa;
    }

    public void setIdUsuarioPessoa(Integer idUsuarioPessoa) {
        this.idUsuarioPessoa = idUsuarioPessoa;
    }

    public String getIdNivel() {
        return idNivel;
    }

    public void setIdNivel(String idNivel) {
        this.idNivel = idNivel;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        UsuarioAcessoId that = (UsuarioAcessoId) o;
        return Objects.equals(idUsuarioPessoa, that.idUsuarioPessoa) &&
                Objects.equals(idNivel, that.idNivel);
    }

    @Override
    public int hashCode() {
        return Objects.hash(idUsuarioPessoa, idNivel);
    }

    @Override
    public String toString() {
        return "UsuarioAcessoId{" +
                "usuario=" + idUsuarioPessoa +
                ", nivel='" + idNivel + '\'' +
                '}';
    }
}