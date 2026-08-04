package org.example.topdeckapi.src.DTOs.mappers;

import org.example.topdeckapi.src.DTOs.request.TagRequest;
import org.example.topdeckapi.src.DTOs.response.TagResponse;
import org.example.topdeckapi.src.model.Tag;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface TagMapper {

    @Mapping(target = "idTag", source = "uuid")
    TagResponse toResponse(Tag tag);

    Tag toEntity(TagRequest tagRequest);
}
