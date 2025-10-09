package analyser;

import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;

/**
 * Classe utilitaire pour parser un projet Java en ASTs avec Eclipse JDT.
 */
public class Parser {

    private final String projectPath;
    private final String sourcePath;

    public Parser(String projectPath) {
        this.projectPath = projectPath;
        
        File mainJava = new File(projectPath, "src/main/java");
        File src = new File(projectPath, "src");
        
        if (mainJava.exists()) {
            this.sourcePath = mainJava.getAbsolutePath();
        } else {
            this.sourcePath = src.getAbsolutePath();
        }
    }

    /**
     * Parse tout le projet et renvoie une liste de CompilationUnit (AST par fichier Java).
     */
    
    public List<CompilationUnit> parseProject() throws IOException {
        List<File> javaFiles = listJavaFiles(new File(sourcePath));
        List<CompilationUnit> units = new ArrayList<>();

        for (File file : javaFiles) {
            String content = Files.readString(file.toPath());
            if (content.isBlank()) continue;
            CompilationUnit unit = parse(content.toCharArray());
            units.add(unit);
        }
        return units;
    }

    /**
     * Liste récursivement tous les fichiers .java d’un dossier.
     */
    private List<File> listJavaFiles(File folder) {
        List<File> javaFiles = new ArrayList<>();
        if (folder.exists() && folder.isDirectory()) {
            File[] files = folder.listFiles();
            if (files != null) {
                for (File entry : files) {
                    if (entry.isDirectory()) {
                        javaFiles.addAll(listJavaFiles(entry));
                    } else if (entry.getName().endsWith(".java")) {
                        System.out.println("Fichier trouvé: " + entry.getAbsolutePath());
                        javaFiles.add(entry);
                    }
                }
            }
        } else {
            System.out.println("Dossier introuvable: " + folder.getAbsolutePath());
        }
        return javaFiles;
    }

    /**
     * Construit un AST (CompilationUnit) pour une classe donnée.
     */
    private CompilationUnit parse(char[] source) {
    	ASTParser parser = ASTParser.newParser(AST.JLS4);;
        parser.setKind(ASTParser.K_COMPILATION_UNIT);
        parser.setResolveBindings(true);
        parser.setBindingsRecovery(true);

        @SuppressWarnings("rawtypes")
        Map options = JavaCore.getOptions();
        parser.setCompilerOptions(options);

        parser.setUnitName(""); // nécessaire pour setEnvironment
        String[] sources = { sourcePath };
        String[] classpath = {};

        parser.setEnvironment(classpath, sources, new String[] { "UTF-8" }, true);
        parser.setSource(source);

        return (CompilationUnit) parser.createAST(null);
    }

    // === Méthode main pour tester le Parser seul ===
    
    public static void main(String[] args) throws IOException {
        if (args.length == 0) {
            System.err.println("Usage: java Parser <chemin_projet>");
            System.exit(1);
        }
        String path = args[0];
        Parser parser = new Parser(path);
        List<CompilationUnit> units = parser.parseProject();

        System.out.println("Projet analysé: " + path);
        System.out.println("Fichiers parsés: " + units.size());
    }
}
