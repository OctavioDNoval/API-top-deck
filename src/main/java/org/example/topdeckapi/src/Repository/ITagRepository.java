package org.example.topdeckapi.src.Repository;

import org.example.topdeckapi.src.model.Tag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ITagRepository extends JpaRepository<Tag,Long> {
}
