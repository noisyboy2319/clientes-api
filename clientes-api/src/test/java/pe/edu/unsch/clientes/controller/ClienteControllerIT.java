package pe.edu.unsch.clientes.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import pe.edu.unsch.clientes.repository.ClienteRepository;

@SpringBootTest
@AutoConfigureMockMvc
class ClienteControllerIT {

    private static final String URL = "/api/v1/clientes";
    private static final String BODY_VALIDO = """
            {"dni":"12345678","nombre":"Carlos Quispe","email":"carlos@correo.com"}
            """;

    @Autowired
    MockMvc mvc;

    @Autowired
    ClienteRepository repository;

    @BeforeEach
    void limpiar() {
        repository.deleteAll();
    }

    @Test
    void crear_exitoso_retorna201ConLocation() throws Exception {
        mvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(BODY_VALIDO))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.dni").value("12345678"));
    }

    @Test
    void crear_conDniDuplicado_retorna409() throws Exception {
        mvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(BODY_VALIDO))
                .andExpect(status().isCreated());

        mvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(BODY_VALIDO))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    void crear_conDatosInvalidos_retorna400() throws Exception {
        String invalido = """
                {"dni":"1234567","nombre":"Carlos","email":"no-es-email"}
                """;
        mvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(invalido))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.dni", notNullValue()))
                .andExpect(jsonPath("$.errores.email", notNullValue()));
    }

    @Test
    void buscar_porDni_retorna200() throws Exception {
        mvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(BODY_VALIDO));

        mvc.perform(get(URL).param("dni", "12345678"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].nombre").value("Carlos Quispe"));
    }

    @Test
    void buscar_porNombreParcialSinDistinguirMayusculas_retorna200() throws Exception {
        mvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(BODY_VALIDO));

        mvc.perform(get(URL).param("nombre", "carLOS"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    void obtenerPorId_inexistente_retorna404() throws Exception {
        mvc.perform(get(URL + "/9999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void eliminar_existente_retorna204_y_luegoInexistente404() throws Exception {
        String location = mvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(BODY_VALIDO))
                .andReturn().getResponse().getHeader("Location");

        mvc.perform(delete(location)).andExpect(status().isNoContent());
        mvc.perform(delete(location)).andExpect(status().isNotFound());
    }
}
