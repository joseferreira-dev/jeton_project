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
 * Entidade que define as categorias de anexos permitidas no sistema.
 */
@Entity
@Table(name = "tipo_anexo")
public class TipoAnexo implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "idTipo")
    private Integer idTipo;

    @Column(name = "nome", length = 50, nullable = false, unique = true)
    private String nome;

    @Column(name = "exigePublicacao", length = 1, nullable = false)
    private String exigePublicacao;

    public TipoAnexo() {
    }

    // ==========================================
    // GETTERS E SETTERS
    // ==========================================

    public Integer getIdTipo() {
        return idTipo;
    }

    public void setIdTipo(Integer idTipo) {
        this.idTipo = idTipo;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getExigePublicacao() {
        return exigePublicacao;
    }

    public void setExigePublicacao(String exigePublicacao) {
        this.exigePublicacao = exigePublicacao;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        TipoAnexo tipoAnexo = (TipoAnexo) o;
        return Objects.equals(idTipo, tipoAnexo.idTipo);
    }

    @Override
    public int hashCode() {
        return Objects.hash(idTipo);
    }
}