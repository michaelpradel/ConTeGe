package contege;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.objectweb.asm.Attribute;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Label;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.util.Printer;

public class MyTextifier extends Printer {

	private int INFO = 0;
	private int WARNING = 1;
	private ArrayList<String> logs = new ArrayList<String>();
	
    protected Map<Label, String> labelNames;
    protected Map<String, MyField> fields = new HashMap<String, MyField>();
    protected MyMethod currentMethod = null;
    protected Map<String, MyMethod> methods = new HashMap<String, MyMethod>();

    public MyTextifier() {
        this(Opcodes.ASM4);
    }

    protected MyTextifier(final int api) {
        super(api);
    }

    // ------------------------------------------------------------------------
    // Classes
    // ------------------------------------------------------------------------

    @Override
    public void visit(final int version, final int access, final String name,
            final String signature, final String superName,
            final String[] interfaces) {
    	log(INFO, "visit");
    }

    @Override
    public void visitSource(final String file, final String debug) {
    	log(INFO, "visitSource");
    }

    @Override
    public void visitOuterClass(final String owner, final String name,
            final String desc) {
    	log(WARNING, "visitOuterClass");
    }

    @Override
    public MyTextifier visitClassAnnotation(final String desc,
            final boolean visible) {
    	log(WARNING, "visitClassAnnotation");
    	return this;
    }

    @Override
    public void visitClassAttribute(final Attribute attr) {
    	log(WARNING, "visitClassAttribute");
    }

    @Override
    public void visitInnerClass(final String name, final String outerName,
            final String innerName, final int access) {
    	log(WARNING, "visitInnerClass");
    }
    
    @Override
    public MyTextifier visitField(final int access, final String name,
            final String desc, final String signature, final Object value) {
        
    	String accessString = accessToString(access);
    	
    	fields.put(name, new MyField(name, accessString, desc));

        return this;
    }

    @Override
    public MyTextifier visitMethod(final int access, final String name,
            final String desc, final String signature, final String[] exceptions) {
    	
    	String accessString = accessToString(access);
    	
        if ((access & Opcodes.ACC_NATIVE) != 0) {
            accessString += " native";
        }
        if ((access & Opcodes.ACC_VARARGS) != 0) {
            accessString += " varargs";
        }
        if ((access & Opcodes.ACC_BRIDGE) != 0) {
            accessString += " bridge";
        }
        
        currentMethod = new MyMethod(name, accessString, desc, signature);
        
        if (exceptions != null && exceptions.length > 0) {
        	for (int i = 0; i < exceptions.length; i++) {
        		currentMethod.addException(exceptions[i]);
        	}
        }

        return this;
    }

    @Override
    public void visitClassEnd() {
    	log(INFO, "visitClassEnd");
    }

    // ------------------------------------------------------------------------
    // Annotations
    // ------------------------------------------------------------------------

    @Override
    public void visit(final String name, final Object value) {
    	log(WARNING, "visit");
    }

    @SuppressWarnings("unused")
	private void visitInt(final int value) {
    	log(WARNING, "visitInt");
    }

    @SuppressWarnings("unused")
    private void visitLong(final long value) {
    	log(WARNING, "visitLong");
    }

    @SuppressWarnings("unused")
    private void visitFloat(final float value) {
    	log(WARNING, "visitFloat");
    }

    @SuppressWarnings("unused")
    private void visitDouble(final double value) {
    	log(WARNING, "visitDouble");
    }

    @SuppressWarnings("unused")
    private void visitChar(final char value) {
    	log(WARNING, "visitChar");
    }

    @SuppressWarnings("unused")
    private void visitShort(final short value) {
    	log(WARNING, "visitShort");
    }

    @SuppressWarnings("unused")
    private void visitByte(final byte value) {
    	log(WARNING, "visitByte");
    }

    @SuppressWarnings("unused")
    private void visitBoolean(final boolean value) {
    	log(WARNING, "visitBoolean");
    }

