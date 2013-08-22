package org.werelate.gedcom.scripts;

import org.werelate.util.Utils;

import java.sql.*;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

/**
 * Created by IntelliJ IDEA.
 * User: npowell
 * Date: Feb 26, 2007
 * Time: 2:47:46 PM
 * To change this template use File | Settings | File Templates.
 */
public class FixEscapedData {
   private static final Pattern pReplaceSourceCitation = Pattern.compile("&lt;source_citation.+?\"/&gt;");
   public static void main (String [] args) throws ClassNotFoundException,
         InstantiationException, IllegalAccessException, SQLException
   {
      Class.forName("com.mysql.jdbc.Driver").newInstance();
      Connection conn = DriverManager.getConnection("jdbc:mysql://malachi/wikidb", "root", "myfolgp");
      Statement s = conn.createStatement();
      Statement s2 = conn.createStatement();
      if(s.execute("select * from familytree_data where fd_data like '%&lt;source_citation%';"))
      {
         ResultSet rs = s.getResultSet();
         Statement updateStatement = conn.createStatement();
         while(rs.next())
         {
            Matcher m = pReplaceSourceCitation.matcher(rs.getString(4));
            StringBuffer replacement = new StringBuffer();
            while (m.find())
            {
               System.out.println(m.group(0));
               m.appendReplacement(replacement, Utils.unencodeXML(m.group(0)));
            }
            m.appendTail(replacement);
            s2.execute("update familytree_data set fd_data = '" + replacement.toString() +
                      "' where fd_tree_id = " + rs.getInt(1) +
                      " and fd_namespace = " + rs.getInt(2) +
                      " and fd_title = '" + rs.getString(3) + "'");
         }
         updateStatement.close();
      }
      s.close();
      s2.close();
      conn.close();
   }
}
