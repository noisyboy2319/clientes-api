package pe.edu.unsch.clientes.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import pe.edu.unsch.clientes.model.Cliente;

public interface ClienteRepository extends JpaRepository<Cliente, Long> {

    boolean existsByDni(String dni);

    Optional<Cliente> findByDni(String dni);

    // Coincidencia parcial sin distinguir mayúsculas/minúsculas (HU-02, escenario 2)
    List<Cliente> findByNombreContainingIgnoreCase(String nombre);
}
