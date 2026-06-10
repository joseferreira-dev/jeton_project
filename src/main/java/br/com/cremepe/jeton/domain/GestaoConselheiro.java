package br.com.cremepe.jeton.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.Objects;

@Entity
@Table(name = "gestao_conselheiro")
public class GestaoConselheiro implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final String SITUACAO_ATIVO = "A";
    public static final String SITUACAO_INATIVO = "I";

    @EmbeddedId
    private GestaoConselheiroId id = new GestaoConselheiroId();

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("idGestao")
    @JoinColumn(name = "idGestao")
    private Gestao gestao;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("idPessoa")
    @JoinColumn(name = "idPessoa")
    private Conselheiro conselheiro;

    @NotNull
    @Column(name = "inSituacao", length = 1, nullable = false)
    private String inSituacao;

    public GestaoConselheiro() {
    }

    public boolean isAtivo() {
        return SITUACAO_ATIVO.equals(inSituacao);
    }

    public boolean isInativo() {
        return SITUACAO_INATIVO.equals(inSituacao);
    }

    @PrePersist
    @PreUpdate
    protected void normalize() {
        if (inSituacao != null) {
            inSituacao = inSituacao.toUpperCase();
        }
        if (!SITUACAO_ATIVO.equals(inSituacao) && !SITUACAO_INATIVO.equals(inSituacao)) {
            inSituacao = SITUACAO_INATIVO; // valor padrão seguro
        }
    }

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
        if (gestao != null && gestao.getIdGestao() != null) {
            this.id.setIdGestao(gestao.getIdGestao());
        }
    }

    public Conselheiro getConselheiro() {
        return conselheiro;
    }

    public void setConselheiro(Conselheiro conselheiro) {
        this.conselheiro = conselheiro;
        if (conselheiro != null && conselheiro.getIdPessoa() != null) {
            this.id.setIdPessoa(conselheiro.getIdPessoa());
        }
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

    @Override
    public String toString() {
        return "GestaoConselheiro{" +
                "id=" + id +
                ", situacao='" + inSituacao + '\'' +
                '}';
    }
}