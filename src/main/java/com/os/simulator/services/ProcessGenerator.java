package com.os.simulator.services;

import com.os.simulator.model.Process;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Genera procesos aleatorios para probar el simulador sin capturar datos manualmente.
 */
public class ProcessGenerator {
    // Generador aleatorio usado para producir cargas distintas en cada corrida.
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
        // Este metodo usa rangos simples por defecto para probar el simulador rapidamente.
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

    /**
     * Genera procesos aleatorios usando rangos personalizados.
     *
     * @param quantity cantidad de procesos.
     * @param minPriority prioridad minima.
     * @param maxPriority prioridad maxima.
     * @param minCpu rafaga minima de CPU.
     * @param maxCpu rafaga maxima de CPU.
     * @param minMemory memoria minima.
     * @param maxMemory memoria maxima.
     * @return lista de procesos generados.
     */
    public List<Process> generateProcesses(int quantity, int minPriority, int maxPriority, int minCpu, int maxCpu, int minMemory, int maxMemory) {
        // Esta sobrecarga permite controlar la forma de la carga para comparar escenarios reales.
        List<Process> processes = new ArrayList<>();

        for (int index = 0; index < quantity; index++) {
            int pid = index + 1;
            int priority = randomRange(minPriority, maxPriority);
            int cpuBurst = randomRange(minCpu, maxCpu);
            int memoryRequired = randomRange(minMemory, maxMemory);
            int arrivalTime = random.nextInt(Math.max(quantity, 1));
            processes.add(new Process(pid, "Proceso " + pid, priority, cpuBurst, memoryRequired, arrivalTime));
        }

        return processes;
    }

    private int randomRange(int min, int max) {
        // Se normalizan limites para no depender del orden en que se reciban los parametros.
        int realMin = Math.min(min, max);
        int realMax = Math.max(min, max);
        return random.nextInt(realMax - realMin + 1) + realMin;
    }
}
