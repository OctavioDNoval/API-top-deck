package org.example.topdeckapi.src.service.IMPL;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.example.topdeckapi.src.DTOs.CreateDTO.CreateTagDTO;
import org.example.topdeckapi.src.DTOs.DTO.TagDTO;
import org.example.topdeckapi.src.Repository.ITagRepository;
import org.example.topdeckapi.src.Security.AuditUtils;
import org.example.topdeckapi.src.model.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TagService {
    private final ITagRepository tagRepository;


    private final AuditUtils auditUtils;




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

    @Transactional
    public TagDTO save(CreateTagDTO dto) {
        Tag tag = new Tag();
        tag.setNombre(dto.getNombre());
        tag.setImg_url(dto.getImg_url());

        Tag tagGuardado = tagRepository.save(tag);

        auditUtils.setCurrentUserForAudit();

        return convertToDTO(tagGuardado);
    }

    @Transactional
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

                    auditUtils.setCurrentUserForAudit();

                    return convertToDTO(tagGuardado);
                });
    }

    @Transactional
    public boolean delete(Long id) {
        if(tagRepository.existsById(id)) {
            auditUtils.setCurrentUserForAudit();
            tagRepository.deleteById(id);
            return true;
        }else {
            return false;
        }
    }
}
