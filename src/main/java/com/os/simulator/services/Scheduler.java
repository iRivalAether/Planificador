package com.os.simulator.services;

import com.os.simulator.model.Process;
import java.util.Queue;

/**
 * Contrato para los planificadores (schedulers) que deciden qué proceso ejecutar.
 * Implementaciones pueden usar la cola tal cual (FCFS/RR) o reevaluar el contenido (SJF/Prioridad).
 */
public interface Scheduler {
    /**
     * Selecciona el siguiente proceso a ejecutar desde la cola de listos.
     *
     * @param readyQueue cola de procesos listos para ejecutar.
     * @return proceso seleccionado o null si la cola está vacía.
     */
    Process selectNextProcess(Queue<Process> readyQueue);

    /**
     * Retorna el nombre legible del algoritmo (para UI/registro).
     *
     * @return nombre legible del planificador.
     */
    String getName();
}
