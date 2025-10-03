package analyser.visitors;

import java.util.*;
import org.eclipse.jdt.core.dom.*;

public class FieldDeclarationVisitor extends ASTVisitor {
    private final List<FieldDeclaration> fields = new ArrayList<>();

    @Override
    public boolean visit(FieldDeclaration node) {
        fields.add(node);
        return super.visit(node);
    }

    public List<FieldDeclaration> getFields() {
        return fields;
    }
}
