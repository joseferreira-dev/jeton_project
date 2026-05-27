package br.com.cremepe.jeton.dominio;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.Immutable;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Entidade de leitura que mapeia a view 'vw_user_login'.
 * Fornece dados de autenticação e permissões de forma imutável.
 */
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

    // =========================================================================
    // CONSTRUTORES
    // =========================================================================
    public ViewUserLogin() {
    }

    // =========================================================================
    // MÉTODOS DE CONVENIÊNCIA
    // =========================================================================
    public boolean hasPermissao(String nivel) {
        if (permissoes == null || permissoes.isBlank())
            return false;
        return Arrays.asList(permissoes.split(",")).contains(nivel);
    }

    public List<String> getPermissoesAsList() {
        if (permissoes == null || permissoes.isBlank()) {
            return Collections.emptyList();
        }
        return Arrays.asList(permissoes.split(","));
    }

    // =========================================================================
    // GETTERS (imutável, sem setters)
    // =========================================================================
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

    // =========================================================================
    // EQUALS, HASHCODE E TO_STRING (seguro)
    // =========================================================================
    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        ViewUserLogin that = (ViewUserLogin) o;
        return Objects.equals(idPessoa, that.idPessoa);
    }

    @Override
    public int hashCode() {
        return Objects.hash(idPessoa);
    }

    @Override
    public String toString() {
        return "ViewUserLogin{" +
                "id=" + idPessoa +
                ", nome='" + nome + '\'' +
                ", cpf='***'" +
                ", email='" + email + '\'' +
                ", permissoes='" + permissoes + '\'' +
                '}';
    }
}