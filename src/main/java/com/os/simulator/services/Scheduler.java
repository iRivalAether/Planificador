package com.os.simulator.services;

import com.os.simulator.model.Process;
import java.util.Queue;

/**
 * Define el contrato de cualquier algoritmo de planificacion dentro del simulador.
 */
public interface Scheduler {
    /**
     * Selecciona el siguiente proceso a ejecutar desde la cola de listos.
     *
     * @param readyQueue cola de procesos listos para ejecutar.
     * @return proceso seleccionado o null si la cola esta vacia.
     */
    Process selectNextProcess(Queue<Process> readyQueue);

    /**
     * Retorna el nombre del algoritmo de planificacion.
     *
     * @return nombre legible del planificador.
     */
    String getName();
}
