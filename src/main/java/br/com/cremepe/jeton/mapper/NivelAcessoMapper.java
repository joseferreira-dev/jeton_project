package br.com.cremepe.jeton.mapper;

import br.com.cremepe.jeton.domain.NivelAcesso;
import br.com.cremepe.jeton.dto.NivelAcessoDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface NivelAcessoMapper {
    @Mapping(target = "id", source = "idNivel")
    @Mapping(target = "nome", source = "nomeNivel")
    NivelAcessoDTO toDto(NivelAcesso entity);
}