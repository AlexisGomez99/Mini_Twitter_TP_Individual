package minitwitter.backend.dto;

import java.util.List;

// Una página de resultados. page empieza en 0. hasNext le dice al front si hay otra
// página después de esta (para saber cuándo dejar de pedir al hacer scroll).
public record PageResponse<T>(
        List<T> content,
        int page,
        int totalPages,
        long totalElements,
        boolean hasNext
) {
    public static <T> PageResponse<T> armar(List<T> contenido, int pagina, int tamanio, long totalElementos) {
        int totalPaginas = (int) Math.ceil((double) totalElementos / tamanio);
        // (long) para que una página enorme (Integer.MAX_VALUE) no desborde al sumar 1.
        return new PageResponse<>(contenido, pagina, totalPaginas, totalElementos, (long) pagina + 1 < totalPaginas);
    }
}
