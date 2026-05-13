package com.os.simulator.services;

import com.os.simulator.model.Process;
import java.util.Queue;

/**
 * Algoritmo Round Robin: ejecuta por quantum y devuelve el proceso al final si no termino.
 */
public class RoundRobinScheduler implements Scheduler {
    private final int quantum;

    /**
     * Crea el planificador con un quantum definido.
     *
     * @param quantum unidades de tiempo por turno.
     */
    public RoundRobinScheduler(int quantum) {
        this.quantum = quantum;
    }

    public int getQuantum() {
        return quantum;
    }

    @Override
    public Process selectNextProcess(Queue<Process> readyQueue) {
        return readyQueue.poll();
    }

    @Override
    public String getName() {
        return "Round Robin";
    }
}
