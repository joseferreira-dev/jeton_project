package br.com.cremepe.jeton.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Entidade que representa as atividades (eventos, reuniões, etc.) realizadas
 * por um conselheiro.
 * Cada atividade está vinculada a uma gestão, uma regra de pontuação e,
 * opcionalmente, a um comprovante.
 * As atividades passam por estados: PENDENTE -> VALIDADA -> FECHADA (após
 * processamento financeiro).
 */
@Entity
@Table(name = "atividade_conselhal")
public class AtividadeConselhal implements Serializable {

    private static final long serialVersionUID = 1L;

    // =========================================================================
    // CONSTANTES PÚBLICAS PARA OS ESTADOS DA ATIVIDADE
    // =========================================================================
    public static final String SITUACAO_PENDENTE = "P";
    public static final String SITUACAO_VALIDADA = "C";
    public static final String SITUACAO_FECHADA = "F";

    public static final String COMPUTADA_SIM = "S";
    public static final String COMPUTADA_NAO = "N";

    public static final String TURNO_MANHA = "M";
    public static final String TURNO_TARDE = "T";
    public static final String TURNO_NOITE = "N";

    // =========================================================================
    // CAMPOS DA ENTIDADE
    // =========================================================================
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "idAtividade")
    private Integer idAtividade;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "idGestao", nullable = false)
    private Gestao gestao;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "idPessoa", nullable = false)
    private Conselheiro conselheiro;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "idRegra", nullable = false)
    private Regras regra;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "idComprovante")
    private Comprovante comprovante;

    @NotNull
    @Column(name = "qtdAtividade", nullable = false)
    private Integer qtdAtividade;

    @NotNull
    @Column(name = "dataHoraAtividade", nullable = false)
    private LocalDateTime dataHoraAtividade;

    @NotNull
    @Column(name = "dataHoraRegistro", nullable = false)
    private LocalDateTime dataHoraRegistro;

    @NotNull
    @Size(min = 1, max = 1)
    @Pattern(regexp = "[MTN]", message = "Turno deve ser M, T ou N")
    @Column(name = "inTurno", nullable = false, length = 1)
    private String inTurno;

    @NotNull
    @Size(min = 1, max = 1)
    @Pattern(regexp = "[PCF]", message = "Situação deve ser P, C ou F")
    @Column(name = "inSituacao", nullable = false, length = 1)
    private String inSituacao;

    @NotNull
    @Size(min = 1, max = 1)
    @Pattern(regexp = "[SN]", message = "inComputada deve ser S ou N")
    @Column(name = "inComputada", nullable = false, length = 1)
    private String inComputada = COMPUTADA_NAO;

    // =========================================================================
    // CONSTRUTORES
    // =========================================================================
    public AtividadeConselhal() {
        // Construtor padrão exigido pelo JPA
    }

    // =========================================================================
    // MÉTODOS DE CONVENIÊNCIA (STATUS HELPERS)
    // =========================================================================
    public boolean isPendente() {
        return SITUACAO_PENDENTE.equals(inSituacao);
    }

    public boolean isValidada() {
        return SITUACAO_VALIDADA.equals(inSituacao);
    }

    public boolean isFechada() {
        return SITUACAO_FECHADA.equals(inSituacao);
    }

    public boolean isComputada() {
        return COMPUTADA_SIM.equals(inComputada);
    }

    public boolean isManha() {
        return TURNO_MANHA.equals(inTurno);
    }

    public boolean isTarde() {
        return TURNO_TARDE.equals(inTurno);
    }

    public boolean isNoite() {
        return TURNO_NOITE.equals(inTurno);
    }

    // =========================================================================
    // MÉTODOS JPA LIFECYCLE (NORMALIZAÇÃO AUTOMÁTICA)
    // =========================================================================
    @PrePersist
    @PreUpdate
    protected void normalizeFields() {
        if (inTurno != null)
            inTurno = inTurno.toUpperCase();
        if (inSituacao != null)
            inSituacao = inSituacao.toUpperCase();
        if (inComputada != null)
            inComputada = inComputada.toUpperCase();

        // Garante que inComputada seja S ou N
        if (!COMPUTADA_SIM.equals(inComputada) && !COMPUTADA_NAO.equals(inComputada)) {
            inComputada = COMPUTADA_NAO;
        }
    }

    // =========================================================================
    // GETTERS E SETTERS
    // =========================================================================
    public Integer getIdAtividade() {
        return idAtividade;
    }

    public void setIdAtividade(Integer idAtividade) {
        this.idAtividade = idAtividade;
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

    public Regras getRegra() {
        return regra;
    }

    public void setRegra(Regras regra) {
        this.regra = regra;
    }

    public Comprovante getComprovante() {
        return comprovante;
    }

    public void setComprovante(Comprovante comprovante) {
        this.comprovante = comprovante;
    }

    public Integer getQtdAtividade() {
        return qtdAtividade;
    }

    public void setQtdAtividade(Integer qtdAtividade) {
        this.qtdAtividade = qtdAtividade;
    }

    public LocalDateTime getDataHoraAtividade() {
        return dataHoraAtividade;
    }

    public void setDataHoraAtividade(LocalDateTime dataHoraAtividade) {
        this.dataHoraAtividade = dataHoraAtividade;
    }

    public LocalDateTime getDataHoraRegistro() {
        return dataHoraRegistro;
    }

    public void setDataHoraRegistro(LocalDateTime dataHoraRegistro) {
        this.dataHoraRegistro = dataHoraRegistro;
    }

    public String getInTurno() {
        return inTurno;
    }

    public void setInTurno(String inTurno) {
        this.inTurno = inTurno;
    }

    public String getInSituacao() {
        return inSituacao;
    }

    public void setInSituacao(String inSituacao) {
        this.inSituacao = inSituacao;
    }

    public String getInComputada() {
        return inComputada;
    }

    public void setInComputada(String inComputada) {
        this.inComputada = inComputada;
    }

    // =========================================================================
    // EQUALS & HASHCODE (BASEADO APENAS NO ID)
    // =========================================================================
    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        AtividadeConselhal that = (AtividadeConselhal) o;
        return Objects.equals(idAtividade, that.idAtividade);
    }

    @Override
    public int hashCode() {
        return Objects.hash(idAtividade);
    }

    // =========================================================================
    // TO_STRING
    // =========================================================================
    @Override
    public String toString() {
        return "AtividadeConselhal{" +
                "id=" + idAtividade +
                ", gestao=" + (gestao != null ? gestao.getIdGestao() : null) +
                ", conselheiro=" + (conselheiro != null ? conselheiro.getIdPessoa() : null) +
                ", regra=" + (regra != null ? regra.getIdRegra() : null) +
                ", data=" + dataHoraAtividade +
                ", turno=" + inTurno +
                ", situacao=" + inSituacao +
                ", computada=" + inComputada +
                '}';
    }
}