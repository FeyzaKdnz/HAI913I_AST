package ui;

import analyser.Parser;
import analyser.StatisticsCollector;
import analyser.CallGraphBuilder;
import org.eclipse.jdt.core.dom.CompilationUnit;
import ui.GraphView;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class MainApp {
    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            System.err.println("Usage: java MainApp <chemin_projet>");
            return;
        }

        String projectPath = args[0];
        System.out.println("Projet analysé: " + projectPath);

        Parser parser = new Parser(projectPath);
        List<CompilationUnit> units = parser.parseProject();
        System.out.println("Fichiers parsés: " + units.size());

        // LANCEMENT DE L'ANALYSE
        StatisticsCollector stats = new StatisticsCollector(units);
        stats.collect();
        System.out.println(stats.generateReport());

        // GRAPHE D'APPEL
        CallGraphBuilder builder = new CallGraphBuilder();
        builder.build(units);
        Map<String, Set<String>> callGraph = builder.getCallGraph();

        // Affichage graphique
        ui.GraphView.showGraph(callGraph);
    }
}
