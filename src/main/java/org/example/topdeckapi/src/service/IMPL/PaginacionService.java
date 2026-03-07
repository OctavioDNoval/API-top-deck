package org.example.topdeckapi.src.service.IMPL;

import org.example.topdeckapi.src.DTOs.response.PaginacionResponse;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class PaginacionService {
    public <T, R> PaginacionResponse<R> crearPaginacionResponse(
            Page<T> pagina,
            Integer numeroPagina,
            Integer tamanio,
            Function<T,R> mapper
    ){
        List<R> contenido = pagina.getContent()
                .stream()
                .map(mapper)
                .collect(Collectors.toList());

        return PaginacionResponse.<R>builder()
                .contenido(contenido)
                .pagina(numeroPagina)
                .tamanio(tamanio)
                .totalElementos(pagina.getTotalElements())
                .totalPaginas(pagina.getTotalPages())
                .last(pagina.isLast())
                .first(pagina.isFirst())
                .build();
    }
}
