package com.os.simulator.services;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

public class SimulationServiceProducerConsumerStressTest {

    @Test
    public void runScenario_bufferSizeTwo_noDeadlockDetected() {
        SimulationService svc = new SimulationService();
        svc.resetProducerConsumerSimulation(2);
        svc.runProducerConsumerScenario(3, 3, 5);
        boolean deadlock = svc.getSystemState().getEventLog().stream().anyMatch(s -> s != null && s.contains("DEADLOCK"));
        assertFalse(deadlock, "No debe detectarse deadlock en escenario con buffer=2 y suficientes ciclos");
    }
}
