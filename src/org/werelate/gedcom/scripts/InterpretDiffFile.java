package org.werelate.gedcom.scripts;

import org.werelate.util.Utils;

import java.io.*;
import java.util.ArrayList;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.net.URLEncoder;

/**
 * Created by IntelliJ IDEA.
 * User: npowell
 * Date: Feb 10, 2007
 * Time: 9:34:35 AM
 * To change this template use File | Settings | File Templates.
 */
public class InterpretDiffFile {
   private static final Pattern pDiffLineNumbers = Pattern.compile("(\\d+)(,\\d+)?[ac](\\d+)(,\\d+)?");
   private static final Pattern pPageTitle = Pattern.compile("<page[^>]+namespace=\"([^\"]+)\"[^>]+title=\"([^\"]+) \\(\\d+\\)?\"");
   public static void main (String [] args) throws FileNotFoundException, IOException
   {
      String xmlPath = args[0];
      String diffPath = args[1];
      String outHtmlPath = args[2];
      String ldsOutPath = args[3];

      File xmlDir = new File(xmlPath);
      Matcher mDiffLineNumbers = pDiffLineNumbers.matcher("blah");
      Matcher mPageTitle = pPageTitle.matcher("blah");
      for (File xmlFile : xmlDir.listFiles())
      {
         if (xmlFile.getName().endsWith(".xml"))
         {
            // Now let's read the file into a String array so that we
            // can access all of the lines.
            ArrayList<String> xmlFileContents  = new ArrayList<String>();
            String line;
            BufferedReader in = new BufferedReader(new FileReader(xmlFile));
            while((line = in.readLine()) != null)
            {
               xmlFileContents.add(line);
            }
            in.close();
            // Now let's start reading in the diff file
            String fn = xmlFile.getName().substring(0, xmlFile.getName().length() - 4);
            // Now let's open the output HTML file
            PrintWriter out = new PrintWriter(new FileWriter(outHtmlPath + '/' + fn + ".html"));
            PrintWriter ldsOut = new PrintWriter(new FileWriter(ldsOutPath + '/' + fn + ".html"));
            in = new BufferedReader(new FileReader(diffPath + '/' + fn + ".diff"));
            String rightSectionContent = "";
            String leftSectionContent = "";
            String overallDiff = "";
            int firstLine = -1;
            while ((line = in.readLine()) != null)
            {
               mDiffLineNumbers.reset(line);
               if (mDiffLineNumbers.find())
               {
                  BufferedReader leftIn = new BufferedReader(new StringReader(leftSectionContent));
                  BufferedReader rightIn = new BufferedReader(new StringReader(rightSectionContent));
                  boolean foundMismatch = false;
                  String leftLine, rightLine;
                  while ((leftLine = leftIn.readLine()) != null &&
                        (rightLine = rightIn.readLine()) != null)
                  {
                     if (!removeJunk(leftLine).trim().equals(removeJunk(rightLine).trim()))
                     {
                        foundMismatch = true;
                        break;
                     }
                  }
                  if (foundMismatch)
                  {
                     // Then we need to end the previous section.
                     // We need to find the previous line that has the page title.
                     String pageTitle = null;
                     String namespace = null;
                     for (int i = firstLine; i >= 0; i--)
                     {
                        mPageTitle.reset(xmlFileContents.get(i));
                        if (mPageTitle.find())
                        {
                           namespace = mPageTitle.group(1);
                           pageTitle = mPageTitle.group(2);
                           break;
                        }
                     }
                     if (!Utils.isEmpty(pageTitle) && !Utils.isEmpty(namespace))
                     {
                        if (rightSectionContent.contains("<data>"))
                        {
                           print(namespace, pageTitle, ldsOut, rightSectionContent, overallDiff);
                        } else
                        {
                           print(namespace, pageTitle, out, rightSectionContent, overallDiff);
                        }
                     } else
                     {
                        // Then we need to end the previous section.
                        // We need to find the previous line that has the page title.
                        pageTitle = null;
                        namespace = null;
                        for (int i = firstLine; i >= 0; i--)
                        {
                           mPageTitle.reset(xmlFileContents.get(i));
                           if (mPageTitle.find())
                           {
                              namespace = mPageTitle.group(1);
                              pageTitle = mPageTitle.group(2);
                              break;
                           }
                        }
                        throw new RuntimeException("I wasn't able to find a pageTitle for this diff section in " + fn);
                     }
                  }
                  rightSectionContent = "";
                  leftSectionContent = "";
                  overallDiff = "";
                  firstLine = Integer.parseInt(mDiffLineNumbers.group(1));
               } else
               {
                  overallDiff += line + '\n';
                  if (line.startsWith(">"))
                  {
                     rightSectionContent += line + '\n';
                  } else if (line.startsWith("<"))
                  {
                     leftSectionContent += line + '\n';
                  }
               }
            }
            in.close();
            out.close();
            ldsOut.close();
         }
      }
   }

   private static void print(String namespace, String pageTitle, PrintWriter out, String diffSectionContent, String overallDiff) throws UnsupportedEncodingException {
      String strUrl = "http://www.werelate.org/wiki/Special:GotoPage?namespace=" + namespace +
            "&pagetitle=" + URLEncoder.encode(pageTitle, "UTF-8") + "&goto=Go+to+page";
      out.println("<br><a href=\"" + strUrl + "\">" + pageTitle + "</a><br>\n");
      out.print(Utils.encodeXML(removeJunk(diffSectionContent)).replaceAll("\n", "<br>\n"));
      out.print("<hr  width=\"20%\" align=\"left\">\n");
      out.print("<pre>\n");
      out.print(Utils.encodeXML(overallDiff).replaceAll("\n", "<br>\n"));
      out.print("</pre>\n");
   }

   private static String removeJunk(String diffSectionContent) {
      return diffSectionContent.replaceAll("(?<=(^|\n))[<>]", "");
   }
}
