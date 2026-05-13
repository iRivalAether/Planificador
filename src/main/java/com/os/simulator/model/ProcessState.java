package com.os.simulator.model;

/**
 * Representa los estados posibles de un proceso dentro del simulador.
 */
public enum ProcessState {
    NEW,
    READY,
    RUNNING,
    WAITING,
    SUSPENDED,
    TERMINATED
}
