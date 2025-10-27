package ui;

import analyser.CouplingGraphBuilder;
import analyser.Parser;
import analyser.StatisticsCollector;
import analyser.CallGraphBuilder;
import org.eclipse.jdt.core.dom.CompilationUnit;
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

        // AFFICHAGE GRAPHIQUE
        ui.GraphView.showGraph(callGraph);

        // TP2: couplage des classes
        CouplingGraphBuilder couplingBuilder = new CouplingGraphBuilder(callGraph);
        couplingBuilder.buildCouplingGraph();
        couplingBuilder.printCouplingGraph();
    }
}
