package br.com.cremepe.jeton.dto;

import br.com.cremepe.jeton.dominio.NivelAcesso;
import br.com.cremepe.jeton.dominio.Usuario;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * DTO (Data Transfer Object) utilizado para transportar dados agrupados 
 * de Utilizadores e Níveis de Acesso entre a camada de Serviço e a camada de Apresentação (Controladores).
 * NOTA: Esta classe não é uma Entidade JPA, não tem reflexo direto na base de dados.
 */
public class PermissaoAcessoDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    // A melhor prática em Java moderno é programar para a interface (List)
    private List<Usuario> usuarios = new ArrayList<>();
    private List<NivelAcesso> niveisAcesso = new ArrayList<>();
    
    public PermissaoAcessoDTO() {
    }    

    public PermissaoAcessoDTO(List<Usuario> usuarios, List<NivelAcesso> niveisAcesso){
        this.usuarios = usuarios;
        this.niveisAcesso = niveisAcesso;
    }    
    
    // ==========================================
    // GETTERS E SETTERS
    // ==========================================

    public List<Usuario> getUsuarios() {
        return usuarios;
    }

    public void setUsuarios(List<Usuario> usuarios) {
        this.usuarios = usuarios;
    }

    public List<NivelAcesso> getNiveisAcesso() {
        return niveisAcesso;
    }

    public void setNiveisAcesso(List<NivelAcesso> niveisAcesso) {
        this.niveisAcesso = niveisAcesso;
    }
}