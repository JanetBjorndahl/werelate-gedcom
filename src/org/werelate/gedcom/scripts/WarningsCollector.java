package org.werelate.gedcom.scripts;

import org.apache.commons.cli.*;
import org.werelate.util.CountsCollector;
import org.werelate.util.Utils;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.PrintWriter;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

/**
 * Created by IntelliJ IDEA.
 * User: npowell
 * Date: Jan 29, 2007
 * Time: 10:17:57 AM
 * This script tries to collect and aggregate warnings produced by the
 * Uploader
 */
public class WarningsCollector {
   private static final Pattern pWarningMessage = Pattern.compile("Dallan:\\S+: ([^\"]+?)((\"[^\"]+\"|F\\d+)([^\"]*?))*$");
   private static final Pattern pUnrecognizedTag = Pattern.compile("Unexpected tag inside(.+)$");
   public static void main (String [] args) throws ParseException, Exception
   {
      Options opt = new Options();
      opt.addOption("w", true, "warn.log file name");
      opt.addOption("h", false, "Print out help information");

      BasicParser parser = new BasicParser();
      CommandLine cl = parser.parse(opt, args);
      if (cl.hasOption("h") || !cl.hasOption("w"))
      {
          System.out.println("Produces a summary of the warnings from a GedcomUpload warn.log");
          HelpFormatter f = new HelpFormatter();
          f.printHelp("OptionsTip", opt);
      }
      else
      {
         BufferedReader in = new BufferedReader(new FileReader (cl.getOptionValue("w")));
         String line;
         CountsCollector cc = new CountsCollector();
         CountsCollector tagCollector = new CountsCollector();
         Matcher m = pWarningMessage.matcher("foo");
         Matcher m2 = pUnrecognizedTag.matcher("bar");

         while ((line = in.readLine()) != null)
         {
            m2.reset(line);
            if (m2.find())
            {
               tagCollector.add(m2.group(1));
            } else
            {
               m.reset(line);
               if (m.find())
               {
                  line = m.group(1);
                  if (!Utils.isEmpty(m.group(4)))
                  {
                     line += m.group(4);
                  }
                  cc.add(line);
               }
               else
               {
                  System.err.println("Line was not understood: " + line);
               }
            }
         }

         cc.writeSorted(false, 1, new PrintWriter(System.out));
         tagCollector.writeSorted(false, 1, new PrintWriter(System.out));
      }
   }
}
