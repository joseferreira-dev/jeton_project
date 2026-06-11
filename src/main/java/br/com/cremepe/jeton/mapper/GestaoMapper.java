package br.com.cremepe.jeton.mapper;

import br.com.cremepe.jeton.domain.Gestao;
import br.com.cremepe.jeton.dto.GestaoDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface GestaoMapper {

    @Mapping(target = "id", source = "idGestao")
    @Mapping(target = "nome", source = "nomeGestao")
    @Mapping(target = "dataInicio", source = "dtInicio")
    @Mapping(target = "dataFim", source = "dtFim")
    GestaoDTO toDto(Gestao gestao);
}