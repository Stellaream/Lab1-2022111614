import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Random;
import java.util.Set;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Accordion;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.stage.FileChooser;
import javafx.stage.Stage;


public class Lab1FX extends Application {
    private static final Map<String, Map<String, Integer>> graph = new HashMap<>();
    private static final Random random = new Random();
    private TextArea outputArea;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        File file = chooseFile(primaryStage);
        if (file == null) {
            showAlertAndExit();
            return;
        }

        parseTextToGraph(file.getAbsolutePath());

        primaryStage.setTitle("TextFlow - Intelligent Text Graph Analyzer");
        outputArea = new TextArea();
        outputArea.setWrapText(true);
        outputArea.setPrefHeight(300);
        outputArea.setFont(Font.font("Consolas", 14));

        Accordion accordion = new Accordion();
        accordion.getPanes().addAll(
                createTitledPane("1. 展示图", createButton("展示图", this::handleShowGraph)),
                createTitledPane("2. 查询桥接词", createBridgeQueryUI()),
                createTitledPane("3. 生成新文本", createTextGenUI()),
                createTitledPane("4. 最短路径", createShortestPathUI()),
                createTitledPane("5. PageRank", createPageRankUI()),
                createTitledPane("6. 随机游走", createButton("随机游走", this::handleRandomWalk))
        );

        VBox layout = new VBox(15, accordion, new Label("输出结果："), outputArea);
        layout.setPadding(new Insets(15));

