package analyser.visitors;

import java.util.*;
import org.eclipse.jdt.core.dom.*;

public class MethodInvocationVisitor extends ASTVisitor {
    private final Set<String> calledMethods = new HashSet<>();
    private final String currentClassName;

    public MethodInvocationVisitor(String currentClassName) {
        this.currentClassName = currentClassName;
    }

    @Override
    public boolean visit(MethodInvocation node) {
        IMethodBinding methodBinding = node.resolveMethodBinding();

        if (methodBinding != null) {
            ITypeBinding declaringClass = methodBinding.getDeclaringClass();
            if (declaringClass != null) {
                // On prend le nom de la classe qui déclare la méthode
                calledMethods.add(declaringClass.getName() + "." + node.getName().getIdentifier());
            }
        } else {
            // Si le binding échoue, on tente une résolution manuelle simple.
            Expression expression = node.getExpression();
            if (expression == null) {
                // Appel à une méthode de la même classe (this.method() ou method())
                calledMethods.add(currentClassName + "." + node.getName().getIdentifier());
            }
            // Les autres cas (appels sur des variables, etc.) sont plus complexes
            // et sont ignorés pour l'instant si le binding échoue.
        }
        return super.visit(node);
    }

    public Set<String> getCalledMethods() {
        return calledMethods;
    }
}