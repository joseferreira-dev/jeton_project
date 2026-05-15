package br.com.cremepe.jeton.servico;

import br.com.cremepe.jeton.dominio.Portaria;
import br.com.cremepe.jeton.dominio.Regras;
import br.com.cremepe.jeton.dominio.Resolucao;
import br.com.cremepe.jeton.repositorio.RegrasRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class RegrasService {

    @Autowired
    private RegrasRepository repository;

    @Transactional(readOnly = true)
    public List<Regras> listarTodos() {
        return repository.findAll();
    }

    @Transactional(readOnly = true)
    public Optional<Regras> buscarPorId(Integer id) {
        return repository.findById(id);
    }

    @Transactional
    public Regras salvar(Regras regra) {
        return repository.save(regra);
    }

    @Transactional
    public void excluir(Integer id) {
        repository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public List<Resolucao> listarResolucoesComRegras() { 
        return repository.findResolucoesComRegras(); 
    }

    @Transactional(readOnly = true)
    public List<Portaria> listarPortariasComRegras() { 
        return repository.findPortariasComRegras(); 
    }

    @Transactional(readOnly = true)
    public List<Portaria> listarPortariasCompativeis(Integer idResolucao) { 
        return repository.findPortariasCompativeis(idResolucao); 
    }

    @Transactional(readOnly = true)
    public List<Resolucao> listarResolucoesCompativeis(Integer idPortaria) { 
        return repository.findResolucoesCompativeis(idPortaria); 
    }

    @Transactional(readOnly = true)
    public List<Regras> listarRegrasExatas(Integer idResolucao, Integer idPortaria) { 
        return repository.findRegrasExatas(idResolucao, idPortaria); 
    }
    
}