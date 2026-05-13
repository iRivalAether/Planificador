package com.os.simulator.services;

import com.os.simulator.model.ExecutionMetrics;
import com.os.simulator.model.Process;
import com.os.simulator.model.ProcessState;
import com.os.simulator.model.SystemState;
import java.util.List;

/**
 * Coordina la logica del simulador sin acoplarla a la interfaz grafica.
 */
public class SimulationService {
    private final SystemState systemState;
    private final ProcessGenerator processGenerator;
    private Scheduler scheduler;
    private int nextPid;

    /**
     * Crea el servicio con recursos base y el algoritmo FCFS por defecto.
     */
    public SimulationService() {
        this.systemState = new SystemState(8, 8192);
        this.processGenerator = new ProcessGenerator();
        this.scheduler = new FCFSScheduler();
        this.nextPid = 1;
    }

    /**
     * Crea y registra un proceso manual en el simulador.
     *
     * @param name nombre del proceso.
     * @param priority prioridad del proceso.
     * @param cpuBurst rafaga de CPU.
     * @param memoryRequired memoria requerida.
     */
    public void addProcess(String name, int priority, int cpuBurst, int memoryRequired) {
        Process process = new Process(nextPid++, name, priority, cpuBurst, memoryRequired, systemState.getCurrentTime());
        systemState.addProcess(process);
    }

    /**
     * Genera varios procesos aleatorios y los agrega al sistema.
     *
     * @param quantity cantidad de procesos.
     */
    public void generateRandomProcesses(int quantity) {
        List<Process> processes = processGenerator.generateProcesses(quantity);
        for (Process process : processes) {
            process.setState(ProcessState.NEW);
            systemState.addProcess(process);
            nextPid = Math.max(nextPid, process.getPid() + 1);
        }
    }

    /**
     * Cambia el algoritmo de planificacion usado por el simulador.
     *
     * @param scheduler nuevo planificador.
     */
    public void setScheduler(Scheduler scheduler) {
        this.scheduler = scheduler;
        systemState.logEvent("Planificador activo: " + scheduler.getName());
    }

    /**
     * Ejecuta un paso de simulacion.
     *
     * @return proceso ejecutado o null si no habia procesos listos.
     */
    public Process runStep() {
        Process process = scheduler.selectNextProcess(systemState.getReadyQueue());
        if (process == null) {
            systemState.logEvent("No hay procesos en cola de listos.");
            systemState.tick();
            return null;
        }

        process.setState(ProcessState.RUNNING);
        systemState.logEvent("Ejecutando PID " + process.getPid() + " con " + scheduler.getName());
        process.executeOneUnit();
        systemState.tick();

        if (process.isFinished()) {
            process.setState(ProcessState.TERMINATED);
            process.setTurnaroundTime(systemState.getCurrentTime() - process.getArrivalTime());
            systemState.logEvent("Proceso terminado: PID " + process.getPid());
        } else {
            process.setState(ProcessState.READY);
            systemState.getReadyQueue().offer(process);
            systemState.logEvent("Proceso devuelto a cola: PID " + process.getPid());
        }

        return process;
    }

    /**
     * Obtiene una referencia al estado completo del sistema.
     *
     * @return estado del sistema.
     */
    public SystemState getSystemState() {
        return systemState;
    }

    /**
     * Calcula una version inicial de las metricas del sistema.
     *
     * @return metricas de ejecucion.
     */
    public ExecutionMetrics calculateMetrics() {
        ExecutionMetrics metrics = new ExecutionMetrics();
        long finished = systemState.getProcesses().stream().filter(Process::isFinished).count();
        metrics.setCompletedProcesses((int) finished);
        metrics.setAverageWaitingTime(systemState.getProcesses().stream().mapToInt(Process::getWaitingTime).average().orElse(0));
        metrics.setAverageTurnaroundTime(systemState.getProcesses().stream().mapToInt(Process::getTurnaroundTime).average().orElse(0));
        metrics.setCpuUtilization(0);
        metrics.setContextSwitches(0);
        return metrics;
    }
}
