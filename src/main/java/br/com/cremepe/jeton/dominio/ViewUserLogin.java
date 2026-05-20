package br.com.cremepe.jeton.dominio;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.Immutable;
import java.io.Serializable;

@Entity
@Immutable
@Table(name = "vw_user_login")
public class ViewUserLogin implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "idPessoa")
    private Integer idPessoa;

    @Column(name = "nome")
    private String nome;

    @Column(name = "cpf")
    private String cpf;

    @Column(name = "email")
    private String email;

    @Column(name = "senha")
    private String senha;

    @Column(name = "permissoes", columnDefinition = "text")
    private String permissoes;

    public ViewUserLogin() {
    }

    // ==========================================
    // APENAS GETTERS (Entidade Imutável)
    // ==========================================

    public Integer getIdPessoa() {
        return idPessoa;
    }

    public String getNome() {
        return nome;
    }

    public String getCpf() {
        return cpf;
    }

    public String getEmail() {
        return email;
    }

    public String getSenha() {
        return senha;
    }

    public String getPermissoes() {
        return permissoes;
    }
}