package ui;

import analyser.Parser;
import analyser.StatisticsCollector;
import analyser.CallGraphBuilder;

import org.eclipse.jdt.core.dom.CompilationUnit;
import java.util.List;

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
        System.out.println("Nombre de fichiers parsés: " + units.size());

        StatisticsCollector stats = new StatisticsCollector(units);
        stats.collect();
        System.out.println(stats.generateReport());
        
        CallGraphBuilder builder = new CallGraphBuilder();
        builder.build(units);
        builder.printGraph();
    }
}
