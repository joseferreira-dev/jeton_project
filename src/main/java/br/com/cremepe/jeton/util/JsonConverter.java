package br.com.cremepe.jeton.util;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.ser.impl.SimpleBeanPropertyFilter;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.hibernate.proxy.HibernateProxy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class JsonConverter {

    private static final Logger log = LoggerFactory.getLogger(JsonConverter.class);
    private final ObjectMapper mapper;

    public JsonConverter() {
        this.mapper = new ObjectMapper();
        this.mapper.registerModule(new JavaTimeModule());
        this.mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        this.mapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
        this.mapper.setDefaultPropertyInclusion(JsonInclude.Include.NON_NULL);
        SimpleFilterProvider filterProvider = new SimpleFilterProvider();
        filterProvider.addFilter("hibernateLazyFilter", SimpleBeanPropertyFilter.serializeAllExcept(
                "hibernateLazyInitializer", "handler", "fieldHandler"));
        this.mapper.setFilterProvider(filterProvider);
    }

    public String toJson(Object obj) {
        try {
            return mapper.writeValueAsString(obj);
        } catch (Exception e) {
            log.error("Erro ao serializar objeto para JSON", e);
            return "{}";
        }
    }

    public Map<String, Object> converterParaMap(Object obj) {
        if (obj == null)
            return Map.of();
        try {
            if (obj instanceof HibernateProxy) {
                obj = ((HibernateProxy) obj).getHibernateLazyInitializer().getImplementation();
            }
            return mapper.convertValue(obj, Map.class);
        } catch (Exception e) {
            log.error("Erro ao converter objeto para Map (auditoria). Usando fallback.", e);
            return extractBasicFields(obj);
        }
    }

    private Map<String, Object> extractBasicFields(Object obj) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("_class", obj.getClass().getSimpleName());
        try {
            java.lang.reflect.Field idField = obj.getClass().getDeclaredField("id");
            idField.setAccessible(true);
            map.put("id", idField.get(obj));
        } catch (Exception e) {
            try {
                java.lang.reflect.Method m = obj.getClass().getMethod("getId");
                map.put("id", m.invoke(obj));
            } catch (Exception ignored) {
            }
        }
        return map;
    }
}