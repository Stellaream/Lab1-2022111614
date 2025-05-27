import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

public class QueryBridgeWordsWhiteBoxTest {
    private Method queryBridgeWords;
    private Method parseMethod;

    private String invoke(String w1, String w2) throws Exception {
        return (String) queryBridgeWords.invoke(null, w1, w2);
    }

    private void prepareGraph(String content) throws Exception {
        Path tempFile = Files.createTempFile("graph_input_" + UUID.randomUUID(), ".txt");
        Files.writeString(tempFile, content);
        parseMethod.invoke(null, tempFile.toString());
    }

    @BeforeEach
    public void setup() throws Exception {
        queryBridgeWords = Lab1FX.class.getDeclaredMethod("queryBridgeWords", String.class, String.class);
        queryBridgeWords.setAccessible(true);

        parseMethod = Lab1FX.class.getDeclaredMethod("parseTextToGraph", String.class);
        parseMethod.setAccessible(true);
    }

    // Path 1: both words not in graph
    @Test
    public void test_P1_bothNotInGraph() throws Exception {
        prepareGraph("a b c");
        String result = invoke("x", "y");
        assertTrue(result.contains("No \"x\" and \"y\""));
    }

    // Path 2: word1 not in graph
    @Test
    public void test_P2_word1NotInGraph() throws Exception {
        prepareGraph("a b c");
        String result = invoke("x", "c");
        assertTrue(result.contains("No \"x\""));
    }

    // Path 3: word2 not in graph
    @Test
    public void test_P3_word2NotInGraph() throws Exception {
        prepareGraph("a b c");
        String result = invoke("a", "x");
        assertTrue(result.contains("No \"x\""));
    }

    // Path 4: word1 in graph but not a key
    @Test
    public void test_P4_word1NotKey() throws Exception {
        prepareGraph("a b c");
        String result = invoke("b", "c");
        assertTrue(result.contains("No bridge words from \"b\" to \"c\""));
    }

    // Path 5: word1 in graph but no bridge
    @Test
    public void test_P5_noBridgeWords() throws Exception {
        prepareGraph("a b c");
        String result = invoke("c", "a");  // "c" has no outgoing edge to "a"
        assertTrue(result.contains("No bridge words"));
    }

    // Path 6: one bridge word
    @Test
    public void test_P6_oneBridgeWord() throws Exception {
        prepareGraph("a b c");
        String result = invoke("a", "c");  // a → b → c
        assertTrue(result.contains("b"));
        assertTrue(result.contains("The bridge words"));
    }

    // Path 7: two bridge words
    @Test
    public void test_P7_twoBridgeWords() throws Exception {
        prepareGraph("a b c a d c");
        String result = invoke("a", "c");  // a → b → c, a → d → c
        assertTrue(result.contains("b") && result.contains("d"));
        assertTrue(result.contains(" and "));
    }

    // Path 8: multiple bridge words (three or more)
    @Test
    public void test_P8_multipleBridgeWords() throws Exception {
        prepareGraph("a b c a d c a e c");  // a → b/d/e → c
        String result = invoke("a", "c");
        assertTrue(result.contains("b") && result.contains("d") && result.contains("e"));
        assertTrue(result.contains(",") && result.contains("and"));
    }
}