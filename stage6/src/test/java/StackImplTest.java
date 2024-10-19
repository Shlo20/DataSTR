import edu.yu.cs.com1320.project.impl.StackImpl;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class StackImplTest {
    @Test
    public void testPopOnEmptyStack() {
        StackImpl<String> stack = new StackImpl<>();

        // Check that popping from an empty stack returns null
        assertNull(stack.pop());
    }
}
