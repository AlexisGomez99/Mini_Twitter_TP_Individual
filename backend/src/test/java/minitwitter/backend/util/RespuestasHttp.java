package minitwitter.backend.util;

import com.jayway.jsonpath.JsonPath;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.charset.StandardCharsets;

// Ayuda para leer la respuesta de MockMvc en los tests web y comprobarla con asserts de JUnit.
public final class RespuestasHttp {

    private RespuestasHttp() {
    }

    public static int estado(MvcResult resultado) {
        return resultado.getResponse().getStatus();
    }

    // Se lee en UTF-8 porque la respuesta lleva acentos ("Sesión inválida") y el valor por
    // defecto de MockMvc los decodificaría mal.
    public static String cuerpo(MvcResult resultado) throws Exception {
        return resultado.getResponse().getContentAsString(StandardCharsets.UTF_8);
    }

    // Lee un valor del JSON de la respuesta con una ruta JsonPath, por ejemplo "$.message"
    // o "$.content[0].authorUsername".
    public static <T> T valorJson(MvcResult resultado, String rutaJson) throws Exception {
        return JsonPath.read(cuerpo(resultado), rutaJson);
    }

    public static int cantidadJson(MvcResult resultado, String rutaJson) throws Exception {
        Object valor = JsonPath.read(cuerpo(resultado), rutaJson);
        return ((java.util.List<?>) valor).size();
    }
}
