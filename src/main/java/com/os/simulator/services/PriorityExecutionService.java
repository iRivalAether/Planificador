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
        // El criterio dominante es la prioridad: el número más alto debe ejecutarse primero.
        SystemState state = simulationService.getSystemState();
        state.tryAdmitEligibleProcesses(); // Intentar admitir procesos que ya llegaron.

        // Si no hay procesos listos, avanzar el tiempo y retornar.
        if (state.getReadyQueue().isEmpty()) {
            state.logEvent("No hay procesos en cola de listos para Prioridad.");
            state.advanceTime();
            simulationService.clearLastDispatchedProcess();
            return null;
        }

        // Seleccionar el proceso según el scheduler (PriorityScheduler decide por prioridad).
        Process process = simulationService.getScheduler().selectNextProcess(state.getReadyQueue());
        if (process == null) {
            state.logEvent("Prioridad no retorno un proceso valido.");
            state.advanceTime();
            simulationService.clearLastDispatchedProcess();
            return null;
        }

        // Registrar cambio de contexto y marcar como RUNNING.
        simulationService.registerContextSwitch(process);
        process.setState(ProcessState.RUNNING);
        state.logEvent("Prioridad ejecuta PID " + process.getPid());
        state.logEvent("EVENT:DISPATCH PID " + process.getPid());
        state.logEvent("EVENT:START PID " + process.getPid());

        // Intento de asignar CPU; si falla, se deja en READY sin perder prioridad.
        if (!state.getResource().allocate(1, 0)) {
            state.logEvent("Prioridad no pudo asignar CPU al PID " + process.getPid());
            process.setState(ProcessState.READY);
            state.enqueueReadyProcess(process);
            state.advanceTime();
            simulationService.clearLastDispatchedProcess();
            return process;
        }

        // Registrar unidad de CPU para la interfaz de usuario.
        process.setAllocatedCpuUnits(process.getAllocatedCpuUnits() + 1);

        // Ejecutar una unidad de CPU: actualizar métricas y liberar recursos.
        process.executeOneUnit();
        simulationService.incrementExecutedCpuUnits(1);
        state.getResource().release(1, 0);
        process.setAllocatedCpuUnits(Math.max(0, process.getAllocatedCpuUnits() - 1));
        state.advanceTime();

        // Si terminó, finalizar correctamente; si no, devolver a READY manteniendo prioridad.
        if (process.isFinished()) {
            simulationService.finishProcess(process, "Finalizado normalmente");
            state.logEvent("EVENT:END PID " + process.getPid());
            state.logEvent("EVENT:FINISH PID " + process.getPid());
        } else {
            process.setState(ProcessState.READY);
            state.enqueueReadyProcess(process);
            state.logEvent("Prioridad devuelve PID " + process.getPid() + " a READY");
            state.logEvent("EVENT:END PID " + process.getPid());
            state.logEvent("EVENT:PREEMPT PID " + process.getPid());
        }

        simulationService.setLastDispatchedProcess(process);
        return process;
    }

    @Override
    public String getName() {
        return "Prioridad";
    }
}
