package contege;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.objectweb.asm.ClassReader;

public class MethodCheck {

	private String jarV1, jarV2, cutString;
	private ArrayList<String> differingMethods = new ArrayList<String>();

	public MethodCheck(String jarV1, String jarV2, String cut)
			throws IOException {
		this.jarV1 = jarV1;
		this.jarV2 = jarV2;
		this.cutString = cut.replace(".", "/") + ".class";
	}

	public void check() throws IOException {
		MyTraceClassVisitor cvV1 = analyzeJar(jarV1);
		MyTraceClassVisitor cvV2 = analyzeJar(jarV2);

		for (String key : cvV1.getMethods().keySet()) {
			if (cvV2.getMethods().keySet().contains(key)) {
				if (!cvV1.getMethods().get(key)
						.equals(cvV2.getMethods().get(key))) {
					differingMethods.add(cvV1.getMethods().get(key).getName());
				}
			}
		}

	}

	private MyTraceClassVisitor analyzeJar(String jarFileName)
			throws IOException {
		JarFile jarFile = new JarFile(jarFileName);
		Enumeration<? extends JarEntry> entries = jarFile.entries();
		MyTraceClassVisitor cv = null;
		while (entries.hasMoreElements()) {
			JarEntry entry = entries.nextElement();
			if (entry.getName().equals(this.cutString)) {
				InputStream stream = jarFile.getInputStream(entry);
				try {
					ClassReader cr = new ClassReader(stream);
					PrintWriter printWriter = new PrintWriter(System.out);
					cv = new MyTraceClassVisitor(null, new MyTextifier(),
							printWriter);
					cr.accept(cv, 0);
				} finally {
					stream.close();
				}
			}
		}
		return cv;
	}

	public ArrayList<String> getDifferingMethods() {
		return differingMethods;
	}
}
