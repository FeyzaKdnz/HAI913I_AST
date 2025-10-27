package analyser;

import java.util.*;

public class HierarchicalClustering {

    // --- Structures de données pour le Dendrogramme ---

    public abstract static class DendrogramNode {
        public abstract Set<String> getClasses();
        public abstract int size();
    }

    public static class LeafNode extends DendrogramNode {
        private final String className;

        public LeafNode(String className) {
            this.className = className;
        }

        @Override
        public Set<String> getClasses() {
            return Collections.singleton(className);
        }
        
        @Override
        public int size() { return 1; }

        public String getClassName() {
            return className;
        }
    }

    public static class MergedNode extends DendrogramNode {
        private final DendrogramNode left;
        private final DendrogramNode right;
        private final double coupling;
        private Set<String> classesCache;

        public MergedNode(DendrogramNode left, DendrogramNode right, double coupling) {
            this.left = left;
            this.right = right;
            this.coupling = coupling;
        }

        @Override
        public Set<String> getClasses() {
            if (classesCache == null) {
                classesCache = new HashSet<>(left.getClasses());
                classesCache.addAll(right.getClasses());
            }
            return classesCache;
        }

        @Override
        public int size() {
            return left.size() + right.size();
        }

        public DendrogramNode getLeft() { return left; }
        public DendrogramNode getRight() { return right; }
        public double getCoupling() { return coupling; }
    }

    /* --------------------------- Algorithme de Clustering -------------------------- */

    private final Map<String, Map<String, Double>> originalCouplingGraph;
    private final Map<String, Map<String, Double>> symmetricCouplingGraph;

    public HierarchicalClustering(Map<String, Map<String, Double>> couplingGraph) {
        this.originalCouplingGraph = couplingGraph;
        this.symmetricCouplingGraph = buildSymmetricCoupling(couplingGraph);
    }

    public DendrogramNode cluster() {
        /* La liste des clusters est initialisée avec chaque classe dans un LeafNode */
        List<DendrogramNode> currentClusters = new ArrayList<>();
        Set<String> classes = new HashSet<>();
        originalCouplingGraph.forEach((key, value) -> {
            classes.add(key);
            value.keySet().forEach(classes::add);
        });

        for (String className : classes) {
            currentClusters.add(new LeafNode(className));
        }

        /* Boucle de fusion principale */
        while (currentClusters.size() > 1) {
            double maxCoupling = -1;
            DendrogramNode clusterA = null;
            DendrogramNode clusterB = null;

            /* Trouver la paire de clusters la + couplée */
            for (int i = 0; i < currentClusters.size(); i++) {
                for (int j = i + 1; j < currentClusters.size(); j++) {
                    DendrogramNode c1 = currentClusters.get(i);
                    DendrogramNode c2 = currentClusters.get(j);
                    double coupling = calculateClusterCoupling(c1.getClasses(), c2.getClasses());

                    if (coupling > maxCoupling) {
                        maxCoupling = coupling;
                        clusterA = c1;
                        clusterB = c2;
                    }
                }
            }
            /* Fusionner les deux clusters dans un nouveau noeud (mergedCluster) */
            if (clusterA != null) {
                currentClusters.remove(clusterA);
                currentClusters.remove(clusterB);
                MergedNode mergedCluster = new MergedNode(clusterA, clusterB, maxCoupling);
                currentClusters.add(mergedCluster);
            } else {
                break; // Arrêt s'il n'y a pas de couplage
            }
        }
        return currentClusters.get(0); // Retourne racine du dendrogramme
    }
    
    /* --------------------------- Algorithme d'identification des modules --------------------------- */

    public List<Set<String>> identifyModules(DendrogramNode root, double couplingThresholdCP) {
        List<Set<String>> modules = new ArrayList<>();
        Queue<DendrogramNode> queue = new LinkedList<>();
        queue.add(root);

        int M = root.size();

        while (!queue.isEmpty()) {
            DendrogramNode node = queue.poll();

            if (node instanceof LeafNode) {
                modules.add(node.getClasses());
                continue;
            }

            /* Calcul de la cohésion interne du noeud */
            double averageCoupling = calculateAverageInternalCoupling(node.getClasses());

            /* Si cohésion est suffisante, on valide le module et on n'explore pas ses enfants */
            if (averageCoupling > couplingThresholdCP) {
                modules.add(node.getClasses());
            } else {
                /* Sinon, on explore ses enfants à la file pour les évaluer */
                MergedNode mergedNode = (MergedNode) node;
                queue.add(mergedNode.getLeft());
                queue.add(mergedNode.getRight());
            }
        }

        if (modules.size() > M / 2) {
            System.out.println("Le nombre de modules (" + modules.size() + ") dépasse M/2 (" + (M/2) + "). Essayez d'augmenter le seuil CP.");
        }
        return modules;
    }

    private double calculateAverageInternalCoupling(Set<String> cluster) {
        if (cluster.size() <= 1) {
            return Double.MAX_VALUE; // Un cluster d'une seule classe est parfaitement cohérent
        }

        double totalCoupling = 0;
        int pairCount = 0;
        List<String> classList = new ArrayList<>(cluster);

        for (int i = 0; i < classList.size(); i++) {
            for (int j = i + 1; j < classList.size(); j++) {
                String classA = classList.get(i);
                String classB = classList.get(j);
                totalCoupling += symmetricCouplingGraph.getOrDefault(classA, Collections.emptyMap()).getOrDefault(classB, 0.0);
                pairCount++;
            }
        }
        return (pairCount == 0) ? 0 : totalCoupling / pairCount;
    }

    private double calculateClusterCoupling(Set<String> cluster1, Set<String> cluster2) {
        double totalCoupling = 0;
        for (String class1 : cluster1) {
            for (String class2 : cluster2) {
                totalCoupling += symmetricCouplingGraph.getOrDefault(class1, Collections.emptyMap()).getOrDefault(class2, 0.0);
            }
        }
        return totalCoupling;
    }

    private static Map<String, Map<String, Double>> buildSymmetricCoupling(Map<String, Map<String, Double>> graph) {
        Map<String, Map<String, Double>> symmetric = new HashMap<>();
        for (String classA : graph.keySet()) {
            for (String classB : graph.get(classA).keySet()) {
                double weight = graph.get(classA).get(classB);
                symmetric.computeIfAbsent(classA, k -> new HashMap<>()).merge(classB, weight, Double::sum);
                symmetric.computeIfAbsent(classB, k -> new HashMap<>()).merge(classA, weight, Double::sum);
            }
        }
        return symmetric;
    }
}