package click.replicatedDataStore.dataStructures;
import java.util.Objects;

public class VectorClock implements Comparable<VectorClock> {
    private final int[] clock;
    private final int serverID;
    public VectorClock(int serverNumber, int serverID) {
        if(serverID > serverNumber-1){
            throw new IllegalArgumentException("serverID is greater than the max amount of server");
        }
        this.clock = new int[serverNumber];
        this.serverID = serverID;
    }

    //Build a new vector clock, offsetting the incoming clock by the given offset. Offset is added to the server's own clock
    public VectorClock(VectorClock incomingClock, int offset){
        this.clock = new int[incomingClock.clock.length];
        this.serverID = incomingClock.serverID;
        System.arraycopy(incomingClock.clock, 0, clock, 0, incomingClock.clock.length);
        clock[serverID] += offset;
    }

    //return a copy of the vector clock
    public int[] getClock() {
        int[] copy = new int[clock.length];
        System.arraycopy(clock, 0, copy, 0, clock.length);
        return copy;
    }

    public void incrementSelfClock(){
        clock[serverID]++;
    }

    public void updateClock(VectorClock incomingClock) throws IllegalCallerException{
        checkIfUpdatable(incomingClock);
        for(int i = 0; i < clock.length; i++){
            clock[i] = Math.max(clock[i], incomingClock.clock[i]);
        }
    }

    private void checkIfUpdatable(VectorClock incomingClock) {
        if (incomingClock == null || incomingClock.clock == null) {
            throw new IllegalArgumentException("The incoming vector clock is null");
        }
        if (this.clock.length != incomingClock.clock.length) {
            throw new IllegalArgumentException("Vector clocks must be of the same size");
        }

        int count = 0;
        for (int i = 0; i < clock.length; i++) {
            if (clock[i] < incomingClock.clock[i]) {
                count = incomingClock.clock[i] - clock[i];
            }
            if (count > 1) {
                throw new IllegalCallerException("Incoming clock is too far ahead");
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
        if(thisGreater && otherGreater){
            return 1; //if clocks are concurrent, this.clock is considered greater
        } else if(thisGreater){
            return 1;
        } else if(otherGreater){
            return -1;
        } else { //equal clocks
            return 0;
        }
    }

    @Override
    public boolean equals(Object object) {
        if (object == null || getClass() != object.getClass()) return false;
        VectorClock that = (VectorClock) object;
        return Objects.deepEquals(clock, that.clock);
    }
}
