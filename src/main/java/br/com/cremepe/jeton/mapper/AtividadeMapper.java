package br.com.cremepe.jeton.mapper;

import br.com.cremepe.jeton.domain.AtividadeConselhal;
import br.com.cremepe.jeton.dto.AtividadeConselhalDTO;
import br.com.cremepe.jeton.dto.AtividadeVinculadaDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface AtividadeMapper {

    @Mapping(target = "regra", source = "regra.nomeRegra")
    @Mapping(target = "data", expression = "java(atividade.getDataHoraAtividade().toLocalDate().toString())")
    @Mapping(target = "qtd", source = "qtdAtividade")
    AtividadeVinculadaDTO toAtividadeVinculadaDto(AtividadeConselhal atividade);

    @Mapping(target = "id", source = "idAtividade")
    @Mapping(target = "idGestao", source = "gestao.idGestao")
    @Mapping(target = "nomeGestao", source = "gestao.nomeGestao")
    @Mapping(target = "idConselheiro", source = "conselheiro.idPessoa")
    @Mapping(target = "nomeConselheiro", source = "conselheiro.pessoa.nome")
    @Mapping(target = "idRegra", source = "regra.idRegra")
    @Mapping(target = "nomeRegra", source = "regra.nomeRegra")
    @Mapping(target = "descricaoRegra", source = "regra.descricao")
    @Mapping(target = "pontosRegra", source = "regra.pontos")
    @Mapping(target = "idComprovante", source = "comprovante.idComprovante")
    @Mapping(target = "nomeComprovanteUsuario", source = "comprovante.nomeComprovante")
    @Mapping(target = "nomeArquivo", source = "comprovante.nomeArquivo")
    @Mapping(target = "idTipoAnexo", source = "comprovante.tipoAnexo.idTipo")
    @Mapping(target = "nomeTipoAnexo", source = "comprovante.tipoAnexo.nome")
    @Mapping(target = "qtdAtividade", source = "qtdAtividade")
    @Mapping(target = "dataHoraAtividade", source = "dataHoraAtividade")
    @Mapping(target = "dataHoraRegistro", source = "dataHoraRegistro")
    @Mapping(target = "turno", source = "inTurno")
    @Mapping(target = "situacao", source = "inSituacao")
    @Mapping(target = "computada", source = "inComputada")
    AtividadeConselhalDTO toFullDto(AtividadeConselhal atividade);
}