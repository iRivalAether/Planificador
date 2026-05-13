package com.os.simulator.services;

import com.os.simulator.model.Process;
import java.util.Queue;

/**
 * Algoritmo FCFS: selecciona el primer proceso que llego a la cola.
 */
public class FCFSScheduler implements Scheduler {
    @Override
    public Process selectNextProcess(Queue<Process> readyQueue) {
        return readyQueue.poll();
    }

    @Override
    public String getName() {
        return "FCFS";
    }
}
