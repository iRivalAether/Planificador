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

        if (state.getReadyQueue().isEmpty()) {
            state.logEvent("No hay procesos en cola de listos para SJF.");
            state.advanceTime();
            simulationService.clearLastDispatchedProcess();
            return null;
        }

        Process process = simulationService.getScheduler().selectNextProcess(state.getReadyQueue());
        if (process == null) {
            state.logEvent("SJF no retorno un proceso valido.");
            state.advanceTime();
            simulationService.clearLastDispatchedProcess();
            return null;
        }

        simulationService.registerContextSwitch(process);
        process.setState(ProcessState.RUNNING);
        state.logEvent("SJF ejecuta PID " + process.getPid() + " con menor rafaga restante");
        state.logEvent("EVENT:DISPATCH PID " + process.getPid());
        state.logEvent("EVENT:START PID " + process.getPid());

        if (!state.getResource().allocate(1, 0)) {
            // Si el recurso no alcanza, se conserva el proceso en READY para reintentarlo luego.
            state.logEvent("SJF no pudo asignar CPU al PID " + process.getPid());
            process.setState(ProcessState.READY);
            state.enqueueReadyProcess(process);
            state.advanceTime();
            simulationService.clearLastDispatchedProcess();
            return process;
        }
        // marcar CPU asignada para UI
        process.setAllocatedCpuUnits(process.getAllocatedCpuUnits() + 1);

        process.executeOneUnit();
        simulationService.incrementExecutedCpuUnits(1);
        state.getResource().release(1, 0);
        process.setAllocatedCpuUnits(Math.max(0, process.getAllocatedCpuUnits() - 1));
        state.advanceTime();

        if (process.isFinished()) {
            // Al terminar, el algoritmo ya no debe volver a considerar este proceso.
            simulationService.finishProcess(process, "Finalizado normalmente");
            state.logEvent("EVENT:END PID " + process.getPid());
        } else {
            // Si sigue vivo, se reevalua en la siguiente iteracion porque SJF puede cambiar la seleccion.
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
