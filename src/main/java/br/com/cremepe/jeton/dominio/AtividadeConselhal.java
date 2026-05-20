package br.com.cremepe.jeton.dominio;

import jakarta.persistence.*;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "atividade_conselhal")
public class AtividadeConselhal implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "idAtividade")
    private Integer idAtividade;

    @ManyToOne
    @JoinColumn(name = "idGestao", nullable = false)
    private Gestao gestao;

    @ManyToOne
    @JoinColumn(name = "idPessoa", nullable = false)
    private Conselheiro conselheiro;

    @ManyToOne
    @JoinColumn(name = "idRegra", nullable = false)
    private Regras regra;

    @ManyToOne
    @JoinColumn(name = "idComprovante")
    private Comprovante comprovante;

    @Column(name = "qtdAtividade", nullable = false)
    private Integer qtdAtividade;

    @Column(name = "dataHoraAtividade", nullable = false)
    private LocalDateTime dataHoraAtividade;

    @Column(name = "dataHoraRegistro", nullable = false)
    private LocalDateTime dataHoraRegistro;

    @Column(name = "inTurno", nullable = false, length = 1)
    private String inTurno;

    @Column(name = "inSituacao", nullable = false, length = 1)
    private String inSituacao;

    @Column(name = "inComputada", nullable = false, length = 1)
    private String inComputada = "N"; // 'S' para computada, 'N' para não computada

    public AtividadeConselhal() {
    }

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
}