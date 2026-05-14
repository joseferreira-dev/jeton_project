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
 * Entidade JPA que representa a tabela 'conselheiro' na base de dados.
 * Possui um relacionamento 1:1 com a entidade Pessoa.
 */
@Entity
@Table(name = "conselheiro")
public class Conselheiro implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "idPessoa")
    private Integer idPessoa;

    // Relacionamento mapeado indicando que o ID deriva da entidade Pessoa
    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
    @MapsId
    @JoinColumn(name = "idPessoa")
    private Pessoa pessoa;

    // Utiliza-se Integer (objeto) em vez de int (primitivo) porque a base de dados permite NULL
    @Column(name = "crm")
    private Integer crm;

    @Transient
    private String senhaAcesso;

    @Column(name = "inSituacao", length = 1, nullable = false)
    private String inSituacao;

    public Conselheiro() {
    }

    // ==========================================
    // GETTERS E SETTERS
    // ==========================================

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
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Conselheiro conselheiro = (Conselheiro) o;
        return Objects.equals(idPessoa, conselheiro.idPessoa);
    }

    @Override
    public int hashCode() {
        return Objects.hash(idPessoa);
    }
}