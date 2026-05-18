package com.os.simulator.services;

import com.os.simulator.model.Process;
import com.os.simulator.model.ProcessState;
import com.os.simulator.model.SystemState;

/**
 * Ejecuta la logica de SJF paso a paso.
 */
public class SjfExecutionService implements AlgorithmExecutionService {
    @Override
    public Process executeStep(SimulationService simulationService) {
        // SJF toma el proceso con menor rafaga pendiente porque busca reducir el promedio de espera.
        SystemState state = simulationService.getSystemState();
        state.tryAdmitEligibleProcesses();

        // Si no hay procesos listos, avanzamos el tiempo y retornamos.
        if (state.getReadyQueue().isEmpty()) {
            state.logEvent("No hay procesos en cola de listos para SJF.");
            state.advanceTime();
            simulationService.clearLastDispatchedProcess();
            return null;
        }

        // Selecciona el proceso con menor rafaga restante usando el scheduler SJF.
        Process process = simulationService.getScheduler().selectNextProcess(state.getReadyQueue());
        if (process == null) {
            state.logEvent("SJF no retorno un proceso valido.");
            state.advanceTime();
            simulationService.clearLastDispatchedProcess();
            return null;
        }

        // Registrar cambio de contexto y marcar como RUNNING.
        simulationService.registerContextSwitch(process);
        process.setState(ProcessState.RUNNING);
        state.logEvent("SJF ejecuta PID " + process.getPid() + " con menor rafaga restante");
        state.logEvent("EVENT:DISPATCH PID " + process.getPid());
        state.logEvent("EVENT:START PID " + process.getPid());

        // Intento de asignar CPU; si falla, reinsertamos en READY para reintentar.
        if (!state.getResource().allocate(1, 0)) {
            state.logEvent("SJF no pudo asignar CPU al PID " + process.getPid());
            process.setState(ProcessState.READY);
            state.enqueueReadyProcess(process);
            state.advanceTime();
            simulationService.clearLastDispatchedProcess();
            return process;
        }

        // Marcar CPU asignada para visualización y ejecutar una unidad.
        process.setAllocatedCpuUnits(process.getAllocatedCpuUnits() + 1);
        process.executeOneUnit();
        simulationService.incrementExecutedCpuUnits(1);
        state.getResource().release(1, 0);
        process.setAllocatedCpuUnits(Math.max(0, process.getAllocatedCpuUnits() - 1));
        state.advanceTime();

        // Si terminó, finalizar; si no, devolver a READY para que SJF reevalúe en la siguiente iteración.
        if (process.isFinished()) {
            simulationService.finishProcess(process, "Finalizado normalmente");
            state.logEvent("EVENT:END PID " + process.getPid());
        } else {
            process.setState(ProcessState.READY);
            state.enqueueReadyProcess(process);
            state.logEvent("SJF devuelve PID " + process.getPid() + " a READY para reevaluacion");
            state.logEvent("EVENT:END PID " + process.getPid());
        }

        simulationService.setLastDispatchedProcess(process);
        return process;
    }

    @Override
    public String getName() {
        return "SJF";
    }
}
