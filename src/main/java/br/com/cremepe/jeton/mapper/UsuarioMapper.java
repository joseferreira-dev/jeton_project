package br.com.cremepe.jeton.mapper;

import br.com.cremepe.jeton.domain.Usuario;
import br.com.cremepe.jeton.dto.UsuarioDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UsuarioMapper {

    @Mapping(target = "id", source = "idUsuarioPessoa")
    @Mapping(target = "nome", source = "pessoa.nome")
    @Mapping(target = "email", source = "pessoa.email")
    @Mapping(target = "situacao", source = "inSituacao")
    UsuarioDTO toDto(Usuario usuario);
}