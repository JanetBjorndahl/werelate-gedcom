package org.werelate.gedcom.scripts;

import org.apache.commons.cli.*;
import org.werelate.gedcom.Uploader;

import javax.xml.xpath.XPathFactoryConfigurationException;
import java.sql.SQLException;
import java.io.IOException;
import java.io.FileInputStream;
import java.util.Properties;

/**
 * Created by IntelliJ IDEA.
 * User: npowell
 * Date: Apr 17, 2007
 * Time: 9:30:52 AM
 * To change this template use File | Settings | File Templates.
 */
public class MeasureOverlap {
   public static void main (String [] args)
         throws ParseException,
         ClassNotFoundException,
         InstantiationException,
         IllegalAccessException,
         SQLException,
         IOException,
         XPathFactoryConfigurationException
   {
      Options opt = new Options();
      opt.addOption("p", true, "Location of the properties file to use.");
      opt.addOption("a", true, "Gedcom id of the file gedcom b is to be compared to.");
      opt.addOption("b", true, "Gedcom id which is to be compared against GEDCOM a to determine overlap.");
      opt.addOption("h", false, "Print out help information");
      BasicParser bp = new BasicParser();
      CommandLine cl = bp.parse(opt, args);

      if (cl.hasOption("h") || !(cl.hasOption("p") && cl.hasOption("a") && cl.hasOption("b")))
      {
         System.out.println("Prints out the percentage of names in GEDCOM b that overlap with names in GEDCOM a.");
         HelpFormatter f = new HelpFormatter();
         f.printHelp("OptionsTip", opt);
      }else
      {
         Properties props = new Properties();
         props.load(new FileInputStream(cl.getOptionValue("p")));
         Uploader uploader = new Uploader(props);
         int percentOfBOverlapsWithA = uploader.getOverlapPercentage(Integer.parseInt(cl.getOptionValue("a")),
               Integer.parseInt(cl.getOptionValue("b")));
         System.out.println(percentOfBOverlapsWithA + "% of " + cl.getOptionValue("b") +
                           ".ged overlaps with " + cl.getOptionValue("a") + ".ged");
      }
   }
}
