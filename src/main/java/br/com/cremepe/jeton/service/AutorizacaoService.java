package br.com.cremepe.jeton.service;

import org.springframework.stereotype.Service;

import br.com.cremepe.jeton.annotation.Auditar;

@Service
public class AutorizacaoService {

    @Auditar(tabela = "acesso_negado", acao = "ACESSO_NEGADO", descricao = "Tentativa de acesso a recurso sem permissão adequada", dadosParametros = "{ 'usuarioId': #usuarioId, 'usuarioNome': #usuarioNome, 'metodo': #metodo, 'uri': #uri, 'permissaoNecessaria': #permissaoNecessaria }", capturarEstadoAnterior = false, auditarExcecao = false, incluirRetorno = false)
    public void registrarAcessoNegado(Integer usuarioId, String usuarioNome, String metodo, String uri,
            String permissaoNecessaria) {
    }
}