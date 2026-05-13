package com.os.simulator.services;

import com.os.simulator.model.Process;
import com.os.simulator.model.ProcessState;
import com.os.simulator.model.SystemState;

/**
 * Ejecuta la logica del algoritmo por prioridad paso a paso.
 */
public class PriorityExecutionService implements AlgorithmExecutionService {
    @Override
    public Process executeStep(SimulationService simulationService) {
        // El criterio dominante es la prioridad: el numero mas alto debe ejecutarse primero.
        SystemState state = simulationService.getSystemState();
        state.tryAdmitEligibleProcesses();

        if (state.getReadyQueue().isEmpty()) {
            state.logEvent("No hay procesos en cola de listos para Prioridad.");
            state.advanceTime();
            simulationService.clearLastDispatchedProcess();
            return null;
        }

        Process process = simulationService.getScheduler().selectNextProcess(state.getReadyQueue());
        if (process == null) {
            state.logEvent("Prioridad no retorno un proceso valido.");
            state.advanceTime();
            simulationService.clearLastDispatchedProcess();
            return null;
        }

        simulationService.registerContextSwitch(process);
        process.setState(ProcessState.RUNNING);
        state.logEvent("Prioridad ejecuta PID " + process.getPid());

        if (!state.getResource().allocate(1, 0)) {
            // Si no hay CPU disponible, el proceso no pierde su prioridad ni su turno logico.
            state.logEvent("Prioridad no pudo asignar CPU al PID " + process.getPid());
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
            // Al terminar se cierra su ciclo de vida y libera los recursos reservados.
            simulationService.finishProcess(process, "Finalizado normalmente");
        } else {
            // Si sigue activo, conserva su prioridad para competir nuevamente en la siguiente seleccion.
            process.setState(ProcessState.READY);
            state.enqueueReadyProcess(process);
            state.logEvent("Prioridad devuelve PID " + process.getPid() + " a READY");
        }

        simulationService.setLastDispatchedProcess(process);
        return process;
    }

    @Override
    public String getName() {
        return "Prioridad";
    }
}
