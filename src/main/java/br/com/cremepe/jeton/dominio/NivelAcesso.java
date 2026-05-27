package br.com.cremepe.jeton.dominio;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.io.Serializable;
import java.util.Objects;

/**
 * Entidade que representa os perfis de acesso (ex: Administrador, Conselheiro).
 * A chave primária é uma letra maiúscula de 'A' a 'Z', sem acentos.
 */
@Entity
@Table(name = "nivel_acesso")
public class NivelAcesso implements Serializable {

    private static final long serialVersionUID = 1L;

    // =========================================================================
    // CONSTANTES PÚBLICAS PARA OS NÍVEIS DE ACESSO
    // =========================================================================
    public static final String NIVEL_ATIVIDADE_CONSELHAL = "A";
    public static final String NIVEL_BLOQUEIO_SISTEMA = "B";
    public static final String NIVEL_COMPROVANTES = "C";
    public static final String NIVEL_GESTAO_CONSELHEIROS = "G";
    public static final String NIVEL_JETONS = "J";
    public static final String NIVEL_NIVEIS_ACESSO = "N";
    public static final String NIVEL_PONTOS_REMANESCENTES = "P";
    public static final String NIVEL_PORTARIAS_RESOLUCOES = "R";
    public static final String NIVEL_SUPER_ADMIN = "S"; // Gerenciar todas as Atividades/Jetons
    public static final String NIVEL_TIPOS_ANEXO = "T";
    public static final String NIVEL_USUARIOS = "U";

    // =========================================================================
    // CAMPOS DA ENTIDADE
    // =========================================================================
    @Id
    @NotNull
    @Size(min = 1, max = 1)
    @Pattern(regexp = "[A-Z]", message = "O nível deve ser uma letra maiúscula de A a Z")
    @Column(name = "idNivel", length = 1)
    private String idNivel;

    @NotNull
    @Size(max = 50)
    @Column(name = "nomeNivel", length = 50, nullable = false, unique = true)
    private String nomeNivel;

    // =========================================================================
    // CONSTRUTORES
    // =========================================================================
    public NivelAcesso() {
    }

    // =========================================================================
    // MÉTODOS DE CONVENIÊNCIA
    // =========================================================================
    public boolean isSuperAdmin() {
        return NIVEL_SUPER_ADMIN.equals(idNivel);
    }

    public boolean isBloqueioSistema() {
        return NIVEL_BLOQUEIO_SISTEMA.equals(idNivel);
    }

    // =========================================================================
    // JPA LIFECYCLE – NORMALIZAÇÃO
    // =========================================================================
    @PrePersist
    @PreUpdate
    protected void normalize() {
        if (idNivel != null) {
            idNivel = idNivel.trim().toUpperCase();
        }
        if (nomeNivel != null) {
            nomeNivel = nomeNivel.trim();
        }
    }

    // =========================================================================
    // GETTERS E SETTERS
    // =========================================================================
    public String getIdNivel() {
        return idNivel;
    }

    public void setIdNivel(String idNivel) {
        this.idNivel = idNivel;
    }

    public String getNomeNivel() {
        return nomeNivel;
    }

    public void setNomeNivel(String nomeNivel) {
        this.nomeNivel = nomeNivel;
    }

    // =========================================================================
    // EQUALS & HASHCODE (baseado no ID)
    // =========================================================================
    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        NivelAcesso that = (NivelAcesso) o;
        return Objects.equals(idNivel, that.idNivel);
    }

    @Override
    public int hashCode() {
        return Objects.hash(idNivel);
    }

    // =========================================================================
    // TO_STRING
    // =========================================================================
    @Override
    public String toString() {
        return "NivelAcesso{" +
                "id='" + idNivel + '\'' +
                ", nome='" + nomeNivel + '\'' +
                '}';
    }
}