package instrumentor.popup.actions;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.SimpleName;

public class MethodVisitor extends ASTVisitor {
	List<MethodDeclaration> methods = new ArrayList<MethodDeclaration>();
	List<SimpleName> exceptions = new ArrayList<SimpleName>();
	Set<String> exceptionSet = new TreeSet<String>();

	@Override
	public boolean visit(MethodDeclaration node) {
		if ((!(node.isConstructor())) && (!(Modifier.isAbstract(node.getModifiers())))) {
				methods.add(node);
		}

		return false;
	}

	public List<MethodDeclaration> getMethods() {
		return methods;
	}

	@SuppressWarnings("unchecked")
	public Set<String> getExceptions(MethodDeclaration node) {
		exceptionSet = new TreeSet<String>();
		exceptions = node.thrownExceptions();
		for (Name exception : exceptions) {
			exceptionSet.add(exception.toString());
		}
		return exceptionSet;
	}
}
