package br.com.cremepe.jeton.domain;

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

    @Column(name = "inTipoPessoa", length = 1)
    private String inTipoPessoa;

    @Column(name = "permissoes", columnDefinition = "text")
    private String permissoes;

    public ViewUserLogin() {
    }

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

    public Integer getIdPessoa() {
        return idPessoa;
    }

    public void setIdPessoa(Integer idPessoa) {
        this.idPessoa = idPessoa;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getCpf() {
        return cpf;
    }

    public void setCpf(String cpf) {
        this.cpf = cpf;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getSenha() {
        return senha;
    }

    public void setSenha(String senha) {
        this.senha = senha;
    }

    public String getInTipoPessoa() {
        return inTipoPessoa;
    }

    public void setInTipoPessoa(String inTipoPessoa) {
        this.inTipoPessoa = inTipoPessoa;
    }

    public String getPermissoes() {
        return permissoes;
    }

    public void setPermissoes(String permissoes) {
        this.permissoes = permissoes;
    }

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