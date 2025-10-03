package analyser.visitors;

import java.util.*;
import org.eclipse.jdt.core.dom.*;

public class TypeDeclarationVisitor extends ASTVisitor {
    private final List<TypeDeclaration> classes = new ArrayList<>();

    @Override
    public boolean visit(TypeDeclaration node) {
        classes.add(node);
        return super.visit(node); // continue la visite
    }

    public List<TypeDeclaration> getClasses() {
        return classes;
    }
}
