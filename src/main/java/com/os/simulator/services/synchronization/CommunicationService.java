package com.os.simulator.services.synchronization;

/**
 * API mínima para leer/escribir sobre `SharedMemory`.
 * Esta clase sirve como adaptador para desacoplar la lógica de comunicación
 * de la implementación concreta del buffer compartido.
 */
public class CommunicationService {
    // Referencia al buffer compartido donde se escriben/leen valores.
    private final SharedMemory sharedMemory;

    // Inyecta la instancia de memoria compartida usada por productores/consumidores.
    public CommunicationService(SharedMemory sharedMemory) { this.sharedMemory = sharedMemory; }

    // Intento de escritura; devuelve true si se pudo escribir.
    public boolean write(int pid, String value) { return sharedMemory.write(pid, value); }

    // Lectura desde memoria compartida; devuelve null si no hay datos.
    public String read(int pid) { return sharedMemory.read(pid); }

    // Acceso directo al objeto SharedMemory para inspección o snapshots.
    public SharedMemory getSharedMemory() { return sharedMemory; }
}