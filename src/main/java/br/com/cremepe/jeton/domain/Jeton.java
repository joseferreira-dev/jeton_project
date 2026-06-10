package br.com.cremepe.jeton.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Entity
@Table(name = "jeton")
public class Jeton implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final String SITUACAO_ATIVO = "A";
    public static final String SITUACAO_PAGO = "P";
    public static final String SITUACAO_EXCLUIDO = "E";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "idJeton")
    private Integer idJeton;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "idGestao", nullable = false)
    private Gestao gestao;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "idPessoa", nullable = false)
    private Conselheiro conselheiro;

    @NotNull
    @Positive(message = "Mês deve ser entre 1 e 12")
    @Column(name = "mes", nullable = false)
    private Integer mes;

    @NotNull
    @Positive(message = "Ano deve ser positivo")
    @Column(name = "ano", nullable = false)
    private Integer ano;

    @NotNull
    @PositiveOrZero(message = "Total de Jetons não pode ser negativo")
    @Column(name = "totalJeton", nullable = false)
    private Integer totalJeton;

    @NotNull
    @PositiveOrZero(message = "Valor não pode ser negativo")
    @Column(name = "valor", precision = 10, scale = 2, nullable = false)
    private BigDecimal valor;

    @NotNull
    @Column(name = "inSituacao", length = 1, nullable = false)
    private String inSituacao;

    @ManyToMany
    @JoinTable(name = "jeton_atividade", joinColumns = @JoinColumn(name = "idJeton"), inverseJoinColumns = @JoinColumn(name = "idAtividade"))
    private List<AtividadeConselhal> atividades = new ArrayList<>();

    public Jeton() {
    }

    public boolean isAtivo() {
        return SITUACAO_ATIVO.equals(inSituacao);
    }

    public boolean isPago() {
        return SITUACAO_PAGO.equals(inSituacao);
    }

    public boolean isExcluido() {
        return SITUACAO_EXCLUIDO.equals(inSituacao);
    }

    @PrePersist
    @PreUpdate
    protected void normalize() {
        if (inSituacao != null) {
            inSituacao = inSituacao.toUpperCase();
        }
        if (!SITUACAO_ATIVO.equals(inSituacao) &&
                !SITUACAO_PAGO.equals(inSituacao) &&
                !SITUACAO_EXCLUIDO.equals(inSituacao)) {
            inSituacao = SITUACAO_ATIVO;
        }
        if (totalJeton == null)
            totalJeton = 0;
        if (valor == null)
            valor = BigDecimal.ZERO;
    }

    public Integer getIdJeton() {
        return idJeton;
    }

    public void setIdJeton(Integer idJeton) {
        this.idJeton = idJeton;
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

    public Integer getTotalJeton() {
        return totalJeton;
    }

    public void setTotalJeton(Integer totalJeton) {
        this.totalJeton = totalJeton;
    }

    public BigDecimal getValor() {
        return valor;
    }

    public void setValor(BigDecimal valor) {
        this.valor = valor;
    }

    public String getInSituacao() {
        return inSituacao;
    }

    public void setInSituacao(String inSituacao) {
        this.inSituacao = inSituacao;
    }

    public List<AtividadeConselhal> getAtividades() {
        return atividades;
    }

    public void setAtividades(List<AtividadeConselhal> atividades) {
        this.atividades = atividades;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        Jeton jeton = (Jeton) o;
        return Objects.equals(idJeton, jeton.idJeton);
    }

    @Override
    public int hashCode() {
        return Objects.hash(idJeton);
    }

    @Override
    public String toString() {
        return "Jeton{" +
                "id=" + idJeton +
                ", mes=" + mes +
                ", ano=" + ano +
                ", totalJeton=" + totalJeton +
                ", valor=" + valor +
                ", situacao=" + inSituacao +
                '}';
    }
}