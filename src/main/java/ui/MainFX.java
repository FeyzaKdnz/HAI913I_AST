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

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.TableView;
import javafx.scene.control.TableColumn;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.Separator;
import javafx.scene.layout.Priority;

/* Interface JavaFX */
public class MainFX extends Application {

    private TextArea outputArea;
    private File selectedDir;
    private Map<String, Set<String>> currentGraph;
    private TableView<StatRow> statsTable;

    public static void main(String[] args) {
        launch(args);
    }
    
    @Override
    public void start(Stage stage) {
        stage.setTitle("Analyse statique - TP HAI913I");
        
        /**  Cette partie permet d'avoir une structure sous forme de tableau **/
        statsTable = new TableView<>();
        TableColumn<StatRow, String> nameCol = new TableColumn<>("Nom");
        nameCol.setCellValueFactory(new PropertyValueFactory<>("name"));
        TableColumn<StatRow, Integer> valueCol = new TableColumn<>("Valeur");
        valueCol.setCellValueFactory(new PropertyValueFactory<>("value"));
        statsTable.getColumns().addAll(nameCol, valueCol);
        statsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        outputArea = new TextArea();
        outputArea.setEditable(false);
        outputArea.setWrapText(true);

        Label folderLabel = new Label("Aucun dossier sélectionné");
        Button chooseBtn = new Button("Choisir projet..");
        Button analyzeBtn = new Button("Analyser");
        Button graphBtn = new Button("Afficher le graphe");
        analyzeBtn.setDisable(true);
        graphBtn.setDisable(true);
        outputArea.setEditable(false);
        outputArea.setWrapText(true);

        ProgressIndicator progress = new ProgressIndicator();
        progress.setVisible(false);
        progress.setPrefSize(24, 24);

        /** --------------- Choix du projet --------------- **/
        chooseBtn.setOnAction(ev -> {
        	/* Ouvre une boîte de dialogue pour sélectionner un dossier et affichage avec showDialog() */
            DirectoryChooser dc = new DirectoryChooser();
            dc.setTitle("Sélectionner un projet");
            File dir = dc.showDialog(stage);
            /* Si un dossier a été sélectionné par l'utilisateur */
            if (dir != null) {
                selectedDir = dir;  // Mémorise le dossier choisi
                folderLabel.setText("Dossier: " + dir.getAbsolutePath());
                analyzeBtn.setDisable(false);
                graphBtn.setDisable(true);
                outputArea.clear(); // Vide la zone de texte des résultats précédents
            }
        });

        /** --------------- Lancement de l'analyse --------------- **/
        
        analyzeBtn.setOnAction(ev -> {
            if (selectedDir == null) return;
            outputArea.clear();
            progress.setVisible(true);
            analyzeBtn.setDisable(true);
            chooseBtn.setDisable(true);
            graphBtn.setDisable(true);
            statsTable.getItems().clear();

            javafx.concurrent.Task<Void> task = new javafx.concurrent.Task<>() {
                @Override
                protected Void call() {
                    try {
                        Parser parser = new Parser(selectedDir.getAbsolutePath());
                        List<CompilationUnit> units = parser.parseProject();

                        /** --------------- Résultat statistique --------------- **/
                        StatisticsCollector stats = new StatisticsCollector(units);
                        stats.collect();
                        Map<String, Integer> statMap = stats.getStatsMap();
                        ObservableList<StatRow> statRows = FXCollections.observableArrayList();
                        statMap.forEach((k, v) -> statRows.add(new StatRow(k, v)));

                        /** --------------- Graphe d'appel --------------- **/
                        CallGraphBuilder builder = new CallGraphBuilder();
                        builder.build(units);
                        currentGraph = builder.getCallGraph();

                        String report = stats.generateReport();
                        StringBuilder sb = new StringBuilder();
                        sb.append("Fichiers parsés: ").append(units.size()).append("\n\n");
                        sb.append(report).append("\n");
                        sb.append("--- Graphe d'appel (texte) ---\n");
                        if (currentGraph.isEmpty()) {
                            sb.append("(aucun appel détecté)\n");
                        } else {
                            currentGraph.forEach((caller, callees) ->
                                sb.append(caller).append(" appelle: ").append(callees).append("\n")
                            );
                        }

                        final String finalText = sb.toString();
                        javafx.application.Platform.runLater(() -> {
                            statsTable.setItems(statRows);
                            outputArea.setText(finalText);
                            graphBtn.setDisable(false);
                        });
                    } catch (Exception ex) {
                        javafx.application.Platform.runLater(() ->
                            outputArea.setText("Erreur lors de l'analyse: " + ex.getClass().getSimpleName() + ex.getMessage()));
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
        
        /** --------------- Affichage du graphe d'appel dans une nouvelle fenêtre --------------- **/
        
        graphBtn.setOnAction(ev -> {
            if (currentGraph != null && !currentGraph.isEmpty()) {
                GraphView.showGraph(currentGraph);
            } else {
                outputArea.setText("Aucun graphe à afficher. Lancez une analyse d'abord.");
            }
        });

        Label statsLabel = new Label("Statistiques globales");
        Label advancedLabel = new Label("Analyses avancées");
        HBox controls = new HBox(10, chooseBtn, analyzeBtn, graphBtn, progress);
        controls.setAlignment(Pos.CENTER_LEFT);
        
        /** --------------- Conteneur de mise en place JavaFX --------------- **/
        
        VBox root = new VBox(10, folderLabel, controls, statsLabel, statsTable, new Separator(), advancedLabel, outputArea);
        
        root.setPadding(new Insets(10));
        Scene scene = new Scene(root, 900, 600);
        stage.setScene(scene);
        stage.show();
        statsTable.getItems().clear();
        statsTable.setPrefHeight(200);
        outputArea.setPrefHeight(250);
        VBox.setVgrow(statsTable, Priority.ALWAYS);
        VBox.setVgrow(outputArea, Priority.ALWAYS);
    }

    /**  --------------- Classe utilitaire qui facilite l’affichage des statistiques dans le tableau de l’interface graphique.  --------------- 

    /** Permet au TableView d’afficher proprement chaque statistique dans une ligne du tableau avec une colonne pour le nom et une pour la valeur.
     */
    
    public static class StatRow {
        private final String name;
        private final Integer value;

        public StatRow(String name, Integer value) {
            this.name = name;
            this.value = value;
        }
        public String getName() { return name; }
        public Integer getValue() { return value; }
    }
}
