package br.com.cremepe.jeton.dominio;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.io.Serializable;

/**
 * Entidade para configurações globais do sistema.
 */
@Entity
@Table(name = "parametros")
public class Parametros implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id // Transformamos o próprio status no ID para satisfazer o JPA
    @Column(name = "bloqueaSistema", length = 1, nullable = false)
    private String bloqueaSistema;

    public Parametros() {}

    public String getBloqueaSistema() {
        return bloqueaSistema;
    }

    public void setBloqueaSistema(String bloqueaSistema) {
        this.bloqueaSistema = bloqueaSistema;
    }
}