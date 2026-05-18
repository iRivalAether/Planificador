package com.os.simulator.model;

/**
 * Representa los estados posibles de un proceso dentro del simulador.
 */
public enum ProcessState {
    // Proceso creado pero aún no admitido en la cola de listos.
    NEW,
    // Proceso listo para ejecutarse, esperando a que el CPU lo tome.
    READY,
    // Proceso actualmente en ejecución en la CPU.
    RUNNING,
    // Proceso bloqueado esperando un evento o recurso (I/O, semáforo, etc.).
    WAITING,
    // Proceso suspendido (por ejemplo, swapped out o pausado explícitamente).
    SUSPENDED,
    // Proceso ya terminó su ejecución; estado terminal.
    TERMINATED
}
