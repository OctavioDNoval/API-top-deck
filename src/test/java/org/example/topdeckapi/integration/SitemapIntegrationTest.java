package org.example.topdeckapi.integration;

import org.example.topdeckapi.src.model.Producto;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** sitemap.xml: public, store links (app.front-url), only listed products. */
class SitemapIntegrationTest extends AbstractIntegrationTest {

    @Test
    void sitemap_listsStorePagesAndActiveProductsOnly() throws Exception {
        Producto activo = crearProducto("1000.00", 3);
        Producto deslistado = crearProducto("1000.00", 3);
        deslistado.setActivo(false);
        productoRepo.save(deslistado);

        String xml = mvc.perform(como(get("/products/public/sitemap.xml"), null))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("application/xml"))
                .andReturn().getResponse().getContentAsString();

        assertThat(xml)
                .startsWith("<?xml")
                .contains("<loc>http://localhost:5173/</loc>")
                .contains("<loc>http://localhost:5173/singles</loc>")
                .contains("<loc>http://localhost:5173/productos/" + activo.getUuid() + "</loc>")
                .doesNotContain(deslistado.getUuid());
    }
}
