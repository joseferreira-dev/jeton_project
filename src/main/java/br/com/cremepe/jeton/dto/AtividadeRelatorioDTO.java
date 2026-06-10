package br.com.cremepe.jeton.dto;

import java.time.LocalDate;

public class AtividadeRelatorioDTO {
    private String regra;
    private LocalDate data;
    private Integer quantidade;

    public String getRegra() {
        return regra;
    }

    public void setRegra(String regra) {
        this.regra = regra;
    }

    public LocalDate getData() {
        return data;
    }

    public void setData(LocalDate data) {
        this.data = data;
    }

    public Integer getQuantidade() {
        return quantidade;
    }

    public void setQuantidade(Integer quantidade) {
        this.quantidade = quantidade;
    }
}