import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

public class CalcShortestPathWhiteBoxTest {
    private Method calcShortestPath;
    private Method parseMethod;

    private String invoke(String w1, String w2) throws Exception {
        return (String) calcShortestPath.invoke(null, w1, w2);
    }

    private void prepareGraph(String content) throws Exception {
        Path tempFile = Files.createTempFile("graph_input_" + UUID.randomUUID(), ".txt");
        Files.writeString(tempFile, content);
        parseMethod.invoke(null, tempFile.toString());
    }

    @BeforeEach
    public void setup() throws Exception {
        calcShortestPath = Lab1FX.class.getDeclaredMethod("calcShortestPath", String.class, String.class);
        calcShortestPath.setAccessible(true);

        parseMethod = Lab1FX.class.getDeclaredMethod("parseTextToGraph", String.class);
        parseMethod.setAccessible(true);
    }

    // Path 1: word1 not in graph
    @Test
    public void test_P1_word1NotInGraph() throws Exception {
        prepareGraph("a b b c");
        String result = invoke("x", "c");
        assertEquals("No \"x\" in the graph!", result);
    }

    // Path 2: word2 not in graph
    @Test
    public void test_P2_word2NotInGraph() throws Exception {
        prepareGraph("a b b c");
        String result = invoke("a", "x");
        assertEquals("No \"x\" in the graph!", result);
    }

    // Path 3: shortest path exists
    @Test
    public void test_P3_shortestPathExists() throws Exception {
        prepareGraph("a b b c c d");
        String result = invoke("a", "d");
        assertTrue(result.contains("Shortest path: a -> b -> c -> d"));
        assertTrue(result.contains("Length: 4"));
    }

    // Path 4: word2 is null or empty -> show all paths
    @Test
    public void test_P4_word2Null() throws Exception {
        prepareGraph("a b b c c d");
        String result = invoke("a", null);
        assertTrue(result.contains("Shortest path to b: a -> b"));
        assertTrue(result.contains("Shortest path to c: a -> b -> c"));
        assertTrue(result.contains("Shortest path to d: a -> b -> c -> d"));
    }

    @Test
    public void test_P4_word2Empty() throws Exception {
        prepareGraph("a b b c");
        String result = invoke("a", "");
        assertTrue(result.contains("Shortest path to b: a -> b"));
        assertTrue(result.contains("Shortest path to c: a -> b -> c"));
    }
    // Path 5: self-loop or trivial path
    @Test
    public void test_P5_trivialPath() throws Exception {
        prepareGraph("a a a b");
        String result = invoke("a", "a");
        assertTrue(result.contains("Shortest path: a"));
    }
}
