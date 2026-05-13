package com.os.simulator.services;

import com.os.simulator.model.Process;

/**
 * Define el contrato para la logica de ejecucion de un algoritmo de planificacion.
 */
public interface AlgorithmExecutionService {
    // Cada algoritmo tiene su propia clase para evitar mezclar la decision de planificacion con la coordinacion general.
    /**
     * Ejecuta un paso del algoritmo sobre el estado actual del simulador.
     *
     * @param simulationService servicio coordinador que contiene el estado global.
     * @return proceso ejecutado o null si no habia trabajo disponible.
     */
    Process executeStep(SimulationService simulationService);

    /**
     * Obtiene el nombre del algoritmo ejecutado por este servicio.
     *
     * @return nombre legible del algoritmo.
     */
    String getName();
}
