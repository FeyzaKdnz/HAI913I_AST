package analyser;

import analyser.visitors.MethodDeclarationVisitor;
import analyser.visitors.TypeDeclarationVisitor;
import analyser.visitors.FieldDeclarationVisitor;

import org.eclipse.jdt.core.dom.*;

import java.util.*;

/**
 * Classe qui collecte des statistiques sur un projet Java à partir de CompilationUnit (AST).
 */
public class StatisticsCollector {

    private final List<CompilationUnit> units;
    private final Map<String, ClassStats> classes = new HashMap<>();

    public StatisticsCollector(List<CompilationUnit> units) {
        this.units = units;
    }

    /**
     * Lance l’analyse et remplit la map `classes`.
     */
    public void collect() {
        for (CompilationUnit unit : units) {
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
                    stats.methods.add(new MethodStats(method.getName().toString(),
                            unit.getLineNumber(method.getStartPosition() + method.getLength()) -
                            unit.getLineNumber(method.getStartPosition()),
                            method.parameters().size()));
                }

                // Compter les attributs (champs de classe)
                FieldDeclarationVisitor fieldVisitor = new FieldDeclarationVisitor();
                clazz.accept(fieldVisitor);
                stats.attributeCount = fieldVisitor.getFields().size();

                // Compter les lignes (heuristique : fin - début)
                int start = unit.getLineNumber(clazz.getStartPosition());
                int end = unit.getLineNumber(clazz.getStartPosition() + clazz.getLength());
                stats.lineCount = end - start + 1;

                classes.put(className, stats);
            }
        }
    }

    /**
     * Génère un rapport textuel basique (3 premières métriques).
     */
    public String generateReport() {
        int totalMethods = classes.values().stream().mapToInt(c -> c.methodCount).sum();
        int totalLines = classes.values().stream().mapToInt(c -> c.lineCount).sum();
        int totalAttributes = classes.values().stream().mapToInt(c -> c.attributeCount).sum();

        StringBuilder sb = new StringBuilder();
        sb.append("===== Rapport d'analyse =====\n");
        sb.append("Nombre de classes : ").append(classes.size()).append("\n");
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

        sb.append("=============================\n");
        return sb.toString();
    }

    // Accès aux stats (utile pour tests ou interface)
    public Map<String, ClassStats> getClasses() { return classes; }

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
}
