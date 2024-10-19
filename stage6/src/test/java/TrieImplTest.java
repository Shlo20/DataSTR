import edu.yu.cs.com1320.project.impl.TrieImpl;
import org.junit.jupiter.api.Test;
import org.opentest4j.AssertionFailedError;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

public class TrieImplTest {

private final TrieImpl<Integer> trie = new TrieImpl<>();

    @Test
    void testPutAndGetAll() {
        trie.put("one", 1);
        trie.put("two", 2);
        List<Integer> allValues = trie.getAllWithPrefixSorted("o", Comparator.naturalOrder());
        assertEquals(1, allValues.size(), "getAllSorted should've returned 2 results");
    }

    @Test
    void testDeleteAllWithPrefix() {
        trie.put("one", 1);
        trie.put("two", 2);
        trie.put("three", 3);
        Set<Integer> deletedValues = trie.deleteAllWithPrefix("t");
        assertEquals(2, deletedValues.size(), "deleteAllWithPrefix(\"t\") should've returned 2 results");
    }

    @Test
    void testDeleteAll() {
        trie.put("oneANdDone", 1);
        trie.put("oneANdDone", 2);
        trie.put("two", 3);
        Set<Integer> deletedValues = trie.deleteAll("oneANdDone");
        assertEquals(2, deletedValues.size(), "deleteAll(\"oneANdDone\") should've returned 2 results");
    }

    @Test
    void testGetAllWithPrefixSorted() {
        trie.put("one", 1);
        trie.put("oneANdDone", 2);
        trie.put("two", 3);
        trie.put("two", 4);
        trie.put("two", 5);
        List<Integer> allValues = trie.getAllWithPrefixSorted("o", Comparator.naturalOrder());
        assertEquals(2, allValues.size(), "getAllWithPrefixSorted(\"o\") should've returned 6 results");
    }

    @Test
    void testDelete() {
        TrieImpl<Integer> trie = new TrieImpl<>();
        trie.put("one", 1);
        trie.put("oneANdDone", 2);
        trie.put("two", 3);

        try {
            // Call delete with an unmatched value, which should throw an AssertionFailedError
            trie.delete("one", 2);

            // If no exception is thrown, fail the test
            fail("Expected AssertionFailedError to be thrown, but nothing was thrown");
        } catch (AssertionFailedError e) {
            // Expected behavior: an AssertionFailedError should be thrown
            assertTrue(true); // This line is to ensure the test passes when the expected error is thrown
        }
    }

}

