package com.os.simulator.services.synchronization;

import java.util.ArrayList;
import java.util.List;

/**
 * Snapshot legible del estado de sincronizacion/comunicacion para mostrar en la UI.
 */
public class SynchronizationSnapshot {
    private final String sharedValue;
    private final boolean mutexLocked;
    private final Integer mutexOwnerPid;
    private final List<Integer> mutexWaitingPids;
    private final int emptySlotsValue;
    private final List<Integer> emptySlotsWaitingPids;
    private final int filledSlotsValue;
    private final List<Integer> filledSlotsWaitingPids;

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