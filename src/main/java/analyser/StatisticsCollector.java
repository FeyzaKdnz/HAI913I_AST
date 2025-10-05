package analyser;

import analyser.visitors.MethodDeclarationVisitor;
import analyser.visitors.TypeDeclarationVisitor;
import analyser.visitors.FieldDeclarationVisitor;
import org.eclipse.jdt.core.dom.*;

import java.util.*;
import java.util.stream.Collectors;

/*** Classe qui collecte des statistiques sur un projet Java à partir de CompilationUnit (AST).*/

public class StatisticsCollector {

    private final List<CompilationUnit> units;
    private final Map<String, ClassStats> classes = new HashMap<>();
    private final Set<String> packages = new HashSet<>();

    public StatisticsCollector(List<CompilationUnit> units) {
        this.units = units;
    }

    /**
     * Lance l’analyse et remplit la map `classes` + liste des packages.
     */
    public void collect() {
        for (CompilationUnit unit : units) {

            // Enregistrer les packages rencontrés
            PackageDeclaration pkg = unit.getPackage();
            if (pkg != null) {
                packages.add(pkg.getName().getFullyQualifiedName());
            }

            // Visiteur de classes
            TypeDeclarationVisitor typeVisitor = new TypeDeclarationVisitor();
            unit.accept(typeVisitor);

            for (TypeDeclaration clazz : typeVisitor.getClasses()) {
                String className = clazz.getName().toString();
                ClassStats stats = new ClassStats(className);

                // Compter les méthodes
                MethodDeclarationVisitor methodVisitor = new MethodDeclarationVisitor();
                clazz.accept(methodVisitor);
                for (MethodDeclaration method : methodVisitor.getMethods()) {
                    stats.methodCount++;
                    stats.methods.add(new MethodStats(
                            method.getName().toString(),
                            unit.getLineNumber(method.getStartPosition() + method.getLength()) -
                                    unit.getLineNumber(method.getStartPosition()),
                            method.parameters().size()
                    ));
                }

                // Compter les attributs
                FieldDeclarationVisitor fieldVisitor = new FieldDeclarationVisitor();
                clazz.accept(fieldVisitor);
                stats.attributeCount = fieldVisitor.getFields().size();

                // Compter les lignes de la classe
                int start = unit.getLineNumber(clazz.getStartPosition());
                int end = unit.getLineNumber(clazz.getStartPosition() + clazz.getLength());
                stats.lineCount = end - start + 1;
                classes.put(className, stats);
            }
        }
    }

