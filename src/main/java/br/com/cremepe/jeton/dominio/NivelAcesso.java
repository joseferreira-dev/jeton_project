package br.com.cremepe.jeton.dominio;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.util.Objects;

/**
 * Entidade que representa os perfis de acesso (ex: Administrador, Conselheiro).
 */
@Entity
@Table(name = "nivel_acesso")
public class NivelAcesso implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "idNivel", length = 1)
    private String idNivel;

    @Column(name = "nomeNivel", length = 50, nullable = false, unique = true)
    private String nomeNivel;

    public NivelAcesso() {
    }

    // ==========================================
    // GETTERS E SETTERS
    // ==========================================

    public String getIdNivel() {
        return idNivel;
    }

    public void setIdNivel(String idNivel) {
        this.idNivel = idNivel;
    }

    public String getNomeNivel() {
        return nomeNivel;
    }

    public void setNomeNivel(String nomeNivel) {
        this.nomeNivel = nomeNivel;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        NivelAcesso that = (NivelAcesso) o;
        return Objects.equals(idNivel, that.idNivel);
    }

    @Override
    public int hashCode() {
        return Objects.hash(idNivel);
    }
}