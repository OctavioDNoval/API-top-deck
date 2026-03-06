package org.example.topdeckapi.src.DTOs.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaginacionResponse <T>{
    private List<T> contenido;
    private Integer pagina;
    private Integer tamanio;
    private Long totalElementos;
    private Integer totalPaginas;
    private Boolean first;
    private Boolean last;
}
