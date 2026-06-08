package br.com.cremepe.jeton.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.io.Serializable;
import java.util.Objects;

/**
 * Entidade JPA que representa a tabela 'pessoa' no banco de dados.
 * Armazena dados comuns a conselheiros (C) e funcionários (F).
 */
@Entity
@Table(name = "pessoa")
public class Pessoa implements Serializable {

    private static final long serialVersionUID = 1L;

    // =========================================================================
    // CONSTANTES PÚBLICAS PARA O TIPO DE PESSOA
    // =========================================================================
    public static final String TIPO_FUNCIONARIO = "F";
    public static final String TIPO_CONSELHEIRO = "C";

    // =========================================================================
    // CAMPOS DA ENTIDADE
    // =========================================================================
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "idPessoa")
    private Integer idPessoa;

    @NotNull
    @Size(min = 3, max = 70)
    @Column(name = "nome", length = 70, nullable = false)
    private String nome;

    @NotNull
    @Size(min = 11, max = 11)
    @Pattern(regexp = "\\d{11}", message = "CPF deve conter exatamente 11 dígitos numéricos")
    @Column(name = "cpf", length = 11, nullable = false, unique = true)
    private String cpf;

    @NotNull
    @Email
    @Size(max = 70)
    @Column(name = "email", length = 70, nullable = false)
    private String email;

    @NotNull
    @Size(min = 1, max = 1)
    @Pattern(regexp = "[FC]", message = "Tipo de pessoa deve ser 'F' (Funcionário) ou 'C' (Conselheiro)")
    @Column(name = "inTipoPessoa", length = 1, nullable = false)
    private String inTipoPessoa;

    // =========================================================================
    // CONSTRUTORES
    // =========================================================================
    public Pessoa() {
        // Construtor padrão exigido pelo JPA
    }

    // =========================================================================
    // MÉTODOS DE CONVENIÊNCIA
    // =========================================================================
    public boolean isFuncionario() {
        return TIPO_FUNCIONARIO.equals(inTipoPessoa);
    }

    public boolean isConselheiro() {
        return TIPO_CONSELHEIRO.equals(inTipoPessoa);
    }

    // =========================================================================
    // JPA LIFECYCLE – NORMALIZAÇÃO AUTOMÁTICA
    // =========================================================================
    @PrePersist
    @PreUpdate
    protected void normalize() {
        if (nome != null)
            nome = nome.trim();
        if (email != null)
            email = email.trim().toLowerCase();
        if (cpf != null)
            cpf = cpf.replaceAll("[^0-9]", "");
        if (inTipoPessoa != null)
            inTipoPessoa = inTipoPessoa.toUpperCase();
    }

    // =========================================================================
    // GETTERS E SETTERS
    // =========================================================================
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

    public String getInTipoPessoa() {
        return inTipoPessoa;
    }

    public void setInTipoPessoa(String inTipoPessoa) {
        this.inTipoPessoa = inTipoPessoa;
    }

    // =========================================================================
    // EQUALS & HASHCODE
    // =========================================================================
    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        Pessoa pessoa = (Pessoa) o;
        return Objects.equals(idPessoa, pessoa.idPessoa);
    }

    @Override
    public int hashCode() {
        return Objects.hash(idPessoa);
    }

    // =========================================================================
    // TO_STRING
    // =========================================================================
    @Override
    public String toString() {
        return "Pessoa{" +
                "id=" + idPessoa +
                ", nome='" + nome + '\'' +
                ", cpf='***'" + // CPF omitido por segurança
                ", email='" + email + '\'' +
                ", tipo=" + inTipoPessoa +
                '}';
    }
}