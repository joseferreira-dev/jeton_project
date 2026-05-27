package br.com.cremepe.jeton.dominio;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.Objects;

/**
 * Entidade que representa a tabela associativa 'usuario_acesso'.
 * Armazena os níveis de acesso concedidos a cada usuário.
 */
@Entity
@Table(name = "usuario_acesso")
public class UsuarioAcesso implements Serializable {

    private static final long serialVersionUID = 1L;

    @EmbeddedId
    private UsuarioAcessoId id = new UsuarioAcessoId();

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("idUsuarioPessoa")
    @JoinColumn(name = "idUsuarioPessoa")
    private Usuario usuario;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("idNivel")
    @JoinColumn(name = "idNivel")
    private NivelAcesso nivelAcesso;

    // =========================================================================
    // CONSTRUTORES
    // =========================================================================
    public UsuarioAcesso() {
    }

    // =========================================================================
    // MÉTODOS DE CONVENIÊNCIA
    // =========================================================================
    public String getUsuarioNome() {
        return usuario != null && usuario.getPessoa() != null ? usuario.getPessoa().getNome() : null;
    }

    public String getNivelNome() {
        return nivelAcesso != null ? nivelAcesso.getNomeNivel() : null;
    }

    // =========================================================================
    // GETTERS E SETTERS
    // =========================================================================
    public UsuarioAcessoId getId() {
        return id;
    }

    public void setId(UsuarioAcessoId id) {
        this.id = id;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public void setUsuario(Usuario usuario) {
        this.usuario = usuario;
        if (usuario != null && usuario.getIdUsuarioPessoa() != null) {
            this.id.setIdUsuarioPessoa(usuario.getIdUsuarioPessoa());
        }
    }

    public NivelAcesso getNivelAcesso() {
        return nivelAcesso;
    }

    public void setNivelAcesso(NivelAcesso nivelAcesso) {
        this.nivelAcesso = nivelAcesso;
        if (nivelAcesso != null && nivelAcesso.getIdNivel() != null) {
            this.id.setIdNivel(nivelAcesso.getIdNivel());
        }
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
        UsuarioAcesso that = (UsuarioAcesso) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    // =========================================================================
    // TO_STRING
    // =========================================================================
    @Override
    public String toString() {
        return "UsuarioAcesso{" +
                "id=" + id +
                ", usuario=" + (usuario != null ? usuario.getIdUsuarioPessoa() : null) +
                ", nivel=" + (nivelAcesso != null ? nivelAcesso.getIdNivel() : null) +
                '}';
    }
}