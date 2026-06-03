package br.com.cremepe.jeton.servico;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Service;
import java.util.HashMap;
import java.util.Map;

@Service
public class EntidadeAnteriorService {

    @PersistenceContext
    private EntityManager entityManager;

    public Map<String, Object> buscarEstadoAnterior(Class<?> entidadeClasse, Object id) {
        if (id == null)
            return null;
        try {
            Object entidade = entityManager.find(entidadeClasse, id);
            if (entidade == null)
                return null;
            // Converte para Map (simples, sem lidar com coleções lazy)
            return converterParaMap(entidade);
        } catch (Exception e) {
            return null;
        }
    }

    private Map<String, Object> converterParaMap(Object obj) {
        // Use Jackson ou reflection simples
        var map = new HashMap<String, Object>();
        try {
            for (var field : obj.getClass().getDeclaredFields()) {
                field.setAccessible(true);
                map.put(field.getName(), field.get(obj));
            }
        } catch (Exception e) {
            // fallback
        }
        return map;
    }
}