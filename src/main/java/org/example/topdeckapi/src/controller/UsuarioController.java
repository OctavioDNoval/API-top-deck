package org.example.topdeckapi.src.controller;

import jakarta.validation.Valid;
import org.example.topdeckapi.src.DTOs.DTO.UsuarioDTO;
import org.example.topdeckapi.src.DTOs.UpdateDTO.UpdateUsuarioDTO;
import org.example.topdeckapi.src.service.IMPL.UsuarioService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/user")
public class UsuarioController {
    private final UsuarioService usuarioService;


    /*
    * En este controlador no vamos a tratar con los metodos
    * para guardar usuarios en la base de datos porque de eso
    * se va a encargar el AuthController una vez creado
    * en este controlador solo se manipulan usuarios ya creados
    * y la idea es que en la proteccion de rutas ningun usuario
    * no autenticado puede acceder a estar rutas
    * */

    @Autowired
    public UsuarioController(UsuarioService usuarioService){
        this.usuarioService = usuarioService;
    }

    @GetMapping("/admin/getAll")
    public ResponseEntity<List<UsuarioDTO> >  getAll(){
        List<UsuarioDTO> lista=usuarioService.findAll();
        return ResponseEntity.ok(lista);
    }

    @GetMapping("/admin/{id}")
    public ResponseEntity<UsuarioDTO>  getById(@PathVariable("id") Long id){
        return usuarioService.buscarPorId(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PatchMapping("/{id}")
    public ResponseEntity<UsuarioDTO>  update(@PathVariable("id") Long id, @RequestBody @Valid UpdateUsuarioDTO dto){
        return usuarioService.actualizarUsuario(dto, id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/admin/delete/{id}")
    public ResponseEntity<UsuarioDTO>  delete(@PathVariable("id") Long id){
        boolean isDeleted = usuarioService.deleteUsuario(id);
        return isDeleted
                ? ResponseEntity.ok().build()
                : ResponseEntity.notFound().build();
    }
}
