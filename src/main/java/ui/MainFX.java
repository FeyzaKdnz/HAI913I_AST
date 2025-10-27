package ui;

import analyser.HierarchicalClustering;
import analyser.Parser;
import analyser.StatisticsCollector;
import analyser.CallGraphBuilder;
import org.eclipse.jdt.core.dom.CompilationUnit;
import analyser.CouplingGraphBuilder;

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
import javafx.scene.control.TextField;
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

public class MainFX extends Application {

    private TextArea outputArea;
    private File selectedDir;
    private Map<String, Set<String>> currentGraph;
    private TableView<StatRow> statsTable;
    private Map<String, Map<String, Double>> currentCouplingGraph;
    private TextField cpThresholdField;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) {
        stage.setTitle("Analyse statique - TP HAI913I");

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
        Button couplingBtn = new Button("Afficher le graphe de couplage");
        Button clusteringBtn = new Button("Identifier les Modules");

        cpThresholdField = new TextField("0.05");
        cpThresholdField.setPrefWidth(60);

        analyzeBtn.setDisable(true);
        graphBtn.setDisable(true);
        couplingBtn.setDisable(true);
        clusteringBtn.setDisable(true);
        cpThresholdField.setDisable(true);

        ProgressIndicator progress = new ProgressIndicator();
        progress.setVisible(false);
        progress.setPrefSize(24, 24);

        chooseBtn.setOnAction(ev -> {
            DirectoryChooser dc = new DirectoryChooser();
            dc.setTitle("Sélectionner un projet");
            File dir = dc.showDialog(stage);
            if (dir != null) {
                selectedDir = dir;
                folderLabel.setText("Dossier: " + dir.getAbsolutePath());
                analyzeBtn.setDisable(false);
                graphBtn.setDisable(true);
                outputArea.clear();
            }
        });

        analyzeBtn.setOnAction(ev -> {
            if (selectedDir == null) return;
            outputArea.clear();
            progress.setVisible(true);
            analyzeBtn.setDisable(true);
            chooseBtn.setDisable(true);
            graphBtn.setDisable(true);
            couplingBtn.setDisable(true);
            clusteringBtn.setDisable(true);
            cpThresholdField.setDisable(true);
            statsTable.getItems().clear();

            javafx.concurrent.Task<Void> task = new javafx.concurrent.Task<>() {
                @Override
                protected Void call() {
                    try {
                        Parser parser = new Parser(selectedDir.getAbsolutePath());
                        List<CompilationUnit> units = parser.parseProject();

                        StatisticsCollector stats = new StatisticsCollector(units);
                        stats.collect();
                        Map<String, Integer> statMap = stats.getStatsMap();
                        ObservableList<StatRow> statRows = FXCollections.observableArrayList();
                        statMap.forEach((k, v) -> statRows.add(new StatRow(k, v)));

                        CallGraphBuilder builder = new CallGraphBuilder();
                        builder.build(units);
                        currentGraph = builder.getCallGraph();

                        CouplingGraphBuilder couplingBuilder = new CouplingGraphBuilder(currentGraph);
                        couplingBuilder.buildCouplingGraph();
                        currentCouplingGraph = couplingBuilder.getCouplingGraph();

                        // ... (rest of the analysis text generation) ...

                        javafx.application.Platform.runLater(() -> {
                            statsTable.setItems(statRows);
                            // outputArea.setText(finalText);
                            graphBtn.setDisable(false);
                            couplingBtn.setDisable(false);
                            clusteringBtn.setDisable(false);
                            cpThresholdField.setDisable(false);
                        });

                    } catch (Exception ex) {
                        javafx.application.Platform.runLater(() ->
                            outputArea.setText("Erreur lors de l'analyse: " + ex.getClass().getSimpleName() + " " + ex.getMessage()));
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
            new Thread(task).start();
        });

        graphBtn.setOnAction(ev -> {
            if (currentGraph != null && !currentGraph.isEmpty()) {
                javafx.application.Platform.runLater(() -> GraphView.showGraph(currentGraph));
            } else {
                outputArea.setText("Aucun graphe à afficher.");
            }
        });

        couplingBtn.setOnAction(ev -> {
            if (currentCouplingGraph != null && !currentCouplingGraph.isEmpty()) {
                javafx.application.Platform.runLater(() ->
                        CouplingGraphView.showCouplingGraph(currentCouplingGraph)
                );
            } else {
                outputArea.appendText("\nAucun graphe de couplage à afficher.\n");
            }
        });

        clusteringBtn.setOnAction(ev -> {
            if (currentCouplingGraph == null || currentCouplingGraph.isEmpty()) {
                outputArea.appendText("\nAucun graphe de couplage disponible. Lancez d'abord l'analyse.\n");
                return;
            }
            try {
                double cp = Double.parseDouble(cpThresholdField.getText());
                outputArea.appendText("\n\n--- Identification des modules (CP = " + cp + ") ---\n");

                HierarchicalClustering clustering = new HierarchicalClustering(currentCouplingGraph);
                HierarchicalClustering.DendrogramNode root = clustering.cluster();
                List<Set<String>> modules = clustering.identifyModules(root, cp);

                StringBuilder result = new StringBuilder();
                result.append("Nombre de modules identifiés: ").append(modules.size()).append("\n");
                int moduleCount = 1;
                for (Set<String> module : modules) {
                    result.append("  - Module ").append(moduleCount++).append(": ").append(module).append("\n");
                }
                outputArea.appendText(result.toString());

            } catch (NumberFormatException e) {
                outputArea.appendText("\nErreur: La valeur de CP doit être un nombre valide (ex: 0.05).\n");
            }
        });

        Label statsLabel = new Label("Statistiques globales");
        Label advancedLabel = new Label("Analyses avancées");
        HBox controls = new HBox(10, chooseBtn, analyzeBtn, graphBtn, couplingBtn, new Label("CP:"), cpThresholdField, clusteringBtn, progress);
        controls.setAlignment(Pos.CENTER_LEFT);

        VBox root = new VBox(10, folderLabel, controls, new Separator(), statsLabel, statsTable, new Separator(), advancedLabel, outputArea);
        root.setPadding(new Insets(10));
        Scene scene = new Scene(root, 900, 600);
        stage.setScene(scene);
        stage.show();
    }

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
