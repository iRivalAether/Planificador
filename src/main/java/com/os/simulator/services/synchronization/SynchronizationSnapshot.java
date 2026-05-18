package com.os.simulator.services.synchronization;

import java.util.ArrayList;
import java.util.List;

/**
 * Snapshot legible del estado de sincronizacion/comunicacion para mostrar en la UI.
 */
public class SynchronizationSnapshot {
    // Valor visible en memoria compartida (representación legible del buffer).
    private final String sharedValue;
    // Estado del mutex y pid del propietario (si existe).
    private final boolean mutexLocked;
    private final Integer mutexOwnerPid;
    // Listas de espera copiadas para evitar acceso concurrente a estructuras internas.
    private final List<Integer> mutexWaitingPids;
    private final int emptySlotsValue;
    private final List<Integer> emptySlotsWaitingPids;
    private final int filledSlotsValue;
    private final List<Integer> filledSlotsWaitingPids;

    // Constructor que clona las colecciones entrantes para mantener inmutabilidad del snapshot.
    public SynchronizationSnapshot(
            String sharedValue,
            boolean mutexLocked,
            Integer mutexOwnerPid,
            List<Integer> mutexWaitingPids,
            int emptySlotsValue,
            List<Integer> emptySlotsWaitingPids,
            int filledSlotsValue,
            List<Integer> filledSlotsWaitingPids) {
        this.sharedValue = sharedValue;
        this.mutexLocked = mutexLocked;
        this.mutexOwnerPid = mutexOwnerPid;
        this.mutexWaitingPids = new ArrayList<>(mutexWaitingPids);
        this.emptySlotsValue = emptySlotsValue;
        this.emptySlotsWaitingPids = new ArrayList<>(emptySlotsWaitingPids);
        this.filledSlotsValue = filledSlotsValue;
        this.filledSlotsWaitingPids = new ArrayList<>(filledSlotsWaitingPids);
    }

    // Getters que devuelven copias cuando es apropiado para mantener inmutabilidad externa.
    public String getSharedValue() {
        return sharedValue;
    }

    public boolean isMutexLocked() {
        return mutexLocked;
    }

    public Integer getMutexOwnerPid() {
        return mutexOwnerPid;
    }

    public List<Integer> getMutexWaitingPids() {
        return new ArrayList<>(mutexWaitingPids);
    }

    public int getEmptySlotsValue() {
        return emptySlotsValue;
    }

    public List<Integer> getEmptySlotsWaitingPids() {
        return new ArrayList<>(emptySlotsWaitingPids);
    }

    public int getFilledSlotsValue() {
        return filledSlotsValue;
    }

    public List<Integer> getFilledSlotsWaitingPids() {
        return new ArrayList<>(filledSlotsWaitingPids);
    }
}