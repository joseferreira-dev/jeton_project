package br.com.cremepe.jeton.dominio;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import java.io.Serializable;

@Entity
@Table(name = "usuario_acesso")
public class UsuarioAcesso implements Serializable {

    private static final long serialVersionUID = 1L;

    @EmbeddedId
    private UsuarioAcessoId id = new UsuarioAcessoId();

    @ManyToOne
    @MapsId("idUsuarioPessoa")
    @JoinColumn(name = "idUsuarioPessoa")
    private Usuario usuario;

    @ManyToOne
    @MapsId("idNivel")
    @JoinColumn(name = "idNivel")
    private NivelAcesso nivelAcesso;

    public UsuarioAcesso() {
    }

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
    }

    public NivelAcesso getNivelAcesso() {
        return nivelAcesso;
    }

    public void setNivelAcesso(NivelAcesso nivelAcesso) {
        this.nivelAcesso = nivelAcesso;
    }
}