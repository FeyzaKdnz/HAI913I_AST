package analyser;

import java.util.*;

/**
 * Construit un graphe de couplage pondéré entre les classes
 * à partir d’un graphe d’appel (CallGraphBuilder).
 */
public class CouplingGraphBuilder {

    private final Map<String, Set<String>> callGraph; // méthode → méthodes appelées
    private final Map<String, Map<String, Double>> couplingGraph = new HashMap<>();

    public CouplingGraphBuilder(Map<String, Set<String>> callGraph) {
        this.callGraph = callGraph;
    }

    /** ---------- Construit le graphe de couplage entre classes. ---------- */
    public void buildCouplingGraph() {
        Map<String, Integer> classCallCount = new HashMap<>();
        int totalRelations = 0;

        /* Compter les appels entre classes (A -> B) */
        for (Map.Entry<String, Set<String>> entry : callGraph.entrySet()) {
            String caller = entry.getKey();
            String callerClass = extractClassName(caller);

            for (String callee : entry.getValue()) {
                String calleeClass = extractClassName(callee);

                if (!callerClass.equals(calleeClass)) { // on ignore les appels internes
                    String key = callerClass + "->" + calleeClass;
                    classCallCount.put(key, classCallCount.getOrDefault(key, 0) + 1);
                    totalRelations++;
                }
            }
        }

        /** "Normalisation" des valeurs pour obtenir un poids (couplage) **/
        for (String key : classCallCount.keySet()) {
            String[] parts = key.split("->");
            String classA = parts[0];
            String classB = parts[1];

            /* poids =  nmb d'appels de la ClasseA vers la ClasseB / nmb total d'appels entre toutes les classes du projet */
            double weight = (double) classCallCount.get(key) / totalRelations;

            couplingGraph.putIfAbsent(classA, new HashMap<>());
            couplingGraph.get(classA).put(classB, weight);
        }
    }

    /** Renvoie le graphe de couplage. */
    public Map<String, Map<String, Double>> getCouplingGraph() {
        return couplingGraph;
    }

    /** Affiche le graphe de couplage. */
    public void printCouplingGraph() {
        System.out.println("===== Graphe de couplage entre classes =====");
        for (String classA : couplingGraph.keySet()) {
            for (Map.Entry<String, Double> entry : couplingGraph.get(classA).entrySet()) {
                System.out.println(classA + " -> " + entry.getKey() + " (poids = " + entry.getValue() + ")");
            }
        }
        System.out.println("============================================");
    }

    /** Extrait le nom de la classe à partir d'une signature "Classe.méthode".*/
    private String extractClassName(String methodSignature) {
        int dot = methodSignature.indexOf('.');
        if (dot != -1) {
            return methodSignature.substring(0, dot);
        } else {
            return methodSignature;
        }
    }
}
