package com.os.simulator.services.synchronization;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

public class ProducerConsumerSimulationTest {

    @Test
    public void produceThenConsume_shouldTransferValue() {
        ProducerConsumerSimulation sim = new ProducerConsumerSimulation();
        assertTrue(sim.produce(1, "hello"));
        String v = sim.consume(2);
        assertEquals("hello", v);
    }

    @Test
    public void consumeWithoutProduce_returnsNull() {
        ProducerConsumerSimulation sim = new ProducerConsumerSimulation();
        // no produce called, consume should block/return null because filledSlots=0
        assertNull(sim.consume(5));
    }
}
