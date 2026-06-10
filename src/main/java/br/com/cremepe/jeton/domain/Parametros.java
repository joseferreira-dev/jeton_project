package br.com.cremepe.jeton.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.io.Serializable;

@Entity
@Table(name = "parametros")
public class Parametros implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    private Integer id = 1;

    @Column(name = "bloqueaSistema", length = 1, nullable = false)
    private String bloqueaSistema;

    public Parametros() {
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getBloqueaSistema() {
        return bloqueaSistema;
    }

    public void setBloqueaSistema(String bloqueaSistema) {
        this.bloqueaSistema = bloqueaSistema;
    }
}