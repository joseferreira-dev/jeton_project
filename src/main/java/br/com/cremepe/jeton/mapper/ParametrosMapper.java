package br.com.cremepe.jeton.mapper;

import br.com.cremepe.jeton.domain.Parametros;
import br.com.cremepe.jeton.dto.ParametrosDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ParametrosMapper {
    @Mapping(target = "bloqueiaSistema", source = "bloqueaSistema")
    ParametrosDTO toDto(Parametros entity);
}