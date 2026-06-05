package br.com.cremepe.jeton.dominio;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Entidade JPA que representa a tabela 'log_jeton'.
 * Responsável por manter a trilha de auditoria e rastreabilidade de todas as
 * operações sensíveis realizadas no sistema.
 */
@Entity
@Table(name = "log_jeton")
public class LogJeton implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "idLog")
    private Integer idLog;

    @Column(name = "nomeTabela", length = 45, nullable = false)
    private String nomeTabela;

    @JoinColumn(name = "idUsuario", nullable = false)
    private Integer idUsuario;

    // Espelha o tipo 'datetime' do MySQL para garantir a ordem cronológica exata
    @Column(name = "dataHoraLog", nullable = false)
    private LocalDateTime dataHoraLog;

    /**
     * Campo configurado especificamente para receber textos longos (ex: JSON ou
     * dumps).
     */
    @Column(name = "textoLog", columnDefinition = "text", nullable = false)
    private String textoLog;

    public LogJeton() {
    }

    // ==========================================
    // GETTERS E SETTERS
    // ==========================================

    public Integer getIdLog() {
        return idLog;
    }

    public void setIdLog(Integer idLog) {
        this.idLog = idLog;
    }

    public String getNomeTabela() {
        return nomeTabela;
    }

    public void setNomeTabela(String nomeTabela) {
        this.nomeTabela = nomeTabela;
    }

    public Integer getIdUsuario() {
        return idUsuario;
    }

    public void setIdUsuario(Integer idUsuario) {
        this.idUsuario = idUsuario;
    }

    public LocalDateTime getDataHoraLog() {
        return dataHoraLog;
    }

    public void setDataHoraLog(LocalDateTime dataHoraLog) {
        this.dataHoraLog = dataHoraLog;
    }

    public String getTextoLog() {
        return textoLog;
    }

    public void setTextoLog(String textoLog) {
        this.textoLog = textoLog;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        LogJeton logJeton = (LogJeton) o;
        return Objects.equals(idLog, logJeton.idLog);
    }

    @Override
    public int hashCode() {
        return Objects.hash(idLog);
    }
}