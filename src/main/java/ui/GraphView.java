package ui;

import javafx.scene.Scene;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.text.Text;
import javafx.stage.Stage;

import java.util.*;

/**
 * Graphe d'appel dessiné avec JavaFX (sans dépendances externes).
 */
public class GraphView {

    public static void showGraph(Map<String, Set<String>> callGraph) {
        Stage stage = new Stage();
        stage.setTitle("Graphe d'appel (JavaFX)");

        Pane root = new Pane();
        Scene scene = new Scene(root, 900, 700, Color.WHITE);
        stage.setScene(scene);

        // Positionnement automatique des noeuds sur un cercle
        int n = callGraph.keySet().size();
        double centerX = 450, centerY = 350, radius = 250;
        double angleStep = 2 * Math.PI / Math.max(n, 1);

        // Map pour stocker les positions
        Map<String, double[]> positions = new HashMap<>();
        int i = 0;
        for (String node : callGraph.keySet()) {
            double x = centerX + radius * Math.cos(i * angleStep);
            double y = centerY + radius * Math.sin(i * angleStep);
            positions.put(node, new double[]{x, y});
            i++;
        }

        // Dessiner les arêtes (lignes)
        for (var entry : callGraph.entrySet()) {
            String caller = entry.getKey();
            for (String callee : entry.getValue()) {
                double[] p1 = positions.get(caller);
                double[] p2 = positions.get(callee);
                if (p1 != null && p2 != null) {
                    Line line = new Line(p1[0], p1[1], p2[0], p2[1]);
                    line.setStroke(Color.GRAY);
                    line.setStrokeWidth(1.5);
                    root.getChildren().add(line);
                }
            }
        }

        // Dessiner les noeuds (cercles + noms)
        for (var entry : positions.entrySet()) {
            String node = entry.getKey();
            double[] pos = entry.getValue();
            Circle circle = new Circle(pos[0], pos[1], 20, Color.web("#317AC1"));
            circle.setStroke(Color.BLACK);
            Text label = new Text(pos[0] - 30, pos[1] - 25, node);
            label.setFill(Color.BLACK);
            root.getChildren().addAll(circle, label);
        }

        stage.show();
    }
}
