package org.example.topdeckapi.src.controller;

import jakarta.servlet.ServletResponse;
import lombok.RequiredArgsConstructor;
import org.example.topdeckapi.src.DTOs.CreateDTO.CreateTagDTO;
import org.example.topdeckapi.src.DTOs.DTO.TagDTO;
import org.example.topdeckapi.src.model.Tag;
import org.example.topdeckapi.src.service.IMPL.TagService;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.support.ResourceTransactionManager;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/tags")
public class TagController {
    private final TagService tagService;

    @GetMapping("/public/getAll")
    public ResponseEntity<List<TagDTO>> getAllTags() {
        return ResponseEntity.ok(tagService.getAllTags());
    }

    @PostMapping("/admin/post")
    public ResponseEntity<TagDTO> addTag(@RequestBody CreateTagDTO dto, ServletResponse servletResponse) {
        return ResponseEntity.ok(tagService.save(dto));
    }

    @PatchMapping("/admin/edit/{id}")
    public ResponseEntity<TagDTO> editTag(@PathVariable Long id, @RequestBody Tag newTag) {
        return tagService.actualizarTag(id,newTag)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/admin/delete/{id}")
    public ResponseEntity<Boolean> delete (@PathVariable Long id){
        boolean res = tagService.delete(id);
        if(res){
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }
}
