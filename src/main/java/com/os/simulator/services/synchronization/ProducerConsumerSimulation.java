package com.os.simulator.services.synchronization;

/**
 * Simula el caso clasico de productor-consumidor sobre memoria compartida.
 */
public class ProducerConsumerSimulation {
    // Buffer compartido donde se almacenan los datos producidos.
    private final SharedMemory sharedMemory;
    // Servicio ligero que encapsula operaciones de lectura/escritura sobre la memoria compartida.
    private final CommunicationService communicationService;
    // Servicio que ofrece mutex y semáforos para coordinar productores/consumidores.
    private final SynchronizationService synchronizationService;

    // Constructores que permiten configurar el tamaño del buffer (por defecto 1).
    public ProducerConsumerSimulation() {
        this(1);
    }

    public ProducerConsumerSimulation(int bufferSize) {
        this.sharedMemory = new SharedMemory(bufferSize);
        this.communicationService = new CommunicationService(sharedMemory);
        // emptySlots inicializado con bufferSize; filledSlots inicia en 0.
        this.synchronizationService = new SynchronizationService(new Mutex(), new Semaphore(bufferSize), new Semaphore(0));
    }

    /**
     * Produce un elemento y lo deja disponible para el consumidor.
     * Coordina semáforos y mutex para asegurar acceso seguro y ordenado.
     *
     * @param pid identificador del productor.
     * @param value valor producido.
     * @return true si el dato pudo producirse (suficiente espacio y sección crítica disponible).
     */
    public boolean produce(int pid, String value) {
        if (!synchronizationService.acquireProducerSlot(pid)) {
            return false; // No hay slots vacíos; queda bloqueado en el semáforo de productores.
        }

        if (!synchronizationService.enterCriticalSection(pid)) return false; // No obtuvo mutex.
        boolean ok = communicationService.write(pid, value); // Intento de escritura.
        synchronizationService.leaveCriticalSection(pid); // Libera mutex.
        synchronizationService.signalProduced(); // Señala que hay un nuevo elemento (incrementa filledSlots).
        return ok;
    }

    /**
     * Consume el dato actual de la memoria compartida.
     * Realiza las operaciones complementarias a `produce` respetando sincronización.
     *
     * @param pid identificador del consumidor.
     * @return valor consumido o null si no había datos.
     */
    public String consume(int pid) {
        if (!synchronizationService.acquireConsumerSlot(pid)) {
            return null; // No hay elementos llenos; el consumidor queda bloqueado.
        }

        if (!synchronizationService.enterCriticalSection(pid)) return null; // No obtuvo mutex.
        String value = communicationService.read(pid); // Lectura del buffer.
        synchronizationService.leaveCriticalSection(pid); // Libera mutex.
        synchronizationService.signalConsumed(); // Señala que quedó un slot vacío (incrementa emptySlots).
        return value;
    }

    // Accesores para inspección desde fuera (UI/tests).
    public SharedMemory getSharedMemory() {
        return sharedMemory;
    }

    public CommunicationService getCommunicationService() {
        return communicationService;
    }

    public SynchronizationService getSynchronizationService() {
        return synchronizationService;
    }
}
