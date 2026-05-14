package br.com.cremepe.jeton.dominio;

import java.io.Serializable;
import java.util.Objects;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

/**
 * Entidade JPA que representa a tabela 'usuario' no banco de dados.
 * Possui um relacionamento 1:1 com a entidade Pessoa, compartilhando a mesma PK.
 */
@Entity
@Table(name = "usuario")
public class Usuario implements Serializable {

    private static final long serialVersionUID = 1L;

    // Não usamos @GeneratedValue aqui, pois o ID vem da Pessoa
    @Id
    @Column(name = "idUsuarioPessoa")
    private Integer idUsuarioPessoa;

    // O @MapsId avisa ao Hibernate: "Use o ID desta entidade Pessoa como o meu próprio ID"
    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
    @MapsId
    @JoinColumn(name = "idUsuarioPessoa")
    private Pessoa pessoa;

    @Column(name = "senha", length = 64)
    private String senha;

    @Column(name = "inSituacao", length = 1, nullable = false)
    private String inSituacao;

    @Transient
    private boolean eConselheiro;

    @Transient
    private Integer crm;

    public Usuario() {
    }

    // ==========================================
    // GETTERS E SETTERS
    // ==========================================

    public Integer getIdUsuarioPessoa() {
        return idUsuarioPessoa;
    }

    public void setIdUsuarioPessoa(Integer idUsuarioPessoa) {
        this.idUsuarioPessoa = idUsuarioPessoa;
    }

    public Pessoa getPessoa() {
        return pessoa;
    }

    public void setPessoa(Pessoa pessoa) {
        this.pessoa = pessoa;
    }

    public String getSenha() {
        return senha;
    }

    public void setSenha(String senha) {
        this.senha = senha;
    }

    public String getInSituacao() {
        return inSituacao;
    }

    public void setInSituacao(String inSituacao) {
        this.inSituacao = inSituacao;
    }

    public boolean iseConselheiro() { return eConselheiro; }
    public void seteConselheiro(boolean eConselheiro) { this.eConselheiro = eConselheiro; }
    public Integer getCrm() { return crm; }
    public void setCrm(Integer crm) { this.crm = crm; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Usuario usuario = (Usuario) o;
        return Objects.equals(idUsuarioPessoa, usuario.idUsuarioPessoa);
    }

    @Override
    public int hashCode() {
        return Objects.hash(idUsuarioPessoa);
    }
}