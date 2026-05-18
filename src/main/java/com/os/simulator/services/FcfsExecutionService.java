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
        // FCFS favorece el orden de llegada, así que no reordena la cola.
        SystemState state = simulationService.getSystemState();
        state.tryAdmitEligibleProcesses(); // Intentar admitir procesos que ya llegaron.

        // Si no hay procesos listos, avanzamos el tiempo y no hacemos nada.
        if (state.getReadyQueue().isEmpty()) {
            state.logEvent("No hay procesos en cola de listos para FCFS.");
            state.advanceTime();
            simulationService.clearLastDispatchedProcess();
            return null;
        }

        // Selecciona el siguiente proceso según el scheduler (FCFS devuelve el primero).
        Process process = simulationService.getScheduler().selectNextProcess(state.getReadyQueue());
        if (process == null) {
            state.logEvent("FCFS no retorno un proceso valido.");
            state.advanceTime();
            simulationService.clearLastDispatchedProcess();
            return null;
        }

        // Registrar posible cambio de contexto y marcar RUNNING.
        simulationService.registerContextSwitch(process);
        process.setState(ProcessState.RUNNING);
        state.logEvent("FCFS ejecuta PID " + process.getPid());
        // Eventos estructurados para la UI (timeline/visualización).
        state.logEvent("EVENT:DISPATCH PID " + process.getPid());
        state.logEvent("EVENT:START PID " + process.getPid());

        // Intento de asignar 1 unidad de CPU; si no hay, devolver a READY.
        if (!state.getResource().allocate(1, 0)) {
            state.logEvent("FCFS no pudo asignar CPU al PID " + process.getPid());
            process.setState(ProcessState.READY);
            state.enqueueReadyProcess(process);
            state.advanceTime();
            simulationService.clearLastDispatchedProcess();
            return process;
        }

        // Registrar visibilidad de CPU asignada para la UI.
        process.setAllocatedCpuUnits(process.getAllocatedCpuUnits() + 1);

        // Ejecutar una unidad, actualizar métricas y liberar la CPU.
        process.executeOneUnit();
        simulationService.incrementExecutedCpuUnits(1);
        state.getResource().release(1, 0);
        process.setAllocatedCpuUnits(Math.max(0, process.getAllocatedCpuUnits() - 1));
        state.advanceTime();

        // Si terminó, finalizar y registrar eventos finales.
        if (process.isFinished()) {
            simulationService.finishProcess(process, "Finalizado normalmente");
            state.logEvent("EVENT:END PID " + process.getPid());
            state.logEvent("EVENT:FINISH PID " + process.getPid());
        } else {
            // Si no terminó, vuelve a READY y se registra el preempt.
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
