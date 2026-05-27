package br.com.cremepe.jeton.dominio;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
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

    // =========================================================================
    // CONSTANTES
    // =========================================================================
    public static final String REVOGADO_SIM = "S";
    public static final String REVOGADO_NAO = "N";

    public static final String JUDICANTE_SIM = "S";
    public static final String JUDICANTE_NAO = "N";

    // =========================================================================
    // CAMPOS
    // =========================================================================
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "idRegra")
    private Integer idRegra;

    @NotNull(message = "A resolução é obrigatória")
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "idResolucao", nullable = false)
    private Resolucao resolucao;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "idPortaria")
    private Portaria portaria;

    @ManyToMany(mappedBy = "regrasAgrupadas")
    private List<RegrasConjuntas> gruposDeRegras = new ArrayList<>();

    @NotNull(message = "O nome da regra é obrigatório")
    @Size(min = 3, max = 70, message = "O nome deve ter entre 3 e 70 caracteres")
    @Column(name = "nomeRegra", length = 70, nullable = false, unique = true)
    private String nomeRegra;

    @Column(name = "descricao", columnDefinition = "text")
    private String descricao;

    @NotNull(message = "A pontuação é obrigatória")
    @Positive(message = "A pontuação deve ser maior que zero")
    @Column(name = "pontos", nullable = false)
    private Integer pontos = 0;

    @NotNull
    @Pattern(regexp = "[SN]", message = "inRevogado deve ser S ou N")
    @Column(name = "inRevogado", nullable = false, length = 1)
    private String inRevogado = REVOGADO_NAO;

    @NotNull(message = "O limite de pontos por turno é obrigatório")
    @PositiveOrZero(message = "O limite de pontos por turno deve ser zero ou positivo")
    @Column(name = "pontosLimitesTurno", nullable = false)
    private Integer pontosLimitesTurno = 0;

    @NotNull
    @Pattern(regexp = "[SN]", message = "inJudicante deve ser S ou N")
    @Column(name = "inJudicante", nullable = false, length = 1)
    private String inJudicante = JUDICANTE_NAO;

    // =========================================================================
    // CONSTRUTORES
    // =========================================================================
    public Regras() {
    }

    // =========================================================================
    // MÉTODOS DE CONVENIÊNCIA
    // =========================================================================
    public boolean isRevogado() {
        return REVOGADO_SIM.equals(inRevogado);
    }

    public boolean isEmVigor() {
        return REVOGADO_NAO.equals(inRevogado);
    }

    public boolean isJudicante() {
        return JUDICANTE_SIM.equals(inJudicante);
    }

    public boolean isNaoJudicante() {
        return JUDICANTE_NAO.equals(inJudicante);
    }

    // =========================================================================
    // JPA LIFECYCLE – NORMALIZAÇÃO
    // =========================================================================
    @PrePersist
    @PreUpdate
    protected void normalize() {
        if (nomeRegra != null) {
            nomeRegra = nomeRegra.trim();
        }
        if (inRevogado != null) {
            inRevogado = inRevogado.toUpperCase();
        }
        if (inJudicante != null) {
            inJudicante = inJudicante.toUpperCase();
        }
        // Garante valores padrão
        if (!REVOGADO_SIM.equals(inRevogado) && !REVOGADO_NAO.equals(inRevogado)) {
            inRevogado = REVOGADO_NAO;
        }
        if (!JUDICANTE_SIM.equals(inJudicante) && !JUDICANTE_NAO.equals(inJudicante)) {
            inJudicante = JUDICANTE_NAO;
        }
        if (pontosLimitesTurno == null) {
            pontosLimitesTurno = 0;
        }
        if (pontos == null) {
            pontos = 0;
        }
    }

    // =========================================================================
    // GETTERS E SETTERS
    // =========================================================================

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

    // =========================================================================
    // MÉTODOS PARA O RELACIONAMENTO BIDIRECIONAL
    // =========================================================================

    public List<RegrasConjuntas> getGruposDeRegras() {
        return gruposDeRegras;
    }

    public void setGruposDeRegras(List<RegrasConjuntas> gruposDeRegras) {
        this.gruposDeRegras = gruposDeRegras;
    }

    // =========================================================================
    // MÉTODOS UTILITÁRIOS
    // =========================================================================

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

    // =========================================================================
    // EQUALS & HASHCODE
    // =========================================================================

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

    // =========================================================================
    // TO_STRING
    // =========================================================================

    @Override
    public String toString() {
        return "Regras{" +
                "id=" + idRegra +
                ", nome='" + nomeRegra + '\'' +
                ", pontos=" + pontos +
                ", revogado=" + inRevogado +
                ", judicante=" + inJudicante +
                '}';
    }
}