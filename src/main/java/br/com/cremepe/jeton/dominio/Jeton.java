package br.com.cremepe.jeton.dominio;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Entidade que representa o fecho financeiro (Jeton) de um Conselheiro num
 * determinado mês.
 */
@Entity
@Table(name = "jeton")
public class Jeton implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "idJeton")
    private Integer idJeton;

    @ManyToOne
    @JoinColumn(name = "idGestao", nullable = false)
    private Gestao gestao;

    /**
     * Relacionamento com o Conselheiro.
     * Na base de dados a coluna é idPessoa, que é a PK de Conselheiro.
     */
    @ManyToOne
    @JoinColumn(name = "idPessoa", nullable = false)
    private Conselheiro conselheiro;

    @Column(name = "mes", nullable = false)
    private Integer mes;

    @Column(name = "ano", nullable = false)
    private Integer ano;

    @Column(name = "totalJeton", nullable = false)
    private Integer totalJeton;

    @Column(name = "valor", precision = 10, scale = 2, nullable = false)
    private BigDecimal valor;

    @Column(name = "inSituacao", length = 1, nullable = false)
    private String inSituacao;

    /**
     * Mapeamento da tabela de ligação 'jeton_atividade'.
     * Associa este pagamento às atividades específicas que o geraram.
     */
    @ManyToMany
    @JoinTable(name = "jeton_atividade", joinColumns = @JoinColumn(name = "idJeton"), inverseJoinColumns = @JoinColumn(name = "idAtividade"))
    private List<AtividadeConselhal> atividades = new ArrayList<>();

    public Jeton() {
    }

    // ==========================================
    // GETTERS E SETTERS
    // ==========================================

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
}