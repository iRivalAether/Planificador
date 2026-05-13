package com.os.simulator.services.synchronization;

/**
 * Simula el caso clasico de productor-consumidor sobre memoria compartida.
 */
public class ProducerConsumerSimulation {
    private final SharedMemory sharedMemory;
    private final Semaphore emptySlots;
    private final Semaphore filledSlots;

    public ProducerConsumerSimulation() {
        this.sharedMemory = new SharedMemory();
        this.emptySlots = new Semaphore(1);
        this.filledSlots = new Semaphore(0);
    }

    /**
     * Produce un elemento y lo deja disponible para el consumidor.
     *
     * @param pid identificador del productor.
     * @param value valor producido.
     * @return true si el dato pudo producirse.
     */
    public boolean produce(int pid, String value) {
        if (!emptySlots.waitFor(pid)) {
            return false;
        }

        boolean written = sharedMemory.write(pid, value);
        filledSlots.signal();
        return written;
    }

    /**
     * Consume el dato actual de la memoria compartida.
     *
     * @param pid identificador del consumidor.
     * @return valor consumido o null si no habia datos.
     */
    public String consume(int pid) {
        if (!filledSlots.waitFor(pid)) {
            return null;
        }

        String value = sharedMemory.read(pid);
        emptySlots.signal();
        return value;
    }

    public SharedMemory getSharedMemory() {
        return sharedMemory;
    }
}
