package ui;

import javafx.application.Application;
import javafx.stage.Stage;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import org.graphstream.graph.Graph;
import org.graphstream.graph.implementations.SingleGraph;
import org.graphstream.ui.fx_viewer.FxViewer;
import org.graphstream.ui.fx_viewer.FxViewPanel;

import java.util.Map;
import java.util.Set;

/**
 * Affiche le graphe d'appel en utilisant GraphStream + JavaFX.
 */
public class GraphView extends Application {

    private static Map<String, Set<String>> callGraph;

    public static void showGraph(Map<String, Set<String>> graphData) {
        callGraph = graphData;
        if (javafx.application.Platform.isFxApplicationThread()) {
            new GraphView().showGraphStage();
        } else {
            launch(); 
        }
    }

    @Override
    public void start(Stage stage) {
        showGraphStage(stage);
    }

    public void showGraphStage() {
        Stage stage = new Stage();
        showGraphStage(stage);
    }

    private void showGraphStage(Stage stage) {
        stage.setTitle("Graphe d'appel (GraphStream)");

        /* Création du graphe */
        
        Graph graph = new SingleGraph("CallGraph");

        /* Style CSS intégré */
        
        graph.setAttribute("ui.stylesheet",
            "node { " +
            "fill-color: #b0383cff; size: 25px; text-size: 14px; text-color: black; text-alignment: above; stroke-mode: plain; stroke-color: black; }" +
            "edge { fill-color: #999; arrow-shape: arrow; arrow-size: 10px, 5px; }"
        );

        /* Construction du graphe à partir de la map callGraph */
        
        for (String caller : callGraph.keySet()) {
            if (graph.getNode(caller) == null)
                graph.addNode(caller).setAttribute("ui.label", caller);

            for (String callee : callGraph.get(caller)) {
                if (graph.getNode(callee) == null)
                    graph.addNode(callee).setAttribute("ui.label", callee);

                String edgeId = caller + "->" + callee;
                if (graph.getEdge(edgeId) == null)
                    graph.addEdge(edgeId, caller, callee, true);
            }
        }

        /* Création du viewer JavaFX */
        
        FxViewer viewer = new FxViewer(graph, FxViewer.ThreadingModel.GRAPH_IN_GUI_THREAD);
        viewer.enableAutoLayout();
        FxViewPanel panel = (FxViewPanel) viewer.addDefaultView(false);

        BorderPane root = new BorderPane(panel);
        Scene scene = new Scene(root, 900, 700);
        stage.setScene(scene);
        stage.show();
    }
}