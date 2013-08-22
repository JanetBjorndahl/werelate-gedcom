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
public class CharacterInTitleInXMLFinder {
   private static Pattern pUnderscore = Pattern.compile(
         "((title|child_of_family)=&quot;([^&]|&amp;|&apos;|&lt;|&gt;)*)\\.(([^&]|&amp;|&apos;|&lt;|&gt;)*&quot;)"
   );
   public static void main (String [] args) throws FileNotFoundException, IOException {
      BufferedReader in = new BufferedReader (new FileReader ("/home/npowell/gedcoms/output/28.xml"));
      String line;
      Matcher m = pUnderscore.matcher("foo");
      int i=1;
      while ((line = in.readLine()) != null)
      {
         m.reset(line);
         if(m.find())
         {
            System.out.println("Line " + i + ": " + line);
         }
         i++;
      }
      in.close();
   }
}
