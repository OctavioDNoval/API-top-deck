package org.example.topdeckapi.src.DTOs.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TagDTO {
    private Long idTag;
    private String nombre;
    private String img_url;
}
