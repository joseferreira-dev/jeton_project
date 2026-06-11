package br.com.cremepe.jeton.mapper;

import br.com.cremepe.jeton.domain.Regras;
import br.com.cremepe.jeton.dto.RegraDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface RegrasMapper {
    @Mapping(target = "id", source = "idRegra")
    @Mapping(target = "nome", source = "nomeRegra")
    @Mapping(target = "descricao", source = "descricao")
    @Mapping(target = "pontos", source = "pontos")
    @Mapping(target = "inRevogado", source = "inRevogado")
    @Mapping(target = "pontosLimitesTurno", source = "pontosLimitesTurno")
    @Mapping(target = "inJudicante", source = "inJudicante")
    @Mapping(target = "resolucaoId", source = "resolucao.idResolucao")
    @Mapping(target = "portariaId", source = "portaria.idPortaria")
    RegraDTO toDto(Regras regras);
}