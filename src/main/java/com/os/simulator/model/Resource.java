package com.os.simulator.model;

/**
 * Representa los recursos globales del sistema simulador.
 * Permite reservar y liberar memoria y CPU de forma controlada.
 */
public class Resource {
    private final int totalCpu;
    private final int totalMemory;
    private int usedCpu;
    private int usedMemory;

    /**
     * Crea el administrador de recursos con sus valores totales.
     *
     * @param totalCpu cantidad total de CPU simulada.
     * @param totalMemory cantidad total de memoria simulada.
     */
    public Resource(int totalCpu, int totalMemory) {
        this.totalCpu = totalCpu;
        this.totalMemory = totalMemory;
        this.usedCpu = 0;
        this.usedMemory = 0;
    }

    /**
     * Verifica si existen recursos suficientes para reservar una carga solicitada.
     *
     * @param cpu cpu solicitada.
     * @param memory memoria solicitada.
     * @return true si se puede asignar, false si no hay capacidad.
     */
    public boolean canAllocate(int cpu, int memory) {
        return usedCpu + cpu <= totalCpu && usedMemory + memory <= totalMemory;
    }

    /**
     * Reserva recursos si existe disponibilidad.
     *
     * @param cpu cpu a reservar.
     * @param memory memoria a reservar.
     * @return true si se reservo correctamente, false si no hay recursos.
     */
    public boolean allocate(int cpu, int memory) {
        if (!canAllocate(cpu, memory)) {
            return false;
        }

        usedCpu += cpu;
        usedMemory += memory;
        return true;
    }

    /**
     * Libera recursos previamente reservados.
     *
     * @param cpu cpu a liberar.
     * @param memory memoria a liberar.
     */
    public void release(int cpu, int memory) {
        usedCpu = Math.max(usedCpu - cpu, 0);
        usedMemory = Math.max(usedMemory - memory, 0);
    }

    public int getTotalCpu() {
        return totalCpu;
    }

    public int getTotalMemory() {
        return totalMemory;
    }

    public int getUsedCpu() {
        return usedCpu;
    }

    public int getUsedMemory() {
        return usedMemory;
    }

    /**
     * Obtiene la cantidad de CPU disponible.
     *
     * @return cpu libre.
     */
    public int getAvailableCpu() {
        return totalCpu - usedCpu;
    }

    /**
     * Obtiene la cantidad de memoria disponible.
     *
     * @return memoria libre.
     */
    public int getAvailableMemory() {
        return totalMemory - usedMemory;
    }
}
