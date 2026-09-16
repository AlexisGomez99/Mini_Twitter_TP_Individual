package minitwitter.backend.web;

import minitwitter.backend.web.dto.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MissingRequestCookieException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

// El término advice viene de AOP: un bloque de código que se ejecuta antes y/o después
// de otro. Acá centralizamos el manejo de errores para todos los controllers.
@RestControllerAdvice
public class GlobalExceptionHandler {


    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ErrorResponse> handleRuntimeExceptions(RuntimeException ex) {
        return ResponseEntity
                .badRequest()
                .body(new ErrorResponse(ex.getMessage()));
    }

    @ExceptionHandler({HttpMessageNotReadableException.class,
            MissingServletRequestParameterException.class})
    public ResponseEntity<ErrorResponse> handleSpringMVCParams() {
        ErrorResponse error = new ErrorResponse("Parámetros inválidos");
        return ResponseEntity.badRequest().body(error);
    }

    // Se lanza cuando un endpoint que requiere estar logueado (@CookieValue("token"))
    // no recibe la cookie: sin este handler, caía en el catch-all de abajo y devolvía
    // un 500 en vez de un 400 claro.
    @ExceptionHandler(MissingRequestCookieException.class)
    public ResponseEntity<ErrorResponse> handleMissingToken() {
        ErrorResponse error = new ErrorResponse("No autenticado");
        return ResponseEntity.badRequest().body(error);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException() {
        ErrorResponse error = new ErrorResponse("Algo salió mal, contactar al administrador.");
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
}
