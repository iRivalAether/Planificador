package com.os.simulator.services.synchronization;

/** Minimal communication API over shared memory. */
public class CommunicationService {
    private final SharedMemory sharedMemory;

    public CommunicationService(SharedMemory sharedMemory) { this.sharedMemory = sharedMemory; }

    public boolean write(int pid, String value) { return sharedMemory.write(pid, value); }

    public String read(int pid) { return sharedMemory.read(pid); }

    public SharedMemory getSharedMemory() { return sharedMemory; }
}