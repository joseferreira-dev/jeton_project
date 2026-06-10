package br.com.cremepe.jeton.dto;

import java.time.LocalDateTime;
import java.util.Map;

public class ErroApiDTO {
    private String mensagem;
    private int status;
    private String erro;
    private String path;
    private LocalDateTime timestamp;
    private Map<String, String> errosValidacao;

    public ErroApiDTO() {
    }

    public ErroApiDTO(int status, String erro, String mensagem, String path) {
        this.status = status;
        this.erro = erro;
        this.mensagem = mensagem;
        this.path = path;
        this.timestamp = LocalDateTime.now();
    }

    public String getMensagem() {
        return mensagem;
    }

    public void setMensagem(String mensagem) {
        this.mensagem = mensagem;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public String getErro() {
        return erro;
    }

    public void setErro(String erro) {
        this.erro = erro;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public Map<String, String> getErrosValidacao() {
        return errosValidacao;
    }

    public void setErrosValidacao(Map<String, String> errosValidacao) {
        this.errosValidacao = errosValidacao;
    }
}