    @SuppressWarnings("unused")
    private void visitString(final String value) {
    	log(WARNING, "visitString");
    }

    @SuppressWarnings("unused")
    private void visitType(final Type value) {
    	log(WARNING, "visitType");
    }

    @Override
    public void visitEnum(final String name, final String desc,
            final String value) {
    	log(WARNING, "visitEnum");
    }

    @Override
    public MyTextifier visitAnnotation(final String name, final String desc) {
    	log(WARNING, "visitAnnotation");
        return this;
    }

    @Override
    public MyTextifier visitArray(final String name) {
    	log(WARNING, "visitArray");
        return this;
    }

    @Override
    public void visitAnnotationEnd() {
    	log(WARNING, "visitAnnotationEnd");
    }

    // ------------------------------------------------------------------------
    // Fields
    // ------------------------------------------------------------------------

    @Override
    public MyTextifier visitFieldAnnotation(final String desc,
            final boolean visible) {
    	log(WARNING, "visitFieldAnnotation");
    	return this;
    }

    @Override
    public void visitFieldAttribute(final Attribute attr) {
    	log(WARNING, "visitFieldAttribute");
    }

    @Override
    public void visitFieldEnd() {
    	log(INFO, "visitFieldEnd");
    }

    // ------------------------------------------------------------------------
    // Methods
    // ------------------------------------------------------------------------

    @Override
    public MyTextifier visitAnnotationDefault() {
    	log(WARNING, "visitAnnotationDefault");
        return this;
    }

    @Override
    public MyTextifier visitMethodAnnotation(final String desc,
            final boolean visible) {
    	log(WARNING, "visitMethodAnnotation");
    	return this;
    }

    @Override
    public MyTextifier visitParameterAnnotation(final int parameter,
            final String desc, final boolean visible) {
    	log(WARNING, "visitParameterAnnotation");
        return this;
    }

    @Override
    public void visitMethodAttribute(final Attribute attr) {
    	log(WARNING, "visitParameterAttribute");
    }

    @Override
    public void visitCode() {
    	log(INFO, "visitCode");
    }

    @Override
    public void visitFrame(final int type, final int nLocal,
            final Object[] local, final int nStack, final Object[] stack) {
    	log(INFO, "visitFrame");
    }

    @Override
    public void visitInsn(final int opcode) {
        currentMethod.addLine(OPCODES[opcode]);
    }

    @Override
    public void visitIntInsn(final int opcode, final int operand) {
        String line = OPCODES[opcode] + " ";
        line += (opcode == Opcodes.NEWARRAY ? TYPES[operand] : Integer.toString(operand)).toString();
        currentMethod.addLine(line);
    }

    @Override
    public void visitVarInsn(final int opcode, final int var) {
        currentMethod.addLine(OPCODES[opcode] + " " + var);
    }

    @Override
    public void visitTypeInsn(final int opcode, final String type) {
        currentMethod.addLine(OPCODES[opcode] + " " + type);
    }

    @Override
    public void visitFieldInsn(final int opcode, final String owner,
            final String name, final String desc) {
        String line = OPCODES[opcode] + " ";
        line += owner + "." + name + " : " + desc; 
        currentMethod.addLine(line);
    }

    @Override
    public void visitMethodInsn(final int opcode, final String owner,
            final String name, final String desc) {
        String line = OPCODES[opcode] + " ";
        line += owner + "." + name + " " + desc;
        currentMethod.addLine(line);
    }

