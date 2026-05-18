package com.os.simulator.services.synchronization;

import java.util.LinkedList;
import java.util.Queue;

/**
 * Representa un buffer de memoria compartida con capacidad configurable.
 */
public class SharedMemory {
    // Buffer interno que almacena cadenas representando los elementos escritos.
    private final Queue<String> buffer;
    // Capacidad máxima del buffer; al alcanzarla los productores deben esperar.
    private final int capacity;

    // Constructor por defecto con capacidad mínima de 1.
    public SharedMemory() {
        this(1);
    }

    // Constructor que permite configurar la capacidad del buffer.
    public SharedMemory(int capacity) {
        this.capacity = Math.max(1, capacity); // Garantiza al menos capacidad 1.
        this.buffer = new LinkedList<>();
    }

    /**
     * Intenta escribir un valor en el buffer.
     * Se sincroniza sobre el buffer para simular acceso concurrente seguro.
     *
     * @param pid escritor (solo para trazas y logs).
     * @param value valor a escribir.
     * @return true si se escribió, false si el buffer estaba lleno.
     */
    public boolean write(int pid, String value) {
        synchronized (buffer) {
            if (buffer.size() >= capacity) return false; // No hay espacio.
            buffer.add(value); // Añade al final de la cola.
            return true;
        }
    }

    /**
     * Intenta leer un valor del buffer de forma segura.
     *
     * @param pid lector (para trazas).
     * @return valor leído o null si el buffer está vacío.
     */
    public String read(int pid) {
        synchronized (buffer) {
            return buffer.poll(); // Extrae cabeza o null si vacía.
        }
    }

    // Devuelve la capacidad configurada del buffer.
    public int getCapacity() { return capacity; }

    // Devuelve el tamaño actual del buffer.
    public int getSize() { return buffer.size(); }

    /** Devuelve una representación legible del contenido del buffer para UI/depuración. */
    public String dumpContents() {
        synchronized (buffer) {
            if (buffer.isEmpty()) return "[]";
            return buffer.toString();
        }
    }
}
