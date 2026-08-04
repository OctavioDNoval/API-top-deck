package org.example.topdeckapi.src.service.IMPL;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.example.topdeckapi.src.DTOs.mappers.TagMapper;
import org.example.topdeckapi.src.DTOs.request.TagRequest;
import org.example.topdeckapi.src.DTOs.response.TagResponse;
import org.example.topdeckapi.src.Exception.BussinesException;
import org.example.topdeckapi.src.Exception.ResourceNotFoundException;
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

    public TagResponse getTagById(String uuid) {
        return tagMapper.toResponse(tagRepository.findByUuid(uuid)
                .orElseThrow(()-> new ResourceNotFoundException("Tag not found")));
    }

    public String obtenerIdPorNombre(String nombre) {
        String nombreNormalizado = normalizar(nombre);

        Tag t = tagRepository.findByNombreNormalizado(nombreNormalizado)
                .orElseGet(()->{
                    Tag tag = new Tag();
                    tag.setNombre(nombre);
                    tag.setNombreNormalizado(nombreNormalizado);
                    return tagRepository.save(tag);
                });

        return t.getUuid();
    }

    private String normalizar(String input) {
        if (input == null) return null;

        String result = input.toLowerCase();

        result = java.text.Normalizer.normalize(result, java.text.Normalizer.Form.NFD);
        result = result.replaceAll("\\p{M}", "");

        result = result.replaceAll("[-]", "");

        return result;
    }

    public TagResponse save(TagRequest request) {
        if(tagRepository.existsByNombre(request.getNombre())){
            throw new BussinesException("El nombre de tag ya existe en el sistema");
        }

        Tag tag = new Tag();
        tag.setNombre(request.getNombre());
        tag.setNombreNormalizado(normalizar(request.getNombre()));
        tag.setImgUrl(request.getImgUrl());
        Tag tagGuardado = tagRepository.save(tag);
        auditUtils.setCurrentUserForAudit();

        return tagMapper.toResponse(tagGuardado);
    }

    public TagResponse actualizarTag(String uuid, TagRequest newTag) {
        Tag tag = tagRepository.findByUuid(uuid).orElseThrow(() -> new ResourceNotFoundException("Tag not found"));
        if(tagRepository.existsByNombre(newTag.getNombre())){
            throw new BussinesException("El nombre de tag ya existe en el sistema");
        }
        tag.setNombre(newTag.getNombre());
        tag.setNombreNormalizado(normalizar(newTag.getNombre()));
        if(newTag.getImgUrl() != null){
            tag.setImgUrl(newTag.getImgUrl());
        }
        Tag tagGuardado = tagRepository.save(tag);
        auditUtils.setCurrentUserForAudit();
        return tagMapper.toResponse(tagGuardado);
    }


    public boolean delete(String uuid) {
        Tag tag = tagRepository.findByUuid(uuid)
                .orElseThrow(() -> new ResourceNotFoundException("Tag not found"));
        auditUtils.setCurrentUserForAudit();
        tagRepository.delete(tag);
        return true;
    }
}
