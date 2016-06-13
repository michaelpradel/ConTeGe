package contege.tools;

import java.util.jar.*;
import java.util.*;
import java.io.*;

public class Classes {

 public static List<String> getClasseNamesInPackage
     (String jarName, String packageName){
   ArrayList<String> classes = new ArrayList<String> ();

   packageName = packageName.replaceAll("\\." , "/");
   try{
     JarInputStream jarFile = new JarInputStream
        (new FileInputStream (jarName));
     JarEntry jarEntry;

     while(true) {
       jarEntry=jarFile.getNextJarEntry ();
       if(jarEntry == null){
         break;
       }
       if((jarEntry.getName ().startsWith (packageName)) &&
            (jarEntry.getName ().endsWith (".class")) ) {
         String name = jarEntry.getName().replaceAll("/", "\\.");
         name = name.substring(0, name.length()-6);
         classes.add (name);
       }
     }
   }
   catch( Exception e){
     e.printStackTrace ();
   }
   return classes;
}

public static void main (String[] args){
   assert(args.length >= 1);
   for(int i=0; i< args.length; i++) {
     List<String> list = Classes.getClasseNamesInPackage(args[i], "");
     for(String s : list) System.out.println(s);
   }
  }
}

