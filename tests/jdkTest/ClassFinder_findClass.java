package jdkTest;

import com.sun.beans.finder.ClassFinder;

public class ClassFinder_findClass {

	public static void main(String[] args) throws Throwable {
		Class cls = ClassFinder.findClass("sun.misc.Cache");
//		Class cls = ClassFinder.findClass("bla");
		System.out.println(cls);
	}

}
