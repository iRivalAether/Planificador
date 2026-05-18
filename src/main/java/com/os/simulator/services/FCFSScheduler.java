package com.os.simulator.services;

import com.os.simulator.model.Process;
import java.util.Queue;

/**
 * Algoritmo FCFS: selecciona el primer proceso que llego a la cola.
 */
public class FCFSScheduler implements Scheduler {
    @Override
    public Process selectNextProcess(Queue<Process> readyQueue) {
        // FCFS respeta estrictamente el orden de llegada, por eso solo extrae el primero.
        return readyQueue.poll(); // poll() devuelve y elimina la cabeza de la cola, o null si vacía.
    }

    @Override
    public String getName() {
        return "FCFS";
    }
}
