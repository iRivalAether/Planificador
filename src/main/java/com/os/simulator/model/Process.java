package com.os.simulator.model;

/**
 * Representa el bloque de control de un proceso dentro del simulador.
 * Contiene la informacion necesaria para planificarlo, ejecutarlo y registrar su historial.
 */
public class Process {
    // Identificador unico del proceso; se mantiene final porque no cambia durante la simulacion.
    private final int pid;
    // Nombre legible para mostrarlo en la interfaz y en los logs.
    private String name;
    // Estado actual del proceso dentro del ciclo de vida del simulador.
    private ProcessState state;
    // Prioridad de planificacion; a mayor valor, mayor preferencia en el algoritmo por prioridades.
    private int priority;
    // Total de CPU que necesita el proceso para terminar.
    private int cpuBurst;
    // CPU consumida hasta el momento.
    private int cpuUsed;
    // Memoria solicitada por el proceso para poder entrar al sistema.
    private int memoryRequired;
    // Instante en el que el proceso llega al sistema.
    private int arrivalTime;
    // Tiempo acumulado esperando en READY o WAITING.
    private int waitingTime;
    // Tiempo total entre llegada y terminacion.
    private int turnaroundTime;
    // Motivo por el que termino el proceso; ayuda a auditoria y depuracion.
    private String terminationReason;
    // Marca si el proceso ya tiene memoria reservada dentro del simulador.
    private boolean memoryAllocated;
    // Recursos asignados actualmente al proceso (estado visible para UI).
    private int allocatedCpuUnits;
    private int allocatedMemoryUnits;

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
        this.memoryAllocated = false;
        this.allocatedCpuUnits = 0;
        this.allocatedMemoryUnits = 0;
    }

    /**
     * Crea una copia del proceso conservando sus datos de planificacion.
     *
     * @return nueva instancia con los mismos valores de negocio.
     */
    public Process copy() {
        // Se crea una copia porque las comparaciones deben correr con datos independientes.
        Process copy = new Process(pid, name, priority, cpuBurst, memoryRequired, arrivalTime);
        copy.setState(state);
        copy.setCpuUsed(cpuUsed);
        copy.setWaitingTime(waitingTime);
        copy.setTurnaroundTime(turnaroundTime);
        copy.setTerminationReason(terminationReason);
        copy.setMemoryAllocated(memoryAllocated);
        return copy;
    }

    /**
     * Ejecuta al proceso por una unidad de tiempo simulada.
     *
     * @return true si el proceso termino su rafaga de CPU, false en caso contrario.
     */
    public boolean executeOneUnit() {
        // Un proceso terminado no debe consumir mas CPU simulada.
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

    /**
     * Incrementa el tiempo de espera acumulado cuando el proceso permanece listo o bloqueado.
     */
    public void incrementWaitingTime() {
        // Solo se incrementa una unidad porque el reloj del simulador avanza de forma discreta.
        waitingTime++;
    }

    /**
     * Marca el proceso como propietario de memoria reservada dentro del simulador.
     *
     * @param memoryAllocated indica si la memoria esta reservada.
     */
    public void setMemoryAllocated(boolean memoryAllocated) {
        // Este dato evita reservar la misma memoria dos veces cuando el proceso se reanuda.
        this.memoryAllocated = memoryAllocated;
    }

    public int getAllocatedCpuUnits() {
        return allocatedCpuUnits;
    }

    public void setAllocatedCpuUnits(int allocatedCpuUnits) {
        this.allocatedCpuUnits = Math.max(0, allocatedCpuUnits);
    }

    public int getAllocatedMemoryUnits() {
        return allocatedMemoryUnits;
    }

    public void setAllocatedMemoryUnits(int allocatedMemoryUnits) {
        this.allocatedMemoryUnits = Math.max(0, allocatedMemoryUnits);
    }

    /**
     * Indica si el proceso tiene memoria asignada dentro del simulador.
     *
     * @return true si ya tiene memoria reservada.
     */
    public boolean isMemoryAllocated() {
        return memoryAllocated;
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
        // Nunca se permiten valores negativos porque eso romperia las comparaciones de SJF.
        return Math.max(cpuBurst - cpuUsed, 0);
    }
}
