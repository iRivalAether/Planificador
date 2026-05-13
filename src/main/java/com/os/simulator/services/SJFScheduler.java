package com.os.simulator.services;

import com.os.simulator.model.Process;
import java.util.Comparator;
import java.util.Queue;

/**
 * Algoritmo SJF: selecciona el proceso con menor rafaga restante de CPU.
 */
public class SJFScheduler implements Scheduler {
    @Override
    public Process selectNextProcess(Queue<Process> readyQueue) {
        Process selected = readyQueue.stream()
                .min(Comparator.comparingInt(Process::getRemainingCpu))
                .orElse(null);

        if (selected != null) {
            readyQueue.remove(selected);
        }

        return selected;
    }

    @Override
    public String getName() {
        return "SJF";
    }
}
