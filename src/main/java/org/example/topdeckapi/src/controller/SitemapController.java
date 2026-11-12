package org.example.topdeckapi.src.controller;

import lombok.RequiredArgsConstructor;
import org.example.topdeckapi.src.Repository.IProductoRepo;
import org.example.topdeckapi.src.configuracion.ContactConfig;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.util.List;

/**
 * sitemap.xml for search engines: the store's fixed pages plus every listed product.
 * nginx serves it at https://<domain>/sitemap.xml (see Front-Top-Deck/nginx.conf).
 * Links point to the store (app.front-url), not to the API.
 */
@RestController
@RequestMapping("/products/public")
@RequiredArgsConstructor
public class SitemapController {

    static final List<String> PAGINAS = List.of("/", "/productos", "/singles", "/eventos");

    private final IProductoRepo productoRepo;
    private final ContactConfig contactConfig;

    @GetMapping(value = "/sitemap.xml", produces = MediaType.APPLICATION_XML_VALUE)
    public ResponseEntity<String> sitemap() {
        String base = contactConfig.getFrontUrl();
        if (base == null || base.isBlank()) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
        }
        base = base.replaceAll("/+$", "");

        StringBuilder xml = new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
                .append("<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">\n");
        for (String pagina : PAGINAS) {
            agregar(xml, base + pagina);
        }
        for (String uuid : productoRepo.findUuidsActivos()) {
            agregar(xml, base + "/productos/" + uuid);
        }
        xml.append("</urlset>\n");

        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(Duration.ofHours(1)).cachePublic())
                .body(xml.toString());
    }

    private static void agregar(StringBuilder xml, String url) {
        xml.append("  <url><loc>").append(escapar(url)).append("</loc></url>\n");
    }

    private static String escapar(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&apos;");
    }
}
