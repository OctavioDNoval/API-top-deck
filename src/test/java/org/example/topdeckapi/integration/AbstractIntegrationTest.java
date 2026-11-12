package org.example.topdeckapi.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.topdeckapi.src.Enumerados.ROL;
import org.example.topdeckapi.src.Repository.ICategoriasRepo;
import org.example.topdeckapi.src.Repository.IProductoRepo;
import org.example.topdeckapi.src.Repository.ITagRepository;
import org.example.topdeckapi.src.Repository.IUsuarioRepo;
import org.example.topdeckapi.src.model.Categoria;
import org.example.topdeckapi.src.model.Producto;
import org.example.topdeckapi.src.model.Tag;
import org.example.topdeckapi.src.model.Usuario;
import org.example.topdeckapi.src.service.IMPL.EmailService;
import org.example.topdeckapi.src.service.IMPL.JwtService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Base for integration tests: the whole app (security, controllers, services, Flyway
 * migrations) against a real PostgreSQL started by Testcontainers. Every subclass shares
 * one Spring context and one container, so the suite starts them only once.
 * Skipped (not failed) when Docker is not available.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("it")
@Testcontainers(disabledWithoutDocker = true)
public abstract class AbstractIntegrationTest {

    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    // Each request gets its own client IP so the per-IP rate limits never trip between tests.
    private static final AtomicInteger NEXT_IP = new AtomicInteger(1);

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        // Started here (not in a static block) so a machine without Docker skips the tests cleanly.
        if (!POSTGRES.isRunning()) POSTGRES.start();
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired protected MockMvc mvc;
    @Autowired protected ObjectMapper json;
    @Autowired protected IUsuarioRepo usuarioRepo;
    @Autowired protected IProductoRepo productoRepo;
    @Autowired protected ICategoriasRepo categoriaRepo;
    @Autowired protected ITagRepository tagRepo;
    @Autowired protected PasswordEncoder passwordEncoder;
    @Autowired protected JwtService jwtService;

    // No real mails: tests read the links (tokens) from the calls instead.
    @MockitoBean protected EmailService emailService;

    protected static final String PASSWORD = "Claude-Test-1234";

    /** Confirmed account with {@link #PASSWORD}, saved straight to the database. */
    protected Usuario crearUsuario(ROL rol) {
        Usuario u = new Usuario();
        u.setNombre("Claude " + rol.name().toLowerCase());
        u.setEmail(emailUnico());
        u.setPassword(passwordEncoder.encode(PASSWORD));
        u.setRol(rol);
        u.setEmailVerificado(true);
        u.setSesionVersion(0);
        u.setTerminosAceptados(true);
        u.setVersionTerminosYCondicionesAceptados("1.0");
        u.setFechaAceptacionTerminos(LocalDateTime.now());
        return usuarioRepo.save(u);
    }

    protected String tokenDe(Usuario u) {
        return jwtService.generarToken(u);
    }

    protected Producto crearProducto(String precio, int stock) {
        Categoria categoria = new Categoria();
        categoria.setNombre("Claude categoria " + sufijo());
        categoria = categoriaRepo.save(categoria);

        Tag tag = new Tag();
        String nombreTag = "Claude tag " + sufijo();
        tag.setNombre(nombreTag);
        tag.setNombreNormalizado(nombreTag.toLowerCase());
        tag = tagRepo.save(tag);

        Producto p = new Producto();
        p.setNombre("Claude producto " + sufijo());
        p.setDescripcion("Producto de prueba");
        p.setPrecio(new BigDecimal(precio));
        p.setStock(stock);
        p.setDescuento(0);
        p.setActivo(true);
        p.setCategoria(categoria);
        p.setTag(tag);
        return productoRepo.save(p);
    }

    /** Adds a unique client IP and, when given, the bearer token. */
    protected MockHttpServletRequestBuilder como(MockHttpServletRequestBuilder request, String token) {
        int n = NEXT_IP.getAndIncrement();
        request.with(r -> {
            r.setRemoteAddr("10.0." + (n / 250) % 250 + "." + (n % 250 + 1));
            return r;
        });
        if (token != null) request.header("Authorization", "Bearer " + token);
        return request;
    }

    protected MockHttpServletRequestBuilder conJson(MockHttpServletRequestBuilder request, Object body) throws Exception {
        return request.contentType("application/json").content(json.writeValueAsString(body));
    }

    protected JsonNode leer(MvcResult result) throws Exception {
        return json.readTree(result.getResponse().getContentAsString());
    }

    protected static String emailUnico() {
        return "claude-it-" + sufijo() + "@example.com";
    }

    private static String sufijo() {
        return UUID.randomUUID().toString().substring(0, 8);
    }
}
