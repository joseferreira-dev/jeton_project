package br.com.cremepe.jeton.dominio;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.util.Objects;

/**
 * Entidade JPA que representa a tabela associativa 'gestao_conselheiro'.
 * Resolve o relacionamento M:N entre Gestao e Conselheiro, incluindo dados
 * extras.
 */
@Entity
@Table(name = "gestao_conselheiro")
public class GestaoConselheiro implements Serializable {

    private static final long serialVersionUID = 1L;

    // A anotação @EmbeddedId avisa que a PK desta entidade está noutra classe
    @EmbeddedId
    private GestaoConselheiroId id = new GestaoConselheiroId();

    // Relacionamento com a Gestao
    @ManyToOne
    @MapsId("idGestao") // Mapeia para o atributo 'idGestao' dentro de GestaoConselheiroId
    @JoinColumn(name = "idGestao")
    private Gestao gestao;

    // Relacionamento com o Conselheiro (utilizamos o Conselheiro, cujo ID é o
    // idPessoa)
    @ManyToOne
    @MapsId("idPessoa") // Mapeia para o atributo 'idPessoa' dentro de GestaoConselheiroId
    @JoinColumn(name = "idPessoa")
    private Conselheiro conselheiro;

    @Column(name = "inSituacao", length = 1, nullable = false)
    private String inSituacao;

    public GestaoConselheiro() {
    }

    // ==========================================
    // GETTERS E SETTERS
    // ==========================================

    public GestaoConselheiroId getId() {
        return id;
    }

    public void setId(GestaoConselheiroId id) {
        this.id = id;
    }

    public Gestao getGestao() {
        return gestao;
    }

    public void setGestao(Gestao gestao) {
        this.gestao = gestao;
    }

    public Conselheiro getConselheiro() {
        return conselheiro;
    }

    public void setConselheiro(Conselheiro conselheiro) {
        this.conselheiro = conselheiro;
    }

    public String getInSituacao() {
        return inSituacao;
    }

    public void setInSituacao(String inSituacao) {
        this.inSituacao = inSituacao;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        GestaoConselheiro that = (GestaoConselheiro) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}