package com.os.simulator.services.synchronization;

import java.util.LinkedList;
import java.util.Queue;

/**
 * Simula un mutex para proteger una seccion critica compartida.
 */
public class Mutex {
    private boolean locked;
    private Integer ownerPid;
    private final Queue<Integer> waitingProcesses;

    public Mutex() {
        this.locked = false;
        this.ownerPid = null;
        this.waitingProcesses = new LinkedList<>();
    }

    /**
     * Intenta tomar el mutex para un proceso determinado.
     *
     * @param pid identificador del proceso.
     * @return true si obtuvo el candado, false si quedo esperando.
     */
    public boolean tryLock(int pid) {
        if (!locked) {
            locked = true;
            ownerPid = pid;
            return true;
        }

        if (!waitingProcesses.contains(pid)) {
            waitingProcesses.offer(pid);
        }
        return false;
    }

    /**
     * Libera el mutex si el proceso actual es el propietario.
     *
     * @param pid identificador del proceso que intenta liberar.
     * @return pid del siguiente proceso que quedo con el lock o null si nadie lo tomo.
     */
    public Integer unlock(int pid) {
        if (ownerPid == null || ownerPid != pid) {
            return ownerPid;
        }

        Integer nextOwner = waitingProcesses.poll();
        ownerPid = nextOwner;
        locked = nextOwner != null;
        return nextOwner;
    }

    public boolean isLocked() {
        return locked;
    }

    public Integer getOwnerPid() {
        return ownerPid;
    }

    public Queue<Integer> getWaitingProcesses() {
        return waitingProcesses;
    }
}
