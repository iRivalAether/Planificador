package com.os.simulator.services;

import com.os.simulator.model.Process;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Genera procesos aleatorios para probar el simulador sin capturar datos manualmente.
 */
public class ProcessGenerator {
    private final Random random;

    /**
     * Inicializa el generador con una semilla aleatoria por defecto.
     */
    public ProcessGenerator() {
        this.random = new Random();
    }

    /**
     * Genera una lista de procesos aleatorios.
     *
     * @param quantity cantidad de procesos a crear.
     * @return lista de procesos generados.
     */
    public List<Process> generateProcesses(int quantity) {
        List<Process> processes = new ArrayList<>();

        for (int index = 0; index < quantity; index++) {
            int pid = index + 1;
            int priority = random.nextInt(10) + 1;
            int cpuBurst = random.nextInt(20) + 1;
            int memoryRequired = random.nextInt(512) + 64;
            int arrivalTime = random.nextInt(10);
            processes.add(new Process(pid, "Proceso " + pid, priority, cpuBurst, memoryRequired, arrivalTime));
        }

        return processes;
    }
}
