package com.os.simulator.services;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

public class SimulationServiceIntegrationTest {

    @Test
    public void produceConsume_viaSimulationService() {
        SimulationService s = new SimulationService();
        assertTrue(s.produceInSharedMemory(10, "msg"));
        String val = s.consumeFromSharedMemory(11);
        assertEquals("msg", val);
        // event log should contain entries about producer and consumer
        assertTrue(s.getSystemState().getEventLog().stream().anyMatch(l -> l.contains("escribio en memoria compartida")));
        assertTrue(s.getSystemState().getEventLog().stream().anyMatch(l -> l.contains("leyo desde memoria compartida")));
    }
}
