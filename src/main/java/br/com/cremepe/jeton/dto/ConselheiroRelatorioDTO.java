package br.com.cremepe.jeton.dto;

import java.math.BigDecimal;
import java.util.List;

public class ConselheiroRelatorioDTO {
    private Integer idPessoa;
    private String nome;
    private Integer totalJetons;
    private BigDecimal valor;
    private Integer saldoAnterior;
    private Integer pontosAcumuladosMes;
    private Integer saldoFuturo;
    private List<AtividadeRelatorioDTO> atividades;

    public Integer getIdPessoa() {
        return idPessoa;
    }

    public void setIdPessoa(Integer idPessoa) {
        this.idPessoa = idPessoa;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public Integer getTotalJetons() {
        return totalJetons;
    }

    public void setTotalJetons(Integer totalJetons) {
        this.totalJetons = totalJetons;
    }

    public BigDecimal getValor() {
        return valor;
    }

    public void setValor(BigDecimal valor) {
        this.valor = valor;
    }

    public Integer getSaldoAnterior() {
        return saldoAnterior;
    }

    public void setSaldoAnterior(Integer saldoAnterior) {
        this.saldoAnterior = saldoAnterior;
    }

    public Integer getPontosAcumuladosMes() {
        return pontosAcumuladosMes;
    }

    public void setPontosAcumuladosMes(Integer pontosAcumuladosMes) {
        this.pontosAcumuladosMes = pontosAcumuladosMes;
    }

    public Integer getSaldoFuturo() {
        return saldoFuturo;
    }

    public void setSaldoFuturo(Integer saldoFuturo) {
        this.saldoFuturo = saldoFuturo;
    }

    public List<AtividadeRelatorioDTO> getAtividades() {
        return atividades;
    }

    public void setAtividades(List<AtividadeRelatorioDTO> atividades) {
        this.atividades = atividades;
    }
}