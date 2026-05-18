package com.os.simulator.services.synchronization;

/**
 * Simula el caso clasico de productor-consumidor sobre memoria compartida.
 */
public class ProducerConsumerSimulation {
    private final SharedMemory sharedMemory;
    private final CommunicationService communicationService;
    private final SynchronizationService synchronizationService;

    public ProducerConsumerSimulation() {
        this(1);
    }

    public ProducerConsumerSimulation(int bufferSize) {
        this.sharedMemory = new SharedMemory(bufferSize);
        this.communicationService = new CommunicationService(sharedMemory);
        this.synchronizationService = new SynchronizationService(new Mutex(), new Semaphore(bufferSize), new Semaphore(0));
    }

    /**
     * Produce un elemento y lo deja disponible para el consumidor.
     *
     * @param pid identificador del productor.
     * @param value valor producido.
     * @return true si el dato pudo producirse.
     */
    public boolean produce(int pid, String value) {
        if (!synchronizationService.acquireProducerSlot(pid)) {
            return false;
        }

        if (!synchronizationService.enterCriticalSection(pid)) return false;
        boolean ok = communicationService.write(pid, value);
        synchronizationService.leaveCriticalSection(pid);
        synchronizationService.signalProduced();
        return ok;
    }

    /**
     * Consume el dato actual de la memoria compartida.
     *
     * @param pid identificador del consumidor.
     * @return valor consumido o null si no habia datos.
     */
    public String consume(int pid) {
        if (!synchronizationService.acquireConsumerSlot(pid)) {
            return null;
        }

        if (!synchronizationService.enterCriticalSection(pid)) return null;
        String value = communicationService.read(pid);
        synchronizationService.leaveCriticalSection(pid);
        synchronizationService.signalConsumed();
        return value;
    }

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
