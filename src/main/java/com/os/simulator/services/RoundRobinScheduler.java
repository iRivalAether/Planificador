package com.os.simulator.services;

import com.os.simulator.model.Process;
import java.util.Queue;

/**
 * Algoritmo Round Robin: ejecuta por quantum y devuelve el proceso al final si no termino.
 */
public class RoundRobinScheduler implements Scheduler {
    // Quantum fijo para controlar cuantos turnos seguidos puede consumir un proceso.
    private final int quantum;

    /**
     * Crea el planificador con un quantum definido.
     *
     * @param quantum unidades de tiempo por turno.
     */
    public RoundRobinScheduler(int quantum) {
        // Un quantum menor que uno no tendria sentido dentro de la simulacion.
        this.quantum = quantum;
    }

    public int getQuantum() {
        return quantum;
    }

    @Override
    public Process selectNextProcess(Queue<Process> readyQueue) {
        // Round Robin solo toma el siguiente proceso de la cola; el tiempo lo controla el service de ejecucion.
        return readyQueue.poll();
    }

    @Override
    public String getName() {
        return "Round Robin";
    }
}
