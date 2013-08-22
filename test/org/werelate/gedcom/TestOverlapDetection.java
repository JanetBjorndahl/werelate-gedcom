package org.werelate.gedcom;

import junit.framework.TestCase;

import java.util.Properties;
import java.io.FileInputStream;
import java.sql.Statement;
import java.sql.SQLException;
import java.sql.ResultSet;

/**
 * Created by IntelliJ IDEA.
 * User: npowell
 * Date: Mar 9, 2007
 * Time: 1:19:03 PM
 * To change this template use File | Settings | File Templates.
 */
public class TestOverlapDetection extends TestCase {
   Uploader uploader;
   Properties properties;
   private void insert(int fileID, int tree_id, int status)
   {
      String query = "INSERT INTO familytree_gedcom VALUES (" +
                  fileID + ", " + tree_id + ", 'Filename', " + status + ", '', '" +
                  Uploader.generateDateString() + "', 1, 'Penelope Blake')";
      uploader.executeQuery(query);
   }
   protected void setUp (int status) throws Exception {
      super.setUp();
      properties = new Properties();
      properties.load(new FileInputStream("conf/GedcomUpload/elijah.properties"));
      uploader = new Uploader(properties);
      uploader.setShouldStopWhenWithoutGedcom(true);
      uploader.executeQuery("DELETE FROM familytree_gedcom");
      uploader.executeQuery("ALTER TABLE familytree_gedcom AUTO_INCREMENT=0");
      uploader.setUnitTesting(true);
   }

   private void assertGedcomStatus (int fg_id, int fg_status) throws SQLException
   {
      Statement s = uploader.getConn().createStatement();
      s.execute("select fg_status from familytree_gedcom where fg_id = " + fg_id);
      ResultSet rs = s.getResultSet();
      if (rs.next())
      {
         assertEquals(rs.getInt(1), fg_status);
      } else
      {
         throw new RuntimeException("Could not find the GEDCOM to check status for.");
      }
      s.close();
   }

   public void testMajorOverlap () throws Exception {
      this.setUp(1);
      insert(536, 744, 4);
      insert(541, 744, 1);
      uploader.loop();
      assertGedcomStatus(541, 101);
   }

   public void testTobiasOverlap () throws Exception {
      this.setUp(105);
      insert(69, 1, 4);
      insert(76, 2, 105);
      uploader.loop();
      assertGedcomStatus(76, 101);
   }

   public void testSubsetOverlap () throws Exception {
      this.setUp(1);
      insert(60, 1, 3);
      insert(61, 1, 1);
      uploader.loop();
      // When we're in unit testing mode,
      // we end up leaving the gedcom in a
      // status 2 state
      assertGedcomStatus(61, 101);
   }

   public void testNonOverlap () throws Exception {
      this.setUp(1);
      insert(70, 1, 3);
      insert(71, 1, 1);
      uploader.loop();
      // When we're in unit testing mode,
      // we end up leaving the gedcom in a
      // status 2 state
      assertGedcomStatus(71, 101);
   }
}
