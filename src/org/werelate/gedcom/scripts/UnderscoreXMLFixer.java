package org.werelate.gedcom.scripts;

import java.io.*;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

/**
 * Created by IntelliJ IDEA.
 * User: npowell
 * Date: Feb 7, 2007
 * Time: 9:22:36 PM
 * To change this template use File | Settings | File Templates.
 */
public class UnderscoreXMLFixer {
   private static Pattern pUnderscore = Pattern.compile(
         "((title|child_of_family)=&quot;([^&]|&amp;|&apos;|&lt;|&gt;)*)_(([^&]|&amp;|&apos;|&lt;|&gt;)*&quot;)"
   );
   public static void main (String [] args) throws FileNotFoundException, IOException {
      BufferedReader in = new BufferedReader (new FileReader ("/home/npowell/gedcoms/output/28.xml.old"));
      PrintWriter out = new PrintWriter(new FileWriter("/home/npowell/gedcoms/output/28.xml.fixed"));
      String line;
      Matcher m = pUnderscore.matcher("foo");
      while ((line = in.readLine()) != null)
      {
         String old;
         do
         {
            old = line;
            m.reset(line);
            line = m.replaceAll("$1 $3");
         } while (!line.equals(old));
         out.println(line);
      }
      in.close();
      out.close();
   }
}
