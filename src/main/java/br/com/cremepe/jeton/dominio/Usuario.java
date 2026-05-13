package br.com.cremepe.jeton.dominio;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

/**
 * ==================================================================
 * ARQUIVO: src/main/java/br/com/cremepe/jeton/dominio/Usuario.java
 * OBJETIVO: Entidade filha que mapeia a tabela 'usuario'.
 * ==================================================================
 */
@Entity
@Table(name = "usuario")
// Aqui ensinamos ao JPA que a coluna 'idUsuarioPessoa' desta tabela 
// se liga com a chave primária da tabela 'pessoa'.
@PrimaryKeyJoinColumn(name = "idUsuarioPessoa") 
public class Usuario extends Pessoa {

    @Column(name = "senha")
    private String senha;

    @Column(name = "inSituacao") 
    private String inSituacao;

    // O @Transient indica que esse campo "inSituacaoDescricao" NÃO existe 
    // como coluna na tabela do banco de dados (ele era gerado no seu select com um 'case when')
    @Transient 
    private String inSituacaoDescricao;

    // Percebi que você tem 'permissoes' na sua versão original. Se ela existir na tabela,
    // remova as "//". Se não, podemos apagar.
    // @Column(name = "permissoes")
    // private String permissoes;
    
    public Usuario() {
    }

    public Usuario(int idPessoa, String nome, String cpf, String email, String inTipoPessoa,
                   String senha, String inSituacao, String inSituacaoDescricao) {
        super(idPessoa, nome, cpf, email, inTipoPessoa);
        this.senha = senha;
        this.inSituacao = inSituacao;
        this.inSituacaoDescricao = inSituacaoDescricao;
    }
    
    // ==============================================================
    // Getters e Setters
    // ==============================================================
    
    public String getSenha() { return senha; }
    public void setSenha(String senha) { this.senha = senha; }

    public String getInSituacao() { return inSituacao; }
    public void setInSituacao(String inSituacao) { this.inSituacao = inSituacao; }

    public String getInSituacaoDescricao() {
        // Agora fazemos a lógica da descrição direto no Java, sem precisar de SQL!
        if ("A".equals(this.inSituacao)) return "Ativo";
        if ("I".equals(this.inSituacao)) return "Inativo";
        return inSituacaoDescricao;
    }
    public void setInSituacaoDescricao(String inSituacaoDescricao) { this.inSituacaoDescricao = inSituacaoDescricao; }
}