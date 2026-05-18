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
        // Se elige la prioridad más alta porque ese es el criterio visible para el usuario en este algoritmo.
        Process selected = readyQueue.stream()
                .max(Comparator.comparingInt(Process::getPriority)) // Busca el mayor valor de prioridad.
                .orElse(null);

        // Si se encontró un candidato, lo eliminamos de la cola para evitar duplicados.
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
