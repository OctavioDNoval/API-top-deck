package org.example.topdeckapi.src.DTOs.CreateDTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CreateTagDTO {
    private String nombre;
    private String img_url;
}
