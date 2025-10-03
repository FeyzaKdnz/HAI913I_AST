package analyser.visitors;

import java.util.*;
import org.eclipse.jdt.core.dom.*;

public class MethodDeclarationVisitor extends ASTVisitor {
    private final List<MethodDeclaration> methods = new ArrayList<>();

    @Override
    public boolean visit(MethodDeclaration node) {
        methods.add(node);
        return super.visit(node);
    }

    public List<MethodDeclaration> getMethods() {
        return methods;
    }
}
