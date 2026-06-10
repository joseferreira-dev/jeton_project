package br.com.cremepe.jeton.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.io.Serializable;
import java.util.Objects;

@Entity
@Table(name = "tipo_anexo")
public class TipoAnexo implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final String EXIGE_PUBLICACAO_SIM = "S";
    public static final String EXIGE_PUBLICACAO_NAO = "N";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "idTipo")
    private Integer idTipo;

    @NotNull(message = "O nome do tipo de anexo é obrigatório")
    @Size(min = 3, max = 50, message = "O nome deve ter entre 3 e 50 caracteres")
    @Column(name = "nome", length = 50, nullable = false, unique = true)
    private String nome;

    @NotNull(message = "Indicador de exigência de publicação é obrigatório")
    @Pattern(regexp = "[SN]", message = "exigePublicacao deve ser S ou N")
    @Column(name = "exigePublicacao", nullable = false, length = 1)
    private String exigePublicacao;

    public TipoAnexo() {
    }

    public boolean isExigePublicacao() {
        return EXIGE_PUBLICACAO_SIM.equals(exigePublicacao);
    }

    public boolean isNaoExigePublicacao() {
        return EXIGE_PUBLICACAO_NAO.equals(exigePublicacao);
    }

    @PrePersist
    @PreUpdate
    protected void normalize() {
        if (nome != null) {
            nome = nome.trim();
        }
        if (exigePublicacao != null) {
            exigePublicacao = exigePublicacao.toUpperCase();
        }
        // Garante valor padrão 'N' caso venha inválido
        if (!EXIGE_PUBLICACAO_SIM.equals(exigePublicacao) && !EXIGE_PUBLICACAO_NAO.equals(exigePublicacao)) {
            exigePublicacao = EXIGE_PUBLICACAO_NAO;
        }
    }

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
        TipoAnexo that = (TipoAnexo) o;
        return Objects.equals(idTipo, that.idTipo);
    }

    @Override
    public int hashCode() {
        return Objects.hash(idTipo);
    }

    @Override
    public String toString() {
        return "TipoAnexo{" +
                "id=" + idTipo +
                ", nome='" + nome + '\'' +
                ", exigePublicacao=" + exigePublicacao +
                '}';
    }
}