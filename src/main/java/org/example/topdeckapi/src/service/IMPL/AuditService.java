package org.example.topdeckapi.src.service.IMPL;

import lombok.RequiredArgsConstructor;
import org.example.topdeckapi.src.Repository.IAuditRepository;
import org.example.topdeckapi.src.model.Audit;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AuditService {
    private final IAuditRepository repository;

    public List<Audit> getAll (){
        return repository.findAll();
    }
}
