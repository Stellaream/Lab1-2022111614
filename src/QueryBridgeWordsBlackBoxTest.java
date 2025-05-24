import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;

public class QueryBridgeWordsBlackBoxTest {
    private static Method queryBridgeWordsMethod;
    @BeforeAll
    public static void setup() throws Exception {
        // 构造测试图：a → b → c, a → d → c, x 不存在
        String content = "a b c a d c";
        Path tempFile = Files.createTempFile("graph_input", ".txt");
        Files.writeString(tempFile, content);

        Method parseMethod = Lab1FX.class.getDeclaredMethod("parseTextToGraph", String.class);
        parseMethod.setAccessible(true);
        parseMethod.invoke(null, tempFile.toString());

        queryBridgeWordsMethod = Lab1FX.class.getDeclaredMethod("queryBridgeWords", String.class, String.class);
        queryBridgeWordsMethod.setAccessible(true);
    }

    private String invoke(String w1, String w2) throws Exception {
        return (String) queryBridgeWordsMethod.invoke(null, w1, w2);
    }

    @Test
    public void testBothWordsNotInGraph() throws Exception {
        String result = invoke("x", "y");
        assertTrue(result.contains("No \"x\" and \"y\" in the graph!"));
    }

    @Test
    public void testFirstWordNotInGraph() throws Exception {
        String result = invoke("x", "c");
        assertTrue(result.contains("No \"x\" in the graph!"));
    }

    @Test
    public void testSecondWordNotInGraph() throws Exception {
        String result = invoke("a", "x");
        assertTrue(result.contains("No \"x\" in the graph!"));
    }

    @Test
    public void testNoBridgeWordsFound() throws Exception {
        String result = invoke("c", "a");
        assertTrue(result.contains("No bridge words"));
    }

    @Test
    public void testOneBridgeWordFound() throws Exception {
        // 构造图：a → b → c
        String content = "a b c";
        Path tempFile = Files.createTempFile("single_bridge", ".txt");
        Files.writeString(tempFile, content);

        Method parseMethod = Lab1FX.class.getDeclaredMethod("parseTextToGraph", String.class);
        parseMethod.setAccessible(true);
        parseMethod.invoke(null, tempFile.toString());

        String result = invoke("a", "c");
        assertTrue(result.contains("b"), "应能找到桥接词 b");
    }

    @Test
    public void testMultipleBridgeWordsFound() throws Exception {
        String result = invoke("a", "c");
        assertTrue(result.contains("b") && result.contains("d"));
        assertTrue(result.contains("and") || result.contains(","), "多个桥接词应以 and 或 , 分隔");
    }
}
