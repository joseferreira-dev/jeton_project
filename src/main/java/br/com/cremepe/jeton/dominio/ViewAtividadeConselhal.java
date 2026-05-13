package br.com.cremepe.jeton.dominio;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.Immutable;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Entidade de leitura que mapeia a view 'vw_atividade_conselhal'.
 */
@Entity
@Immutable
@Table(name = "vw_atividade_conselhal")
public class ViewAtividadeConselhal implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id 
    @Column(name = "idAtividade")
    private Integer idAtividade;

    private Integer idGestao;
    private Integer idPessoa;
    private Integer idRegra;
    private Integer qtdAtividade;
    private LocalDateTime dataHoraAtividade;
    private LocalDateTime dataHoraRegistro;
    private String inTurno;
    private String situacaoAtividade;
    
    private String nomeGestao;
    private String nome; 
    private String cpf;
    private String nomeRegra;
    private Integer pontos;
    private String resolucao;
    private String portaria;

    public ViewAtividadeConselhal() {}

    // ==========================================
    // APENAS GETTERS (Entidade Imutável)
    // ==========================================

    public Integer getIdAtividade() { return idAtividade; }
    public Integer getIdGestao() { return idGestao; }
    public Integer getIdPessoa() { return idPessoa; }
    public Integer getIdRegra() { return idRegra; }
    public Integer getQtdAtividade() { return qtdAtividade; }
    public LocalDateTime getDataHoraAtividade() { return dataHoraAtividade; }
    public LocalDateTime getDataHoraRegistro() { return dataHoraRegistro; }
    public String getInTurno() { return inTurno; }
    public String getSituacaoAtividade() { return situacaoAtividade; }
    public String getNomeGestao() { return nomeGestao; }
    public String getNome() { return nome; }
    public String getCpf() { return cpf; }
    public String getNomeRegra() { return nomeRegra; }
    public Integer getPontos() { return pontos; }
    public String getResolucao() { return resolucao; }
    public String getPortaria() { return portaria; }
}