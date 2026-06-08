package br.com.cremepe.jeton.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "pontos_saldo")
public class PontosSaldo implements Serializable {

    private static final long serialVersionUID = 1L;

    // =========================================================================
    // CONSTANTES PARA inSituacao (valores do banco)
    // =========================================================================
    public static final String SITUACAO_ATIVO = "A";
    public static final String SITUACAO_INATIVO = "I";
    public static final String SITUACAO_PENDENTE = "P";
    public static final String SITUACAO_UTILIZADO = "U";
    public static final String SITUACAO_EXCLUIDO = "E";

    // =========================================================================
    // CAMPOS DA ENTIDADE
    // =========================================================================
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "idPontosSaldo")
    private Integer idPontosSaldo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "idAtividade")
    private AtividadeConselhal atividade;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "idPessoa")
    private Conselheiro conselheiro;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "idJeton")
    private Jeton jeton;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "idGestao")
    private Gestao gestao;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "idResolucao")
    private Resolucao resolucao;

    @NotNull
    @Column(name = "dataHora", nullable = false)
    private LocalDateTime dataHora;

    @NotNull
    @PositiveOrZero
    @Column(name = "pontosTrabalhados", nullable = false)
    private Integer pontosTrabalhados = 0;

    @NotNull
    @PositiveOrZero
    @Column(name = "pontosUtilizados", nullable = false)
    private Integer pontosUtilizados = 0;

    @NotNull
    @PositiveOrZero
    @Column(name = "pontosSobrando", nullable = false)
    private Integer pontosSobrando = 0;

    @NotNull
    @Pattern(regexp = "[AIPUE]", message = "inSituacao deve ser A, I, P, U ou E")
    @Column(name = "inSituacao", nullable = false, length = 1)
    private String inSituacao;

    // =========================================================================
    // CONSTRUTORES
    // =========================================================================
    public PontosSaldo() {
    }

    // =========================================================================
    // MÉTODOS DE CONVENIÊNCIA
    // =========================================================================
    public boolean isAtivo() {
        return SITUACAO_ATIVO.equals(inSituacao);
    }

    public boolean isInativo() {
        return SITUACAO_INATIVO.equals(inSituacao);
    }

    public boolean isPendente() {
        return SITUACAO_PENDENTE.equals(inSituacao);
    }

    public boolean isUtilizado() {
        return SITUACAO_UTILIZADO.equals(inSituacao);
    }

    public boolean isExcluido() {
        return SITUACAO_EXCLUIDO.equals(inSituacao);
    }

    public boolean hasSaldoDisponivel() {
        return isAtivo() && pontosSobrando != null && pontosSobrando > 0;
    }

    // =========================================================================
    // JPA LIFECYCLE – NORMALIZAÇÃO
    // =========================================================================
    @PrePersist
    @PreUpdate
    protected void normalize() {
        if (inSituacao != null) {
            inSituacao = inSituacao.toUpperCase();
        }
        // Garante que valores padrão sejam consistentes
        if (!SITUACAO_ATIVO.equals(inSituacao) && !SITUACAO_INATIVO.equals(inSituacao) &&
                !SITUACAO_PENDENTE.equals(inSituacao) && !SITUACAO_UTILIZADO.equals(inSituacao) &&
                !SITUACAO_EXCLUIDO.equals(inSituacao)) {
            inSituacao = SITUACAO_ATIVO;
        }
        if (pontosTrabalhados == null)
            pontosTrabalhados = 0;
        if (pontosUtilizados == null)
            pontosUtilizados = 0;
        if (pontosSobrando == null)
            pontosSobrando = 0;
    }

    // =========================================================================
    // GETTERS E SETTERS
    // =========================================================================
    public Integer getIdPontosSaldo() {
        return idPontosSaldo;
    }

    public void setIdPontosSaldo(Integer idPontosSaldo) {
        this.idPontosSaldo = idPontosSaldo;
    }

    public AtividadeConselhal getAtividade() {
        return atividade;
    }

    public void setAtividade(AtividadeConselhal atividade) {
        this.atividade = atividade;
    }

    public Conselheiro getConselheiro() {
        return conselheiro;
    }

    public void setConselheiro(Conselheiro conselheiro) {
        this.conselheiro = conselheiro;
    }

    public Jeton getJeton() {
        return jeton;
    }

    public void setJeton(Jeton jeton) {
        this.jeton = jeton;
    }

    public Gestao getGestao() {
        return gestao;
    }

    public void setGestao(Gestao gestao) {
        this.gestao = gestao;
    }

    public Resolucao getResolucao() {
        return resolucao;
    }

    public void setResolucao(Resolucao resolucao) {
        this.resolucao = resolucao;
    }

    public LocalDateTime getDataHora() {
        return dataHora;
    }

    public void setDataHora(LocalDateTime dataHora) {
        this.dataHora = dataHora;
    }

    public Integer getPontosTrabalhados() {
        return pontosTrabalhados;
    }

    public void setPontosTrabalhados(Integer pontosTrabalhados) {
        this.pontosTrabalhados = pontosTrabalhados;
    }

    public Integer getPontosUtilizados() {
        return pontosUtilizados;
    }

    public void setPontosUtilizados(Integer pontosUtilizados) {
        this.pontosUtilizados = pontosUtilizados;
    }

    public Integer getPontosSobrando() {
        return pontosSobrando;
    }

    public void setPontosSobrando(Integer pontosSobrando) {
        this.pontosSobrando = pontosSobrando;
    }

    public String getInSituacao() {
        return inSituacao;
    }

    public void setInSituacao(String inSituacao) {
        this.inSituacao = inSituacao;
    }

    // =========================================================================
    // EQUALS & HASHCODE
    // =========================================================================
    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        PontosSaldo that = (PontosSaldo) o;
        return Objects.equals(idPontosSaldo, that.idPontosSaldo);
    }

    @Override
    public int hashCode() {
        return Objects.hash(idPontosSaldo);
    }

    // =========================================================================
    // TO_STRING
    // =========================================================================
    @Override
    public String toString() {
        return "PontosSaldo{" +
                "id=" + idPontosSaldo +
                ", pontosTrabalhados=" + pontosTrabalhados +
                ", pontosUtilizados=" + pontosUtilizados +
                ", pontosSobrando=" + pontosSobrando +
                ", situacao=" + inSituacao +
                '}';
    }
}