    @Override
    public void visitInvokeDynamicInsn(String name, String desc, Handle bsm,
            Object... bsmArgs) {
        String line = "INVOKEDYNAMIC " + name + " " + desc + " [";
        line += bsm.toString();
        line += "// arguemnts:";
        if (bsmArgs.length == 0) {
            line += " none";
        } else {
            for (int i = 0; i < bsmArgs.length; i++) {
                Object cst = bsmArgs[i];
                if (cst instanceof String) {
                    line += (String) cst;
                } else if (cst instanceof Type) {
                    line += ((Type) cst).getDescriptor() + ".class";
                } else if (cst instanceof Handle) {
                    line += cst;
                } else {
                    line += cst;
                }
                line += ", ";
            }
        }
        line += "]";
        currentMethod.addLine(line);
    }

    @Override
    public void visitJumpInsn(final int opcode, final Label label) {
        // labels can change, so they are not added to the method body
        currentMethod.addLine(OPCODES[opcode]);
    }

    @Override
    public void visitLabel(final Label label) {
        // labels can change, so they are not added to the method body
    }

    @Override
    public void visitLdcInsn(final Object cst) {
        String line = "LDC ";
        if (cst instanceof String) {
        	line += cst;
        } else if (cst instanceof Type) {
            line += ((Type) cst).getDescriptor() + ".class";
        } else {
        	line += cst;
        }
        currentMethod.addLine(line);
    }

    @Override
    public void visitIincInsn(final int var, final int increment) {
        currentMethod.addLine("IINC " + var + " " + increment);
    }

    @Override
    public void visitTableSwitchInsn(final int min, final int max,
            final Label dflt, final Label... labels) {
    	// label not added because they change
        String line = "TABLESWITCH";
        for (int i = 0; i < labels.length; ++i) {
        	line += (min + i) + ": LABEL";
        }
        line += "default: LABEL";
        
        currentMethod.addLine(line);
    }

    @Override
    public void visitLookupSwitchInsn(final Label dflt, final int[] keys,
            final Label[] labels) {
    	// label not added because they change
        String line = "LOOKUPSWITCH";
        for (int i = 0; i < labels.length; ++i) {
        	line += keys[i] + ": LABEL";
        }
        line += "default: LABEL";
        currentMethod.addLine(line);
    }

    @Override
    public void visitMultiANewArrayInsn(final String desc, final int dims) {
        currentMethod.addLine("MULTIANEWARRAY " + desc + " " + dims);
    }

    @Override
    public void visitTryCatchBlock(final Label start, final Label end,
            final Label handler, final String type) {
    	// Label not added because they change
        String line = "TRYCATCHBLOCK ";
        line += "LABEL ";
        line += "LABEL ";
        line += "LABEL ";
        line += type;
        
        currentMethod.addLine(line);
    }

    @Override
    public void visitLocalVariable(final String name, final String desc,
            final String signature, final Label start, final Label end,
            final int index) {
    	// Label not added because they change
        String line = "LOCALVARIABLE " + name + " ";
        line += desc + " ";
        line += "LABEL ";
        line += "LABEL ";
        line += index;
        currentMethod.addLine(line);
    }

    @Override
    public void visitLineNumber(final int line, final Label start) {
    	log(INFO, "visitLineNumber");
    }

    @Override
    public void visitMaxs(final int maxStack, final int maxLocals) {
        currentMethod.addLine("MAXSTACK = " + maxStack);
        currentMethod.addLine("MAXLOCALS = " + maxLocals);
    }

    @Override
    public void visitMethodEnd() {
    	methods.put(currentMethod.getKey(), currentMethod);
    }

    // ------------------------------------------------------------------------
    // Common methods
    // ------------------------------------------------------------------------

    /**
     * Prints a disassembled view of the given annotation.
     * 
     * @param desc
     *            the class descriptor of the annotation class.
     * @param visible
     *            <tt>true</tt> if the annotation is visible at runtime.
     * @return a visitor to visit the annotation values.
     */
    public MyTextifier visitAnnotation(final String desc, final boolean visible) {
    	log(WARNING, "visitAnnotation");
        return this;
    }

    /**
     * Prints a disassembled view of the given attribute.
     * 
     * @param attr
     *            an attribute.
     */
    public void visitAttribute(final Attribute attr) {
    	log(WARNING, "visitAttribute");
    }

