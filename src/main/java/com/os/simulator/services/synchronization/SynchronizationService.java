package com.os.simulator.services.synchronization;

public class SynchronizationService {
    private final Mutex mutex; private final Semaphore emptySlots; private final Semaphore filledSlots;
    public SynchronizationService(Mutex mutex, Semaphore emptySlots, Semaphore filledSlots) {
        this.mutex = mutex; this.emptySlots = emptySlots; this.filledSlots = filledSlots;
    }
    public boolean acquireProducerSlot(int pid) { return emptySlots.waitFor(pid); }
    public boolean acquireConsumerSlot(int pid) { return filledSlots.waitFor(pid); }
    public boolean enterCriticalSection(int pid) { return mutex.tryLock(pid); }
    public Integer leaveCriticalSection(int pid) { return mutex.unlock(pid); }
    public Integer signalProduced() { return filledSlots.signal(); }
    public Integer signalConsumed() { return emptySlots.signal(); }
    // Accessors for UI snapshot
    public boolean isMutexLocked() { return mutex.isLocked(); }
    public Integer getMutexOwnerPid() { return mutex.getOwnerPid(); }
    public java.util.Queue<Integer> getMutexWaiting() { return mutex.getWaitingProcesses(); }
    public int getEmptyValue() { return emptySlots.getValue(); }
    public java.util.Queue<Integer> getEmptyWaiting() { return emptySlots.getWaitingProcesses(); }
    public int getFilledValue() { return filledSlots.getValue(); }
    public java.util.Queue<Integer> getFilledWaiting() { return filledSlots.getWaitingProcesses(); }
}