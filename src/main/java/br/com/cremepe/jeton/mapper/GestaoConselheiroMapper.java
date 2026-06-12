package br.com.cremepe.jeton.mapper;

import br.com.cremepe.jeton.domain.GestaoConselheiro;
import br.com.cremepe.jeton.dto.GestaoConselheiroDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface GestaoConselheiroMapper {

    @Mapping(target = "idGestao", source = "gestao.idGestao")
    @Mapping(target = "nomeGestao", source = "gestao.nomeGestao")
    @Mapping(target = "idConselheiro", source = "conselheiro.idPessoa")
    @Mapping(target = "nomeConselheiro", source = "conselheiro.pessoa.nome")
    @Mapping(target = "situacao", source = "inSituacao")
    GestaoConselheiroDTO toDto(GestaoConselheiro entity);
}