    // ------------------------------------------------------------------------
    // Utility methods
    // ------------------------------------------------------------------------

    /**
     * Creates a new TraceVisitor instance.
     * 
     * @return a new TraceVisitor.
     */
    protected MyTextifier createTextifier() {
        return new MyTextifier();
    }

    private String accessToString(final int access) {
    	String result = "";
        if ((access & Opcodes.ACC_PUBLIC) != 0) {
            result += "public ";
        }
        if ((access & Opcodes.ACC_PRIVATE) != 0) {
            result += "private ";
        }
        if ((access & Opcodes.ACC_PROTECTED) != 0) {
            result += "protected ";
        }
        if ((access & Opcodes.ACC_FINAL) != 0) {
            result += "final ";
        }
        if ((access & Opcodes.ACC_STATIC) != 0) {
            result += "static ";
        }
        if ((access & Opcodes.ACC_SYNCHRONIZED) != 0) {
            result += "synchronized ";
        }
        if ((access & Opcodes.ACC_VOLATILE) != 0) {
            result += "volatile ";
        }
        if ((access & Opcodes.ACC_TRANSIENT) != 0) {
            result += "transient ";
        }
        if ((access & Opcodes.ACC_ABSTRACT) != 0) {
            result += "abstract ";
        }
        if ((access & Opcodes.ACC_STRICT) != 0) {
            result += "strictfp ";
        }
        if ((access & Opcodes.ACC_SYNTHETIC) != 0) {
            result += "synthetic ";
        }
        if ((access & Opcodes.ACC_ENUM) != 0) {
            result += "enum ";
        }
        return result;
    }

    public void printMethods() {
    	for (MyMethod method : methods.values()) {
    		System.out.println(method.toString());
    	}
    }
    
    private void log(int level, String method) {
    	if (!logs.contains(method)) {
    		// warn only once per method
			logs.add(method);
			if (level == INFO) {
				System.out.println("INFO: " + method
						+ " not implemented on purpose");
			} else if (level == WARNING) {
				System.out.println(">>>>>>>> WARNING: " + method
						+ " not implemented");
			}
		}
	}
   
    abstract class Body {
    	String name, access, descr;
    	
    	public Body(String name, String access, String descr) {
    		this.name = name;
    		this.access = access;
    		this.descr = descr;
    	}
    	
    	abstract String getKey();
    	
    	public String getName() {
    		return name;
    	}
    }
    
    class MyField extends Body {
    	
    	public MyField(String name, String access, String descr) {
    		super(name, access, descr);
    	}
    	
    	public String getKey() {
    		return name;
    	}
    	
    	public Boolean equals(MyField other) {
    		Boolean result = false;
    		if (getKey().equals(other.getKey())) {
    			if (access.equals(other.access) && descr.equals(other.descr)) {
    				result = true;
    			}
    		}
    		return result;
    	}
    }
    
    class MyMethod extends Body {
    	String signature;
    	ArrayList<String> exceptions = new ArrayList<String>();
    	StringBuilder body = new StringBuilder();
    	
    	public MyMethod(String name, String access, String descr, String signature) {
    		super(name, access, descr);
    		this.signature = signature;
    	}
    	
    	public String getKey() {
    		return name + descr + ": " + signature;
    	}
    	
    	public void addException(String exception) {
    		exceptions.add(exception);
    	}
    	
    	public void addLine(String line) {
    		body.append(line + "\n");
    	}
    	
    	@Override
    	public String toString() {
    		return body.toString();
    	}
    	
    	public Boolean equals(MyMethod other) {
    		Boolean result = false;
    		if (getKey().equals(other.getKey())) {
    			if (access.equals(other.access) && body.toString().equals(other.body.toString())) {
    				result = true;
    			}
    		}
    		return result;
    	}
    }
}
