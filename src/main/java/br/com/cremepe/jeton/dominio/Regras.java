package br.com.cremepe.jeton.dominio;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Entidade JPA que representa a tabela 'regras'.
 * Eixo central que define as regras de pontuação de atividades baseadas em
 * Portarias e Resoluções.
 */
@Entity
@Table(name = "regras")
public class Regras implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "idRegra")
    private Integer idRegra;

    @ManyToOne(optional = false)
    @JoinColumn(name = "idResolucao", nullable = false)
    private Resolucao resolucao;

    @ManyToOne
    @JoinColumn(name = "idPortaria", nullable = true)
    private Portaria portaria;

    // Mapeamento inverso do Muitos-para-Muitos
    @ManyToMany(mappedBy = "regrasAgrupadas")
    private List<RegrasConjuntas> gruposDeRegras = new ArrayList<>();

    @Column(name = "nomeRegra", length = 70, nullable = false, unique = true)
    private String nomeRegra;

    // Campo de texto longo opcional
    @Column(name = "descricao", columnDefinition = "text")
    private String descricao;

    @Column(name = "pontos", nullable = false)
    private Integer pontos = 0;

    @Column(name = "inRevogado", length = 1, nullable = false)
    private String inRevogado;

    @Column(name = "pontosLimitesTurno", nullable = false)
    private Integer pontosLimitesTurno;

    @Column(name = "inJudicante", length = 1, nullable = false)
    private String inJudicante;

    public Regras() {
    }

    // ==========================================
    // GETTERS E SETTERS
    // ==========================================

    public Integer getIdRegra() {
        return idRegra;
    }

    public void setIdRegra(Integer idRegra) {
        this.idRegra = idRegra;
    }

    public Resolucao getResolucao() {
        return resolucao;
    }

    public void setResolucao(Resolucao resolucao) {
        this.resolucao = resolucao;
    }

    public Portaria getPortaria() {
        return portaria;
    }

    public void setPortaria(Portaria portaria) {
        this.portaria = portaria;
    }

    public String getNomeRegra() {
        return nomeRegra;
    }

    public void setNomeRegra(String nomeRegra) {
        this.nomeRegra = nomeRegra;
    }

    public String getDescricao() {
        return descricao;
    }

    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }

    public Integer getPontos() {
        return pontos;
    }

    public void setPontos(Integer pontos) {
        this.pontos = pontos;
    }

    public String getInRevogado() {
        return inRevogado;
    }

    public void setInRevogado(String inRevogado) {
        this.inRevogado = inRevogado;
    }

    public Integer getPontosLimitesTurno() {
        return pontosLimitesTurno;
    }

    public void setPontosLimitesTurno(Integer pontosLimitesTurno) {
        this.pontosLimitesTurno = pontosLimitesTurno;
    }

    public String getInJudicante() {
        return inJudicante;
    }

    public void setInJudicante(String inJudicante) {
        this.inJudicante = inJudicante;
    }

    // ==========================================
    // MÉTODOS PARA O RELACIONAMENTO BIDIRECIONAL
    // ==========================================

    /**
     * Retorna a lista de grupos de regras conjuntas aos quais esta regra pertence.
     * Útil para validar limites cumulativos na camada de serviço.
     */
    public List<RegrasConjuntas> getGruposDeRegras() {
        return gruposDeRegras;
    }

    /**
     * Define a lista de grupos de regras conjuntas.
     * 
     * @param gruposDeRegras Nova lista de associações.
     */
    public void setGruposDeRegras(List<RegrasConjuntas> gruposDeRegras) {
        this.gruposDeRegras = gruposDeRegras;
    }

    // -------------------------------------------------------------------------
    // DICA DE ARQUITETURA: Métodos Utilitários (Helper Methods)
    // -------------------------------------------------------------------------
    // Em relacionamentos Muitos-para-Muitos, é recomendável ter métodos para
    // garantir a consistência de ambos os lados da associação em memória.

    public void adicionarAoGrupo(RegrasConjuntas grupo) {
        this.gruposDeRegras.add(grupo);
        if (!grupo.getRegrasAgrupadas().contains(this)) {
            grupo.getRegrasAgrupadas().add(this);
        }
    }

    public void removerDoGrupo(RegrasConjuntas grupo) {
        this.gruposDeRegras.remove(grupo);
        grupo.getRegrasAgrupadas().remove(this);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        Regras regras = (Regras) o;
        return Objects.equals(idRegra, regras.idRegra);
    }

    @Override
    public int hashCode() {
        return Objects.hash(idRegra);
    }
}