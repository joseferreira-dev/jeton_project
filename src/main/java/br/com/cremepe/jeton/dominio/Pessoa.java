package br.com.cremepe.jeton.dominio;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.util.Objects;

/**
 * Entidade JPA que representa a tabela 'pessoa' no banco de dados.
 */
@Entity
@Table(name = "pessoa")
public class Pessoa implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // O MySQL gerencia o Auto-Incremento
    @Column(name = "idPessoa")
    private Integer idPessoa;

    @Column(name = "nome", length = 70, nullable = false)
    private String nome;

    // O parâmetro unique = true garante a restrição 'UNI' mapeada no banco
    @Column(name = "cpf", length = 11, nullable = false, unique = true)
    private String cpf;

    @Column(name = "email", length = 70, nullable = false)
    private String email;

    @Column(name = "inTipoPessoa", length = 1, nullable = false)
    private String inTipoPessoa;

    // Construtor vazio obrigatório para o JPA
    public Pessoa() {
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

    // HashCode e Equals baseados no ID (Recomendação forte para Entidades JPA)
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Pessoa pessoa = (Pessoa) o;
        return Objects.equals(idPessoa, pessoa.idPessoa);
    }

    @Override
    public int hashCode() {
        return Objects.hash(idPessoa);
    }
}