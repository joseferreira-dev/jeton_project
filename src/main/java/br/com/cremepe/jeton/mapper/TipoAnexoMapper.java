package br.com.cremepe.jeton.mapper;

import br.com.cremepe.jeton.domain.TipoAnexo;
import br.com.cremepe.jeton.dto.TipoAnexoDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface TipoAnexoMapper {

    @Mapping(target = "id", source = "idTipo")
    @Mapping(target = "exigePublicacao", expression = "java(tipoAnexo.isExigePublicacao())")
    TipoAnexoDTO toDto(TipoAnexo tipoAnexo);
}