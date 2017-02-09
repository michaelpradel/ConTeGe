package instrumentor.popup.actions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageDeclaration;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeHierarchy;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.CatchClause;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.IfStatement;
import org.eclipse.jdt.core.dom.ImportDeclaration;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.NullLiteral;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.StringLiteral;
import org.eclipse.jdt.core.dom.ThisExpression;
import org.eclipse.jdt.core.dom.ThrowStatement;
import org.eclipse.jdt.core.dom.TryStatement;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.text.edits.MalformedTreeException;
import org.eclipse.text.edits.TextEdit;
import org.eclipse.ui.IActionDelegate;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;

public class InstrumentAction implements IObjectActionDelegate {

	private ISelection selection;
	private AST ast;

	private static List<ICompilationUnit> instrumentedClasses = new ArrayList<ICompilationUnit>();

	private HashMap<Integer, Block> methodOriginalBody = new HashMap<Integer, Block>();

	/**
	 * Constructor for Action.
	 */
	public InstrumentAction() {
		super();
	}

	/**
	 * @see IObjectActionDelegate#setActivePart(IAction, IWorkbenchPart)
	 */
	public void setActivePart(IAction action, IWorkbenchPart targetPart) {
	}

	/**
	 * @see IActionDelegate#run(IAction)
	 */
	@SuppressWarnings({ "rawtypes", "unused" })
	public void run(IAction action) {
		try {
			System.out.println("Start instrumentation");

			Iterator it = ((IStructuredSelection) selection).iterator();
			if (it.hasNext()) {
				ICompilationUnit cu = (ICompilationUnit) it.next();
				IJavaProject project = cu.getJavaProject();

				// Copy Project
				copyProject(project);
				addInstrumentMethodClass(project, cu.getResource()
						.getFullPath().segment(1));
			}
			it = ((IStructuredSelection) selection).iterator();
			while (it.hasNext()) {

				ICompilationUnit unit = (ICompilationUnit) it.next();
				IJavaProject project = unit.getJavaProject();

				// Get package name
				IPackageDeclaration[] pkgs = null;
				pkgs = unit.getPackageDeclarations();
				String myPackage = null;
				if (pkgs.length > 0) {
					myPackage = pkgs[0].getElementName();
				} else {
					myPackage = "";
				}

				// Get Class name
				String unitName = null;
				if (unit.getElementName().lastIndexOf('.') <= 0) {
					unitName = unit.getElementName();
				} else {
					unitName = unit.getElementName().substring(0,
							unit.getElementName().lastIndexOf('.'));
				}

				// Instrument class and its superclasses
				getSuperClassAndInstrument(unit, myPackage, unitName);

				System.out.println("Done instrumentation");

			}
		} catch (JavaModelException e) {
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (MalformedTreeException e) {
			e.printStackTrace();
		} catch (BadLocationException e) {
			e.printStackTrace();
		} catch (CoreException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Call instreumentation on the class and all its superclasses recursively
	 * 
	 * @param unit
	 * @param myPackage
	 * @param unitName
	 * @throws JavaModelException
	 * @throws MalformedTreeException
	 * @throws BadLocationException
	 */
	public void getSuperClassAndInstrument(ICompilationUnit unit,
			String myPackage, String unitName) throws JavaModelException,
			MalformedTreeException, BadLocationException {
		IType[] types;
		types = unit.getAllTypes();
		IPackageDeclaration[] pkgs = null;
		pkgs = unit.getPackageDeclarations();
		String pkg = null;
		if (pkgs.length > 0) {
			pkg = pkgs[0].getElementName();
		} else {
			pkg = "";
		}
		instrumentedClasses.add(unit);
		System.out.println("Instrumenting Class - " + pkg + "."
				+ unit.getElementName());
		for (IType type : types) {
			ITypeHierarchy typeHierarchy = type.newSupertypeHierarchy(null);
			IType superclass = typeHierarchy.getSuperclass(type);
			if ((superclass != null
					&& !("java.lang.Object".equals(superclass
							.getFullyQualifiedName())) && !(""
						.equals(superclass.getFullyQualifiedName())))
					&& !(instrumentedClasses.contains(superclass
							.getCompilationUnit()))) {
				if (!superclass.isReadOnly()) {
					System.out.println("Instrumenting Super Class - "
							+ superclass.getFullyQualifiedName());
					getSuperClassAndInstrument(superclass.getCompilationUnit(),
							myPackage, unitName);
				}
			}
		}
		instrumentClass(unit, myPackage, unitName);
	}

	/**
	 * For each parameter in the method, get Class.getName() of the type.
	 * 
	 * @param qualifiedType
	 * @return
	 */
	public String getMethodType(String qualifiedType) {
		if (qualifiedType.contains("[]")) {
			int lastIndex = 0;
			int count = 0;

			while (lastIndex != -1) {

				lastIndex = qualifiedType.indexOf("[]", lastIndex);

				if (lastIndex != -1) {
					count++;
					lastIndex += "[]".length();
				}
			}

			String returnString = "";
			for (int i = 0; i < count; i++) {
				returnString = returnString + "[";
			}

			if (qualifiedType.contains("byte")) {
				return returnString + "B";
			} else if (qualifiedType.contains("boolean")) {
				return returnString + "Z";
			} else if (qualifiedType.contains("char")) {
				return returnString + "C";
			} else if (qualifiedType.contains("double")) {
				return returnString + "D";
			} else if (qualifiedType.contains("float")) {
				return returnString + "F";
			} else if (qualifiedType.contains("int")) {
				return returnString + "I";
			} else if (qualifiedType.contains("long")) {
				return returnString + "L";
			} else if (qualifiedType.contains("short")) {
				return returnString + "S";
			} else {
				qualifiedType = qualifiedType.split("\\[")[0];
				return returnString + "L" + qualifiedType + ";";
			}
		}
		return qualifiedType;
	}

	/**
	 * Instrument a class
	 * 
	 * @param unit
	 * @param myPackage
	 * @param unitName
	 * @throws JavaModelException
	 * @throws MalformedTreeException
	 * @throws BadLocationException
	 */
	@SuppressWarnings({ "unchecked", "deprecation" })
	public void instrumentClass(ICompilationUnit unit, String myPackage,
			String unitName) throws JavaModelException, MalformedTreeException,
			BadLocationException {
		MethodVisitor visitor = new MethodVisitor();
		Document document = null;
		TextEdit edits = null;
		TextEdit editModifyMethods = null;
		Block instrumentedBody = null;

		CompilationUnit parse = parse(unit);
		parse = parse(unit);
		unit.becomeWorkingCopy(null);
		ast = parse.getAST();
		ASTRewrite rewriter = ASTRewrite.create(ast);
		visitor = new MethodVisitor();
		parse.accept(visitor);

		String className = null;

		if ("".equals(myPackage)) {
			className = unitName;
		} else {
			className = myPackage + "." + unitName;
		}

		IPackageDeclaration[] pkgs = null;
		pkgs = unit.getPackageDeclarations();
		String instrPackage = null;
		if (pkgs.length > 0) {
			instrPackage = pkgs[0].getElementName();
		} else {
			instrPackage = "";
		}
		String instrUnitName = null;
		if (unit.getElementName().lastIndexOf('.') <= 0) {
			instrUnitName = unit.getElementName();
		} else {
			instrUnitName = unit.getElementName().substring(0,
					unit.getElementName().lastIndexOf('.'));
		}

		String instrClassName = null;

		if ("".equals(instrPackage)) {
			instrClassName = instrUnitName;
		} else {
			instrClassName = instrPackage + "." + instrUnitName;
		}

		// Adding instrumentation
		for (MethodDeclaration method : visitor.getMethods()) {

			StringBuffer sParameterList = new StringBuffer();
			sParameterList.append("(");
			for (Object params : method.parameters()) {
				SingleVariableDeclaration sParam = (SingleVariableDeclaration) params;
				String paramTmp = getMethodType(sParam.getName()
						.resolveTypeBinding().getQualifiedName());
				paramTmp = paramTmp.split("\\<")[0];
				sParameterList.append(paramTmp);
				sParameterList.append(",");
			}
			if (sParameterList.length() > 1) {
				sParameterList.deleteCharAt(sParameterList.length() - 1);
			}
			sParameterList.append(")");

			methodOriginalBody.put(visitor.getMethods().indexOf(method),
					method.getBody());

			Block body = method.getBody();
			ListRewrite listRewrite = rewriter.getListRewrite(body,
					Block.STATEMENTS_PROPERTY);
			boolean staticFlag = false;
			if (Modifier.isStatic(method.getModifiers())) {
				staticFlag = true;
			}

			for (String exception : visitor.getExceptions(method)) {
				ThrowStatement throwStatement = ast.newThrowStatement();
				ClassInstanceCreation newInstance = ast
						.newClassInstanceCreation();
				newInstance.setType(ast.newSimpleType(ast.newName(exception)));
				newInstance.arguments().add(ast.newStringLiteral());
				throwStatement.setExpression(newInstance);
				IfStatement instrExceptionStmt = ast.newIfStatement();
				instrExceptionStmt.setThenStatement(throwStatement);
				instrExceptionStmt.setExpression(ast.newBooleanLiteral(false));
				listRewrite.insertFirst(instrExceptionStmt, null);
			}

			String methodKey = method.resolveBinding().getKey();
			String[] methodKeyTokens = methodKey.split("\\;");
			String methodName = methodKeyTokens[0].substring(1);
			methodName = methodName + methodKeyTokens[1].split("\\(")[0];
			methodName = methodName.split("\\<")[0];
			methodName = methodName.replace("/", ".");
			methodName = methodName.replace(instrClassName, className);
			Statement instrStartStmt = methodInvocation(ast,
					"instrStartMethod", methodName + sParameterList.toString(),
					staticFlag, null);
			listRewrite.insertFirst(instrStartStmt, null);

			edits = rewriter.rewriteAST();
			document = new Document(unit.getSource());
			edits.apply(document);
		}
		if (document != null) {
			unit.getBuffer().setContents(document.get());
			unit.commitWorkingCopy(false, null);
		}

		CompilationUnit parseModifyMethods = parse(unit);
		unit.becomeWorkingCopy(null);
		ast = parseModifyMethods.getAST();
		ASTParser parser = ASTParser.newParser(AST.JLS3);
		parser.setSource(unit);
		visitor = new MethodVisitor();
		parseModifyMethods.accept(visitor);
		parseModifyMethods.recordModifications();

		ImportDeclaration id = ast.newImportDeclaration();
		id.setName(ast.newName(new String[] { "instrumentClasses",
				"InstrumentMethods" }));
		parseModifyMethods.imports().add(id);

		System.out.println("Hashmap" + methodOriginalBody);

		// Adding try catch block
		for (MethodDeclaration method : visitor.getMethods()) {
			StringBuffer sParameterList = new StringBuffer();
			sParameterList.append("(");
			for (Object params : method.parameters()) {
				SingleVariableDeclaration sParam = (SingleVariableDeclaration) params;
				String paramTmp = getMethodType(sParam.getName()
						.resolveTypeBinding().getQualifiedName());
				paramTmp = paramTmp.split("\\<")[0];
				sParameterList.append(paramTmp);
				sParameterList.append(",");
			}
			if (sParameterList.length() > 1) {
				sParameterList.deleteCharAt(sParameterList.length() - 1);
			}
			sParameterList.append(")");

			boolean staticFlag = false;
			if (Modifier.isStatic(method.getModifiers())) {
				staticFlag = true;
			}

			String methodKey = method.resolveBinding().getKey();
			String[] methodKeyTokens = methodKey.split("\\;");
			String methodName = methodKeyTokens[0].substring(1);
			methodName = methodName + methodKeyTokens[1].split("\\(")[0];
			methodName = methodName.split("\\<")[0];
			methodName = methodName.replace("/", ".");
			methodName = methodName.replace(instrClassName, className);

			Block newBody = surroundBodyWithTryCatch(method,
					visitor.getExceptions(method), sParameterList.toString(),
					staticFlag, methodName);
			method.setBody(null);
			method.setBody(newBody);
			instrumentedBody = newBody;

			// Add a check so that logging is done only on concurrent execution
			Expression doLogIfStmtExpression = ast.newQualifiedName(
					ast.newSimpleName("InstrumentMethods"),
					ast.newSimpleName("doInstrumentLog"));
			IfStatement doLogIfStmt = ast.newIfStatement();
			doLogIfStmt.setExpression(doLogIfStmtExpression);
			Block thenStmtBlock = ast.newBlock();
			for (int i = 0; i < instrumentedBody.statements().size(); i++) {
				ASTNode singleStmt = (ASTNode) instrumentedBody.statements()
						.get(i);
				thenStmtBlock.statements()
						.add(ASTNode.copySubtree(thenStmtBlock.getAST(),
								singleStmt));
			}
			Block elseStmtBlock = ast.newBlock();
			Block originalBody = methodOriginalBody.get(visitor.getMethods()
					.indexOf(method));
			for (int i = 0; i < originalBody.statements().size(); i++) {
				ASTNode singleStmt = (ASTNode) originalBody.statements().get(i);
				elseStmtBlock.statements()
						.add(ASTNode.copySubtree(elseStmtBlock.getAST(),
								singleStmt));
			}
			doLogIfStmt.setThenStatement(thenStmtBlock);
			doLogIfStmt.setElseStatement(elseStmtBlock);
			Block methodBody = method.getBody();
			methodBody.statements().clear();
			ASTNode singleStmt = (ASTNode) doLogIfStmt;
			methodBody.statements().add(
					ASTNode.copySubtree(methodBody.getAST(), singleStmt));
			method.setBody(null);
			method.setBody(methodBody);

			document = new Document(unit.getSource());
			editModifyMethods = parseModifyMethods.rewrite(document, unit
					.getJavaProject().getOptions(true));
			editModifyMethods.apply(document);
		}
		if (document != null) {
			unit.getBuffer().setContents(document.get());
			unit.commitWorkingCopy(false, null);

		}
	}

	/**
	 * @see IActionDelegate#selectionChanged(IAction, ISelection)
	 */
	public void selectionChanged(IAction action, ISelection selection) {
		this.selection = selection;
	}

	/**
	 * Parse a compilation unit
	 * 
	 * @param unit
	 * @return
	 */
	@SuppressWarnings("deprecation")
	private CompilationUnit parse(ICompilationUnit unit) {
		ASTParser parser = ASTParser.newParser(AST.JLS3);
		parser.setKind(ASTParser.K_COMPILATION_UNIT);
		parser.setSource(unit);
		parser.setResolveBindings(true);
		return (CompilationUnit) parser.createAST(null); // parse
	}

	/**
	 * Surrounds the method body with try catch finally block
	 * 
	 * @param method
	 * @param exceptionList
	 * @param sParameterList
	 * @param staticFlag
	 * @param methodName
	 * @return
	 */
	@SuppressWarnings("unchecked")
	private Block surroundBodyWithTryCatch(MethodDeclaration method,
			Set<String> exceptionList, String sParameterList,
			boolean staticFlag, String methodName) {
		AST ast = method.getAST();
		Block oldBody = method.getBody();
		Block newBody = ast.newBlock();
		method.setBody(newBody);
		TryStatement tryStatement = ast.newTryStatement();
		newBody.statements().add(tryStatement);
		tryStatement.setBody(oldBody);
		boolean exceptionFlag = false;
		boolean runtimeExceptionFlag = false;
		boolean throwableFlag = false;
		boolean classNotFoundExceptionFlag = false;
		boolean ioExceptionFlag = false;
		boolean clonenotSupportedExceptionFlag = false;
		if (exceptionList != null) {
			if (exceptionList.size() != 0) {
				for (String exceptionName : exceptionList) {
					if (exceptionName.toString().equals("Exception")) {
						exceptionFlag = true;
					} else if (exceptionName.toString().equals("Throwable")) {
						throwableFlag = true;
					} else if (exceptionName.toString().equals(
							"RuntimeException")) {
						runtimeExceptionFlag = true;
					} else if (exceptionName.toString().equals(
							"CloneNotSupportedException")) {
						clonenotSupportedExceptionFlag = true;
					} else if (exceptionName.toString().equals("IOException")) {
						ioExceptionFlag = true;
					} else if (exceptionName.toString().equals(
							"ClassNotFoundException")) {
						classNotFoundExceptionFlag = true;
					} else {
						CatchClause catchClause = catchBlock(
								exceptionName.toString(), ast);
						tryStatement.catchClauses().add(catchClause);
					}
				}
			}
		}
		if (ioExceptionFlag) {
			CatchClause catchClause = catchBlock("IOException", ast);
			tryStatement.catchClauses().add(catchClause);
		}
		if (classNotFoundExceptionFlag) {
			CatchClause catchClause = catchBlock("ClassNotFoundException", ast);
			tryStatement.catchClauses().add(catchClause);
		}
		if (clonenotSupportedExceptionFlag) {
			CatchClause catchClause = catchBlock("CloneNotSupportedException",
					ast);
			tryStatement.catchClauses().add(catchClause);
		}
		if (runtimeExceptionFlag) {
			CatchClause catchClause = catchBlock("RuntimeException", ast);
			tryStatement.catchClauses().add(catchClause);
		}
		if (exceptionFlag) {
			CatchClause catchClause = catchBlock("Exception", ast);
			tryStatement.catchClauses().add(catchClause);
		}
		if (throwableFlag) {
			CatchClause catchClause = catchBlock("Throwable", ast);
			tryStatement.catchClauses().add(catchClause);
		}
		if ((!(exceptionFlag)) && (!(runtimeExceptionFlag))
				&& (!(throwableFlag))) {
			CatchClause catchClause = catchBlock("RuntimeException", ast);
			tryStatement.catchClauses().add(catchClause);
		}

		Block finallyBlock = ast.newBlock();
		Statement instrEndStmt = methodInvocation(ast, "instrEndMethod",
				methodName + sParameterList.toString(), staticFlag, null);

		finallyBlock.statements().add(instrEndStmt);
		tryStatement.setFinally(finallyBlock);
		return newBody;
	}

	/**
	 * Adds catch expression for an exception passed in parameter
	 * 
	 * @param exceptionName
	 * @param ast
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public CatchClause catchBlock(String exceptionName, AST ast) {
		CatchClause catchClause = ast.newCatchClause();
		SingleVariableDeclaration exceptionDeclaration = ast
				.newSingleVariableDeclaration();
		try {
			exceptionDeclaration.setName(ast.newSimpleName("instrument"
					+ exceptionName));
		} catch (IllegalArgumentException iae) {
			if (exceptionName.contains(".")) {
				String eName[] = exceptionName.split("\\.");
				exceptionDeclaration.setName(ast.newSimpleName("instrument"
						+ eName[eName.length - 1]));
			} else {
				throw iae;
			}
		}
		Name typeName = ast.newName(exceptionName);
		exceptionDeclaration.setType(ast.newSimpleType(typeName));
		catchClause.setException(exceptionDeclaration);
		Block catchBlock = ast.newBlock();
		ThrowStatement throwStmt = ast.newThrowStatement();
		try {
			throwStmt.setExpression(ast.newSimpleName("instrument"
					+ exceptionName));
		} catch (IllegalArgumentException iae) {
			if (exceptionName.contains(".")) {
				String eName[] = exceptionName.split("\\.");
				throwStmt.setExpression(ast.newSimpleName("instrument"
						+ eName[eName.length - 1]));
			} else {
				throw iae;
			}
		}
		catchBlock.statements().add(throwStmt);
		catchClause.setBody(catchBlock);
		return catchClause;
	}

	/**
	 * Add the class which logs to trace file.
	 * @param project
	 * @param sourceDir
	 * @throws JavaModelException
	 */
	public void addInstrumentMethodClass(IJavaProject project, String sourceDir)
			throws JavaModelException {
		IProject projectMain = project.getProject();
		IFolder folder = projectMain.getFolder(sourceDir);
		IPackageFragmentRoot srcFolder = project.getPackageFragmentRoot(folder);
		String classDec = "package instrumentClasses;"
				+ "\n"
				+ "\n"
				+ "import java.io.BufferedWriter;"
				+ "\n"
				+ "import java.io.File;"
				+ "\n"
				+ "import java.io.FileWriter;"
				+ "\n"
				+ "import java.io.IOException;"
				+ "\n"
				+ "import java.util.concurrent.atomic.AtomicInteger;"
				+ "\n"
				+ "\n"
				+ "public class InstrumentMethods  {"
				+ "\n"
				+ "\n"
				+ "\t"
				+ "public static AtomicInteger instrumentGlobalTS = new AtomicInteger(0);"
				+ "\n" + "\t"
				+ "public static boolean doInstrumentLog = false;" + "\n"
				+ "\n";

		String methodWriteToFile = "\t"
				+ "public static void instrWriteTofile(String message) {"
				+ "\n"
				+ "\t"
				+ "\t"
				+ "String directoryName = \"Instrument_Traces\";"
				+ "\n"
				+ "\t"
				+ "\t"
				+ "String fileName = directoryName + File.separator + \"Instrument\" + Thread.currentThread().getId();"
				+ "\n"
				+ "\t"
				+ "\t"
				+ "try {"
				+ "\n"
				+ "\t"
				+ "\t"
				+ "\t"
				+ "File dir = new File(directoryName);"
				+ "\n"
				+ "\t"
				+ "\t"
				+ "\t"
				+ "if (!dir.exists()) {"
				+ "\n"
				+ "\t"
				+ "\t"
				+ "\t"
				+ "\t"
				+ "dir.mkdir();"
				+ "\n"
				+ "\t"
				+ "\t"
				+ "\t"
				+ "}"
				+ "File file = new File(fileName);"
				+ "\n"
				+ "\t"
				+ "\t"
				+ "\t"
				+ "if (!file.exists()) {"
				+ "\n"
				+ "\t"
				+ "\t"
				+ "\t"
				+ "\t"
				+ "file.createNewFile();"
				+ "\n"
				+ "\t"
				+ "\t"
				+ "\t"
				+ "}"
				+ "\n"
				+ "\t"
				+ "\t"
				+ "\t"
				+ "FileWriter fw = new FileWriter(file.getAbsoluteFile(),true);"
				+ "\n" + "\t" + "\t" + "\t"
				+ "BufferedWriter bw = new BufferedWriter(fw);" + "\n" + "\t"
				+ "\t" + "\t" + "bw.write(message + \"\\n\");" + "\n" + "\t"
				+ "\t" + "\t" + "bw.close();" + "\n" + "\t" + "\t"
				+ "} catch (IOException e) {" + "\n" + "\t" + "\t" + "\t"
				+ "	e.printStackTrace();" + "\n" + "\t" + "\t" + "}" + "\n"
				+ "\t" + "}" + "\n" + "\n";

		String methodInstrStartMethod = "\t"
				+ "public static void instrStartMethod(Object currentObject, String methodExecuted) {"
				+ "\n"
				+ "\t"
				+ "\t"
				+ "String message=\"Start method@\" + System.identityHashCode(currentObject) + \"@\" + methodExecuted + \"@\" + \"@\" + instrumentGlobalTS.incrementAndGet();"
				+ "\n" + "\t" + "\t" + "instrWriteTofile(message);" + "\n"
				+ "\t" + "}" + "\n" + "\n";

		String methodInstrEndMethod = "\t"
				+ "public static void instrEndMethod(Object currentObject, String methodExecuted) {"
				+ "\n"
				+ "\t"
				+ "\t"
				+ "String message=\"End method@\" + System.identityHashCode(currentObject) + \"@\" + methodExecuted + \"@\" + \"@\" + instrumentGlobalTS.incrementAndGet();"
				+ "\n" + "\t" + "\t" + "instrWriteTofile(message);" + "\n"
				+ "\t" + "}" + "\n" + "\n";

		String methodInstrEndWithExceptionMethod = "\t"
				+ "public static void instrEndWithExceptionMethod(Object currentObject, String methodExecuted, String exceptionCaught) {"
				+ "\n"
				+ "\t"
				+ "\t"
				+ "String message=\"End method with exception@\" + System.identityHashCode(currentObject) + \"@\" + methodExecuted + \"@\" + exceptionCaught + \"@\" + instrumentGlobalTS.incrementAndGet();"
				+ "\n" + "\t" + "\t" + "instrWriteTofile(message);" + "\n"
				+ "\t" + "}" + "\n" + "\n";

		classDec = classDec + methodWriteToFile + methodInstrStartMethod
				+ methodInstrEndMethod + methodInstrEndWithExceptionMethod
				+ "}";

		IPackageFragment fragment = srcFolder.createPackageFragment(
				"instrumentClasses", true, null);
		fragment.createCompilationUnit("InstrumentMethods.java", classDec,
				false, null);
	}

	/**
	 * Copy the project and keep the insitrumented version as old version
	 * @param project
	 * @throws CoreException
	 */
	public void copyProject(IJavaProject project) throws CoreException {
		IPath projectCopy = new Path(project.getProject().getFullPath() + "Old");
		project.getProject().copy(projectCopy, true, null);
	}

	/**
	 * Write code for invoking logging for start and end of a method
	 * 
	 * @param ast
	 * @param methodName
	 * @param methodExecuted
	 * @param staticFlag
	 * @param exceptionCaught
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public Statement methodInvocation(AST ast, String methodName,
			String methodExecuted, boolean staticFlag, String exceptionCaught) {
		MethodInvocation instrMethod = ast.newMethodInvocation();
		SimpleName name = ast.newSimpleName("InstrumentMethods");
		instrMethod.setExpression(name);
		instrMethod.setName(ast.newSimpleName(methodName));
		ThisExpression thisExpression = ast.newThisExpression();
		NullLiteral nullLiteral = ast.newNullLiteral();
		StringLiteral literalMethodExecuted = ast.newStringLiteral();
		literalMethodExecuted.setLiteralValue(methodExecuted);
		if (staticFlag) {
			instrMethod.arguments().add(nullLiteral);
		} else {
			instrMethod.arguments().add(thisExpression);
		}
		instrMethod.arguments().add(literalMethodExecuted);
		if (exceptionCaught != null) {
			StringLiteral literalException = ast.newStringLiteral();
			literalException.setLiteralValue(exceptionCaught);
			instrMethod.arguments().add(literalException);
		}
		return ast.newExpressionStatement(instrMethod);
	}

}
