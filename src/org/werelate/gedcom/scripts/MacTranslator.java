package org.werelate.gedcom.scripts;

import org.werelate.util.Utils;

import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.sql.*;
import java.io.*;
import java.net.URL;
import java.net.MalformedURLException;

/**
 * Created by IntelliJ IDEA.
 * User: npowell
 * Date: Feb 27, 2007
 * Time: 11:01:26 AM
 * To change this template use File | Settings | File Templates.
 */
public class MacTranslator {
   public static void main (String [] args) throws MalformedURLException, IOException
   {
      BufferedReader in = new BufferedReader(new InputStreamReader(
            new FileInputStream(args[0]),"x-MacRoman"));
      PrintWriter out = new PrintWriter(args[1]);
      String line;
      while ((line = in.readLine()) != null)
      {
         out.println(line);
      }
      out.close();
   }
}
