package analyser;

import analyser.visitors.MethodInvocationVisitor;
import analyser.visitors.TypeDeclarationVisitor;

import org.eclipse.jdt.core.dom.*;

import java.util.*;

public class CallGraphBuilder {

    /** Graphe d'appel : clé = méthode appelante, valeur = méthodes appelées **/
	
    private final Map<String, Set<String>> callGraph = new HashMap<>();
       
    /** Construction du graphe d’appel à partir d’une liste de CompilationUnit. **/
    
    public void build(List<CompilationUnit> units) {
        for (CompilationUnit unit : units) {
            TypeDeclarationVisitor typeVisitor = new TypeDeclarationVisitor();
            unit.accept(typeVisitor);

            for (TypeDeclaration clazz : typeVisitor.getClasses()) {
                String className = clazz.getName().toString();

                for (MethodDeclaration method : clazz.getMethods()) {
                    String caller = className + "." + method.getName();

                    /* Visiteur pour trouver les appels dans cette méthode */
                    MethodInvocationVisitor invocationVisitor = new MethodInvocationVisitor(className);
                    method.accept(invocationVisitor);

                    for (String callee : invocationVisitor.getCalledMethods()) {
                        callGraph
                            .computeIfAbsent(caller, k -> new HashSet<>())
                            .add(callee);
                    }
                }
            }
        }
    }

    /* Renvoie le graphe d’appel */
    public Map<String, Set<String>> getCallGraph() {
        return callGraph;
    }

    /* Affichage du graphe d’appel */
    
    public void printGraph() {
        callGraph.forEach((caller, callers) -> {
            System.out.println(caller + " appelle : " + callers);
        });
    }
}
