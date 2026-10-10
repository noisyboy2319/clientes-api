package pe.edu.unsch.clientes.exception;

public class DniDuplicadoException extends RuntimeException {

    public DniDuplicadoException(String dni) {
        super("Ya existe un cliente registrado con el DNI " + dni);
    }
}
