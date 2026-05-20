package br.com.cremepe.jeton.dominio;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.util.Objects;

/**
 * Entidade que armazena as referências aos ficheiros de comprovação das
 * atividades.
 */
@Entity
@Table(name = "comprovante")
public class Comprovante implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "idComprovante")
    private Integer idComprovante;

    // Relacionamento obrigatório com o tipo de anexo
    @ManyToOne
    @JoinColumn(name = "idTipo", nullable = false)
    private TipoAnexo tipoAnexo;

    @Column(name = "nomeComprovante", length = 70, nullable = false, unique = true)
    private String nomeComprovante;

    @Column(name = "nomeArquivo", length = 70, nullable = false)
    private String nomeArquivo;

    @Column(name = "contentType", length = 20, nullable = false)
    private String contentType;

    @Column(name = "mes", nullable = false)
    private Integer mes;

    @Column(name = "ano", nullable = false)
    private Integer ano;

    public Comprovante() {
    }

    // ==========================================
    // GETTERS E SETTERS
    // ==========================================

    public Integer getIdComprovante() {
        return idComprovante;
    }

    public void setIdComprovante(Integer idComprovante) {
        this.idComprovante = idComprovante;
    }

    public TipoAnexo getTipoAnexo() {
        return tipoAnexo;
    }

    public void setTipoAnexo(TipoAnexo tipoAnexo) {
        this.tipoAnexo = tipoAnexo;
    }

    public String getNomeComprovante() {
        return nomeComprovante;
    }

    public void setNomeComprovante(String nomeComprovante) {
        this.nomeComprovante = nomeComprovante;
    }

    public String getNomeArquivo() {
        return nomeArquivo;
    }

    public void setNomeArquivo(String nomeArquivo) {
        this.nomeArquivo = nomeArquivo;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public Integer getMes() {
        return mes;
    }

    public void setMes(Integer mes) {
        this.mes = mes;
    }

    public Integer getAno() {
        return ano;
    }

    public void setAno(Integer ano) {
        this.ano = ano;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        Comprovante that = (Comprovante) o;
        return Objects.equals(idComprovante, that.idComprovante);
    }

    @Override
    public int hashCode() {
        return Objects.hash(idComprovante);
    }
}