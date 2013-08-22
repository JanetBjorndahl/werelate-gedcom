package org.werelate.gedcom;

import junit.framework.TestCase;

import java.util.Properties;
import java.io.FileInputStream;

import org.werelate.util.PageEdit;

/**
 * Created by IntelliJ IDEA.
 * User: npowell
 * Date: Feb 9, 2007
 * Time: 11:48:02 AM
 * To change this template use File | Settings | File Templates.
 */
public class TestSpecificUploader extends TestCase {
   Uploader uploader;
   Properties properties;
   protected void setUp() throws Exception {
      super.setUp();
      //PageEdit.setLoginRequired(false);
      properties = new Properties();
      properties.load(new FileInputStream("../conf/GedcomUpload/mytesting.properties"));
      //properties.load(new FileInputStream("../conf/GedcomUpload/localtesting.properties"));
      //properties.load(new FileInputStream("conf/GedcomUpload/testing.properties"));
      uploader = new Uploader(properties);
      uploader.setUnitTesting(true);
      uploader.setStubMatching(true);
   }

   private static final Integer [] TEST_IDS = {
          13
   };

   public void testSpecific() throws Exception {
      //uploader.executeQuery("DELETE FROM familytree_gedcom");
      //uploader.executeQuery("ALTER TABLE familytree_gedcom AUTO_INCREMENT=0");
      //uploader.executeQuery("DELETE FROM familytree_gedcom_data WHERE fg_id = " + id);

      for (int id : TEST_IDS)
      {
         // TODO: add a test for excluding sources
         uploader.executeQuery("UPDATE familytree_gedcom SET fg_status = " + Uploader.STATUS_CREATE_PAGES +
               " WHERE fg_id = " + id);
         //uploader.executeQuery("INSERT INTO familytree_gedcom VALUES ("
         //      + id + ", 2, 'Hutchinson.ged', " + Uploader.STATUS_UPLOADED + ", '', '" +
         //      Uploader.generateDateString() + "', 1, 'Dallan')");
         //uploader.executeQuery("INSERT INTO familytree_gedcom_data VALUES (" +
         //      + id + ", 'S1674', 1, 0, 0, '','')");
         //uploader.executeQuery("INSERT INTO familytree_gedcom_data VALUES (" +
         //      + id + ", 'S1684', 1, 0, 0, '','')");
         /*uploader.executeQuery("INSERT INTO familytree_gedcom_data VALUES (" +
               + id + ", 'I125', 0, 0, 0, '','')");
         uploader.executeQuery("INSERT INTO familytree_gedcom_data VALUES (" +
               + id + ", 'I111', 1, 0, 0, '','')");
         uploader.executeQuery("INSERT INTO familytree_gedcom_data VALUES (" +
               + id + ", 'F4', 0, 0, 0, '','')");
         uploader.executeQuery("INSERT INTO familytree_gedcom_data VALUES (" +
               + id + ", 'F11', 1, 0, 0, '','')");
         uploader.executeQuery("INSERT INTO familytree_gedcom_data VALUES (" +
               + id + ", 'F12', 0, 0, 1, 'Edward Osborne and Jane Hinson (1)','Edward Osborne and Jane Hinson (1)')");
         uploader.executeQuery("INSERT INTO familytree_gedcom_data VALUES (" +
               + id + ", 'WRP022', 0, 0, 1, 'Blatchington (near Brighton), Sussex, England','')");*/
      }
      //uploader.updateGedcom(Uploader.STATUS_ERROR, 22, "Families not included in gedcom");
      uploader.getConn().setAutoCommit(true);

      uploader.setIgnoreUnexpectedTags(true);
      //uploader.setShouldCheckOverlap(false);
      System.out.println("About to loop.");
      try
      {
         uploader.loop();
         System.in.read();
      } catch (Exception e) {
         System.err.println("There was a problem while uploading");
         e.printStackTrace();
         throw e;
      }
      finally {
         if (uploader != null)
         {
            uploader.close();
         }
      }
   }
}
