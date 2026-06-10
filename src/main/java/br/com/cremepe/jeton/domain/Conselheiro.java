package br.com.cremepe.jeton.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnore;

@Entity
@Table(name = "conselheiro")
public class Conselheiro implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final String SITUACAO_ATIVO = "A";
    public static final String SITUACAO_INATIVO = "I";

    @Id
    @Column(name = "idPessoa")
    private Integer idPessoa;

    @NotNull
    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "idPessoa")
    private Pessoa pessoa;

    @Column(name = "crm")
    private Integer crm;

    @Transient
    @JsonIgnore
    private String senhaAcesso;

    @NotNull
    @Column(name = "inSituacao", length = 1, nullable = false)
    private String inSituacao;

    public Conselheiro() {
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
        if (inSituacao != null)
            inSituacao = inSituacao.toUpperCase();
        // Garante que a situação seja sempre A ou I
        if (!SITUACAO_ATIVO.equals(inSituacao) && !SITUACAO_INATIVO.equals(inSituacao)) {
            inSituacao = SITUACAO_ATIVO;
        }
    }

    public Integer getIdPessoa() {
        return idPessoa;
    }

    public void setIdPessoa(Integer idPessoa) {
        this.idPessoa = idPessoa;
    }

    public Pessoa getPessoa() {
        return pessoa;
    }

    public void setPessoa(Pessoa pessoa) {
        this.pessoa = pessoa;
    }

    public Integer getCrm() {
        return crm;
    }

    public void setCrm(Integer crm) {
        this.crm = crm;
    }

    public String getSenhaAcesso() {
        return senhaAcesso;
    }

    public void setSenhaAcesso(String senhaAcesso) {
        this.senhaAcesso = senhaAcesso;
    }

    public String getInSituacao() {
        return inSituacao;
    }

    public void setInSituacao(String inSituacao) {
        this.inSituacao = inSituacao;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        Conselheiro that = (Conselheiro) o;
        return Objects.equals(idPessoa, that.idPessoa);
    }

    @Override
    public int hashCode() {
        return Objects.hash(idPessoa);
    }

    @Override
    public String toString() {
        return "Conselheiro{" +
                "id=" + idPessoa +
                ", nome=" + (pessoa != null ? pessoa.getNome() : "null") +
                ", crm=" + crm +
                ", situacao=" + inSituacao +
                '}';
    }
}