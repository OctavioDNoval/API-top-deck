package org.example.topdeckapi.src.DTOs.mappers;

import org.example.topdeckapi.src.DTOs.request.EventoRequest;
import org.example.topdeckapi.src.DTOs.response.EventoResponse;
import org.example.topdeckapi.src.model.Evento;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface EventoMapper {

    @Mapping(target = "idEvento", source = "uuid")
    EventoResponse toResponse(Evento evento);

    @Mapping(target = "estado", constant = "PROXIMAMENTE")
    Evento toEntity(EventoRequest eventoRequest);
}
