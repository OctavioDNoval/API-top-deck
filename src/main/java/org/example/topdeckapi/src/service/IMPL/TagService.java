package org.example.topdeckapi.src.service.IMPL;

import org.example.topdeckapi.src.DTOs.CreateDTO.CreateTagDTO;
import org.example.topdeckapi.src.DTOs.DTO.TagDTO;
import org.example.topdeckapi.src.Repository.ITagRepository;
import org.example.topdeckapi.src.model.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class TagService {
    private final ITagRepository tagRepository;

    @Autowired
    public TagService(ITagRepository tagRepository) {
        this.tagRepository = tagRepository;
    }

    protected TagDTO convertToDTO(Tag tag) {
        TagDTO tagDTO = new TagDTO();
        tagDTO.setIdTag(tag.getIdTag());
        tagDTO.setNombre(tag.getNombre());
        tagDTO.setImg_url(tag.getImg_url());
        return tagDTO;
    }

    protected Tag convertToEntity(TagDTO tagDTO) {
        Tag tag = new Tag();
        tag.setIdTag(tagDTO.getIdTag());
        tag.setNombre(tagDTO.getNombre());
        tag.setImg_url(tagDTO.getImg_url());
        return tag;
    }

    public List<TagDTO> getAllTags() {
        return tagRepository.findAll()
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public TagDTO save(CreateTagDTO dto) {
        Tag tag = new Tag();
        tag.setNombre(dto.getNombre());
        tag.setImg_url(dto.getImg_url());

        Tag tagGuardado = tagRepository.save(tag);
        return convertToDTO(tagGuardado);
    }

    public Optional<TagDTO> actualizarTag(Long id, Tag newTag) {
        return tagRepository.findById(id)
                .map(t -> {
                    if((newTag.getNombre()!=null) && (!newTag.getNombre().isEmpty())) {
                        t.setNombre(newTag.getNombre());
                    }
                    if(newTag.getImg_url()!=null){
                        t.setImg_url(newTag.getImg_url());
                    }

                    Tag tagGuardado = tagRepository.save(t);

                    return convertToDTO(tagGuardado);
                });
    }

    public boolean delete(Long id) {
        if(tagRepository.existsById(id)) {
            tagRepository.deleteById(id);
            return true;
        }else {
            return false;
        }
    }
}
