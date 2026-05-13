package com.os.simulator.services;

import com.os.simulator.model.Process;
import com.os.simulator.model.ProcessState;
import com.os.simulator.model.SystemState;

/**
 * Ejecuta la logica de FCFS paso a paso.
 */
public class FcfsExecutionService implements AlgorithmExecutionService {
    @Override
    public Process executeStep(SimulationService simulationService) {
        // FCFS favorece el orden de llegada, asi que no reordena la cola.
        SystemState state = simulationService.getSystemState();
        state.tryAdmitEligibleProcesses();

        if (state.getReadyQueue().isEmpty()) {
            state.logEvent("No hay procesos en cola de listos para FCFS.");
            state.advanceTime();
            simulationService.clearLastDispatchedProcess();
            return null;
        }

        Process process = simulationService.getScheduler().selectNextProcess(state.getReadyQueue());
        if (process == null) {
            state.logEvent("FCFS no retorno un proceso valido.");
            state.advanceTime();
            simulationService.clearLastDispatchedProcess();
            return null;
        }

        simulationService.registerContextSwitch(process);
        process.setState(ProcessState.RUNNING);
        state.logEvent("FCFS ejecuta PID " + process.getPid());

        if (!state.getResource().allocate(1, 0)) {
            // Si no hay CPU disponible, el proceso vuelve a READY para no perder su lugar logico.
            state.logEvent("FCFS no pudo asignar CPU al PID " + process.getPid());
            process.setState(ProcessState.READY);
            state.enqueueReadyProcess(process);
            state.advanceTime();
            simulationService.clearLastDispatchedProcess();
            return process;
        }

        process.executeOneUnit();
        simulationService.incrementExecutedCpuUnits(1);
        state.getResource().release(1, 0);
        state.advanceTime();

        if (process.isFinished()) {
            // Cuando termina se libera memoria y se registra la causa de cierre.
            simulationService.finishProcess(process, "Finalizado normalmente");
        } else {
            // Si aun no termina, se vuelve a colocar al final de la cola para mantener el comportamiento de cola.
            process.setState(ProcessState.READY);
            state.enqueueReadyProcess(process);
            state.logEvent("FCFS devuelve PID " + process.getPid() + " al final de READY");
        }

        simulationService.setLastDispatchedProcess(process);
        return process;
    }

    @Override
    public String getName() {
        return "FCFS";
    }
}
