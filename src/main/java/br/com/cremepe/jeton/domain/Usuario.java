package br.com.cremepe.jeton.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.io.Serializable;
import java.util.Objects;

@Entity
@Table(name = "usuario")
public class Usuario implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final String SITUACAO_ATIVO = "A";
    public static final String SITUACAO_INATIVO = "I";

    @Id
    @Column(name = "idUsuarioPessoa")
    private Integer idUsuarioPessoa;

    @NotNull
    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "idUsuarioPessoa")
    private Pessoa pessoa;

    @Size(max = 64)
    @Column(name = "senha", length = 64)
    private String senha;

    @NotNull
    @Column(name = "inSituacao", length = 1, nullable = false)
    private String inSituacao;

    @Transient
    private boolean eConselheiro;

    @Transient
    private Integer crm;

    public Usuario() {
    }

    public boolean isAtivo() {
        return SITUACAO_ATIVO.equals(inSituacao);
    }

    public boolean isInativo() {
        return SITUACAO_INATIVO.equals(inSituacao);
    }

    @PrePersist
    @PreUpdate
    protected void normalize() {
        if (inSituacao != null) {
            inSituacao = inSituacao.toUpperCase();
        }
        if (!SITUACAO_ATIVO.equals(inSituacao) && !SITUACAO_INATIVO.equals(inSituacao)) {
            inSituacao = SITUACAO_ATIVO; // valor padrão
        }
    }

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

    public boolean iseConselheiro() {
        return eConselheiro;
    }

    public void seteConselheiro(boolean eConselheiro) {
        this.eConselheiro = eConselheiro;
    }

    public Integer getCrm() {
        return crm;
    }

    public void setCrm(Integer crm) {
        this.crm = crm;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        Usuario usuario = (Usuario) o;
        return Objects.equals(idUsuarioPessoa, usuario.idUsuarioPessoa);
    }

    @Override
    public int hashCode() {
        return Objects.hash(idUsuarioPessoa);
    }

    @Override
    public String toString() {
        return "Usuario{" +
                "id=" + idUsuarioPessoa +
                ", nome=" + (pessoa != null ? pessoa.getNome() : "null") +
                ", situacao=" + inSituacao +
                '}';
    }
}