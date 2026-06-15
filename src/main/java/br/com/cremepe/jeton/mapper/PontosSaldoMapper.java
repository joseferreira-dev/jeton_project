package br.com.cremepe.jeton.mapper;

import br.com.cremepe.jeton.domain.PontosSaldo;
import br.com.cremepe.jeton.dto.PontosSaldoDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface PontosSaldoMapper {

    @Mapping(target = "id", source = "idPontosSaldo")
    @Mapping(target = "situacao", source = "inSituacao")
    @Mapping(target = "idAtividade", source = "atividade.idAtividade")
    @Mapping(target = "idConselheiro", source = "conselheiro.idPessoa")
    @Mapping(target = "idGestao", source = "gestao.idGestao")
    @Mapping(target = "idResolucao", source = "resolucao.idResolucao")
    PontosSaldoDTO toDto(PontosSaldo entity);
}