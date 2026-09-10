package org.example.topdeckapi.src.DTOs.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ValorFacetaResponse {
    private String valor;
    private Long cantidad;
}
