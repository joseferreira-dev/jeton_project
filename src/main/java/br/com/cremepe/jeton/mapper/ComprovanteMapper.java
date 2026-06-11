package br.com.cremepe.jeton.mapper;

import br.com.cremepe.jeton.domain.Comprovante;
import br.com.cremepe.jeton.dto.ComprovanteDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ComprovanteMapper {

    @Mapping(target = "id", source = "idComprovante")
    @Mapping(target = "idTipoAnexo", source = "tipoAnexo.idTipo")
    @Mapping(target = "tipoAnexoNome", source = "tipoAnexo.nome")
    ComprovanteDTO toDto(Comprovante comprovante);
}