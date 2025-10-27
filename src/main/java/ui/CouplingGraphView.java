package ui;

import javafx.scene.Scene;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.scene.text.Text;
import javafx.stage.Stage;

import java.util.*;

/**
 * Affiche le graphe de couplage entre classes (nœuds + arêtes + poids).
 */
public class CouplingGraphView {

    public static void showCouplingGraph(Map<String, Map<String, Double>> couplingGraph) {
        Stage stage = new Stage();
        stage.setTitle("Graphe de couplage entre classes");

        Pane pane = new Pane();
        ScrollPane scroll = new ScrollPane(pane);
        scroll.setPannable(true);

        // Positionnement circulaire des nœuds
        double centerX = 500;
        double centerY = 400;
        double radius = 250;

        Set<String> classes = new HashSet<>(couplingGraph.keySet());
        for (Map<String, Double> map : couplingGraph.values()) {
            classes.addAll(map.keySet());
        }

        int n = classes.size();
        List<String> classList = new ArrayList<>(classes);
        Map<String, double[]> positions = new HashMap<>();

        // Placer les classes en cercle
        for (int i = 0; i < n; i++) {
            double angle = 2 * Math.PI * i / n;
            double x = centerX + radius * Math.cos(angle);
            double y = centerY + radius * Math.sin(angle);
            positions.put(classList.get(i), new double[]{x, y});

            Text node = new Text(x - 20, y, classList.get(i));
            node.setFill(Color.DARKBLUE);
            node.setStyle("-fx-font-weight: bold;");
            pane.getChildren().add(node);
        }

        /** Dessiner les arêtes avec poids **/

        for (String classA : couplingGraph.keySet()) {
            for (Map.Entry<String, Double> entry : couplingGraph.get(classA).entrySet()) {
                String classB = entry.getKey();
                double weight = entry.getValue();

                double[] posA = positions.get(classA);
                double[] posB = positions.get(classB);

                // Vérification de la présence des deux positions
                if (posA == null || posB == null) continue;

                /* Lignes + couleur selon intensité */
                Line line = new Line(posA[0], posA[1], posB[0], posB[1]);
                double intensity = Math.min(1.0, weight * 10); // limite à 1.0
                line.setStroke(Color.color(intensity, 0, 0)); // plus rouge si plus fort
                line.setStrokeWidth(1.5 + weight * 3);
                pane.getChildren().add(line);

                // Poids affiché au milieu
                double midX = (posA[0] + posB[0]) / 2;
                double midY = (posA[1] + posB[1]) / 2;
                Text weightText = new Text(midX, midY, String.format("%.3f", weight));
                weightText.setFill(Color.DARKRED);
                pane.getChildren().add(weightText);
            }
        }

        Scene scene = new Scene(scroll, 1000, 800);
        stage.setScene(scene);
        stage.show();
    }
}
