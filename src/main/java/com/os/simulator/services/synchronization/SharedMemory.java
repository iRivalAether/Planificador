package com.os.simulator.services.synchronization;

/**
 * Representa un bloque de memoria compartida protegido por un mutex.
 */
public class SharedMemory {
    private final Mutex mutex;
    private String data;

    public SharedMemory() {
        this.mutex = new Mutex();
        this.data = "";
    }

    /**
     * Escribe informacion en la memoria compartida.
     *
     * @param pid proceso que realiza la escritura.
     * @param value valor a almacenar.
     * @return true si pudo escribir, false si el mutex estaba ocupado.
     */
    public boolean write(int pid, String value) {
        if (!mutex.tryLock(pid)) {
            return false;
        }

        data = value;
        mutex.unlock(pid);
        return true;
    }

    /**
     * Lee el contenido de la memoria compartida.
     *
     * @param pid proceso que realiza la lectura.
     * @return texto contenido en memoria o null si el mutex no pudo tomarse.
     */
    public String read(int pid) {
        if (!mutex.tryLock(pid)) {
            return null;
        }

        String currentData = data;
        mutex.unlock(pid);
        return currentData;
    }

    public Mutex getMutex() {
        return mutex;
    }
}
