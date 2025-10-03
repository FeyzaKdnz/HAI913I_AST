package analyser.visitors;

import java.util.*;
import org.eclipse.jdt.core.dom.*;

public class MethodInvocationVisitor extends ASTVisitor {
    private final Set<String> calledMethods = new HashSet<>();

    @Override
    public boolean visit(MethodInvocation node) {
        // Nom de la méthode appelée
        calledMethods.add(node.getName().toString());
        return super.visit(node);
    }

    public Set<String> getCalledMethods() {
        return calledMethods;
    }
}
