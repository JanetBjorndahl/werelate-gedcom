package org.werelate.gedcom.scripts;

import java.sql.*;

/**
 * Created by IntelliJ IDEA.
 * User: npowell
 * Date: Feb 8, 2007
 * Time: 3:59:06 PM
 * To change this template use File | Settings | File Templates.
 */
public class DeleteExtraWSpaces {
    public static void main (String [] args) throws ClassNotFoundException,
         InstantiationException, IllegalAccessException, SQLException
   {
      Class.forName("com.mysql.jdbc.Driver").newInstance();
      Connection conn = DriverManager.getConnection("jdbc:mysql://malachi/wikidb", "root", "myfolgp");
      Statement s = conn.createStatement();
      if(s.execute("DELETE from titleids where ti_title LIKE '% %' and NOT ti_title = 'John Spriggs'"))
      {

      }
      s.close();
      conn.close();
   }
}
