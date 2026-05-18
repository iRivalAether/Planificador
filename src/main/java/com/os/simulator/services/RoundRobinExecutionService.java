package com.os.simulator.services;

import com.os.simulator.model.Process;
import com.os.simulator.model.ProcessState;
import com.os.simulator.model.SystemState;

/**
 * Ejecuta la logica de Round Robin paso a paso usando el quantum configurado.
 */
public class RoundRobinExecutionService implements AlgorithmExecutionService {
    @Override
    public Process executeStep(SimulationService simulationService) {
        // Round Robin reparte CPU por turnos fijos para evitar que un solo proceso acapare el procesador.
        SystemState state = simulationService.getSystemState();
        state.tryAdmitEligibleProcesses();

        if (state.getReadyQueue().isEmpty()) {
            state.logEvent("No hay procesos en cola de listos para Round Robin.");
            state.advanceTime();
            simulationService.clearLastDispatchedProcess();
            return null;
        }

        Process process = simulationService.getScheduler().selectNextProcess(state.getReadyQueue());
        if (process == null) {
            state.logEvent("Round Robin no retorno un proceso valido.");
            state.advanceTime();
            simulationService.clearLastDispatchedProcess();
            return null;
        }

        simulationService.registerContextSwitch(process);
        process.setState(ProcessState.RUNNING);
        state.logEvent("Round Robin ejecuta PID " + process.getPid());
        state.logEvent("EVENT:DISPATCH PID " + process.getPid());
        state.logEvent("EVENT:START PID " + process.getPid());

        int quantum = simulationService.getRoundRobinQuantum();
        // Se ejecuta hasta agotar el quantum o hasta que el proceso termine.
        int executedUnits = 0;
        while (executedUnits < quantum && !process.isFinished()) {
            if (!state.getResource().allocate(1, 0)) {
                state.logEvent("Round Robin no pudo asignar CPU al PID " + process.getPid());
                break;
            }
            // marcar CPU asignada para UI
            process.setAllocatedCpuUnits(process.getAllocatedCpuUnits() + 1);

            process.executeOneUnit();
            simulationService.incrementExecutedCpuUnits(1);
            state.getResource().release(1, 0);
            process.setAllocatedCpuUnits(Math.max(0, process.getAllocatedCpuUnits() - 1));
            state.advanceTime();
            executedUnits++;
        }

        if (process.isFinished()) {
            // Si termina dentro de su turno, se registra como concluido de inmediato.
            simulationService.finishProcess(process, "Finalizado normalmente");
            state.logEvent("EVENT:END PID " + process.getPid());
            state.logEvent("EVENT:FINISH PID " + process.getPid());
        } else {
            // Si no termina, vuelve al final de la cola para que otros procesos tengan oportunidad.
            process.setState(ProcessState.READY);
            state.enqueueReadyProcess(process);
            state.logEvent("Round Robin devuelve PID " + process.getPid() + " al final de READY");
            state.logEvent("EVENT:END PID " + process.getPid());
            state.logEvent("EVENT:PREEMPT PID " + process.getPid());
        }

        simulationService.setLastDispatchedProcess(process);
        return process;
    }

    @Override
    public String getName() {
        return "Round Robin";
    }
}
