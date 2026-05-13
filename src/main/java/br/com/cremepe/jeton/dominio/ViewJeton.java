package br.com.cremepe.jeton.dominio;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.Immutable;
import java.io.Serializable;
import java.math.BigDecimal;

@Entity
@Immutable
@Table(name = "vw_jeton")
public class ViewJeton implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "idJeton")
    private Integer idJeton;

    private String nome;
    private String cpf;
    private Integer mes;
    private String mesExtenso;
    private Integer ano;
    private Integer totalJeton;
    private BigDecimal valor;
    private String valorFormatado;
    private String inSituacaoDescricao;
    private String nomeGestao;

    public ViewJeton() {}

    // ==========================================
    // APENAS GETTERS (Entidade Imutável)
    // ==========================================

    public Integer getIdJeton() { return idJeton; }
    public String getNome() { return nome; }
    public String getCpf() { return cpf; }
    public Integer getMes() { return mes; }
    public String getMesExtenso() { return mesExtenso; }
    public Integer getAno() { return ano; }
    public Integer getTotalJeton() { return totalJeton; }
    public BigDecimal getValor() { return valor; }
    public String getValorFormatado() { return valorFormatado; }
    public String getInSituacaoDescricao() { return inSituacaoDescricao; }
    public String getNomeGestao() { return nomeGestao; }
}