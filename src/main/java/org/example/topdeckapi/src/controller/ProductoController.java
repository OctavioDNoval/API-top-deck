package org.example.topdeckapi.src.controller;


import java.net.URI;
import java.util.regex.Pattern;
import org.example.topdeckapi.src.Exception.BussinesException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.topdeckapi.src.DTOs.request.ProductoRequest;
import org.example.topdeckapi.src.DTOs.response.FacetaResponse;
import org.example.topdeckapi.src.DTOs.response.PaginacionResponse;
import org.example.topdeckapi.src.DTOs.response.ProductoResponse;
import org.example.topdeckapi.src.service.IMPL.ProductoService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/products")
@RequiredArgsConstructor
public class ProductoController {
    private final ProductoService productoService;
    private final RestTemplate restTemplate = new RestTemplate();


    @Value("${tcg.api.key}")
    private String tcgApiKey;

    private static final Pattern TCG_SLUG = Pattern.compile("^[a-z0-9-]{1,50}$");

    @GetMapping("/public/obtenerPaginados")
    public ResponseEntity<PaginacionResponse<ProductoResponse>> obtenerPaginados(
            @RequestParam(defaultValue = "1") Integer pagina,
            @RequestParam(defaultValue = "15") Integer tamanio,
            @RequestParam(defaultValue = "idProducto") String sortBy,
            @RequestParam(defaultValue = "desc") String direction,
            @RequestParam(defaultValue = "") String filter,
            @RequestParam(defaultValue = "") String idTag,
            @RequestParam(defaultValue = "") String idCategoria
    ) {
        String tag = (idTag == null || idTag.trim().isEmpty() || "0".equals(idTag.trim())) ? null : idTag.trim();
        String categoria = (idCategoria == null || idCategoria.trim().isEmpty() || "0".equals(idCategoria.trim())) ? null : idCategoria.trim();
        String search = (filter == null || filter.trim().isEmpty()) ? null : filter.trim();

        PaginacionResponse<ProductoResponse> paginacionResponse =
                productoService.obtenerPaginadosConFiltro(pagina, tamanio, sortBy, direction, search, categoria, tag, false, "PRODUCTO");

        return ResponseEntity.ok(paginacionResponse);
    }

    @GetMapping("/admin/obtenerPaginados")
    public ResponseEntity<PaginacionResponse<ProductoResponse>> obtenerPaginadosAdmin(
            @RequestParam(defaultValue = "1") Integer pagina,
            @RequestParam(defaultValue = "15") Integer tamanio,
            @RequestParam(defaultValue = "idProducto") String sortBy,
            @RequestParam(defaultValue = "desc") String direction,
            @RequestParam(defaultValue = "") String filter,
            @RequestParam(defaultValue = "") String idTag,
            @RequestParam(defaultValue = "") String idCategoria,
            @RequestParam(defaultValue = "") String tipo,
            @RequestParam(defaultValue = "") String estado
    ) {
        String tag = (idTag == null || idTag.trim().isEmpty() || "0".equals(idTag.trim())) ? null : idTag.trim();
        String categoria = (idCategoria == null || idCategoria.trim().isEmpty() || "0".equals(idCategoria.trim())) ? null : idCategoria.trim();
        String search = (filter == null || filter.trim().isEmpty()) ? null : filter.trim();
        // tipo: PRODUCTO | SINGLE ; estado: activo | inactivo (anything else = all)
        String tipoProducto = "PRODUCTO".equalsIgnoreCase(tipo) || "SINGLE".equalsIgnoreCase(tipo)
                ? tipo.toUpperCase() : null;
        Boolean activo = "activo".equalsIgnoreCase(estado) ? Boolean.TRUE
                : "inactivo".equalsIgnoreCase(estado) ? Boolean.FALSE : null;

        PaginacionResponse<ProductoResponse> paginacionResponse =
                productoService.obtenerPaginadosConFiltro(pagina, tamanio, sortBy, direction, search, categoria, tag, true, tipoProducto, activo);

        return ResponseEntity.ok(paginacionResponse);
    }

    @GetMapping("/public/singles")
    public ResponseEntity<PaginacionResponse<ProductoResponse>> obtenerSingles(
            @RequestParam(defaultValue = "1") Integer pagina,
            @RequestParam(defaultValue = "15") Integer tamanio,
            @RequestParam(defaultValue = "idProducto") String sortBy,
            @RequestParam(defaultValue = "desc") String direction,
            @RequestParam(defaultValue = "") String filter,
            @RequestParam(defaultValue = "") String idTag,
            @RequestParam(value = "attr", required = false) List<String> attr
    ) {
        String tag = (idTag == null || idTag.trim().isEmpty() || "0".equals(idTag.trim())) ? null : idTag.trim();
        Map<String, List<String>> atributos = parseAtributos(attr);

        PaginacionResponse<ProductoResponse> paginacionResponse =
                productoService.obtenerSinglesConFiltro(pagina, tamanio, sortBy, direction, filter, tag, atributos);

        return ResponseEntity.ok(paginacionResponse);
    }

