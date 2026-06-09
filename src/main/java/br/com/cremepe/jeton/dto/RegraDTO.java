package br.com.cremepe.jeton.dto;

import java.io.Serializable;
import java.util.Objects;

public class RegraDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Integer id;
    private String nome;
    private String descricao;
    private Integer pontos;

    public RegraDTO() {
    }

    public RegraDTO(Integer id, String nome, String descricao, Integer pontos) {
        this.id = id;
        this.nome = nome;
        this.descricao = descricao;
        this.pontos = pontos;
    }

    // Getters e Setters
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
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

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        RegraDTO regraDTO = (RegraDTO) o;
        return Objects.equals(id, regraDTO.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "RegraDTO{" +
                "id=" + id +
                ", nome='" + nome + '\'' +
                ", pontos=" + pontos +
                '}';
    }
}