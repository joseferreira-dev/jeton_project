package br.com.cremepe.jeton.dto;

import org.springframework.web.multipart.MultipartFile;
import java.time.LocalDateTime;
import java.util.List;

public class LoteAtividadeDTO {

    private Integer idGestao;
    private LocalDateTime dataHoraAtividade;
    private Integer idRegra;
    private String inTurno;
    private MultipartFile file;
    private Integer idTipoAnexo;
    private String nomeComprovanteUsuario;
    private List<Integer> idsConselheiros;

    public LoteAtividadeDTO() {
    }

    public Integer getIdGestao() {
        return idGestao;
    }

    public void setIdGestao(Integer idGestao) {
        this.idGestao = idGestao;
    }

    public LocalDateTime getDataHoraAtividade() {
        return dataHoraAtividade;
    }

    public void setDataHoraAtividade(LocalDateTime dataHoraAtividade) {
        this.dataHoraAtividade = dataHoraAtividade;
    }

    public Integer getIdRegra() {
        return idRegra;
    }

    public void setIdRegra(Integer idRegra) {
        this.idRegra = idRegra;
    }

    public String getInTurno() {
        return inTurno;
    }

    public void setInTurno(String inTurno) {
        this.inTurno = inTurno;
    }

    public MultipartFile getFile() {
        return file;
    }

    public void setFile(MultipartFile file) {
        this.file = file;
    }

    public Integer getIdTipoAnexo() {
        return idTipoAnexo;
    }

    public void setIdTipoAnexo(Integer idTipoAnexo) {
        this.idTipoAnexo = idTipoAnexo;
    }

    public String getNomeComprovanteUsuario() {
        return nomeComprovanteUsuario;
    }

    public void setNomeComprovanteUsuario(String nomeComprovanteUsuario) {
        this.nomeComprovanteUsuario = nomeComprovanteUsuario;
    }

    public List<Integer> getIdsConselheiros() {
        return idsConselheiros;
    }

    public void setIdsConselheiros(List<Integer> idsConselheiros) {
        this.idsConselheiros = idsConselheiros;
    }
}