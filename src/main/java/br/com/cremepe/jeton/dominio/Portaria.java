package br.com.cremepe.jeton.dominio;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.time.LocalDate;
import java.util.Objects;

/**
 * Entidade JPA que representa a tabela 'portaria'.
 * Armazena a base legal e os links de publicação.
 */
@Entity
@Table(name = "portaria")
public class Portaria implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "idPortaria")
    private Integer idPortaria;

    @Column(name = "numero", nullable = false)
    private Integer numero;

    @Column(name = "ano", nullable = false)
    private Integer ano;

    @Column(name = "dtInicioVigencia")
    private LocalDate dtInicioVigencia;

    @Column(name = "dtFimVigencia")
    private LocalDate dtFimVigencia;

    @Column(name = "linkPublicado", length = 100)
    private String linkPublicado;

    @Column(name = "inRevogado", nullable = false, length = 1)
    private String inRevogado;

    // Construtores
    public Portaria() {
    }

    // Getters e Setters
    public Integer getIdPortaria() {
        return idPortaria;
    }

    public void setIdPortaria(Integer idPortaria) {
        this.idPortaria = idPortaria;
    }

    public Integer getNumero() {
        return numero;
    }

    public void setNumero(Integer numero) {
        this.numero = numero;
    }

    public Integer getAno() {
        return ano;
    }

    public void setAno(Integer ano) {
        this.ano = ano;
    }

    public LocalDate getDtInicioVigencia() {
        return dtInicioVigencia;
    }

    public void setDtInicioVigencia(LocalDate dtInicioVigencia) {
        this.dtInicioVigencia = dtInicioVigencia;
    }

    public LocalDate getDtFimVigencia() {
        return dtFimVigencia;
    }

    public void setDtFimVigencia(LocalDate dtFimVigencia) {
        this.dtFimVigencia = dtFimVigencia;
    }

    public String getLinkPublicado() {
        return linkPublicado;
    }

    public void setLinkPublicado(String linkPublicado) {
        this.linkPublicado = linkPublicado;
    }

    public String getInRevogado() {
        return inRevogado;
    }

    public void setInRevogado(String inRevogado) {
        this.inRevogado = inRevogado;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Portaria portaria = (Portaria) o;
        return Objects.equals(idPortaria, portaria.idPortaria);
    }

    @Override
    public int hashCode() {
        return Objects.hash(idPortaria);
    }
}