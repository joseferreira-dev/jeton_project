package br.com.cremepe.jeton.domain;

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

    @Column(name = "nome")
    private String nome;

    @Column(name = "cpf")
    private String cpf;

    @Column(name = "mes")
    private Integer mes;

    @Column(name = "mesExtenso")
    private String mesExtenso;

    @Column(name = "ano")
    private Integer ano;

    @Column(name = "totalJeton")
    private Integer totalJeton;

    @Column(name = "valor")
    private BigDecimal valor;

    @Column(name = "valorFormatado")
    private String valorFormatado;

    @Column(name = "inSituacaoDescricao")
    private String inSituacaoDescricao;

    @Column(name = "nomeGestao")
    private String nomeGestao;

    public ViewJeton() {
    }

    // =========================================================================
    // GETTERS (imutável, sem setters)
    // =========================================================================
    public Integer getIdJeton() {
        return idJeton;
    }

    public String getNome() {
        return nome;
    }

    public String getCpf() {
        return cpf;
    }

    public Integer getMes() {
        return mes;
    }

    public String getMesExtenso() {
        return mesExtenso;
    }

    public Integer getAno() {
        return ano;
    }

    public Integer getTotalJeton() {
        return totalJeton;
    }

    public BigDecimal getValor() {
        return valor;
    }

    public String getValorFormatado() {
        return valorFormatado;
    }

    public String getInSituacaoDescricao() {
        return inSituacaoDescricao;
    }

    public String getNomeGestao() {
        return nomeGestao;
    }
}