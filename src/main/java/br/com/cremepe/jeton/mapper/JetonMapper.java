package br.com.cremepe.jeton.mapper;

import br.com.cremepe.jeton.domain.Jeton;
import br.com.cremepe.jeton.dto.JetonDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface JetonMapper {

    @Mapping(target = "id", source = "idJeton")
    @Mapping(target = "idConselheiro", source = "conselheiro.idPessoa")
    @Mapping(target = "nomeConselheiro", source = "conselheiro.pessoa.nome")
    @Mapping(target = "idGestao", source = "gestao.idGestao")
    @Mapping(target = "nomeGestao", source = "gestao.nomeGestao")
    @Mapping(target = "situacao", source = "inSituacao")
    JetonDTO toDto(Jeton jeton);
}