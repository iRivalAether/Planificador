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
    // Lista maestra de procesos que existen dentro del simulador.
    private final List<Process> processes;
    // Cola de listos; el scheduler toma de aqui el siguiente proceso a ejecutar.
    private final Queue<Process> readyQueue;
    // Gestor de recursos para CPU y memoria.
    private final Resource resource;
    // Registro textual de eventos para mostrar en la GUI y depurar.
    private final List<String> eventLog;
    // Reloj discreto del simulador; avanza una unidad por paso.
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
        // Primero se registra el proceso en la lista general para que pueda ser consultado aunque aun no sea listo.
        processes.add(process);

        if (process.getArrivalTime() > currentTime) {
            process.setState(ProcessState.NEW);
            logEvent("Proceso programado para llegada futura: PID " + process.getPid());
            return;
        }

        admitProcessIfPossible(process);
    }

    /**
     * Elimina un proceso del sistema buscando por PID.
     *
     * @param pid identificador del proceso.
     */
    public void removeProcess(int pid) {
        // Se busca antes de eliminar para poder liberar recursos y registrar el evento correcto.
        Process process = findProcess(pid);
        if (process == null) {
            return;
        }

        releaseResourcesIfNeeded(process);
        processes.remove(process);
        readyQueue.removeIf(item -> item.getPid() == pid);
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
     * Intenta admitir un proceso en READY si su llegada ya ocurrio y hay memoria disponible.
     *
     * @param process proceso candidato.
     * @return true si quedo listo para planificacion.
     */
    public boolean admitProcessIfPossible(Process process) {
        // Solo los procesos que ya llegaron y no terminaron pueden entrar a READY.
        if (process == null || process.getArrivalTime() > currentTime || process.getState() == ProcessState.TERMINATED) {
            return false;
        }

        if (process.isMemoryAllocated()) {
            process.setState(ProcessState.READY);
            enqueueReadyProcess(process);
            logEvent("Proceso listo: PID " + process.getPid());
            return true;
        }

        if (!resource.allocate(0, process.getMemoryRequired())) {
            process.setState(ProcessState.WAITING);
            logEvent("Proceso en espera por memoria: PID " + process.getPid());
            return false;
        }

        process.setMemoryAllocated(true);
        process.setAllocatedMemoryUnits(process.getMemoryRequired());
        process.setState(ProcessState.READY);
        enqueueReadyProcess(process);
        logEvent("Proceso admitido a READY: PID " + process.getPid());
        return true;
    }

    /**
     * Promueve procesos que ya llegaron y cuyo bloqueo desaparecio.
     */
    public void tryAdmitEligibleProcesses() {
        // Se revisan NEW y WAITING porque ambos pueden convertirse en READY cuando el sistema avanza.
        for (Process process : processes) {
            if (process.getState() == ProcessState.NEW || process.getState() == ProcessState.WAITING) {
                admitProcessIfPossible(process);
            }
        }
    }

    /**
     * Incrementa los tiempos de espera de los procesos que siguen esperando o listos.
     */
    public void updateWaitingTimes() {
        // READY y WAITING acumulan espera porque no estan consumiendo CPU.
        for (Process process : processes) {
            if (process.getState() == ProcessState.READY || process.getState() == ProcessState.WAITING) {
                process.incrementWaitingTime();
            }
        }
    }

    /**
     * Suspende un proceso por PID, liberando memoria si estaba asignada.
     *
     * @param pid identificador del proceso.
     * @return true si fue suspendido correctamente.
     */
    public boolean suspendProcess(int pid) {
        // Suspender equivale a sacar al proceso de la planificacion activa sin borrarlo del sistema.
        Process process = findProcess(pid);
        if (process == null || process.getState() == ProcessState.TERMINATED) {
            return false;
        }

        readyQueue.remove(process);
        releaseResourcesIfNeeded(process);
        process.setState(ProcessState.SUSPENDED);
        logEvent("Proceso suspendido: PID " + pid);
        return true;
    }

    /**
     * Reanuda un proceso suspendido y lo devuelve a READY si hay memoria disponible.
     *
     * @param pid identificador del proceso.
     * @return true si logro reanudarse.
     */
    public boolean resumeProcess(int pid) {
        // Se intenta recuperar un proceso suspendido solo si vuelve a tener memoria disponible.
        Process process = findProcess(pid);
        if (process == null || process.getState() != ProcessState.SUSPENDED) {
            return false;
        }

        if (!resource.allocate(0, process.getMemoryRequired())) {
            process.setState(ProcessState.WAITING);
            logEvent("Proceso suspendido reubicado a WAITING por falta de memoria: PID " + pid);
            return false;
        }

        process.setMemoryAllocated(true);
        process.setState(ProcessState.READY);
        enqueueReadyProcess(process);
        logEvent("Proceso reanudado: PID " + pid);
        return true;
    }

    /**
     * Termina un proceso de forma forzada o natural liberando sus recursos.
     *
     * @param pid identificador del proceso.
     * @param reason causa de terminacion.
     * @return true si el proceso existia y se termino.
     */
    public boolean terminateProcess(int pid, String reason) {
        // La terminacion forzada o natural siempre libera recursos y cierra el ciclo de vida del proceso.
        Process process = findProcess(pid);
        if (process == null || process.getState() == ProcessState.TERMINATED) {
            return false;
        }

        readyQueue.remove(process);
        releaseResourcesIfNeeded(process);
        process.setState(ProcessState.TERMINATED);
        process.setTerminationReason(reason == null ? "Terminacion normal" : reason);
        process.setTurnaroundTime(currentTime - process.getArrivalTime());
        logEvent("Proceso terminado: PID " + pid + " - " + process.getTerminationReason());
        return true;
    }

    /**
     * Reintegra un proceso a la cola de listos sin duplicarlo.
     *
     * @param process proceso a insertar.
     */
    public void enqueueReadyProcess(Process process) {
        // Se evita duplicar entradas en READY al remover antes de volver a insertar.
        readyQueue.remove(process);
        if (process.getState() == ProcessState.READY) {
            readyQueue.offer(process);
        }
    }

    /**
     * Avanza el reloj del simulador una unidad y actualiza espera y admisiones.
     */
    public void advanceTime() {
        // El reloj avanza, luego se actualizan esperas y admisiones para mantener la simulacion coherente.
        currentTime++;
        updateWaitingTimes();
        tryAdmitEligibleProcesses();
    }

    /**
     * Libera recursos asociados al proceso si estan reservados.
     *
     * @param process proceso afectado.
     */
    public void releaseResourcesIfNeeded(Process process) {
        // Solo se libera lo que realmente fue reservado para evitar inconsistencias.
        if (process != null && process.isMemoryAllocated()) {
            resource.release(0, process.getMemoryRequired());
            process.setMemoryAllocated(false);
            process.setAllocatedMemoryUnits(0);
        }
    }

    /**
     * Detecta si el sistema parece estar en un estado de interbloqueo simple.
     * Condicion simplificada: todos los procesos activos estan en WAITING (no RUNNING ni READY).
     * @return true si se detecta posible deadlock.
     */
    public boolean detectDeadlock() {
        boolean hasActive = false;
        boolean hasWaiting = false;
        boolean hasRunningOrReady = false;
        for (Process p : processes) {
            if (p.getState() != ProcessState.TERMINATED && p.getState() != ProcessState.SUSPENDED) {
                hasActive = true;
                if (p.getState() == ProcessState.WAITING) hasWaiting = true;
                if (p.getState() == ProcessState.RUNNING || p.getState() == ProcessState.READY) hasRunningOrReady = true;
            }
        }
        return hasActive && hasWaiting && !hasRunningOrReady;
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
        advanceTime();
    }
}
