package com.os.simulator.services.synchronization;

import java.util.LinkedList;
import java.util.Queue;

/**
 * Representa un buffer de memoria compartida con capacidad configurable.
 */
public class SharedMemory {
    private final Queue<String> buffer;
    private final int capacity;

    public SharedMemory() {
        this(1);
    }

    public SharedMemory(int capacity) {
        this.capacity = Math.max(1, capacity);
        this.buffer = new LinkedList<>();
    }

    /**
     * Intenta escribir un valor en el buffer.
     * @param pid escritor (solo para traza)
     * @param value valor a escribir
     * @return true si se escribio, false si el buffer esta lleno
     */
    public boolean write(int pid, String value) {
        synchronized (buffer) {
            if (buffer.size() >= capacity) return false;
            buffer.add(value);
            return true;
        }
    }

    /**
     * Intenta leer un valor del buffer.
     * @param pid lector (solo para traza)
     * @return valor leido o null si esta vacio
     */
    public String read(int pid) {
        synchronized (buffer) {
            return buffer.poll();
        }
    }

    public int getCapacity() { return capacity; }

    public int getSize() { return buffer.size(); }

    /** Devuelve una representacion legible del contenido del buffer. */
    public String dumpContents() {
        synchronized (buffer) {
            if (buffer.isEmpty()) return "[]";
            return buffer.toString();
        }
    }
}
