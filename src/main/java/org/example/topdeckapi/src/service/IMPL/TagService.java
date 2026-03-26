package org.example.topdeckapi.src.service.IMPL;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.example.topdeckapi.src.DTOs.mappers.TagMapper;
import org.example.topdeckapi.src.DTOs.request.TagRequest;
import org.example.topdeckapi.src.DTOs.response.TagResponse;
import org.example.topdeckapi.src.Repository.ITagRepository;
import org.example.topdeckapi.src.Security.AuditUtils;
import org.example.topdeckapi.src.model.Categoria;
import org.example.topdeckapi.src.model.Tag;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class TagService {
    private final ITagRepository tagRepository;
    private final AuditUtils auditUtils;
    private final TagMapper tagMapper;

    public List<TagResponse> getAllTags() {
        return tagRepository.findAll()
                .stream()
                .map(tagMapper::toResponse)
                .collect(Collectors.toList());
    }

    public Long obtenerIdPorNombre(String nombre) {
        Tag t = tagRepository.findByNombre(nombre)
                .orElseGet(()->{
                   Tag tag = new Tag();
                   tag.setNombre(nombre);
                   return tagRepository.save(tag);
                });

        return t.getIdTag();
    }

    public TagResponse save(TagRequest request) {
        if(tagRepository.existsByNombre(request.getNombre())){
            throw new RuntimeException("El nombre existe en el sistema");
        }

        Tag tag = new Tag();
        tag.setNombre(request.getNombre());
        tag.setImgUrl(request.getImgUrl());
        Tag tagGuardado = tagRepository.save(tag);
        auditUtils.setCurrentUserForAudit();

        return tagMapper.toResponse(tagGuardado);
    }

    public TagResponse actualizarTag(Long id, TagRequest newTag) {
        Tag tag = tagRepository.findById(id).orElseThrow(() -> new RuntimeException("Tag not found"));
        if(tagRepository.existsByNombre(newTag.getNombre())){
            throw new RuntimeException("El nombre existe en el sistema");
        }
        tag.setNombre(newTag.getNombre());
        if(newTag.getImgUrl() != null){
            tag.setImgUrl(newTag.getImgUrl());
        }
        Tag tagGuardado = tagRepository.save(tag);
        auditUtils.setCurrentUserForAudit();
        return tagMapper.toResponse(tagGuardado);
    }


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
