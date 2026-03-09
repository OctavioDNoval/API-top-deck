package org.example.topdeckapi.src.controller;


import lombok.RequiredArgsConstructor;


import org.example.topdeckapi.src.DTOs.request.TagRequest;
import org.example.topdeckapi.src.DTOs.response.TagResponse;
import org.example.topdeckapi.src.service.IMPL.TagService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/tags")
public class TagController {
    private final TagService tagService;

    @GetMapping("/public/getAll")
    public ResponseEntity<List<TagResponse>> getAllTags() {
        return ResponseEntity.ok(tagService.getAllTags());
    }

    @PostMapping("/admin/post")
    public ResponseEntity<TagResponse> addTag(@RequestBody TagRequest dto) {
        return ResponseEntity.ok(tagService.save(dto));
    }

    @PatchMapping("/admin/edit/{id}")
    public ResponseEntity<TagResponse> editTag(@PathVariable Long id, @RequestBody TagRequest newTag) {
        return ResponseEntity.ok(tagService.actualizarTag(id, newTag));
    }

    @DeleteMapping("/admin/delete/{id}")
    public ResponseEntity<Void> delete (@PathVariable Long id){
        boolean res = tagService.delete(id);
        if(res){
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }
}
