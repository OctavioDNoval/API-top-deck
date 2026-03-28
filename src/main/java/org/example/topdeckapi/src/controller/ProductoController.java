package org.example.topdeckapi.src.controller;


import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.topdeckapi.src.DTOs.request.ProductoRequest;
import org.example.topdeckapi.src.DTOs.response.PaginacionResponse;
import org.example.topdeckapi.src.DTOs.response.ProductoResponse;
import org.example.topdeckapi.src.service.IMPL.ProductoService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@RestController
@RequestMapping("/products")
@RequiredArgsConstructor
public class ProductoController {
    private final ProductoService productoService;
    private final RestTemplate restTemplate = new RestTemplate();


    @Value("${tcg.api.key}")
    private String tcgApiKey;

    @GetMapping("/test")
    public ResponseEntity<String> test(){
        return ResponseEntity.ok("El Servicio funciona!");
    }

    @GetMapping("/public/obtenerPaginados")
    public ResponseEntity<PaginacionResponse<ProductoResponse>> obtenerPaginados(
            @RequestParam(defaultValue = "1") Integer pagina,
            @RequestParam(defaultValue = "15") Integer tamanio,
            @RequestParam(defaultValue = "idProducto") String sortBy,
            @RequestParam(defaultValue = "desc") String direction,
            @RequestParam(defaultValue = "") String filter,
            @RequestParam(defaultValue = "0") Long idTag,
            @RequestParam(defaultValue = "0") Long idCategoria
    ) {
        Long categoria = (idCategoria == 0) ? null : idCategoria;
        Long tag = (idTag == 0) ? null : idTag;
        String search = (filter == null || filter.trim().isEmpty()) ? null : filter.trim();

        PaginacionResponse<ProductoResponse> paginacionResponse =
                productoService.obtenerPaginadosConFiltro(pagina, tamanio, sortBy, direction, search, categoria, tag, false);

        return ResponseEntity.ok(paginacionResponse);
    }

    @GetMapping("/admin/obtenerPaginados")
    public ResponseEntity<PaginacionResponse<ProductoResponse>> obtenerPaginadosAdmin(
            @RequestParam(defaultValue = "1") Integer pagina,
            @RequestParam(defaultValue = "15") Integer tamanio,
            @RequestParam(defaultValue = "idProducto") String sortBy,
            @RequestParam(defaultValue = "desc") String direction,
            @RequestParam(defaultValue = "") String filter,
            @RequestParam(defaultValue = "0") Long idTag,
            @RequestParam(defaultValue = "0") Long idCategoria
    ) {
        Long categoria = (idCategoria == 0) ? null : idCategoria;
        Long tag = (idTag == 0) ? null : idTag;
        String search = (filter == null || filter.trim().isEmpty()) ? null : filter.trim();

        PaginacionResponse<ProductoResponse> paginacionResponse =
                productoService.obtenerPaginadosConFiltro(pagina, tamanio, sortBy, direction, search, categoria, tag, true);

        return ResponseEntity.ok(paginacionResponse);
    }

    @GetMapping("/public/image-proxy")
    public ResponseEntity<byte[]> proxyImage(@RequestParam String url) {
        try {
            RestTemplate restTemplate = new RestTemplate();
            byte[] imageBytes = restTemplate.getForObject(url, byte[].class);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.IMAGE_PNG); // o detectar dinámicamente
            
            return new ResponseEntity<>(imageBytes, headers, HttpStatus.OK);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/admin/tcg/{franquicia}/{nombreCarta}/{page}/{limit}")
    public ResponseEntity<?> tcg(@PathVariable String franquicia, @PathVariable String nombreCarta, @PathVariable Integer page, @PathVariable Integer limit) {
        try {
            String url = "https://www.apitcg.com/api/" + franquicia + "/cards";
            if (nombreCarta != null && !nombreCarta.isEmpty()) {
                url += "?name=" + URLEncoder.encode(nombreCarta, StandardCharsets.UTF_8);
            }

            url += "&page=" + page + "&limit=" + limit;

            HttpHeaders headers = new HttpHeaders();
            headers.set("x-api-key", tcgApiKey);
            headers.set("Accept", "application/json");


            HttpEntity<?> entity = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    String.class
            );

            return ResponseEntity.ok(response.getBody());

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error al consultar API TCG: " + e.getMessage()));
        }
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

    @PatchMapping("/admin/deslistar/{idProducto}")
    public ResponseEntity<ProductoResponse> deslistar(@PathVariable("idProducto") Long idProducto){
        return ResponseEntity.ok(productoService.cambiarEstadoProducto(idProducto));
    }

    @DeleteMapping("/admin/delete/{id}")
    public ResponseEntity<Void> delete(@PathVariable("id") Long id){
        boolean isDeleted = productoService.borrarProducto(id);
        return isDeleted
                ? ResponseEntity.ok().build()
                : ResponseEntity.notFound().build();
    }
}
