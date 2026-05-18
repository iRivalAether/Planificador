package com.os.simulator.services.synchronization;

import java.util.LinkedList;
import java.util.Queue;

/**
 * Simula un semaforo contador para coordinar el acceso a recursos compartidos.
 */
public class Semaphore {
    // Valor actual del semáforo (contador de recursos disponibles).
    private int value;
    // Cola de procesos que esperan porque el semáforo no tenía suficientes recursos.
    private final Queue<Integer> waitingProcesses;

    // Inicializa el semáforo con un valor inicial (puede ser 0 o superior).
    public Semaphore(int initialValue) {
        this.value = initialValue;
        this.waitingProcesses = new LinkedList<>();
    }

    /**
     * Operación wait (P) del semáforo. Si hay valor positivo, se decrementa y el proceso continúa.
     * Si no hay valor, el proceso se coloca en la cola de espera.
     *
     * @param pid identificador del proceso.
     * @return true si pudo continuar sin bloquearse, false si quedó en espera.
     */
    public boolean waitFor(int pid) {
        if (value > 0) {
            value--; // Consume un recurso disponible.
            return true;
        }

        // Si no había recursos, registramos al proceso en la cola de espera (si no está ya).
        if (!waitingProcesses.contains(pid)) {
            waitingProcesses.offer(pid);
        }
        return false;
    }

    /**
     * Operación signal (V) del semáforo. Incrementa el valor y, si hay procesos esperando,
     * devuelve el PID del proceso que debe ser liberado.
     *
     * @return pid del proceso liberado o null si nadie esperaba.
     */
    public Integer signal() {
        Integer released = waitingProcesses.poll();
        value++; // Se incrementa el contador de recursos.
        return released;
    }

    // Devuelve el valor actual del semáforo.
    public int getValue() {
        return value;
    }

    // Proporciona acceso a la cola de espera para inspección/UI.
    public Queue<Integer> getWaitingProcesses() {
        return waitingProcesses;
    }
}
