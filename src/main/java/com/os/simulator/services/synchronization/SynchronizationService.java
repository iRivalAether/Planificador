package com.os.simulator.services.synchronization;

public class SynchronizationService {
    // Mutex para proteger la sección crítica.
    private final Mutex mutex;
    // Semáforo que representa ranuras vacías (producir requiere un emptySlot).
    private final Semaphore emptySlots;
    // Semáforo que representa ranuras ocupadas (consumir requiere un filledSlot).
    private final Semaphore filledSlots;

    // Constructor inyecta las primitivas de sincronización usadas por la simulación.
    public SynchronizationService(Mutex mutex, Semaphore emptySlots, Semaphore filledSlots) {
        this.mutex = mutex; this.emptySlots = emptySlots; this.filledSlots = filledSlots;
    }

    // Operaciones de alto nivel que combinan llamadas a semáforos y mutex.
    public boolean acquireProducerSlot(int pid) { return emptySlots.waitFor(pid); }
    public boolean acquireConsumerSlot(int pid) { return filledSlots.waitFor(pid); }
    public boolean enterCriticalSection(int pid) { return mutex.tryLock(pid); }
    public Integer leaveCriticalSection(int pid) { return mutex.unlock(pid); }
    public Integer signalProduced() { return filledSlots.signal(); }
    public Integer signalConsumed() { return emptySlots.signal(); }

    // Accessores para snapshot/UI: exponen el estado interno para visualización sin alterar la lógica.
    public boolean isMutexLocked() { return mutex.isLocked(); }
    public Integer getMutexOwnerPid() { return mutex.getOwnerPid(); }
    public java.util.Queue<Integer> getMutexWaiting() { return mutex.getWaitingProcesses(); }
    public int getEmptyValue() { return emptySlots.getValue(); }
    public java.util.Queue<Integer> getEmptyWaiting() { return emptySlots.getWaitingProcesses(); }
    public int getFilledValue() { return filledSlots.getValue(); }
    public java.util.Queue<Integer> getFilledWaiting() { return filledSlots.getWaitingProcesses(); }
}