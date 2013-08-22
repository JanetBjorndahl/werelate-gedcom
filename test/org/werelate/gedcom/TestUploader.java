package org.werelate.gedcom;

import junit.framework.TestCase;

import java.util.Properties;
import java.io.*;

import org.werelate.util.PageEdit;

/**
 * Created by IntelliJ IDEA.
 * User: npowell
 * Date: Feb 1, 2007
 * Time: 7:23:34 PM
 * To change this template use File | Settings | File Templates.
 */
public class TestUploader extends TestCase {
   Uploader uploader;
   Properties properties;
   protected void setUp () throws Exception {
      super.setUp();
      properties = new Properties();
      properties.load(new FileInputStream("conf/GedcomUpload/testing.properties"));
      //properties.load(new FileInputStream("conf/GedcomUpload/diff.properties"));
      PageEdit.setLoginRequired(false);
      uploader = new Uploader(properties);
      uploader.setIgnoreUnexpectedTags(true);
      uploader.setShouldStopWhenWithoutGedcom(true);
      uploader.setShouldCheckOverlap(false);
      uploader.executeQuery("DELETE FROM familytree_gedcom");
      uploader.executeQuery("ALTER TABLE familytree_gedcom AUTO_INCREMENT=0");
      uploader.setUnitTesting(true);
      File inputDir = new File(properties.getProperty("gedcom_dir"));
      int startID = 53;

      String outputDirName = properties.getProperty("xml_output");
      // We need to delete the files that are there so
      // that we don't compare an older version of the file in the
      // case of a failure
      File outputDir = new File(outputDirName);
      if (outputDir.isDirectory())
      {
         for (File f : outputDir.listFiles())
         {
            f.delete();
         }
      }

      if (inputDir.isDirectory())
      {
         for (File fn : inputDir.listFiles())
         {
            if (fn.getName().endsWith(".ged"))
            {
               int fileID = Integer.parseInt(fn.getName().substring(0, fn.getName().length() - 4));
               if (fileID >= startID)
               {
                  uploader.executeQuery("INSERT INTO familytree_gedcom VALUES (" +
                     fileID + ", 2, 'mygedcom.ged', 1, '', '" +
                     Uploader.generateDateString() + "', 1, 'Dallan')");
                  uploader.loop();
                  File outputFile = new File(getXMLFN(outputDirName, fileID));
                  String standardDirName = properties.getProperty("standard_dir");
                  File standardFile = new File(getXMLFN(standardDirName, fileID));
                  if (outputFile.exists() && standardFile.exists())
                  {
                     System.out.println("Comparing " + outputFile.getName());
                     StringBuffer outputBuf = new StringBuffer();
                     BufferedReader in = new BufferedReader (new FileReader(outputFile));
                     String line;
                     while ((line = in.readLine()) != null)
                     {
                        outputBuf.append(line).append('\n');
                     }
                     in.close();
                     StringBuffer standardBuf = new StringBuffer();
                     in = new BufferedReader(new FileReader(standardFile));
                     while ((line = in.readLine()) != null)
                     {
                        standardBuf.append(line).append('\n');
                     }
                     in.close();
                     assertEquals(standardBuf.toString(), outputBuf.toString());
                  } else if (!outputFile.exists())
                  {
                     throw new Exception ("Output file \"" + outputFile.getName() + "\" does not exist");
                  } else if (!standardFile.exists())
                  {
                     throw new Exception ("Standard file \"" + standardFile.getName() + "\" does not exist");
                  }
               }
            }
         }
      } else
      {
         throw new Exception ("Specified gedcom input directory " + properties.getProperty("gedcom_dir") +
               " is not a directory");
      }
   }

   private String getXMLFN(String dir, int fileID) {
      return dir + '/' + fileID + ".xml";
   }

   public void testRunUploader()
   {

   }
   /*public void testRunUploader() throws IOException, Uploader.GenerateException
   {


      // Now let's compare all of the file outputs with the standard in the
      // the standard directory.

      File outputDir = new File(properties.getProperty("xml_output"));
      String standardDir = properties.getProperty("standard_dir");

      for (File outputFile : outputDir.listFiles())
      {
         if (outputFile.getName().endsWith(".xml"))
         {
            File standardFile = new File(standardDir + '/' + outputFile.getName());
            if (standardFile.exists())
            {

            }
         }
      }
   } */
}
