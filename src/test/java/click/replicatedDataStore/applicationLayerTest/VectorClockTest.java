package click.replicatedDataStore.applicationLayerTest;

import click.replicatedDataStore.dataStructures.VectorClock;
import org.junit.Test;

import java.util.PriorityQueue;

import static org.junit.Assert.*;

public class VectorClockTest {
    @Test
    public void testConstructorAndGetClock() {
        VectorClock vc = new VectorClock(3, 1);
        int[] clock = vc.getClock();
        assertEquals(3, clock.length);
        for (int value : clock) {
            assertEquals(0, value);
        }
    }

    @Test
    public void testIncrementSelfClock() {
        VectorClock vc = new VectorClock(3, 1);
        vc.incrementSelfClock();  // vc: [0, 1, 0]
        int[] clock = vc.getClock();
        assertEquals(0, clock[0]);
        assertEquals(1, clock[1]);
        assertEquals(0, clock[2]);

        // Increment the same index again
        vc.incrementSelfClock();
        clock = vc.getClock();
        assertEquals(2, clock[1]);
    }

    @Test
    public void testSecondaryConstructor() {
        VectorClock vc1 = new VectorClock(3, 1);
        vc1.incrementSelfClock();  // vc1: [0, 1, 0]
        // Build a new clock from vc1 incrementing index 2 by 1
        VectorClock vc2 = new VectorClock(vc1, 1); // expected vc2: [0, 2, 0]

        int[] clock1 = vc1.getClock();
        int[] clock2 = vc2.getClock();
        // Ensure vc1 remains unchanged
        assertEquals(0, clock1[0]);
        assertEquals(1, clock1[1]);
        assertEquals(0, clock1[2]);

        // Validate vc2's updated clock
        assertEquals(0, clock2[0]);
        assertEquals(2, clock2[1]);
        assertEquals(0, clock2[2]);
    }

    @Test
    public void testUpdateLegalClock() {
        VectorClock vc1 = new VectorClock(3, 0);
        vc1.incrementSelfClock();  // vc1: [1, 0, 0]

        VectorClock vc2 = new VectorClock(3, 1);
        vc2.incrementSelfClock();  // vc2: [0, 1, 0]

        // Update vc1 with vc2; each index should become the max of the two clocks
        vc1.updateClock(vc2);
        int[] updatedClock = vc1.getClock();
        assertEquals(1, updatedClock[0]);
        assertEquals(1, updatedClock[1]);
        assertEquals(0, updatedClock[2]);
    }

    @Test
    public void testUpdateClockTooFarAhead() {
        VectorClock vc1 = new VectorClock(3, 0);
        vc1.incrementSelfClock();  // vc1: [1, 0, 0]

        VectorClock vc2 = new VectorClock(3, 1);
        vc2.incrementSelfClock();  // vc2: [0, 1, 0]
        vc2.incrementSelfClock();  // vc2: [0, 2, 0]

        IllegalCallerException exception = assertThrows(IllegalCallerException.class, () -> {
            // vc2 is too far ahead of vc1, vc1 should not be updated
            vc1.updateClock(vc2);
        });
        assertTrue(exception.getMessage().contains("Incoming clock is too far ahead"));
        // Ensure vc1 remains unchanged
        int[] clock = vc1.getClock();
        assertEquals(1, clock[0]);
        assertEquals(0, clock[1]);
        assertEquals(0, clock[2]);
    }

    @Test
    public void testCompareTo() {
        // Test equal clocks
        VectorClock vc1 = new VectorClock(3, 0);
        VectorClock vc2 = new VectorClock(3, 0);
        assertEquals(0, vc1.compareTo(vc2));

        // Test when one clock is less than the other
        vc2.incrementSelfClock();  // vc2: [1,0,0]
        //vc2 is greater than vc1
        assertEquals(-1, vc1.compareTo(vc2));
        //vc1 is smaller than vc2
        assertEquals(1, vc2.compareTo(vc1));

        // vc2: [1,0,0] and vc3: [0,1,0] are concurrent
        VectorClock vc3 = new VectorClock(3, 1);
        vc3.incrementSelfClock();

        // According to the implementation, if clocks are concurrent the first clock is considered greater
        assertEquals(1, vc2.compareTo(vc3));
    }

    @Test 
    public void PriorityQueue(){
        VectorClock vc1 = new VectorClock(3, 0);
        VectorClock vc2 = new VectorClock(3, 1);
        VectorClock vc3 = new VectorClock(3, 2);
        vc2.incrementSelfClock();
        vc3.updateClock(vc2);
        vc2.incrementSelfClock(); //vc2: [0, 2, 0]
        vc3.updateClock(vc2);
        vc3.incrementSelfClock(); //vc3: [0, 2, 1]

        PriorityQueue<VectorClock> pq = new PriorityQueue<VectorClock>();
        pq.add(vc2);
        pq.add(vc1);
        pq.add(vc3);

        //Given the compareTo, the order should be vc1, vc2, vc3
        assertEquals(vc1, pq.poll());
        assertEquals(vc2, pq.poll());
        assertEquals(vc3, pq.poll());
    }

    @Test
    public void testEquals() {
        VectorClock vc1 = new VectorClock(3, 0);
        VectorClock vc2 = new VectorClock(3, 0);
        assertEquals(vc1, vc2);

        vc1.incrementSelfClock(); // vc1: [1, 0, 0]
        assertNotEquals(vc1, vc2);

        vc2.incrementSelfClock(); // vc2: [0, 1, 0]
        assertEquals(vc1, vc2);

        VectorClock vc3 = new VectorClock(4, 0);
        vc3.incrementSelfClock(); // vc3: [1, 0, 0, 0]
        assertNotEquals(vc1, vc3); // Different size should not be equal
    }

    @Test
    public void testGetClockImmutability() {
        VectorClock vc = new VectorClock(3, 0);
        int[] returnedClock = vc.getClock();
        returnedClock[0] = 100;  // Modify the returned copy
        int[] internalClock = vc.getClock();
        //Modifying the returned clock should not affect internal state
        assertNotEquals(returnedClock[0], internalClock[0]);
    }
}
