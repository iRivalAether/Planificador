package com.os.simulator.controllers;

import com.os.simulator.model.ExecutionMetrics;
import com.os.simulator.model.Process;
import com.os.simulator.model.SystemState;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;
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
    // El controlador solo coordina acciones de la UI y delega la logica al service central.
    private final SimulationService simulationService;

    /**
     * Crea el controlador con el servicio principal del simulador.
     *
     * @param simulationService servicio de simulacion.
     */
    public MainController(SimulationService simulationService) {
        // Se inyecta el servicio para facilitar pruebas y mantener el controlador delgado.
        this.simulationService = simulationService;
    }

    public void createProcess(String name, int priority, int cpuBurst, int memoryRequired) {
        // La vista entrega valores y el service decide como registrar el proceso.
        simulationService.addProcess(name, priority, cpuBurst, memoryRequired);
    }

    public void generateRandomProcesses(int quantity) {
        simulationService.generateRandomProcesses(quantity);
    }

    public Process runStep() {
        // El controlador no conoce la mecanica interna; solo pide un paso de simulacion.
        return simulationService.runStep();
    }

    public ExecutionMetrics runSimulationToCompletion() {
        return simulationService.runUntilComplete();
    }

    public void setScheduler(String schedulerName) {
        // La vista trabaja con texto legible, asi que aqui se traduce al algoritmo concreto.
        simulationService.setSchedulerByName(schedulerName);
    }

    public void setScheduler(Scheduler scheduler) {
        simulationService.setScheduler(scheduler);
    }

    public boolean suspendProcess(int pid) {
        // Estas acciones permiten que la UI administre procesos sin tocar el modelo directamente.
        return simulationService.suspendProcess(pid);
    }

    public boolean resumeProcess(int pid) {
        return simulationService.resumeProcess(pid);
    }

    public boolean terminateProcess(int pid, String reason) {
        return simulationService.terminateProcess(pid, reason);
    }

    public void importProcesses(Path path) throws IOException {
        // Se expone para que la interfaz pueda cargar cargas de trabajo desde archivo.
        simulationService.importProcesses(path);
    }

    public void exportProcesses(Path path) throws IOException {
        simulationService.exportProcesses(path);
    }

    public Map<String, ExecutionMetrics> compareAlgorithms() {
        // La comparacion la hace el service y el controlador solo la entrega a la vista.
        return simulationService.compareAlgorithms();
    }

    public void appendExecutionToHistory() {
        // Se permite guardar la corrida actual desde la accion del usuario en la interfaz.
        simulationService.appendExecutionToHistory();
    }

    public java.util.List<String> loadHistory() throws IOException {
        return simulationService.loadHistory();
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
        // La vista consulta metricas ya calculadas para mostrarlas sin duplicar logica.
        return simulationService.calculateMetrics();
    }

    public String getSchedulerName() {
        return simulationService.getSchedulerName();
    }

    public boolean hasPendingWork() {
        return simulationService.hasPendingWork();
    }

    public void resetSimulation() {
        simulationService.resetSimulation();
    }
}
