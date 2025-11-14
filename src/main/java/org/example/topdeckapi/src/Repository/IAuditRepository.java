package org.example.topdeckapi.src.Repository;

import org.example.topdeckapi.src.model.Audit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface IAuditRepository extends JpaRepository<Audit,Long> {
}
