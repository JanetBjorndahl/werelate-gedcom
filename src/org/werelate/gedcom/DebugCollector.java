package org.werelate.gedcom;

import org.apache.commons.cli.*;
import org.werelate.util.CountsCollector;

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
public class DebugCollector {
   private static final String sIgnoringTag = ".*Ignoring tag inside(.+)$";
   public static void main (String [] args) throws ParseException, Exception
   {
      Options opt = new Options();
      opt.addOption("d", true, "debug.log file name");
      opt.addOption("l", true, "Level of statements to print out");
      opt.addOption("h", false, "Print out help information");

      BasicParser parser = new BasicParser();
      CommandLine cl = parser.parse(opt, args);
      if (cl.hasOption("h") || !cl.hasOption("d") || !cl.hasOption("l"))
      {
          System.out.println("Produces a summary of the warnings from a GedcomUpload warn.log");
          HelpFormatter f = new HelpFormatter();
          f.printHelp("OptionsTip", opt);
      }
      else
      {
         BufferedReader in = new BufferedReader(new FileReader (cl.getOptionValue("d")));
         String line;
         CountsCollector tagCollector = new CountsCollector();
         Pattern pIgnoringTag = Pattern.compile('^' + cl.getOptionValue("l") + sIgnoringTag);
         Matcher m2 = pIgnoringTag.matcher("bar");

         while ((line = in.readLine()) != null)
         {
            m2.reset(line);
            if (m2.find())
            {
               tagCollector.add(m2.group(1));
            }
         }
         tagCollector.writeSorted(false, 10, new PrintWriter(System.out));
      }
   }
}
