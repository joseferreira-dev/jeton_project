package br.com.cremepe.jeton.mapper;

import br.com.cremepe.jeton.domain.Portaria;
import br.com.cremepe.jeton.dto.PortariaDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface PortariaMapper {

    @Mapping(target = "id", source = "idPortaria")
    @Mapping(target = "dtInicioVigencia", source = "dtInicioVigencia")
    @Mapping(target = "dtFimVigencia", source = "dtFimVigencia")
    @Mapping(target = "linkPublicado", source = "linkPublicado")
    @Mapping(target = "inRevogado", source = "inRevogado")
    PortariaDTO toDto(Portaria portaria);
}