package org.example.topdeckapi.src.DTOs.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class TagResponse {
    private String idTag;
    private String nombre;
    private String imgUrl;
}
