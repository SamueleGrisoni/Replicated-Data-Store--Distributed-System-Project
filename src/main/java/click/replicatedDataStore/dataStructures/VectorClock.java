package click.replicatedDataStore.dataStructures;

import click.replicatedDataStore.applicationLayer.serverComponents.dataManager.VectorClockComparation;
import click.replicatedDataStore.utils.ClockTooFarAhead;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Objects;

public class VectorClock implements Comparable<VectorClock>, Serializable {
    protected final int[] clock;
    private final int serverIndex;
    private final String serverName;

    public VectorClock(String serverName, int serverNumber, int serverIndex) {
        this.serverName = serverName;
        if (serverIndex > serverNumber - 1) {
            throw new IllegalArgumentException("serverIndex is greater than the max amount of server");
        }
        this.clock = new int[serverNumber];
        this.serverIndex = serverIndex;
    }

    public VectorClock(VectorClock other) {
        this.clock = new int[other.clock.length];
        System.arraycopy(other.clock, 0, this.clock, 0, other.clock.length);
        this.serverIndex = other.serverIndex;
        this.serverName = other.serverName;
    }

    //Build a new vector clock, offsetting the incoming clock by the given offset. Offset is added to the server's own clock
    public VectorClock(VectorClock incomingClock, int offset) {
        this.serverName = incomingClock.serverName;
        this.clock = new int[incomingClock.clock.length];
        this.serverIndex = incomingClock.serverIndex;
        System.arraycopy(incomingClock.clock, 0, clock, 0, incomingClock.clock.length);
        clock[serverIndex] += offset;
    }

    //return a copy of the vector clock
    public int[] getClock() {
        int[] copy = new int[clock.length];
        System.arraycopy(clock, 0, copy, 0, clock.length);
        return copy;
    }

    public void incrementSelfClock() {
        clock[serverIndex]++;
    }

    public void updateClock(VectorClock incomingClock) throws ClockTooFarAhead {
        //System.out.println("Incoming clock: " + incomingClock);
        //System.out.println("Actual clock: " + this);
        try {
            checkIfUpdatable(serverIndex, this, incomingClock);
            for (int i = 0; i < clock.length; i++) {
                clock[i] = Math.max(clock[i], incomingClock.clock[i]);
            }
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        }
        //System.out.println("Updated clock: " + this);
    }

    //Compare the incoming clock with the server's clock. If the incoming clock is too far ahead, throw an exception
    public static void checkIfUpdatable(Integer serverIndex, VectorClock serverVectorClock, VectorClock incomingVectorClock) throws ClockTooFarAhead, IllegalArgumentException {
        if (incomingVectorClock == null || incomingVectorClock.clock == null) {
            throw new IllegalArgumentException("The incoming vector clock is null");
        }
        if (serverVectorClock.clock.length != incomingVectorClock.clock.length) {
            throw new IllegalArgumentException("Vector clocks must be of the same size");
        }

        int delta = 0;
        for (int i = 0; i < serverVectorClock.clock.length; i++) {
            if (serverVectorClock.clock[i] < incomingVectorClock.clock[i]) {
                delta = Math.max(delta, incomingVectorClock.clock[i] - serverVectorClock.clock[i]);
            }
            if (delta > 1) {
                System.out.println("Server's clock: " + serverVectorClock);
                System.out.println("Incoming clock: " + incomingVectorClock);
                throw new ClockTooFarAhead("Incoming clock is " + delta + " steps ahead");
            }
        }
    }

    @Override
    public int compareTo(VectorClock incomingClock) {
        if (incomingClock == null || incomingClock.clock == null) {
            throw new IllegalArgumentException("The incoming vector clock is null");
        }
        if (this.clock.length != incomingClock.clock.length) {
            throw new IllegalArgumentException("Vector clocks must be of the same size");
        }

        boolean thisGreater = false;
        boolean otherGreater = false;
        for (int i = 0; i < this.clock.length; i++) {
            if (this.clock[i] > incomingClock.clock[i]) {
                thisGreater = true;
            } else if (this.clock[i] < incomingClock.clock[i]) {
                otherGreater = true;
            }
        }
        if (thisGreater && otherGreater) {
            return VectorClockComparation.CONCURRENT.getCompareResult(); //if clocks are concurrent, this.clock is considered greater
        } else if (thisGreater) {
            return VectorClockComparation.GREATER_THAN.getCompareResult();
        } else if (otherGreater) {
            return VectorClockComparation.LESS_THAN.getCompareResult();
        } else { //equal clocks
            return VectorClockComparation.EQUAL.getCompareResult();
        }
    }

    @Override
    public boolean equals(Object object) {
        if (object == null || getClass() != object.getClass()) return false;
        VectorClock that = (VectorClock) object;
        return Objects.deepEquals(clock, that.clock);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(clock);
    }

    @Override
    public String toString() {
        return "VectorClock{" +
                "clock=" + Arrays.toString(clock) +
                ", serverName=" + serverName +
                '}';
    }
}
