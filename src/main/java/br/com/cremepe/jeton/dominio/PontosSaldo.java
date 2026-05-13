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
 * Entidade JPA que representa a tabela 'pontos_saldo'.
 * Atua como o livro-razão do sistema, controlando os pontos que sobram e os que são utilizados.
 */
@Entity
@Table(name = "pontos_saldo")
public class PontosSaldo implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "idPontosSaldo")
    private Integer idPontosSaldo;

    /**
     * Relacionamento Opcional com a Atividade Conselhal.
     * Como a base de dados permite NULL (NULO: YES), omitimos o parâmetro nullable = false.
     */
    @ManyToOne
    @JoinColumn(name = "idAtividade")
    private AtividadeConselhal atividade;

    /**
     * Relacionamento Opcional com o fecho financeiro (Jeton).
     * Indica em que ciclo de pagamento estes pontos foram contabilizados ou liquidados.
     */
    @ManyToOne
    @JoinColumn(name = "idJeton")
    private Jeton jeton;

    // Espelha o tipo 'datetime' do MySQL legado
    @Column(name = "dataHora", nullable = false)
    private LocalDateTime dataHora;

    // Utilização de Integer (Wrapper) em vez de int primitivo para segurança transacional
    @Column(name = "pontosSobrando", nullable = false)
    private Integer pontosSobrando;

    @Column(name = "pontosUtilizados", nullable = false)
    private Integer pontosUtilizados;

    @Column(name = "inSituacao", length = 1, nullable = false)
    private String inSituacao;

    public PontosSaldo() {
    }

    // ==========================================
    // GETTERS E SETTERS
    // ==========================================

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

    public Jeton getJeton() {
        return jeton;
    }

    public void setJeton(Jeton jeton) {
        this.jeton = jeton;
    }

    public LocalDateTime getDataHora() {
        return dataHora;
    }

    public void setDataHora(LocalDateTime dataHora) {
        this.dataHora = dataHora;
    }

    public Integer getPontosSobrando() {
        return pontosSobrando;
    }

    public void setPontosSobrando(Integer pontosSobrando) {
        this.pontosSobrando = pontosSobrando;
    }

    public Integer getPontosUtilizados() {
        return pontosUtilizados;
    }

    public void setPontosUtilizados(Integer pontosUtilizados) {
        this.pontosUtilizados = pontosUtilizados;
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
        PontosSaldo that = (PontosSaldo) o;
        return Objects.equals(idPontosSaldo, that.idPontosSaldo);
    }

    @Override
    public int hashCode() {
        return Objects.hash(idPontosSaldo);
    }
}