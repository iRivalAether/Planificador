package com.os.simulator.services.synchronization;

import java.util.LinkedList;
import java.util.Queue;

/**
 * Simula un mutex para proteger una seccion critica compartida.
 */
public class Mutex {
    // Indica si el mutex está actualmente ocupado.
    private boolean locked;
    // PID del proceso que actualmente posee el mutex; null si está libre.
    private Integer ownerPid;
    // Cola de PIDs que están esperando el mutex en orden FIFO.
    private final Queue<Integer> waitingProcesses;

    // Inicializa un mutex libre sin propietario y con cola vacía.
    public Mutex() {
        this.locked = false;
        this.ownerPid = null;
        this.waitingProcesses = new LinkedList<>();
    }

    /**
     * Intenta tomar el mutex para un proceso determinado.
     * Si está libre se asigna al solicitante; si no, se encola para esperar.
     *
     * @param pid identificador del proceso.
     * @return true si obtuvo el candado, false si quedó en espera.
     */
    public boolean tryLock(int pid) {
        // Si no está bloqueado, el proceso obtiene la propiedad inmediatamente.
        if (!locked) {
            locked = true;
            ownerPid = pid;
            return true;
        }

        // Si ya está bloqueado, registramos al pid en la cola de espera (si no está ya).
        if (!waitingProcesses.contains(pid)) {
            waitingProcesses.offer(pid);
        }
        return false;
    }

    /**
     * Libera el mutex si el proceso actual es el propietario.
     * Si hay procesos esperando, entrega la propiedad al siguiente en la cola.
     *
     * @param pid identificador del proceso que intenta liberar.
     * @return pid del siguiente proceso que recibió el lock o null si ninguno.
     */
    public Integer unlock(int pid) {
        // Si el que intenta liberar no es el dueño actual, no se cambia el estado.
        if (ownerPid == null || ownerPid != pid) {
            return ownerPid;
        }

        // Pasamos el mutex al siguiente en la cola (si existe) y actualizamos el estado.
        Integer nextOwner = waitingProcesses.poll();
        ownerPid = nextOwner;
        locked = nextOwner != null;
        return nextOwner;
    }

    // Indica si el mutex está actualmente bloqueado.
    public boolean isLocked() {
        return locked;
    }

    // Devuelve el PID del propietario actual, o null si no hay propietario.
    public Integer getOwnerPid() {
        return ownerPid;
    }

    // Provee acceso a la cola de espera (útil para snapshots/UI).
    public Queue<Integer> getWaitingProcesses() {
        return waitingProcesses;
    }
}
