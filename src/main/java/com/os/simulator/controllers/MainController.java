package com.os.simulator.controllers;

import com.os.simulator.model.ExecutionMetrics;
import com.os.simulator.model.Process;
import com.os.simulator.model.SystemState;
import com.os.simulator.services.FCFSScheduler;
import com.os.simulator.services.PriorityScheduler;
import com.os.simulator.services.RoundRobinScheduler;
import com.os.simulator.services.SJFScheduler;
import com.os.simulator.services.Scheduler;
import com.os.simulator.services.SimulationService;
import java.util.List;

/**
 * Controlador principal que conecta la vista con la logica del simulador.
 */
public class MainController {
    private final SimulationService simulationService;

    /**
     * Crea el controlador con el servicio principal del simulador.
     *
     * @param simulationService servicio de simulacion.
     */
    public MainController(SimulationService simulationService) {
        this.simulationService = simulationService;
    }

    public void createProcess(String name, int priority, int cpuBurst, int memoryRequired) {
        simulationService.addProcess(name, priority, cpuBurst, memoryRequired);
    }

    public void generateRandomProcesses(int quantity) {
        simulationService.generateRandomProcesses(quantity);
    }

    public void runStep() {
        simulationService.runStep();
    }

    public void setScheduler(String schedulerName) {
        Scheduler scheduler;

        switch (schedulerName) {
            case "SJF":
                scheduler = new SJFScheduler();
                break;
            case "Round Robin":
                scheduler = new RoundRobinScheduler(2);
                break;
            case "Prioridad":
                scheduler = new PriorityScheduler();
                break;
            default:
                scheduler = new FCFSScheduler();
                break;
        }

        simulationService.setScheduler(scheduler);
    }

    public List<Process> getProcesses() {
        return simulationService.getSystemState().getProcesses();
    }

    public List<String> getEventLog() {
        return simulationService.getSystemState().getEventLog();
    }

    public SystemState getSystemState() {
        return simulationService.getSystemState();
    }

    public ExecutionMetrics getMetrics() {
        return simulationService.calculateMetrics();
    }
}
