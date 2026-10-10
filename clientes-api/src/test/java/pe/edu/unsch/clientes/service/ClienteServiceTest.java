package pe.edu.unsch.clientes.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import pe.edu.unsch.clientes.dto.ClienteRequestDTO;
import pe.edu.unsch.clientes.dto.ClienteResponseDTO;
import pe.edu.unsch.clientes.exception.ClienteNoEncontradoException;
import pe.edu.unsch.clientes.exception.DniDuplicadoException;
import pe.edu.unsch.clientes.model.Cliente;
import pe.edu.unsch.clientes.repository.ClienteRepository;

@ExtendWith(MockitoExtension.class)
class ClienteServiceTest {

    @Mock
    ClienteRepository repository;

    @InjectMocks
    ClienteService service;

    @Test
    void crear_conDatosValidos_guardaYRetornaCliente() {
        var dto = new ClienteRequestDTO("12345678", "Carlos", "carlos@correo.com");
        when(repository.existsByDni("12345678")).thenReturn(false);
        when(repository.save(any(Cliente.class))).thenAnswer(inv -> {
            Cliente c = inv.getArgument(0);
            c.setId(1L);
            return c;
        });

        ClienteResponseDTO resp = service.crear(dto);

        assertEquals(1L, resp.id());
        assertEquals("12345678", resp.dni());
    }

    @Test
    void crear_conDniDuplicado_lanzaExcepcion() {
        when(repository.existsByDni("12345678")).thenReturn(true);
        var dto = new ClienteRequestDTO("12345678", "Carlos", "carlos@correo.com");

        assertThrows(DniDuplicadoException.class, () -> service.crear(dto));
        verify(repository, never()).save(any());
    }

    @Test
    void obtener_conIdInexistente_lanzaExcepcion() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ClienteNoEncontradoException.class, () -> service.obtener(99L));
    }

    @Test
    void buscar_porNombre_usaBusquedaParcialCaseInsensitive() {
        Cliente c = new Cliente("12345678", "Carlos Quispe", "carlos@correo.com");
        c.setId(1L);
        when(repository.findByNombreContainingIgnoreCase("carlos")).thenReturn(List.of(c));

        List<ClienteResponseDTO> resultado = service.buscar(null, "carlos");

        assertEquals(1, resultado.size());
        assertEquals("Carlos Quispe", resultado.get(0).nombre());
    }

    @Test
    void eliminar_conIdInexistente_lanzaExcepcion() {
        when(repository.existsById(99L)).thenReturn(false);

        assertThrows(ClienteNoEncontradoException.class, () -> service.eliminar(99L));
        verify(repository, never()).deleteById(any());
    }
}
