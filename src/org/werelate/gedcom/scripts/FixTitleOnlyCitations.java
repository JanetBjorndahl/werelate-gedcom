package org.werelate.gedcom.scripts;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.werelate.gedcom.GedcomXML;
import org.xml.sax.SAXException;

import javax.xml.xpath.XPathException;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by IntelliJ IDEA.
 * User: dallan
 */
public class FixTitleOnlyCitations {
   static final String MSG = "Cannot update existing user-edited page:";
   static final Pattern EXTENDED_CHAR = Pattern.compile("\\\\x(..)\\\\x(..)");
   static final Pattern CITATION_ONLY = Pattern.compile("<source_citation id=\"(S\\d+)\">(.*?)</source_citation>");

   // apache logger writes extended characters as \xnn\xnn
   private static String unescape(String s) throws UnsupportedEncodingException {
      StringBuffer buf = new StringBuffer();
      Matcher m = EXTENDED_CHAR.matcher(s);
      while (m.find()) {
         byte[] b = new byte[2];
         b[0] = (byte)Integer.parseInt(m.group(1),16);
         b[1] = (byte)Integer.parseInt(m.group(2),16);
         m.appendReplacement(buf, new String(b, "utf-8"));
      }
      m.appendTail(buf);
      return buf.toString();
   }

   public static void main(String[] args) throws XPathException, IOException, SAXException, GedcomXML.GedcomXMLException {
      String logFilename = args[0];
      String gedcomFilename = args[1];

      // read logfile to get pages that must be manually edited
      Set<String> titles = new HashSet<String>();
      BufferedReader reader = new BufferedReader(new FileReader(logFilename));
      while (reader.ready()) {
         String line = reader.readLine();
         int pos = line.indexOf(MSG);
         if (pos >= 0) {
            String title = unescape(line.substring(pos + MSG.length()).trim());
            titles.add(title);
         }
      }

      // read gedcom
      GedcomXML gedXml = new GedcomXML();
      gedXml.parse(gedcomFilename);
      for (Node page : gedXml.getPages()) {
         // get title
         String title = ((Element)page).getAttribute("title");
         String namespace = ((Element)page).getAttribute("namespace");
         String prefix = "";
         if ("108".equals(namespace)) {
            prefix = "Person:";
         }
         else if ("110".equals(namespace)) {
            prefix = "Family:";
         }
         else if ("112".equals(namespace)) {
            prefix = "MySource:";
         }
         else if ("104".equals(namespace)) {
            prefix = "Source";
         }
         title = prefix+title;

         // if title is one of the ones we're looking for
         if (titles.contains(title)) {
            // get content
            String content = "";
            int len = page.getChildNodes().getLength();
            for (int i = 0; i < len; i++) {
               Node child = page.getChildNodes().item(i);
               if (child.getNodeName().equals("content")) {
                  content = child.getTextContent();
                  break;
               }
            }

            // are there citation-only source citations?
            Matcher m = CITATION_ONLY.matcher(content);
            List<String> citations = new ArrayList<String>();
            while (m.find()) {
               citations.add(m.group(1)+"|"+m.group(2));
            }
            if (citations.size() > 0) {
               System.out.println("title="+title);
               for (String citation : citations) {
                  String[] fields = citation.split("\\|");
                  System.out.println("  id="+fields[0]+" text="+fields[1]);
                  String[] lines = content.split("\\n");
                  for (String line : lines) {
                     if (!line.startsWith("<source_citation") && line.matches(".*\\b"+fields[0]+"\\b.*")) {
                        System.out.println("        "+line);
                     }
                  }
               }
            }
         }
      }
   }
}
