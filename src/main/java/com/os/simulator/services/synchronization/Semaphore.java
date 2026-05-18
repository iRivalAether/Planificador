package com.os.simulator.services.synchronization;

import java.util.LinkedList;
import java.util.Queue;

/**
 * Simula un semaforo contador para coordinar el acceso a recursos compartidos.
 */
public class Semaphore {
    private int value;
    private final Queue<Integer> waitingProcesses;

    public Semaphore(int initialValue) {
        this.value = initialValue;
        this.waitingProcesses = new LinkedList<>();
    }

    /**
     * Operacion wait del semaforo. Si el valor no alcanza, el proceso espera.
     *
     * @param pid identificador del proceso.
     * @return true si pudo continuar, false si quedo bloqueado.
     */
    public boolean waitFor(int pid) {
        if (value > 0) {
            value--;
            return true;
        }

        if (!waitingProcesses.contains(pid)) {
            waitingProcesses.offer(pid);
        }
        return false;
    }

    /**
     * Operacion signal del semaforo.
     *
     * @return pid del proceso liberado o null si nadie esperaba.
     */
    public Integer signal() {
        Integer released = waitingProcesses.poll();
        value++;
        return released;
    }

    public int getValue() {
        return value;
    }

    public Queue<Integer> getWaitingProcesses() {
        return waitingProcesses;
    }
}
