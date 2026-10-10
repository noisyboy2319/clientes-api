package pe.edu.unsch.clientes.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pe.edu.unsch.clientes.dto.ClienteRequestDTO;
import pe.edu.unsch.clientes.dto.ClienteResponseDTO;
import pe.edu.unsch.clientes.exception.ClienteNoEncontradoException;
import pe.edu.unsch.clientes.exception.DniDuplicadoException;
import pe.edu.unsch.clientes.model.Cliente;
import pe.edu.unsch.clientes.repository.ClienteRepository;

@Service
@Transactional
public class ClienteService {

    private final ClienteRepository repository;

    public ClienteService(ClienteRepository repository) {
        this.repository = repository;
    }

    public ClienteResponseDTO crear(ClienteRequestDTO dto) {
        // HU-01, escenario 2: DNI duplicado -> 409
        if (repository.existsByDni(dto.dni())) {
            throw new DniDuplicadoException(dto.dni());
        }
        return toResponse(repository.save(toEntity(dto)));
    }

    @Transactional(readOnly = true)
    public List<ClienteResponseDTO> buscar(String dni, String nombre) {
        // HU-02, escenario 1: búsqueda exacta por DNI
        if (dni != null && !dni.isBlank()) {
            return repository.findByDni(dni)
                    .map(c -> List.of(toResponse(c)))
                    .orElse(List.of());
        }
        // HU-02, escenario 2: búsqueda parcial por nombre (case-insensitive)
        if (nombre != null && !nombre.isBlank()) {
            return repository.findByNombreContainingIgnoreCase(nombre).stream()
                    .map(this::toResponse)
                    .toList();
        }
        return repository.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public ClienteResponseDTO obtener(Long id) {
        return repository.findById(id)
                .map(this::toResponse)
                .orElseThrow(() -> new ClienteNoEncontradoException(id));
    }

    public ClienteResponseDTO actualizar(Long id, ClienteRequestDTO dto) {
        Cliente cliente = repository.findById(id)
                .orElseThrow(() -> new ClienteNoEncontradoException(id));

        // El DNI no puede pertenecer a otro cliente
        repository.findByDni(dto.dni())
                .filter(otro -> !otro.getId().equals(id))
                .ifPresent(otro -> {
                    throw new DniDuplicadoException(dto.dni());
                });

        cliente.setDni(dto.dni());
        cliente.setNombre(dto.nombre());
        cliente.setEmail(dto.email());
        return toResponse(repository.save(cliente));
    }

    public void eliminar(Long id) {
        if (!repository.existsById(id)) {
            throw new ClienteNoEncontradoException(id);
        }
        repository.deleteById(id);
    }

    private Cliente toEntity(ClienteRequestDTO dto) {
        return new Cliente(dto.dni(), dto.nombre(), dto.email());
    }

    private ClienteResponseDTO toResponse(Cliente c) {
        return new ClienteResponseDTO(c.getId(), c.getDni(), c.getNombre(), c.getEmail());
    }
}
