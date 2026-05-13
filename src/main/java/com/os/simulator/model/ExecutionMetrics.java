package com.os.simulator.model;

/**
 * Reune las metricas de una ejecucion del simulador para mostrar comparaciones y resultados.
 */
public class ExecutionMetrics {
    private double averageWaitingTime;
    private double averageTurnaroundTime;
    private int contextSwitches;
    private double cpuUtilization;
    private int completedProcesses;

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
}
