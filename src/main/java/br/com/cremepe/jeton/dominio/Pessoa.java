package br.com.cremepe.jeton.dominio;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.Table;

/**
 * ==================================================================
 * Entidade base que mapeia a tabela 'pessoa' no banco.
 * ==================================================================
 */
@Entity
@Table(name = "pessoa")
// O PULO DO GATO: InheritanceType.JOINED diz ao JPA que as classes filhas 
// terão suas próprias tabelas interligadas por chaves estrangeiras.
@Inheritance(strategy = InheritanceType.JOINED) 
public class Pessoa {
    
    @Id // Marca este campo como a Chave Primária
    @GeneratedValue(strategy = GenerationType.IDENTITY) // O banco gerará o ID automaticamente (auto-incremento)
    @Column(name = "idPessoa") // Nome exato da coluna no seu MySQL
    private int idPessoa;
    
    @Column(name = "nome")
    private String nome;
    
    @Column(name = "cpf")
    private String cpf;
    
    @Column(name = "email")
    private String email;
    
    @Column(name = "inTipoPessoa")
    private String inTipoPessoa;

    // Construtor vazio exigido pelo JPA
    public Pessoa() {
    }
    
    public Pessoa(int idPessoa, String nome, String cpf, String email, String inTipoPessoa){
        this.idPessoa = idPessoa;
        this.nome = nome;
        this.cpf = cpf;
        this.email = email;
        this.inTipoPessoa = inTipoPessoa;
    }
    
    // ==============================================================
    // Getters e Setters
    // ==============================================================
    
    public int getIdPessoa() { return idPessoa; }
    public void setIdPessoa(int idPessoa) { this.idPessoa = idPessoa; }

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }

    public String getCpf() { return cpf; }
    public void setCpf(String cpf) { this.cpf = cpf; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getInTipoPessoa() { return inTipoPessoa; }
    public void setInTipoPessoa(String inTipoPessoa) { this.inTipoPessoa = inTipoPessoa; }
}