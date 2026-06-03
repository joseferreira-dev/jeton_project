package br.com.cremepe.jeton.servico;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
public class EntidadeAnteriorService {

    @PersistenceContext
    private EntityManager entityManager;

    private final ObjectMapper mapper = new ObjectMapper();

    @Transactional(readOnly = true)
    public Map<String, Object> buscarEstadoAnterior(Object entidade, Long id) {
        if (entidade == null || id == null) return null;
        try {
            
            Object anterior = entityManager.find(entidade.getClass(), id);
            if (anterior == null) return null;
            // Converte para Map ignorando coleções lazy
            return mapper.convertValue(anterior, Map.class);
        } catch (Exception e) {
            // Log de erro, mas não interrompe a auditoria
            return null;
        }
    }
}