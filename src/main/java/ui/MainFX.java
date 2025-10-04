package ui;

import analyser.Parser;
import analyser.StatisticsCollector;
import analyser.CallGraphBuilder;
import org.eclipse.jdt.core.dom.CompilationUnit;

import javafx.application.Application;
import javafx.stage.Stage;
import javafx.stage.DirectoryChooser;
import javafx.scene.Scene;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.geometry.Insets;
import javafx.geometry.Pos;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Interface JavaFX minimale :
 * - choisir un dossier (DirectoryChooser)
 * - cliquer "Analyser"
 * - afficher le rapport + graphe d'appel (texte)
 */
public class MainFX extends Application {

    private TextArea outputArea;
    private File selectedDir;

    public static void main(String[] args) {
        launch(args);
    }
    
    @Override
    public void start(Stage stage) {
        stage.setTitle("Analyse statique - TP HAI913I");

        Label folderLabel = new Label("Aucun dossier sélectionné");
        Button chooseBtn = new Button("Choisir dossier...");
        Button analyzeBtn = new Button("Analyser");
        analyzeBtn.setDisable(true);

        outputArea = new TextArea();
        outputArea.setEditable(false);
        outputArea.setWrapText(true);

        ProgressIndicator progress = new ProgressIndicator();
        progress.setVisible(false);
        progress.setPrefSize(24, 24);

        // Choix du dossier (racine du projet à analyser)
        chooseBtn.setOnAction(ev -> {
            DirectoryChooser dc = new DirectoryChooser();
            dc.setTitle("Sélectionner le dossier du projet (ex: /.../TP.POO)");
            File dir = dc.showDialog(stage);
            if (dir != null) {
                selectedDir = dir;
                folderLabel.setText("Dossier : " + dir.getAbsolutePath());
                analyzeBtn.setDisable(false);
            }
        });

        // Analyse (en tâche de fond pour ne pas bloquer l'IHM)
        analyzeBtn.setOnAction(ev -> {
            if (selectedDir == null) return;
            outputArea.clear();
            progress.setVisible(true);
            analyzeBtn.setDisable(true);
            chooseBtn.setDisable(true);

            javafx.concurrent.Task<Void> task = new javafx.concurrent.Task<>() {
                @Override
                protected Void call() {
                    try {
                        Parser parser = new Parser(selectedDir.getAbsolutePath());
                        List<CompilationUnit> units = parser.parseProject();

                        // Stats
                        StatisticsCollector stats = new StatisticsCollector(units);
                        stats.collect();
                        String report = stats.generateReport();

                        // Call graph
                        CallGraphBuilder builder = new CallGraphBuilder();
                        builder.build(units);

                        StringBuilder sb = new StringBuilder();
                        sb.append("Fichiers parsés : ").append(units.size()).append("\n\n");
                        sb.append(report).append("\n");
                        sb.append("--- Graphe d'appel (texte) ---\n");
                        Map<String, Set<String>> graph = builder.getCallGraph();
                        if (graph.isEmpty()) {
                            sb.append("(aucun appel détecté)\n");
                        } else {
                            graph.forEach((caller, callees) ->
                                sb.append(caller).append(" appelle : ").append(callees).append("\n")
                            );
                        }

                        final String finalText = sb.toString();
                        javafx.application.Platform.runLater(() -> outputArea.setText(finalText));
                    } catch (Exception ex) {
                        javafx.application.Platform.runLater(() ->
                            outputArea.setText("Erreur lors de l'analyse : " + ex.getClass().getSimpleName() + " - " + ex.getMessage()));
                    } finally {
                        javafx.application.Platform.runLater(() -> {
                            progress.setVisible(false);
                            analyzeBtn.setDisable(false);
                            chooseBtn.setDisable(false);
                        });
                    }
                    return null;
                }
            };

            Thread t = new Thread(task);
            t.setDaemon(true);
            t.start();
        });

        HBox controls = new HBox(10, chooseBtn, analyzeBtn, progress);
        controls.setAlignment(Pos.CENTER_LEFT);

        VBox root = new VBox(10, folderLabel, controls, outputArea);
        root.setPadding(new Insets(10));
        Scene scene = new Scene(root, 900, 600);
        stage.setScene(scene);
        stage.show();
    }

}