    /**
     * Génère un rapport textuel complet.
     */
    public String generateReport() {
        int totalMethods = classes.values().stream().mapToInt(c -> c.methodCount).sum();
        int totalLines = classes.values().stream().mapToInt(c -> c.lineCount).sum();
        int totalAttributes = classes.values().stream().mapToInt(c -> c.attributeCount).sum();

        StringBuilder sb = new StringBuilder();
        sb.append("===== Rapport d'analyse =====\n");
        sb.append("Nombre de classes : ").append(classes.size()).append("\n");
        sb.append("Nombre total de packages : ").append(packages.size()).append("\n");
        sb.append("Nombre total de méthodes : ").append(totalMethods).append("\n");
        sb.append("Nombre total de lignes : ").append(totalLines).append("\n");
        sb.append("Nombre total d'attributs : ").append(totalAttributes).append("\n");

        if (!classes.isEmpty()) {
            sb.append("Moyenne de méthodes par classe : ")
              .append((double) totalMethods / classes.size()).append("\n");
            sb.append("Moyenne d'attributs par classe : ")
              .append((double) totalAttributes / classes.size()).append("\n");
            sb.append("Moyenne de lignes par méthode : ")
              .append(totalMethods > 0 ? (double) totalLines / totalMethods : 0).append("\n");
        }

        // CALCULS AVANCÉS

        // 10% des classes avec le plus de méthodes
        List<ClassStats> sortedByMethods = new ArrayList<>(classes.values());
        sortedByMethods.sort(Comparator.comparingInt((ClassStats c) -> c.methodCount).reversed());
        int topCount = Math.max(1, (int) Math.ceil(classes.size() * 0.1));
        List<ClassStats> topMethods = sortedByMethods.subList(0, Math.min(topCount, sortedByMethods.size()));

        // 10% des classes avec le plus d'attributs
        List<ClassStats> sortedByAttributes = new ArrayList<>(classes.values());
        sortedByAttributes.sort(Comparator.comparingInt((ClassStats c) -> c.attributeCount).reversed());
        List<ClassStats> topAttributes = sortedByAttributes.subList(0, Math.min(topCount, sortedByAttributes.size()));

        // Intersection des deux catégories
        Set<String> both = topMethods.stream()
                .map(c -> c.name)
                .filter(name -> topAttributes.stream().anyMatch(c -> c.name.equals(name)))
                .collect(Collectors.toSet());

        // Classes avec plus de X méthodes
        int X = 10;
        List<String> moreThanX = classes.values().stream()
                .filter(c -> c.methodCount > X)
                .map(c -> c.name)
                .toList();

        // 10% des méthodes les plus longues
        List<MethodStats> allMethods = classes.values().stream()
                .flatMap(c -> c.methods.stream())
                .collect(Collectors.toList());
        allMethods.sort(Comparator.comparingInt((MethodStats m) -> m.lineCount).reversed());
        int topMethodCount = Math.max(1, (int) Math.ceil(allMethods.size() * 0.1));
        List<MethodStats> topLongestMethods = allMethods.subList(0, Math.min(topMethodCount, allMethods.size()));

        // Nombre maximal de paramètres parmi toutes les méthodes
        int maxParams = allMethods.stream().mapToInt(m -> m.parameterCount).max().orElse(0);

        // === Ajout au rapport ===
        sb.append("\n--- Analyses avancées ---\n");
        
        sb.append("Top 10% classes par nb de méthodes : ")
          .append(topMethods.stream().map(c -> c.name).toList()).append("\n");
        
        sb.append("Top 10% classes par nb d'attributs : ")
          .append(topAttributes.stream().map(c -> c.name).toList()).append("\n");
        
        sb.append("Classes présentes dans les deux catégories : ").append(both).append("\n");
        
        sb.append("Classes avec plus de ").append(X).append(" méthodes : ").append(moreThanX).append("\n");
        
        sb.append("Top 10% méthodes les plus longues : ")
          .append(topLongestMethods.stream().map(m -> m.name).toList()).append("\n");
        
        sb.append("Nombre maximal de paramètres : ").append(maxParams).append("\n");

        sb.append("=============================\n");
        return sb.toString();
    }

    // Accès aux stats (utile pour l'IHM ou tests)
    public Map<String, ClassStats> getClasses() { return classes; }

    public Set<String> getPackages() { return packages; }

    // ================= Classes internes pour stocker les stats =================
    public static class ClassStats {
        String name;
        int methodCount;
        int attributeCount;
        int lineCount;
        List<MethodStats> methods = new ArrayList<>();

        public ClassStats(String name) { this.name = name; }
    }

    public static class MethodStats {
        String name;
        int lineCount;
        int parameterCount;

        public MethodStats(String name, int lineCount, int parameterCount) {
            this.name = name;
            this.lineCount = lineCount;
            this.parameterCount = parameterCount;
        }
    }

    public Map<String, Integer> getStatsMap() {
    Map<String, Integer> stats = new LinkedHashMap<>();
    stats.put("Nombre de classes", classes.size());
    stats.put("Nombre de packages", packages.size());
    int totalMethods = classes.values().stream().mapToInt(c -> c.methodCount).sum();
    int totalAttributes = classes.values().stream().mapToInt(c -> c.attributeCount).sum();
    int totalLines = classes.values().stream().mapToInt(c -> c.lineCount).sum();
    stats.put("Nombre total de méthodes", totalMethods);
    stats.put("Nombre total d'attributs", totalAttributes);
    stats.put("Nombre total de lignes", totalLines);

    // Ajout des moyennes (arrondies)
    if (!classes.isEmpty()) {
        stats.put("Moyenne de méthodes par classe", (int)Math.round((double)totalMethods / classes.size()));
        stats.put("Moyenne d'attributs par classe", (int)Math.round((double)totalAttributes / classes.size()));
    }
    if (totalMethods > 0) {
        stats.put("Moyenne de lignes par méthode", (int)Math.round((double)totalLines / totalMethods));
    }
    return stats;
}
}