    @GetMapping("/public/singles/facetas")
    public ResponseEntity<List<FacetaResponse>> obtenerFacetasSingles(
            @RequestParam(defaultValue = "") String idTag
    ) {
        String tag = (idTag == null || idTag.trim().isEmpty() || "0".equals(idTag.trim())) ? null : idTag.trim();
        return ResponseEntity.ok(productoService.obtenerFacetasSingles(tag));
    }

    private Map<String, List<String>> parseAtributos(List<String> attr) {
        Map<String, List<String>> atributos = new LinkedHashMap<>();
        if (attr == null) return atributos;
        for (String par : attr) {
            if (par == null) continue;
            int idx = par.indexOf(':');
            if (idx <= 0) continue;
            String clave = par.substring(0, idx).trim();
            String valor = par.substring(idx + 1).trim();
            if (clave.isEmpty() || valor.isEmpty()) continue;
            atributos.computeIfAbsent(clave, k -> new ArrayList<>()).add(valor);
        }
        return atributos;
    }

    @GetMapping("/public/ofertas")
    public ResponseEntity<List<ProductoResponse>> obtenerOfertas() {
        return ResponseEntity.ok(productoService.obtenerOfertas());
    }

    @GetMapping("/admin/tcg/{franquicia}/{nombreCarta}/{page}/{limit}")
    public ResponseEntity<?> tcg(@PathVariable String franquicia, @PathVariable String nombreCarta, @PathVariable Integer page, @PathVariable Integer limit) {
        // franquicia is a slug like "one-piece": anything else could inject extra query params.
        if (franquicia == null || !TCG_SLUG.matcher(franquicia).matches()) {
            throw new BussinesException("Franquicia inválida");
        }
        int safePage = Math.max(1, page == null ? 1 : page);
        int safeLimit = Math.min(50, Math.max(1, limit == null ? 26 : limit));
        try {
            String url = "https://api.apitcg.com/api/products?tcg="
                    + URLEncoder.encode(franquicia, StandardCharsets.UTF_8) + "&type=card";
            if (nombreCarta != null && !nombreCarta.isEmpty()) {
                url += "&name=" + URLEncoder.encode(nombreCarta, StandardCharsets.UTF_8);
            }

            url += "&page=" + safePage + "&limit=" + safeLimit;

            HttpHeaders headers = new HttpHeaders();
            headers.set("x-api-key", tcgApiKey);
            headers.set("Accept", "application/json");


            HttpEntity<?> entity = new HttpEntity<>(headers);

            // URI (not String) so RestTemplate does not encode the already-encoded query again.
            ResponseEntity<String> response = restTemplate.exchange(
                    URI.create(url),
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
    public ResponseEntity<ProductoResponse> post(@RequestBody@Valid ProductoRequest producto){
        return new ResponseEntity<>(productoService.guardar(producto), HttpStatus.CREATED);
    }

    /** Admin lookup: also returns products hidden from the store. */
    @GetMapping("/admin/{id}")
    public ResponseEntity<ProductoResponse> getByIdAdmin(@PathVariable("id") String id){
        return ResponseEntity.ok(productoService.buscarPorIdAdmin(id));
    }

    @GetMapping("/public/{id}")
    public ResponseEntity<ProductoResponse> getByID(@PathVariable("id") String id){
        ProductoResponse productoReponse = productoService.buscarPorId(id);
        return ResponseEntity.ok(productoReponse);
    }

    @PatchMapping("/admin/edit/{id}")
    public ResponseEntity<ProductoResponse> edit(@PathVariable("id") String id, @RequestBody @Valid ProductoRequest producto){
        ProductoResponse productoActualizado = productoService.actualizarProducto(id,producto);
        return ResponseEntity.ok(productoActualizado);
    }

    @PatchMapping("/admin/deslistar/{idProducto}")
    public ResponseEntity<ProductoResponse> deslistar(@PathVariable("idProducto") String idProducto){
        return ResponseEntity.ok(productoService.cambiarEstadoProducto(idProducto));
    }

    @PatchMapping("/admin/relistar/{idProducto}")
    public ResponseEntity<ProductoResponse> relistar(@PathVariable("idProducto") String idProducto){
        return ResponseEntity.ok(productoService.listarProducto(idProducto));
    }

    @DeleteMapping("/admin/delete/{id}")
    public ResponseEntity<Void> delete(@PathVariable("id") String id){
        boolean isDeleted = productoService.borrarProducto(id);
        return isDeleted
                ? ResponseEntity.ok().build()
                : ResponseEntity.notFound().build();
    }
}
