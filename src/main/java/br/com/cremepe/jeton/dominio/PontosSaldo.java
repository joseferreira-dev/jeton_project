package br.com.cremepe.jeton.dominio;

import jakarta.persistence.*;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "pontos_saldo")
public class PontosSaldo implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "idPontosSaldo")
    private Integer idPontosSaldo;

    @ManyToOne
    @JoinColumn(name = "idAtividade", nullable = true)
    private AtividadeConselhal atividade;

    @ManyToOne
    @JoinColumn(name = "idPessoa")
    private Conselheiro conselheiro;

    @ManyToOne
    @JoinColumn(name = "idJeton")
    private Jeton jeton;

    @ManyToOne
    @JoinColumn(name = "idGestao")
    private Gestao gestao;

    @ManyToOne
    @JoinColumn(name = "idResolucao")
    private Resolucao resolucao;

    @Column(name = "dataHora", nullable = false)
    private LocalDateTime dataHora;

    @Column(name = "pontosTrabalhados", nullable = false)
    private Integer pontosTrabalhados = 0;

    @Column(name = "pontosUtilizados", nullable = false)
    private Integer pontosUtilizados = 0;

    @Column(name = "pontosSobrando", nullable = false)
    private Integer pontosSobrando = 0;

    @Column(name = "inSituacao", nullable = false, length = 1)
    private String inSituacao;

    public PontosSaldo() {
    }

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
}