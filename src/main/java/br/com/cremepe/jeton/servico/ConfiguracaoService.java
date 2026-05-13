package br.com.cremepe.jeton.servico;

import br.com.cremepe.jeton.dominio.*;
import br.com.cremepe.jeton.repositorio.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Serviço que centraliza a gestão de tabelas de apoio, regras de negócio e parâmetros globais.
 * Consolida múltiplas fachadas do sistema legado.
 */
@Service
public class ConfiguracaoService {

    @Autowired
    private GestaoRepository gestaoRepository;

    @Autowired
    private ResolucaoRepository resolucaoRepository;

    @Autowired
    private RegrasRepository regrasRepository;

    @Autowired
    private PortariaRepository portariaRepository;

    @Autowired
    private TipoAnexoRepository tipoAnexoRepository;

    @Autowired
    private ParametrosRepository parametrosRepository;

    // ==========================================
    // GESTÃO DE MANDATOS (GESTAO)
    // ==========================================

    @Transactional(readOnly = true)
    public List<Gestao> listarTodasGestoes() {
        return gestaoRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Optional<Gestao> obterGestaoVigente() {
        return gestaoRepository.buscarGestaoVigente();
    }

    @Transactional
    public Gestao salvarGestao(Gestao gestao) {
        return gestaoRepository.save(gestao);
    }

    // ==========================================
    // REGRAS E BASE LEGAL
    // ==========================================

    @Transactional(readOnly = true)
    public List<Resolucao> listarResolucoesAtivas() {
        return resolucaoRepository.findByInRevogado("N");
    }

    @Transactional(readOnly = true)
    public List<Regras> listarRegrasPorResolucao(Integer idResolucao) {
        return regrasRepository.findByResolucaoIdResolucaoAndInRevogado(idResolucao, "N");
    }

    @Transactional(readOnly = true)
    public List<Portaria> listarPortariasVigentes() {
        return portariaRepository.findByInRevogado("N");
    }

    // ==========================================
    // TABELAS DE APOIO E PARÂMETROS
    // ==========================================

    @Transactional(readOnly = true)
    public List<TipoAnexo> listarTiposAnexo() {
        return tipoAnexoRepository.findAll();
    }

    @Transactional(readOnly = true)
    public boolean isSistemaBloqueado() {
        return parametrosRepository.obterStatusBloqueioSistema()
                .map(status -> "S".equalsIgnoreCase(status))
                .orElse(false);
    }

    @Transactional
    public void alternarBloqueioSistema(boolean bloquear) {
        // Como não temos um ID numérico real, buscamos o que existir e atualizamos
        List<Parametros> lista = parametrosRepository.findAll();
        if (!lista.isEmpty()) {
            Parametros p = lista.get(0);
            p.setBloqueaSistema(bloquear ? "S" : "N");
            parametrosRepository.save(p);
        }
    }
}