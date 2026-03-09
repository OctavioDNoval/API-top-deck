package org.example.topdeckapi.src.controller;


import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.topdeckapi.src.DTOs.request.ProductoRequest;
import org.example.topdeckapi.src.DTOs.response.PaginacionResponse;
import org.example.topdeckapi.src.DTOs.response.ProductoResponse;
import org.example.topdeckapi.src.service.IMPL.ProductoService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/products")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class ProductoController {
    private final ProductoService productoService;

    @GetMapping("/test")
    public ResponseEntity<String> test(){
        return ResponseEntity.ok("El Servicio funciona!");
    }

    @GetMapping("/public/getAll")
    public ResponseEntity<PaginacionResponse<ProductoResponse>> obtenerPaginados(
            @RequestParam(defaultValue = "1") Integer pagina,
            @RequestParam(defaultValue = "15") Integer tamanio,
            @RequestParam(defaultValue = "idPedido") String sortBy,
            @RequestParam(defaultValue = "desc") String direction,
            @RequestParam(defaultValue = "") String filter
    ){
        PaginacionResponse<ProductoResponse> paginacionResponse;

        if(filter == null || filter.trim().isEmpty()){
            paginacionResponse = productoService.obtenerPaginados(pagina, tamanio, sortBy, direction);
        }else{
            paginacionResponse = productoService.obtenerPaginadosConFiltro(pagina, tamanio, sortBy, direction, filter);
        }

        return ResponseEntity.ok(paginacionResponse);
    }

    @PostMapping("/admin/post")
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<ProductoResponse> post(@RequestBody@Valid ProductoRequest producto){
        return ResponseEntity.ok(productoService.guardar(producto));
    }

    @GetMapping("/public/{id}")
    public ResponseEntity<ProductoResponse> getByID(@PathVariable("id") Long id){
        ProductoResponse productoReponse = productoService.buscarPorId(id);
        return ResponseEntity.ok(productoReponse);
    }

    @PatchMapping("/admin/edit/{id}")
    public ResponseEntity<ProductoResponse> edit(@PathVariable("id") Long id, @RequestBody @Valid ProductoRequest producto){
        ProductoResponse productoActualizado = productoService.actualizarProducto(id,producto);
        return ResponseEntity.ok(productoActualizado);
    }

    @DeleteMapping("/admin/delete/{id}")
    public ResponseEntity<Void> delete(@PathVariable("id") Long id){
        boolean isDeleted = productoService.borrarProducto(id);
        return isDeleted
                ? ResponseEntity.ok().build()
                : ResponseEntity.notFound().build();
    }
}
