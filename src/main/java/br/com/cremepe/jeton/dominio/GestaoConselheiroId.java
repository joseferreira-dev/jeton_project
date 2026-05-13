package br.com.cremepe.jeton.dominio;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;

/**
 * Classe que representa a Chave Primária Composta da tabela associativa 'gestao_conselheiro'.
 */
@Embeddable
public class GestaoConselheiroId implements Serializable {

    private static final long serialVersionUID = 1L;

    @Column(name = "idGestao")
    private Integer idGestao;

    @Column(name = "idPessoa")
    private Integer idPessoa;

    public GestaoConselheiroId() {
    }

    public GestaoConselheiroId(Integer idGestao, Integer idPessoa) {
        this.idGestao = idGestao;
        this.idPessoa = idPessoa;
    }

    // ==========================================
    // GETTERS E SETTERS
    // ==========================================

    public Integer getIdGestao() {
        return idGestao;
    }

    public void setIdGestao(Integer idGestao) {
        this.idGestao = idGestao;
    }

    public Integer getIdPessoa() {
        return idPessoa;
    }

    public void setIdPessoa(Integer idPessoa) {
        this.idPessoa = idPessoa;
    }

    // O equals e o hashCode são OBRIGATÓRIOS para chaves compostas no JPA
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GestaoConselheiroId that = (GestaoConselheiroId) o;
        return Objects.equals(idGestao, that.idGestao) && 
               Objects.equals(idPessoa, that.idPessoa);
    }

    @Override
    public int hashCode() {
        return Objects.hash(idGestao, idPessoa);
    }
}