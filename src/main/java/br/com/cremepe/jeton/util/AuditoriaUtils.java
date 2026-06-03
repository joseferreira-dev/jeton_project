package br.com.cremepe.jeton.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

public class AuditoriaUtils {

    private static final ObjectMapper mapper = new ObjectMapper();

    static {
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    public static String toJson(Object object) {

        try {

            String json = mapper.writerWithDefaultPrettyPrinter()
                    .writeValueAsString(object);

            System.out.println(json);

            return json;

        } catch (Exception e) {

            System.err.println("ERRO AO SERIALIZAR:");
            e.printStackTrace();

            return "{}";
        }
    }
}