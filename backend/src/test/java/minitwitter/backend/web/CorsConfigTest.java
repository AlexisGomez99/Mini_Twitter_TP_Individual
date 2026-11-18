package minitwitter.backend.web;

import minitwitter.backend.main.Main;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static minitwitter.backend.util.RespuestasHttp.estado;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;

// Estos tests no usan la base de datos (solo consultan la configuración de CORS),
// por eso no necesitan vaciar las tablas ni cargar el escenario inicial.
@SpringBootTest(classes = Main.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CorsConfigTest {

    private static final String ORIGEN_AJENO = "http://sitio-ajeno.com";

    @Autowired
    private MockMvc mockMvc;

    private MvcResult preflight(String origen, String metodo, String cabeceras) throws Exception {
        var pedido = options("/login")
                .header(HttpHeaders.ORIGIN, origen)
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, metodo);
        if (cabeceras != null) {
            pedido.header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, cabeceras);
        }
        return mockMvc.perform(pedido).andReturn();
    }

    @Test
    @DisplayName("El preflight desde el frontend se acepta y permite enviar credenciales")
    void preflight_aceptaElOrigenDelFrontendYPermiteCredenciales() throws Exception {
        // Setup: un pedido de verificación previa (preflight) desde el origen del frontend
        // (WebConfig.ORIGEN_FRONTEND)

        // Ejercitación: enviar el preflight
        MvcResult resultado = preflight(WebConfig.ORIGEN_FRONTEND, "POST", null);

        // Verificación: se acepta y se permite el uso de cookies
        assertEquals(200, estado(resultado), "El preflight del frontend debe aceptarse");
        assertEquals(WebConfig.ORIGEN_FRONTEND,
                resultado.getResponse().getHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN),
                "Debe permitir el origen del frontend");
        assertEquals("true", resultado.getResponse().getHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS),
                "Debe permitir enviar credenciales (la cookie de sesión)");
    }

    @Test
    @DisplayName("El preflight de un POST con JSON permite la cabecera Content-Type")
    void preflight_permiteLaCabeceraContentTypeEnUnPostConJson() throws Exception {
        // Setup: un preflight de un POST que enviará JSON
        String cabeceraSolicitada = "Content-Type";

        // Ejercitación: enviar el preflight
        MvcResult resultado = preflight(WebConfig.ORIGEN_FRONTEND, "POST", cabeceraSolicitada);

        // Verificación: la cabecera Content-Type está permitida
        String permitidas = resultado.getResponse().getHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS);
        assertNotNull(permitidas, "Debe indicar qué cabeceras permite");
        assertTrue(permitidas.toLowerCase().contains("content-type"),
                "Debe permitir Content-Type para poder enviar JSON");
    }

    @Test
    @DisplayName("El preflight desde otro origen se rechaza con 403")
    void preflight_rechazaUnOrigenAjeno() throws Exception {
        // Setup: un preflight desde un sitio que no es el frontend
        // (ORIGEN_AJENO)

        // Ejercitación: enviar el preflight
        MvcResult resultado = preflight(ORIGEN_AJENO, "POST", null);

        // Verificación: se rechaza
        assertEquals(403, estado(resultado), "Un origen ajeno no debe aceptarse");
    }
}
