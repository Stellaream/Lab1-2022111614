import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.lang.reflect.Method;
import java.lang.reflect.Field;
import java.util.Map;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

public class QueryBridgeWordsBlackBoxTest {
    private Method queryBridgeWordsMethod;
    private Method parseMethod;

    @BeforeEach
    public void setup() throws Exception {
        queryBridgeWordsMethod = Lab1FX.class.getDeclaredMethod("queryBridgeWords", String.class, String.class);
        queryBridgeWordsMethod.setAccessible(true);
        parseMethod = Lab1FX.class.getDeclaredMethod("parseTextToGraph", String.class);
        parseMethod.setAccessible(true);

        // 清空静态 graph 字段
        Field graphField = Lab1FX.class.getDeclaredField("graph");
        graphField.setAccessible(true);
        Map<String, Map<String, Integer>> graph = (Map<String, Map<String, Integer>>) graphField.get(null);
        graph.clear();
    }

    private String invoke(String w1, String w2) throws Exception {
        return (String) queryBridgeWordsMethod.invoke(null, w1, w2);
    }

    private void prepareGraph(String content) throws Exception {
        Path tempFile = Files.createTempFile("graph_input_" + UUID.randomUUID(), ".txt");
        Files.writeString(tempFile, content);
        parseMethod.invoke(null, tempFile.toString());
    }

    @Test
    public void testBothWordsNotInGraph() throws Exception {
        prepareGraph("a b c");
        String result = invoke("x", "y");
        assertEquals("No \"x\" and \"y\" in the graph!", result);
    }

    @Test
    public void testFirstWordNotInGraph() throws Exception {
        prepareGraph("a b c");
        String result = invoke("x", "c");
        assertEquals("No \"x\" in the graph!", result);
    }

    @Test
    public void testSecondWordNotInGraph() throws Exception {
        prepareGraph("a b c");
        String result = invoke("a", "x");
        assertEquals("No \"x\" in the graph!", result);
    }

    @Test
    public void testWord1NotKey() throws Exception {
        prepareGraph("a b c");
        String result = invoke("b", "c");
        assertEquals("No bridge words from \"b\" to \"c\"!", result);
    }

    @Test
    public void testNoBridgeWordsFound() throws Exception {
        prepareGraph("a b c");
        String result = invoke("c", "a");
        assertEquals("No bridge words from \"c\" to \"a\"!", result);
    }

    @Test
    public void testOneBridgeWordFound() throws Exception {
        prepareGraph("a b c");
        String result = invoke("a", "c");
        assertEquals("The bridge words from \"a\" to \"c\" are: b.", result);
    }

    @Test
    public void testTwoBridgeWordsFound() throws Exception {
        prepareGraph("a b c a d c");
        String result = invoke("a", "c");
        assertEquals("The bridge words from \"a\" to \"c\" are: b and d.", result);
    }

    @Test
    public void testMultipleBridgeWordsFound() throws Exception {
        prepareGraph("a b c a d c a e c");
        String result = invoke("a", "c");
        assertTrue(result.contains("b") && result.contains("d") && result.contains("e"));
        assertTrue(result.contains(",") && result.contains("and"));
    }

    @Test
    public void testEmptyStringInput() throws Exception {
        prepareGraph("a b c");
        String result = invoke("", "c");
        assertTrue(result.contains("No \"\" in the graph!") || result.contains("No bridge words"));
    }

    @Test
    public void testEmptyGraph() throws Exception {
        prepareGraph("");
        String result = invoke("a", "c");
        assertEquals("No \"a\" and \"c\" in the graph!", result);
    }

    @Test
    public void testCaseSensitivity() throws Exception {
        prepareGraph("a b c");
        String result = invoke("A", "C");
        assertEquals("No \"A\" and \"C\" in the graph!", result);
    }
}