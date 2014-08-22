package contege;
import java.io.PrintWriter;
import java.util.Map;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Attribute;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.util.Printer;
import org.objectweb.asm.util.TraceAnnotationVisitor;
import org.objectweb.asm.util.TraceClassVisitor;
import org.objectweb.asm.util.TraceFieldVisitor;
import org.objectweb.asm.util.TraceMethodVisitor;


public class MyTraceClassVisitor extends ClassVisitor {
	 /**
     * The print writer to be used to print the class. May be null.
     */
    private final PrintWriter pw;

    /**
     * The object that actually converts visit events into text.
     */
    public final MyTextifier p;

    /**
     * Constructs a new {@link TraceClassVisitor}.
     * 
     * @param pw
     *            the print writer to be used to print the class.
     */
    public MyTraceClassVisitor(final PrintWriter pw) {
        this(null, pw);
    }

    /**
     * Constructs a new {@link TraceClassVisitor}.
     * 
     * @param cv
     *            the {@link ClassVisitor} to which this visitor delegates
     *            calls. May be <tt>null</tt>.
     * @param pw
     *            the print writer to be used to print the class.
     */
    public MyTraceClassVisitor(final ClassVisitor cv, final PrintWriter pw) {
        this(cv, new MyTextifier(), pw);
    }

    /**
     * Constructs a new {@link TraceClassVisitor}.
     * 
     * @param cv
     *            the {@link ClassVisitor} to which this visitor delegates
     *            calls. May be <tt>null</tt>.
     * @param p
     *            the object that actually converts visit events into text.
     * @param pw
     *            the print writer to be used to print the class. May be null if
     *            you simply want to use the result via
     *            {@link Printer#getText()}, instead of printing it.
     */
    public MyTraceClassVisitor(final ClassVisitor cv, final MyTextifier p,
            final PrintWriter pw) {
        super(Opcodes.ASM4, cv);
        this.pw = pw;
        this.p = p;
    }

    @Override
    public void visit(final int version, final int access, final String name,
            final String signature, final String superName,
            final String[] interfaces) {
        p.visit(version, access, name, signature, superName, interfaces);
        super.visit(version, access, name, signature, superName, interfaces);
    }

    @Override
    public void visitSource(final String file, final String debug) {
        p.visitSource(file, debug);
        super.visitSource(file, debug);
    }

    @Override
    public void visitOuterClass(final String owner, final String name,
            final String desc) {
        p.visitOuterClass(owner, name, desc);
        super.visitOuterClass(owner, name, desc);
    }

    @Override
    public AnnotationVisitor visitAnnotation(final String desc,
            final boolean visible) {
        Printer p = this.p.visitClassAnnotation(desc, visible);
        AnnotationVisitor av = cv == null ? null : cv.visitAnnotation(desc,
                visible);
        return new TraceAnnotationVisitor(av, p);
    }

    @Override
    public void visitAttribute(final Attribute attr) {
        p.visitClassAttribute(attr);
        super.visitAttribute(attr);
    }

    @Override
    public void visitInnerClass(final String name, final String outerName,
            final String innerName, final int access) {
        p.visitInnerClass(name, outerName, innerName, access);
        super.visitInnerClass(name, outerName, innerName, access);
    }

    @Override
    public FieldVisitor visitField(final int access, final String name,
            final String desc, final String signature, final Object value) {
        Printer p = this.p.visitField(access, name, desc, signature, value);
        FieldVisitor fv = cv == null ? null : cv.visitField(access, name, desc,
                signature, value);
        return new TraceFieldVisitor(fv, p);
    }

    @Override
    public MethodVisitor visitMethod(final int access, final String name,
            final String desc, final String signature, final String[] exceptions) {
        Printer p = this.p.visitMethod(access, name, desc, signature, exceptions);
        MethodVisitor mv = cv == null ? null : cv.visitMethod(access, name, desc, signature, exceptions);
        return new TraceMethodVisitor(mv, p);
    }

    @Override
    public void visitEnd() {
        p.visitClassEnd();
        if (pw != null) {
            pw.flush();
        }
        super.visitEnd();
    }
    
    public Map<String, MyTextifier.MyField> getFields() {
    	return p.fields;
    }
    
    public Map<String, MyTextifier.MyMethod> getMethods() {
    	return p.methods;
    }
}
