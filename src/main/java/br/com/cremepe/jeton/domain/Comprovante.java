package br.com.cremepe.jeton.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
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

    // =========================================================================
    // CONSTANTES PÚBLICAS (para uso em outros serviços)
    // =========================================================================
    public static final String CONTENT_TYPE_PDF = "application/pdf";
    public static final String CONTENT_TYPE_IMAGE = "image/jpeg";
    public static final String CONTENT_TYPE_FALLBACK = "application/octet-stream";

    // =========================================================================
    // CAMPOS DA ENTIDADE
    // =========================================================================
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "idComprovante")
    private Integer idComprovante;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "idTipo", nullable = false)
    private TipoAnexo tipoAnexo;

    @NotNull
    @Size(max = 70)
    @Column(name = "nomeComprovante", length = 70, nullable = false)
    private String nomeComprovante;

    @NotNull
    @Size(max = 70)
    @Column(name = "nomeArquivo", length = 70, nullable = false)
    private String nomeArquivo;

    @NotNull
    @Size(max = 20)
    @Pattern(regexp = "^(application/pdf|image/jpeg|image/png|application/octet-stream)$", message = "Tipo de conteúdo inválido")
    @Column(name = "contentType", length = 20, nullable = false)
    private String contentType;

    @NotNull
    @Column(name = "mes", nullable = false)
    private Integer mes;

    @NotNull
    @Column(name = "ano", nullable = false)
    private Integer ano;

    // =========================================================================
    // CONSTRUTORES
    // =========================================================================
    public Comprovante() {
        // Construtor padrão exigido pelo JPA
    }

    // =========================================================================
    // GETTERS E SETTERS
    // =========================================================================
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

    // =========================================================================
    // EQUALS & HASHCODE (baseado apenas no ID)
    // =========================================================================
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

    // =========================================================================
    // TO_STRING
    // =========================================================================
    @Override
    public String toString() {
        return "Comprovante{" +
                "id=" + idComprovante +
                ", nome='" + nomeComprovante + '\'' +
                ", arquivo='" + nomeArquivo + '\'' +
                ", tipo=" + (tipoAnexo != null ? tipoAnexo.getNome() : null) +
                ", mes=" + mes +
                ", ano=" + ano +
                '}';
    }
}