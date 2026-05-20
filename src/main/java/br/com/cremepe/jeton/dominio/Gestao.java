package br.com.cremepe.jeton.dominio;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "gestao")
public class Gestao implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "idGestao")
    private Integer idGestao;

    @Column(name = "nomeGestao", length = 70, nullable = false)
    private String nomeGestao;

    @Column(name = "dtInicio", nullable = false)
    private LocalDate dtInicio;

    @Column(name = "dtFim", nullable = false)
    private LocalDate dtFim;

    public Gestao() {
    }

    public Integer getIdGestao() {
        return idGestao;
    }

    public void setIdGestao(Integer idGestao) {
        this.idGestao = idGestao;
    }

    public String getNomeGestao() {
        return nomeGestao;
    }

    public void setNomeGestao(String nomeGestao) {
        this.nomeGestao = nomeGestao;
    }

    public LocalDate getDtInicio() {
        return dtInicio;
    }

    public void setDtInicio(LocalDate dtInicio) {
        this.dtInicio = dtInicio;
    }

    public LocalDate getDtFim() {
        return dtFim;
    }

    public void setDtFim(LocalDate dtFim) {
        this.dtFim = dtFim;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        Gestao gestao = (Gestao) o;
        return Objects.equals(idGestao, gestao.idGestao);
    }

    @Override
    public int hashCode() {
        return Objects.hash(idGestao);
    }
}