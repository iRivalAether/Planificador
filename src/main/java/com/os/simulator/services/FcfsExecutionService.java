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
        // Structured events for UI timeline
        state.logEvent("EVENT:DISPATCH PID " + process.getPid());
        state.logEvent("EVENT:START PID " + process.getPid());

        if (!state.getResource().allocate(1, 0)) {
            // Si no hay CPU disponible, el proceso vuelve a READY para no perder su lugar logico.
            state.logEvent("FCFS no pudo asignar CPU al PID " + process.getPid());
            process.setState(ProcessState.READY);
            state.enqueueReadyProcess(process);
            state.advanceTime();
            simulationService.clearLastDispatchedProcess();
            return process;
        }

        // registrar unidad de CPU asignada para visibilidad en UI
        process.setAllocatedCpuUnits(process.getAllocatedCpuUnits() + 1);

        process.executeOneUnit();
        simulationService.incrementExecutedCpuUnits(1);
        state.getResource().release(1, 0);
        process.setAllocatedCpuUnits(Math.max(0, process.getAllocatedCpuUnits() - 1));
        state.advanceTime();

        if (process.isFinished()) {
            // Cuando termina se libera memoria y se registra la causa de cierre.
            simulationService.finishProcess(process, "Finalizado normalmente");
            state.logEvent("EVENT:END PID " + process.getPid());
            state.logEvent("EVENT:FINISH PID " + process.getPid());
        } else {
            // Si aun no termina, se vuelve a colocar al final de la cola para mantener el comportamiento de cola.
            process.setState(ProcessState.READY);
            state.enqueueReadyProcess(process);
            state.logEvent("FCFS devuelve PID " + process.getPid() + " al final de READY");
            state.logEvent("EVENT:END PID " + process.getPid());
            state.logEvent("EVENT:PREEMPT PID " + process.getPid());
        }

        simulationService.setLastDispatchedProcess(process);
        return process;
    }

    @Override
    public String getName() {
        return "FCFS";
    }
}
