package org.example.topdeckapi.src.service.IMPL;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.example.topdeckapi.src.DTOs.CreateDTO.CreateCategoriaDTO;
import org.example.topdeckapi.src.DTOs.UpdateDTO.UpdateCategoriaDTO;
import org.example.topdeckapi.src.Repository.ICategoriasRepo;
import org.example.topdeckapi.src.Security.AuditUtils;
import org.example.topdeckapi.src.model.Categoria;
import org.example.topdeckapi.src.service.Interface.ICategoriaService;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CategoriaService implements ICategoriaService {
    private final ICategoriasRepo categoriasRepo;
    private final AuditUtils auditUtils;

    public List<Categoria> findAll() {
        return categoriasRepo.findAll();
    }

    public Optional<Categoria> buscarPorId(Long idCategoria) {
        return categoriasRepo.findById(idCategoria);
    }

    @Transactional
    public Categoria guardar(CreateCategoriaDTO newCategoria) {
        Categoria categoria = new Categoria();
        categoria.setNombre(newCategoria.getNombre());

        auditUtils.setCurrentUserForAudit();

        return categoriasRepo.save(categoria);
    }

    @Transactional
    public Optional<Categoria> actualizar(UpdateCategoriaDTO categoria, Long idCategoria) {
        return categoriasRepo.findById(idCategoria)
                .map(c->{
                    if(categoria.getNombre() != null && !categoria.getNombre().trim().isEmpty()){
                        c.setNombre(categoria.getNombre());
                    }

                    auditUtils.setCurrentUserForAudit();
                    return categoriasRepo.save(c);
                });
    }

    @Transactional
    public boolean borrarCategoria(Long idCategoria) {
        if(categoriasRepo.existsById(idCategoria)){
            auditUtils.setCurrentUserForAudit();
            categoriasRepo.deleteById(idCategoria);
            return true;
        }else{
            return false;
        }
    }
}
