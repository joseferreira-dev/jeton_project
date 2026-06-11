package br.com.cremepe.jeton.mapper;

import br.com.cremepe.jeton.domain.Resolucao;
import br.com.cremepe.jeton.dto.ResolucaoDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ResolucaoMapper {
    @Mapping(target = "id", source = "idResolucao")
    @Mapping(target = "numero", source = "numero")
    @Mapping(target = "ano", source = "ano")
    @Mapping(target = "dtInicioVigencia", source = "dtInicioVigencia")
    @Mapping(target = "dtFimVigencia", source = "dtFimVigencia")
    @Mapping(target = "linkPublicado", source = "linkPublicado")
    @Mapping(target = "inRevogado", source = "inRevogado")
    @Mapping(target = "ementa", source = "ementa")
    @Mapping(target = "pontosPorJeton", source = "pontosPorJeton")
    @Mapping(target = "maxJetonsDia", source = "maxJetonsDia")
    @Mapping(target = "maxJetonsPeriodo", source = "maxJetonsPeriodo")
    @Mapping(target = "maxJetonsMes", source = "maxJetonsMes")
    @Mapping(target = "valorJeton", source = "valorJeton")
    ResolucaoDTO toDto(Resolucao resolucao);
}