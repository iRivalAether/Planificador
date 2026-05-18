package com.os.simulator.services;

import com.os.simulator.model.Process; // Modelo de proceso usado como unidad de ejecución.

/**
 * Contrato para servicios que implementan la lógica de ejecución de un algoritmo.
 * Cada implementación recibe el `SimulationService` para consultar y mutar
 * el estado global sin acoplar la lógica de planificación con la coordinación.
 */
public interface AlgorithmExecutionService {
    // Ejecuta un solo paso (unidad de tiempo lógico) del algoritmo sobre el simulador.
    /**
     * Ejecuta un paso del algoritmo sobre el estado actual del simulador.
     *
     * @param simulationService servicio coordinador que contiene el estado global.
     * @return proceso ejecutado o null si no había trabajo disponible.
     */
    Process executeStep(SimulationService simulationService);

    // Retorna el nombre legible del algoritmo (ej. "FCFS", "SJF").
    /**
     * Obtiene el nombre del algoritmo ejecutado por este servicio.
     *
     * @return nombre legible del algoritmo.
     */
    String getName();
}
