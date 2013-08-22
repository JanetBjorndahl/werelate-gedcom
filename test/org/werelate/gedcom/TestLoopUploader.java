package org.werelate.gedcom;

import junit.framework.TestCase;

import java.util.Properties;
import java.io.FileInputStream;

/**
 * Created by IntelliJ IDEA.
 * User: npowell
 * Date: Feb 9, 2007
 * Time: 11:48:02 AM
 * To change this template use File | Settings | File Templates.
 */
public class TestLoopUploader extends TestCase {
   Uploader uploader;
   Properties properties;
   protected void setUp() throws Exception {
      super.setUp();
      properties = new Properties();
      properties.load(new FileInputStream("conf/GedcomUpload/100.properties"));
      uploader = new Uploader(properties);
   }

   private static int startIndex = 46;
   private static int endIndex = 98;

   public void testSpecific() throws Exception {
      uploader.executeQuery("DELETE FROM familytree_gedcom");
      uploader.executeQuery("ALTER TABLE familytree_gedcom AUTO_INCREMENT=0");

      for (int id = startIndex; id <= endIndex; id++)
      {
         uploader.executeQuery("INSERT INTO familytree_gedcom VALUES ("
               + id + ", 1, '', 1, '', '" +
               Uploader.generateDateString() + "', 1)");
      }
      uploader.updateGedcom(Uploader.STATUS_ERROR, 22, "Families not included in gedcom");
      uploader.getConn().setAutoCommit(true);

      //uploader.setUnitTesting(true);
      //uploader.setShouldStopWhenWithoutGedcom(true);
      uploader.loop();
   }
}
