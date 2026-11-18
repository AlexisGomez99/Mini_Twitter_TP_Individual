package minitwitter.backend.web;

import minitwitter.backend.web.dto.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MissingRequestCookieException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    public static final String ERROR_PARAMETROS_INVALIDOS = "Parámetros inválidos";
    public static final String ERROR_NO_AUTENTICADO = "No autenticado";
    public static final String ERROR_INESPERADO = "Algo salió mal, contactar al administrador.";

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ErrorResponse> manejarErroresDeNegocio(RuntimeException excepcion) {
        return ResponseEntity
                .badRequest()
                .body(new ErrorResponse(excepcion.getMessage()));
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ErrorResponse> manejarEstadoHttp(ResponseStatusException excepcion) {
        return ResponseEntity
                .status(excepcion.getStatusCode())
                .body(new ErrorResponse(excepcion.getReason()));
    }

    @ExceptionHandler({HttpMessageNotReadableException.class,
            MissingServletRequestParameterException.class,
            MethodArgumentTypeMismatchException.class})
    public ResponseEntity<ErrorResponse> manejarParametrosInvalidos() {
        ErrorResponse respuestaError = new ErrorResponse(ERROR_PARAMETROS_INVALIDOS);
        return ResponseEntity.badRequest().body(respuestaError);
    }

    @ExceptionHandler(MissingRequestCookieException.class)
    public ResponseEntity<ErrorResponse> manejarTokenFaltante() {
        ErrorResponse respuestaError = new ErrorResponse(ERROR_NO_AUTENTICADO);
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(respuestaError);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> manejarErrorInesperado() {
        ErrorResponse respuestaError = new ErrorResponse(ERROR_INESPERADO);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(respuestaError);
    }
}