        Scene scene = new Scene(layout, 700, 600);
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private File chooseFile(Stage stage) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("选择输入文本文件");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Text Files", "*.txt"));
        return chooser.showOpenDialog(stage);
    }

    private void showAlertAndExit() {
        Alert alert = new Alert(Alert.AlertType.ERROR, "必须选择一个文本文件！", ButtonType.OK);
        alert.setHeaderText("错误");
        alert.showAndWait();
        System.exit(0);
    }

    private Button createButton(String text, Runnable action) {
        Button btn = new Button(text);
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setOnAction(e -> action.run());
        return btn;
    }

    private TitledPane createTitledPane(String title, Node content) {
        VBox box = new VBox(content);
        box.setPadding(new Insets(10));
        return new TitledPane(title, box);
    }

    private HBox createBridgeQueryUI() {
        TextField w1 = new TextField();
        w1.setPromptText("word1");
        TextField w2 = new TextField();
        w2.setPromptText("word2");
        Button queryBtn = new Button("查询");
        queryBtn.setOnAction(e ->
                outputArea.setText(queryBridgeWords(w1.getText().toLowerCase(), w2.getText().toLowerCase()))
        );
        return new HBox(10, new Label("word1:"), w1, new Label("word2:"), w2, queryBtn);
    }

    private HBox createTextGenUI() {
        TextField input = new TextField();
        input.setPromptText("输入一句话");
        Button genBtn = new Button("生成");
        genBtn.setOnAction(e ->
                outputArea.setText(generateNewText(input.getText()))
        );
        return new HBox(10, new Label("输入:"), input, genBtn);
    }

    private HBox createShortestPathUI() {
        TextField from = new TextField();
        from.setPromptText("起点");
        TextField to = new TextField();
        to.setPromptText("终点");
        Button pathBtn = new Button("计算");
        pathBtn.setOnAction(e ->
                outputArea.setText(calcShortestPath(from.getText().toLowerCase(), to.getText().toLowerCase()))
        );
        return new HBox(10, new Label("From:"), from, new Label("To:"), to, pathBtn);
    }

    private HBox createPageRankUI() {
        TextField dInput = new TextField("0.85");
        Button prBtn = new Button("计算");
        prBtn.setOnAction(e -> {
            try {
                double d = Double.parseDouble(dInput.getText());
                if (d < 0 || d > 1) {
                    throw new NumberFormatException();
                }
                var pr = calPageRank(d);
                StringBuilder sb = new StringBuilder("PageRank 结果：\n");
                pr.entrySet().stream()
                        .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
                        .forEach(e2 -> sb.append(String.format("%-15s: %.6f%n", e2.getKey(), e2.getValue())));
                outputArea.setText(sb.toString());
            } catch (NumberFormatException ex) {
                outputArea.setText("阻尼因子必须是 0 到 1 之间的小数！");
            }
        });
        return new HBox(10, new Label("阻尼因子:"), dInput, prBtn);
    }

    private void handleShowGraph() {
        StringBuilder dot = new StringBuilder("digraph G {\n");
        for (Map.Entry<String, Map<String, Integer>> entry : graph.entrySet()) {
            String from = entry.getKey();
            Map<String, Integer> neighbors = entry.getValue();
            for (Map.Entry<String, Integer> toEntry : neighbors.entrySet()) {
                String to = toEntry.getKey();
                int weight = toEntry.getValue();
                dot.append(String.format("  \"%s\" -> \"%s\" [label=\"%d\"];%n", from, to, weight));
            }
        }
        dot.append("}");

        try {
            Files.writeString(Path.of("graph.dot"), dot.toString());
            new ProcessBuilder("dot", "-Tpng", "graph.dot", "-o", "graph.png").start().waitFor();
            Desktop.getDesktop().open(new File("graph.png"));
            outputArea.setText("图生成成功，已打开 graph.png");
        } catch (Exception e) {
            outputArea.setText("图生成失败：" + e.getMessage());
        }
    }

    private void handleRandomWalk() {
        String result = "随机游走路径：\n" + randomWalk();
        outputArea.setText(result);

        String timestamp = java.time.LocalDateTime.now()
                .format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String filename = "random_walk_" + timestamp + ".txt";

        try {
            Files.writeString(Path.of(filename), result);
        } catch (IOException e) {
            outputArea.appendText("\n写入 " + filename + " 失败：" + e.getMessage());
        }
    }

    private static void parseTextToGraph(String filePath) throws IOException {
        String content = Files.readString(Path.of(filePath)).replaceAll("[^a-zA-Z\\s]", " ").toLowerCase();
        String[] words = content.trim().split("\\s+");
        for (int i = 0; i < words.length - 1; i++) {
            graph.putIfAbsent(words[i], new HashMap<>());
            graph.get(words[i]).merge(words[i + 1], 1, Integer::sum);
        }
        if (words.length > 0) {
            graph.putIfAbsent(words[words.length - 1], new HashMap<>());
        }
    }

    private static boolean inGraph(String word) {
        if (graph.containsKey(word)) {
            return true;
        }
        return graph.values().stream().anyMatch(m -> m.containsKey(word));
    }

    private static String queryBridgeWords(String word1, String word2) {
        if (!inGraph(word1) && !inGraph(word2)) {
            return "No \"" + word1 + "\" and \"" + word2 + "\" in the graph!";
        }
        if (!inGraph(word1)) {
            return "No \"" + word1 + "\" in the graph!";
        }
        if (!inGraph(word2)) {
            return "No \"" + word2 + "\" in the graph!";
        }
        if (!graph.containsKey(word1)) {
            return "No bridge words from \"" + word1 + "\" to \"" + word2 + "\"!";
        }

        Set<String> bridges = new HashSet<>();
        for (String mid : graph.get(word1).keySet()) {
            if (graph.containsKey(mid) && graph.get(mid).containsKey(word2)) {
                bridges.add(mid);
            }
        }

        if (bridges.isEmpty()) {
            return "No bridge words from \"" + word1 + "\" to \"" + word2 + "\"!";
        }
        List<String> list = new ArrayList<>(bridges);
        String res = switch (list.size()) {
            case 1 -> list.getFirst();
            case 2 -> list.get(0) + " and " + list.get(1);
            default -> String.join(", ", list.subList(0, list.size() - 1)) + ", and " + list.getLast();
        };
        return "The bridge words from \"" + word1 + "\" to \"" + word2 + "\" are: " + res + ".";
    }

    private static String generateNewText(String text) {
        String[] words = text.toLowerCase().replaceAll("[^a-zA-Z\\s]", " ").split("\\s+");
        if (words.length == 0) {
            return "";
        }
        StringBuilder sb = new StringBuilder(words[0]);
        for (int i = 0; i < words.length - 1; i++) {
            String w1 = words[i], w2 = words[i + 1];
            List<String> bridges = new ArrayList<>();
            if (graph.containsKey(w1)) {
                for (String mid : graph.get(w1).keySet()) {
                    if (graph.containsKey(mid) && graph.get(mid).containsKey(w2)) {
                        bridges.add(mid);
                    }
                }
            }
            if (!bridges.isEmpty()) {
                sb.append(" ").append(bridges.get(random.nextInt(bridges.size())));
            }
            sb.append(" ").append(w2);
        }
        return sb.toString();
    }

    private static String calcShortestPath(String word1, String word2) {
        if (!graph.containsKey(word1)) {
            return "No \"" + word1 + "\" in the graph!";
        }
        Map<String, Integer> dist = new HashMap<>();
        Map<String, String> prev = new HashMap<>();
        for (String node : graph.keySet()) {
            dist.put(node, Integer.MAX_VALUE);
        }
        dist.put(word1, 0);

        PriorityQueue<String> pq = new PriorityQueue<>(Comparator.comparingInt(dist::get));
        pq.add(word1);

        while (!pq.isEmpty()) {
            String u = pq.poll();
            for (var e : graph.get(u).entrySet()) {
                String v = e.getKey();
                int weight = e.getValue();
                if (dist.get(u) + weight < dist.getOrDefault(v, Integer.MAX_VALUE)) {
                    dist.put(v, dist.get(u) + weight);
                    prev.put(v, u);
                    pq.add(v);
                }
            }
        }

        if (word2 == null || word2.isEmpty()) {
            // Display the shortest paths from word1 to all others
            StringBuilder sb = new StringBuilder();
            for (String target : graph.keySet()) {
                if (target.equals(word1)) {
                    continue;
                }
                if (dist.get(target) == Integer.MAX_VALUE) {
                    sb.append("No path from \"").append(word1).append("\" to \"").append(target).append("\"\n");
                } else {
                    LinkedList<String> path = new LinkedList<>();
                    for (String at = target; at != null; at = prev.get(at)) {
                        path.addFirst(at);
                    }
                    sb.append("Shortest path to ").append(target).append(": ")
                            .append(String.join(" -> ", path))
                            .append(" (Length: ").append(dist.get(target)).append(")\n");
                }
            }
            return sb.toString();
        } else {
            if (!graph.containsKey(word2)) {
                return "No \"" + word2 + "\" in the graph!";
            }
            if (dist.get(word2) == Integer.MAX_VALUE) {
                return "No path from \"" + word1 + "\" to \"" + word2 + "\"";
            }
            LinkedList<String> path = new LinkedList<>();
            for (String at = word2; at != null; at = prev.get(at)) {
                path.addFirst(at);
            }
            return "Shortest path: " + String.join(" -> ", path) + "\nLength: " + dist.get(word2);
        }
    }

    private static Map<String, Double> calPageRank(double d) {
        int maxIter = 100;
        double tol = 1e-6;
        Set<String> nodes = graph.keySet();
        Map<String, Double> pr = new HashMap<>();
        for (String n : nodes) {
            pr.put(n, 1.0 / nodes.size());
        }
        for (int i = 0; i < maxIter; i++) {
            Map<String, Double> next = new HashMap<>();
            for (String n : nodes) {
                next.put(n, (1 - d) / nodes.size());
            }
            double sink = 0;

            for (Map.Entry<String, Map<String, Integer>> entry : graph.entrySet()) {
                String u = entry.getKey();
                Map<String, Integer> out = entry.getValue();
                if (out.isEmpty()) {
                    sink += pr.get(u);
                    continue;
                }
                double share = pr.get(u) / out.size();
                for (String v : out.keySet()) {
                    next.put(v, next.get(v) + d * share);
                }
            }

            double sinkShare = d * sink / nodes.size();
            for (String n : nodes) {
                next.put(n, next.get(n) + sinkShare);
            }
            double delta = 0;
            for (String n : nodes) {
                delta += Math.abs(next.get(n) - pr.get(n));
            }
            pr = next;
            if (delta < tol) {
                break;
            }
        }
        return pr;
    }

    private static String randomWalk() {
        StringBuilder sb = new StringBuilder();
        Set<String> visitedEdges = new HashSet<>();
        List<String> keys = new ArrayList<>(graph.keySet());
        if (keys.isEmpty()) {
            return "";
        }
        String current = keys.get(random.nextInt(keys.size()));
        sb.append(current);
        while (graph.containsKey(current) && !graph.get(current).isEmpty()) {
            List<String> nextNodes = new ArrayList<>(graph.get(current).keySet());
            String next = nextNodes.get(random.nextInt(nextNodes.size()));
            String edge = current + "->" + next;
            if (visitedEdges.contains(edge)) {
                break;
            }
            visitedEdges.add(edge);
            sb.append(" ").append(next);
            current = next;
        }
        return sb.toString();
    }
}
