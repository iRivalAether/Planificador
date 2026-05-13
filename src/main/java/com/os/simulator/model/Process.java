package com.os.simulator.model;

/**
 * Representa el bloque de control de un proceso dentro del simulador.
 * Contiene la informacion necesaria para planificarlo, ejecutarlo y registrar su historial.
 */
public class Process {
    private final int pid;
    private String name;
    private ProcessState state;
    private int priority;
    private int cpuBurst;
    private int cpuUsed;
    private int memoryRequired;
    private int arrivalTime;
    private int waitingTime;
    private int turnaroundTime;
    private String terminationReason;

    /**
     * Crea un proceso con sus parametros principales.
     *
     * @param pid identificador unico del proceso.
     * @param name nombre visible del proceso.
     * @param priority prioridad de planificacion, donde un valor mayor representa mas urgencia.
     * @param cpuBurst rafaga total de CPU que necesita el proceso.
     * @param memoryRequired memoria requerida por el proceso.
     * @param arrivalTime instante en el que llega al sistema.
     */
    public Process(int pid, String name, int priority, int cpuBurst, int memoryRequired, int arrivalTime) {
        this.pid = pid;
        this.name = name;
        this.priority = priority;
        this.cpuBurst = cpuBurst;
        this.memoryRequired = memoryRequired;
        this.arrivalTime = arrivalTime;
        this.state = ProcessState.NEW;
        this.cpuUsed = 0;
        this.waitingTime = 0;
        this.turnaroundTime = 0;
        this.terminationReason = "";
    }

    /**
     * Ejecuta al proceso por una unidad de tiempo simulada.
     *
     * @return true si el proceso termino su rafaga de CPU, false en caso contrario.
     */
    public boolean executeOneUnit() {
        if (state == ProcessState.TERMINATED) {
            return true;
        }

        if (cpuUsed < cpuBurst) {
            cpuUsed++;
            if (cpuUsed >= cpuBurst) {
                state = ProcessState.TERMINATED;
                return true;
            }
        }

        return false;
    }

    /**
     * Indica si el proceso ya completo su ejecucion.
     *
     * @return true si ya termino, false en caso contrario.
     */
    public boolean isFinished() {
        return cpuUsed >= cpuBurst;
    }

    public int getPid() {
        return pid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public ProcessState getState() {
        return state;
    }

    public void setState(ProcessState state) {
        this.state = state;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public int getCpuBurst() {
        return cpuBurst;
    }

    public void setCpuBurst(int cpuBurst) {
        this.cpuBurst = cpuBurst;
    }

    public int getCpuUsed() {
        return cpuUsed;
    }

    public void setCpuUsed(int cpuUsed) {
        this.cpuUsed = cpuUsed;
    }

    public int getMemoryRequired() {
        return memoryRequired;
    }

    public void setMemoryRequired(int memoryRequired) {
        this.memoryRequired = memoryRequired;
    }

    public int getArrivalTime() {
        return arrivalTime;
    }

    public void setArrivalTime(int arrivalTime) {
        this.arrivalTime = arrivalTime;
    }

    public int getWaitingTime() {
        return waitingTime;
    }

    public void setWaitingTime(int waitingTime) {
        this.waitingTime = waitingTime;
    }

    public int getTurnaroundTime() {
        return turnaroundTime;
    }

    public void setTurnaroundTime(int turnaroundTime) {
        this.turnaroundTime = turnaroundTime;
    }

    public String getTerminationReason() {
        return terminationReason;
    }

    public void setTerminationReason(String terminationReason) {
        this.terminationReason = terminationReason;
    }

    /**
     * Retorna la rafaga de CPU que falta por ejecutar.
     *
     * @return tiempo de CPU pendiente.
     */
    public int getRemainingCpu() {
        return Math.max(cpuBurst - cpuUsed, 0);
    }
}
