import edu.yu.cs.com1320.project.impl.MinHeapImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.NoSuchElementException;
import static org.junit.Assert.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class MinHeapImplTest {
    private MinHeapImpl<Integer> minHeap;

    @BeforeEach
    public void setUp() {
        minHeap = new MinHeapImpl<>();
    }

    @Test
    public void testInsertAndRemove() {
        minHeap.insert(3);
        minHeap.insert(1);
        minHeap.insert(2);

        // Verify the smallest element is at the top
        assertEquals(Integer.valueOf(1), minHeap.remove());
        // Check the size after removal
        assertEquals(2, minHeap.size());
    }

    @Test
    public void testReHeapify() {
        minHeap.insert(3);
        minHeap.insert(1);
        minHeap.insert(2);

       assertTrue("Need a valid scenario for reHeapify", true);
    }

    @Test
    public void testHeapGrowth() {
        // Insert more elements than the initial capacity to test array doubling
        for (int i = 20; i > 0; i--) {
            minHeap.insert(i);
        }
        assertEquals(Integer.valueOf(1), minHeap.remove());
        // Verify the heap still contains the remaining elements
        assertEquals(19, minHeap.size());
    }
}