package br.com.cremepe.jeton.mapper;

import br.com.cremepe.jeton.domain.Conselheiro;
import br.com.cremepe.jeton.dto.ConselheiroDTO;
import br.com.cremepe.jeton.dto.ConselheiroResponseDTO;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ConselheiroMapper {
    @Mapping(target = "id", source = "idPessoa")
    @Mapping(target = "nome", source = "pessoa.nome")
    @Mapping(target = "email", source = "pessoa.email")
    @Mapping(target = "cpf", source = "pessoa.cpf")
    @Mapping(target = "crm", source = "crm")
    @Mapping(target = "situacao", source = "inSituacao")
    @Mapping(target = "senha", ignore = true)
    ConselheiroDTO toDto(Conselheiro conselheiro);

    @Mapping(target = "id", source = "idPessoa")
    @Mapping(target = "nome", source = "pessoa.nome")
    @Mapping(target = "email", source = "pessoa.email")
    @Mapping(target = "cpf", source = "pessoa.cpf")
    @Mapping(target = "crm", source = "crm")
    @Mapping(target = "situacao", source = "inSituacao")
    ConselheiroResponseDTO toResponseDto(Conselheiro conselheiro);
}