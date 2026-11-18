package minitwitter.backend.dto;

import java.util.List;

public record PageResponse<T>(
        List<T> content,
        int page,
        int totalPages,
        long totalElements,
        boolean hasNext
) {
    public static <T> PageResponse<T> armar(List<T> contenido, int pagina, int tamanio, long totalElementos) {
        int totalPaginas = (int) Math.ceil((double) totalElementos / tamanio);
        return new PageResponse<>(contenido, pagina, totalPaginas, totalElementos, (long) pagina + 1 < totalPaginas);
    }
}
