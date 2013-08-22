package org.werelate.gedcom.scripts;

import java.sql.*;

/**
 * Created by IntelliJ IDEA.
 * User: npowell
 * Date: Feb 8, 2007
 * Time: 11:46:31 AM
 * To change this template use File | Settings | File Templates.
 */
public class FixTitleIDSpaces {

   public static void main (String [] args) throws ClassNotFoundException,
         InstantiationException, IllegalAccessException, SQLException
   {
      Class.forName("com.mysql.jdbc.Driver").newInstance();
      Connection conn = DriverManager.getConnection("jdbc:mysql://malachi/wikidb", "root", "myfolgp");
      Statement s = conn.createStatement();
      if(s.execute("select ti_title, namespace from titleids where ti_title LIKE '% %'"))
      {
         ResultSet rs = s.getResultSet();
         Statement updateStatement = conn.createStatement();
         while(rs.next())
         {
            String title = rs.getString(1);
            int namespace = rs.getInt(2);
            String newTitle = title.replaceAll(" ", "_");
            Statement checkDuplicate = conn.createStatement();
            checkDuplicate.execute("select COUNT(*) from titleids where ti_title = \"" + newTitle +
                  "\" and ti_namespace = " + namespace);
            ResultSet rs2 = checkDuplicate.getResultSet();
            rs2.next();
            int count = rs2.getInt(1);
            if (count == 0)
            {
               updateStatement.execute("update titleids set ti_title = \"" + newTitle + "\" where ti_title = \"" + title + "\"");
            } else
            {

            }
            checkDuplicate.close();            
         }
         updateStatement.close();
      }
      s.close();
      conn.close();
   }
}
