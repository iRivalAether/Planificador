package com.os.simulator.model;

/**
 * Reune las metricas de una ejecucion del simulador para mostrar comparaciones y resultados.
 */
public class ExecutionMetrics {
    // Promedio de tiempo de espera entre todos los procesos analizados.
    private double averageWaitingTime;
    // Promedio de turnaround, util para comparar la rapidez de cada algoritmo.
    private double averageTurnaroundTime;
    // Numero de cambios de contexto simulados durante la ejecucion.
    private int contextSwitches;
    // Porcentaje de CPU efectivamente utilizada en la simulacion.
    private double cpuUtilization;
    // Cantidad de procesos que ya concluyeron.
    private int completedProcesses;
    // Total de procesos considerados en la corrida.
    private int totalProcesses;
    // Tiempo total transcurrido en el simulador.
    private int totalTime;
    // Procesos terminados por unidad de tiempo; sirve para comparar rendimiento.
    private double throughput;

    public double getAverageWaitingTime() {
        return averageWaitingTime;
    }

    public void setAverageWaitingTime(double averageWaitingTime) {
        this.averageWaitingTime = averageWaitingTime;
    }

    public double getAverageTurnaroundTime() {
        return averageTurnaroundTime;
    }

    public void setAverageTurnaroundTime(double averageTurnaroundTime) {
        this.averageTurnaroundTime = averageTurnaroundTime;
    }

    public int getContextSwitches() {
        return contextSwitches;
    }

    public void setContextSwitches(int contextSwitches) {
        this.contextSwitches = contextSwitches;
    }

    public double getCpuUtilization() {
        return cpuUtilization;
    }

    public void setCpuUtilization(double cpuUtilization) {
        this.cpuUtilization = cpuUtilization;
    }

    public int getCompletedProcesses() {
        return completedProcesses;
    }

    public void setCompletedProcesses(int completedProcesses) {
        this.completedProcesses = completedProcesses;
    }

    public int getTotalProcesses() {
        return totalProcesses;
    }

    public void setTotalProcesses(int totalProcesses) {
        this.totalProcesses = totalProcesses;
    }

    public int getTotalTime() {
        return totalTime;
    }

    public void setTotalTime(int totalTime) {
        this.totalTime = totalTime;
    }

    public double getThroughput() {
        return throughput;
    }

    public void setThroughput(double throughput) {
        this.throughput = throughput;
    }
}
