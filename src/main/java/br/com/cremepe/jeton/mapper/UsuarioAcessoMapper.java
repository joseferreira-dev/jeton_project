package br.com.cremepe.jeton.mapper;

import br.com.cremepe.jeton.domain.UsuarioAcesso;
import br.com.cremepe.jeton.dto.UsuarioAcessoDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UsuarioAcessoMapper {

    @Mapping(target = "idUsuario", source = "usuario.idUsuarioPessoa")
    @Mapping(target = "idNivel", source = "nivelAcesso.idNivel")
    @Mapping(target = "usuarioNome", source = "usuario.pessoa.nome")
    @Mapping(target = "nivelNome", source = "nivelAcesso.nomeNivel")
    UsuarioAcessoDTO toDto(UsuarioAcesso entity);
}