package com.os.simulator.services;

import com.os.simulator.model.Process;
import java.util.Comparator;
import java.util.Queue;

/**
 * Algoritmo por prioridades: elige el proceso con prioridad mas alta.
 */
public class PriorityScheduler implements Scheduler {
    @Override
    public Process selectNextProcess(Queue<Process> readyQueue) {
        Process selected = readyQueue.stream()
                .max(Comparator.comparingInt(Process::getPriority))
                .orElse(null);

        if (selected != null) {
            readyQueue.remove(selected);
        }

        return selected;
    }

    @Override
    public String getName() {
        return "Prioridad";
    }
}
