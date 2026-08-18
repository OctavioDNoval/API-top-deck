package org.example.topdeckapi.src.service.IMPL;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.example.topdeckapi.src.DTOs.mappers.CategoriaMapper;
import org.example.topdeckapi.src.DTOs.request.CategoriaRequest;
import org.example.topdeckapi.src.DTOs.response.CategoriaResponse;
import org.example.topdeckapi.src.Exception.ResourceNotFoundException;
import org.example.topdeckapi.src.Repository.ICategoriasRepo;
import org.example.topdeckapi.src.model.Categoria;
import org.example.topdeckapi.src.service.Interface.ICategoriaService;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class CategoriaService implements ICategoriaService {
    private final ICategoriasRepo categoriasRepo;
    private final CategoriaMapper categoriaMapper;
    private final AuditService auditService;

    public List<CategoriaResponse> findAll() {
        return categoriasRepo.findAll().stream()
                .map(categoriaMapper::toResponse)
                .collect(Collectors.toList());
    }

    public CategoriaResponse buscarPorId(String uuid) {
        Categoria c = categoriasRepo.findByUuid(uuid)
                .orElseThrow(()-> new ResourceNotFoundException("Categoria no encontrada"));

        return categoriaMapper.toResponse(c);
    }

    public String obtenerIdPorNombre(String nombre) {
        Categoria c = categoriasRepo.findByNombre(nombre)
                .orElseGet(()->{
                    Categoria categoria = new Categoria();
                    categoria.setNombre(nombre);
                    Categoria guardada = categoriasRepo.save(categoria);
                    auditService.registrar("INSERT", "categoria");
                    return guardada;
                });

        return c.getUuid();
    }

    public CategoriaResponse guardar(CategoriaRequest newCategoria) {
        Categoria categoria = new Categoria();
        categoria.setNombre(newCategoria.getNombre());
        Categoria categoriaGuardada = categoriasRepo.save(categoria);
        auditService.registrar("INSERT", "categoria");
        return categoriaMapper.toResponse(categoriaGuardada);
    }

    public CategoriaResponse actualizar(CategoriaRequest categoria, String uuid) {
        Categoria c = categoriasRepo.findByUuid(uuid)
                .orElseThrow(()-> new ResourceNotFoundException("Categoria no encontrada"));
        if(!c.getNombre().equals(categoria.getNombre())) {
            c.setNombre(categoria.getNombre());
        }
        Categoria categoriaGuardada = categoriasRepo.save(c);
        auditService.registrar("UPDATE", "categoria");
        return categoriaMapper.toResponse(categoriaGuardada);
    }

    public boolean borrarCategoria(String uuid) {
        Categoria c = categoriasRepo.findByUuid(uuid)
                .orElseThrow(() -> new ResourceNotFoundException("Categoria no encontrada"));
        auditService.registrar("DELETE", "categoria");
        categoriasRepo.delete(c);
        return true;
    }
}
