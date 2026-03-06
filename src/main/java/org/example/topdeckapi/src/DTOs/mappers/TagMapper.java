package org.example.topdeckapi.src.DTOs.mappers;

import org.example.topdeckapi.src.DTOs.request.TagRequest;
import org.example.topdeckapi.src.DTOs.response.TagResponse;
import org.example.topdeckapi.src.model.Tag;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface TagMapper {

    TagResponse toResponse(Tag tag);

    @Mapping(target = "idTag", ignore = true)
    Tag toEntity(TagRequest tagRequest);
}
