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
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Entidade JPA que representa a tabela 'atividade_conselhal' na base de dados.
 * Regista as atividades diárias executadas pelos Conselheiros.
 */
@Entity
@Table(name = "atividade_conselhal")
public class AtividadeConselhal implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "idAtividade")
    private Integer idAtividade;

    // Relacionamento com a Gestao
    @ManyToOne
    @JoinColumn(name = "idGestao", nullable = false)
    private Gestao gestao;

    // Relacionamento com o Conselheiro (na base de dados a coluna chama-se idPessoa)
    @ManyToOne
    @JoinColumn(name = "idPessoa", nullable = false)
    private Conselheiro conselheiro;

    // Relacionamento com as Regras
    @ManyToOne
    @JoinColumn(name = "idRegra", nullable = false)
    private Regras regra;

    // Relacionamento Opcional com o Comprovante (pois permite NULL na base de dados)
    @ManyToOne
    @JoinColumn(name = "idComprovante")
    private Comprovante comprovante;

    @Column(name = "qtdAtividade", nullable = false)
    private Integer qtdAtividade;

    // Uso do LocalDateTime para espelhar perfeitamente o tipo 'datetime' do MySQL
    @Column(name = "dataHoraAtividade", nullable = false)
    private LocalDateTime dataHoraAtividade;

    @Column(name = "dataHoraRegistro", nullable = false)
    private LocalDateTime dataHoraRegistro;

    @Column(name = "inTurno", length = 1, nullable = false)
    private String inTurno;

    @Column(name = "inSituacao", length = 1, nullable = false)
    private String inSituacao;

    public AtividadeConselhal() {
    }

    // ==========================================
    // GETTERS E SETTERS
    // ==========================================

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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AtividadeConselhal that = (AtividadeConselhal) o;
        return Objects.equals(idAtividade, that.idAtividade);
    }

    @Override
    public int hashCode() {
        return Objects.hash(idAtividade);
    }
}