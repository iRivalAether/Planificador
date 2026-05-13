package com.os.simulator.model;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

/**
 * Contiene el estado global del simulador.
 * Centraliza la lista de procesos, la cola de listos, los recursos y el registro de eventos.
 */
public class SystemState {
    private final List<Process> processes;
    private final Queue<Process> readyQueue;
    private final Resource resource;
    private final List<String> eventLog;
    private int currentTime;

    /**
     * Crea el estado inicial del sistema con recursos definidos.
     *
     * @param totalCpu cpu total simulada.
     * @param totalMemory memoria total simulada.
     */
    public SystemState(int totalCpu, int totalMemory) {
        this.processes = new ArrayList<>();
        this.readyQueue = new LinkedList<>();
        this.resource = new Resource(totalCpu, totalMemory);
        this.eventLog = new ArrayList<>();
        this.currentTime = 0;
    }

    /**
     * Agrega un proceso al sistema y lo coloca en la cola de listos.
     *
     * @param process proceso a registrar.
     */
    public void addProcess(Process process) {
        process.setState(ProcessState.READY);
        processes.add(process);
        readyQueue.offer(process);
        logEvent("Proceso agregado: PID " + process.getPid() + " - " + process.getName());
    }

    /**
     * Elimina un proceso del sistema buscando por PID.
     *
     * @param pid identificador del proceso.
     */
    public void removeProcess(int pid) {
        processes.removeIf(process -> process.getPid() == pid);
        readyQueue.removeIf(process -> process.getPid() == pid);
        logEvent("Proceso eliminado: PID " + pid);
    }

    /**
     * Busca un proceso por su PID.
     *
     * @param pid identificador del proceso.
     * @return proceso encontrado o null si no existe.
     */
    public Process findProcess(int pid) {
        return processes.stream()
                .filter(process -> process.getPid() == pid)
                .findFirst()
                .orElse(null);
    }

    /**
     * Registra un mensaje en el log de eventos del simulador.
     *
     * @param event mensaje a registrar.
     */
    public void logEvent(String event) {
        eventLog.add("[t=" + currentTime + "] " + event);
    }

    public List<Process> getProcesses() {
        return processes;
    }

    public Queue<Process> getReadyQueue() {
        return readyQueue;
    }

    public Resource getResource() {
        return resource;
    }

    public List<String> getEventLog() {
        return eventLog;
    }

    public int getCurrentTime() {
        return currentTime;
    }

    /**
     * Avanza el reloj simulador una unidad.
     */
    public void tick() {
        currentTime++;
    }
}
