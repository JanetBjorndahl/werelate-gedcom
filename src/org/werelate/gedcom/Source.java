package org.werelate.gedcom;

import org.werelate.util.Utils;
import org.apache.log4j.Logger;

import java.io.PrintWriter;
import java.util.*;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

/**
 * User: npowell
 * Date: Nov 16, 2006
 * Time: 3:00:45 PM
 * Represents a top level source object in the GEDCOM,
 * and a MySource in WeRelate.
 * If the MySource title would be empty,no MySource is created,
 * and the content is put into the source citation when a person or family is printed.
 */
public class Source extends EventContainer {
   private static final Logger logger = Logger.getLogger("org.werelate.gedcom.Source");
   private static final int LARGE_USERNAME_LEN = 40;

   private String Abbreviation = null;
   private String url = null;
   private String author = null;
   private String pubInfo = null;
   private String callNum = null;
   private String type = null;
   private String peri = null;
   private String place = null;
   //private String fromYear = null;
   // We're going to temporarily
   // put the repository ID in the
   // repositoryID until we do the post
   // processing.
   private String repositoryID;
   private String repositoryName;
   private String repAddr;
   private String text;
   private String title;  

   public Source() {
      
   }
   public String getRepositoryName() {
      return repositoryName;
   }

   public void setRepositoryName(String repositoryName) {
      this.repositoryName = Utils.setVal(this.repositoryName,repositoryName);
   }

   private String hashString()
   {
      String rval = Abbreviation + url + author + pubInfo + callNum + type +
            peri + place + repositoryID + repositoryName + repAddr + text + title;
      for (Note.Citation cit : getNoteCitations())
      {
         rval += cit.getId();
      }
      for (String note : getNotes())
      {
         rval += note;
      }
      return rval;
   }
   public int hashCode ()
   {
      return hashString().hashCode();
   }

   public boolean equals(Object obj)
   {
      String otherHashString = ((Source) obj).hashString();
      String thisHashString = hashString();
      return (thisHashString == null && otherHashString == null) || thisHashString.equals(otherHashString);
   }

   /**
    *
    * @return the generated title of the source
    */
   public String getTitle() {
      String result = null;

      if (!Utils.isEmpty(title))
      {
         result = title;
      } else if (!Utils.isEmpty(getAbbreviation()))
      {
         result = getAbbreviation();
      } else if (!Utils.isEmpty(getAuthor()))
      {
         if (!Utils.isEmpty(getPubInfo()))
         {
            result = getAuthor() + ' ' + getPubInfo();
         } else
         {
            result = getAuthor();
         }
      }
//      else if (!Utils.isEmpty(text)) {
//         // use the first line of text
//         String t = text.trim();
//         int pos = t.indexOf('\n');
//         if (pos > 0) {
//            result = t.substring(0, pos);
//         }
//         else {
//            result = t;
//         }
//      }
      else
      {
         result = "";
      }

      return Utils.prepareWikiTitle(result, Utils.MAX_TITLE_LEN - LARGE_USERNAME_LEN); // leave room for "username/" prefix
   }

   /**
    * Sets the title
    * @param title to be set for the source.
    */
   public void setTitle(String title) {
      title = title.trim();
      this.title = title;
   }

   public String getText() {
      return text;
//      if (Utils.isEmpty(text)) return text;
//      // if we used the entire text for the title, don't return it
//      String t = getTitle();
//      if (t.equals(text.trim())) {
//         return "";
//      }
//      else {
//         return text;
//      }
   }

   public void setText(String text) {
      this.text = Utils.setVal(this.text, text);
   }

   public void appendText(String newText)
   {
      text = Uploader.append(text, newText, "\n\n");
   }

   public String getAbbreviation() {
      return Abbreviation;
   }

   public void setAbbreviation(String abbreviation) {
      Abbreviation = Utils.setVal(Abbreviation,abbreviation);
   }

   public String getUrl() {
      return url;
   }

   public void setUrl(String url) {
      this.url = Utils.setVal(this.url,url);
   }

   public String getAuthor() {
      return author;
   }

   public void setAuthor(String author) {
      this.author = Utils.setVal(this.author,author);
   }

   public String getPubInfo() {
      return pubInfo;
   }

   public void setPubInfo(String pubInfo) {
      this.pubInfo = Utils.setVal(this.pubInfo,pubInfo);
   }

   public String getCallNum() {
      return callNum;
   }

   public void setCallNum(String callNum) {
      this.callNum = Utils.setVal(this.callNum, callNum);
   }

   public String getType() {
      return type;
   }

   public void appendType(String type) {
      this.type = Uploader.append(this.type, type, ", ");
   }

   public String getRepositoryID() {
      return repositoryID;
   }

   public void setRepositoryID(String repositoryID) {
      this.repositoryID = Utils.setVal(this.repositoryID,repositoryID);
   }

   /**
    *
    * @return the address contained in the repository address attribute.
    */
   public String getRepAddr() {
      String addr = null;
      if (!Utils.isEmpty(getPlace()))
      {
         if (!Utils.isEmpty(repAddr))
         {
            addr = getPlace() + ' ' + repAddr;
         } else
         {
            addr = getPlace();
         }
      } else
      {
         addr = repAddr;
      }
      return addr != null ? addr.replace("\n", " ") : null;
   }



   public String getPlace() {
      return place;
   }

   private static String [] STR_IGNORE = {
         "FILN", "REGI", "FILE", "_SUBQ", "_BIBL",
         "_BIB", "_SUB",
         "_CONFIDENCE", "EDTR", "QUAY", "OWNR",
         "VOL", "PAGE", "DATV", "CNTC", "SUBM", "_OTHER",
         "DETA", "FILM", "REFN", "INTE", "INTV",
         "DATA", "RIN", "UID", "_UID"
   };
   private static Set<String> SET_IGNORE = new HashSet<String>();
   {
      for (String s : STR_IGNORE)
      {
         SET_IGNORE.add(s);
      }
   }
   public static boolean shouldIgnore(String localName)
   {
      return SET_IGNORE.contains(localName);
   }

   public void appendPlace(String place) {
      this.place = Uploader.append(this.place, place, " ");
   }

   public void appendAddr(String repAddr) {
      this.repAddr = Uploader.append(this.repAddr, repAddr, " ");
   }

   public String getPeri() {
      return peri;
   }

   public void setPeri(String peri) {
      this.peri = peri;
   }

   /**
    * @param gedcom
    * @return true if the Source has a title
    * @throws Gedcom.PostProcessException
    */
   public boolean shouldPrint(Gedcom gedcom) throws Gedcom.PostProcessException {
      return !Utils.isEmpty(getTitle());
//      && !(
//            Utils.isEmpty(getText()) &&
//                  Utils.isEmpty(getAuthor()) &&
//                  Utils.isEmpty(getCallNum()) &&
//                  Utils.isEmpty(getPubInfo())  &&
//                  Utils.isEmpty(getType()) &&
//                  Utils.isEmpty(getUrl()) &&
//                  Utils.isEmpty(getRepositoryID()) &&
//                  Utils.isEmpty(getRepAddr()) &&
//                  Utils.isEmpty(getRepositoryName()) &&
//                  getNotes(gedcom, new LinkedList<Citation>()).size() == 0
//            );
   }

   public String getWikiTitle (Gedcom gedcom) {
      return Utils.prepareWikiTitle(gedcom.getUserName() + '/' +getTitle());
   }

   protected int getNamespace () {
      return Utils.MYSOURCE_NAMESPACE;
   }

   private static final Pattern pGedcomExtensions = Pattern.compile("\\.(ged|gedcom|ftm|ftw|paf)\\b", Pattern.CASE_INSENSITIVE);

   public boolean shouldExclude(Gedcom gedcom) throws Gedcom.PostProcessException {
      // exclude filename-only gedcom files: gedcom extension and < 2 spaces in the title, and no other fields
      if (!Utils.isEmpty(title)) {
         Matcher m = pGedcomExtensions.matcher(title);
         if (m.find() &&
             (title.indexOf(' ') < 0 || title.indexOf(' ', title.indexOf(' ')+1) < 0) &&
              Utils.isEmpty(getText()) &&
              Utils.isEmpty(getAuthor()) &&
              Utils.isEmpty(getCallNum()) &&
              Utils.isEmpty(getPubInfo())  &&
              Utils.isEmpty(getType()) &&
              Utils.isEmpty(getUrl()) &&
              Utils.isEmpty(getRepositoryID()) &&
              Utils.isEmpty(getRepAddr()) &&
              Utils.isEmpty(getRepositoryName()) &&
              getNotes(gedcom, new LinkedList<Citation>()).size() == 0) {
            return true;
         }
      }

      // exclude sources that aren't referenced anywhere
      for (Person person : gedcom.getPeople().values()) {
         if (person.shouldPrint(gedcom)) {
            for (Citation cit : person.getAllCitations(gedcom)) {
               if (getID().equals(cit.getId())) {
                  return false;
               }
            }
         }
      }
      for (Family family : gedcom.getFamilies().values()) {
         if (family.shouldPrint(gedcom)) {
            for (Citation cit : family.getAllCitations(gedcom)) {
               if (getID().equals(cit.getId())) {
                  return false;
               }
            }
         }
      }

      return true;
   }

   /**
    * Prints out the mysource to the XML
    * @param gedcom
    * @param out
    * @param xmlEncode
    * @throws Uploader.PrintException
    * @throws Gedcom.PostProcessException
    */
   public void print(Gedcom gedcom, PrintWriter out, boolean xmlEncode) throws Uploader.PrintException,
         Gedcom.PostProcessException
   {
      //setReservedTitle(Uploader.getDBKey(getWikiTitle(gedcom)));
      startPage(out, gedcom, false, shouldExclude(gedcom), false, false);
      // Done printing the images
      out.print("<content>");
      String outText = "<mysource>\n";
      //outText += Uploader.printTag("from_year", getFromYear());
      outText += Uploader.printTag("title", getTitle());
      outText += Uploader.printTag("url", getUrl());
      outText += Uploader.printTag("abbrev", getAbbreviation());
      outText += Uploader.printTag("author", getAuthor());
      outText += Uploader.printTag("publication_info", getPubInfo());
      outText += Uploader.printTag("call_number", getCallNum());
      outText += Uploader.printTag("type", getType());
      outText += Uploader.printTag("repository_name", getRepositoryName());
      outText += Uploader.printTag("repository_addr", getRepAddr());
      outText += "</mysource>\n";
      String bodyString = "";
      if (!Utils.isEmpty(getPeri()))
      {
         bodyString = appendBreak(bodyString);
         bodyString += "Book/periodical title: " + getPeri();
      }
      ArrayList<Citation> cits = new ArrayList <Citation> ();
      for (String note : getNotes(gedcom, cits))
      {
         bodyString = appendBreak(bodyString);
         bodyString += note;
      }
      if (cits.size() > 0)
      {
         logger.info("Source citations present inside a note inside of a source");
      }
      String text = getText();
      if (!Utils.isEmpty(text))
      {
         bodyString = appendBreak(bodyString);
         bodyString += text;
      }
      outText += Utils.replaceHTMLFormatting(bodyString);
      //outText += "<show_sources_images_notes/>\n";
      if (xmlEncode)
      {
         //outText = Utils.encodeXML(outText);
         outText = encloseInCDATA(outText);
      }
      out.print(outText);
      out.println("</content>");
      out.println("</page>");
   }

   private void appendCitationText(String text, StringBuilder buf) {
      if (!Utils.isEmpty(text)) {
         if (buf.length() > 0) {
            buf.append("\n\n");
         }
         buf.append(text);
      }
   }

   public String getAsCitationText(Gedcom gedcom) throws Gedcom.PostProcessException {
      StringBuilder buf = new StringBuilder();
      appendCitationText(getTitle(), buf);
      appendCitationText(getUrl(), buf);
      appendCitationText(getAbbreviation(), buf);
      appendCitationText(getAuthor(), buf);
      appendCitationText(getPubInfo(), buf);
      appendCitationText(getCallNum(), buf);
      appendCitationText(getType(), buf);
      appendCitationText(getRepositoryName(), buf);
      appendCitationText(getRepAddr(), buf);
      appendCitationText(getPeri(), buf);
      ArrayList<Citation> cits = new ArrayList <Citation> ();
      for (String note : getNotes(gedcom, cits)) {
         appendCitationText(note, buf);
      }
      appendCitationText(getText(), buf);
      return buf.toString();
   }

   private String appendBreak(String bodyString) {
      if (!Utils.isEmpty(bodyString))
      {
         bodyString += "\n\n";
      }
      return bodyString;
   }

   /**
    * Generates the title of the source for when we're
    * printing out a source citation.
    * @param gedcom
    * @param containerID
    * @param citation
    * @return
    * @throws Uploader.PrintException
    * @throws Gedcom.PostProcessException
    */

   /*
   * TODO: We will use this in phase III to generate the source title.
   public String getSourceTitle(Gedcom gedcom, String containerID, Citation citation)
         throws Uploader.PrintException, Gedcom.PostProcessException
   {
      String sourceTitle;
      if (shouldPrint(gedcom))
      {

         sourceTitle = "MySource:" + getReservedTitle();
      } else if (!Utils.isEmpty(getWikiTitle(gedcom)))
      {
         sourceTitle = getTitle();
      } else
      {
         throw new Uploader.PrintException("I don't have a title for source \"" + citation.getId()
            + "\" in person \"" + containerID + "\"", gedcom, getID());
      }
      return sourceTitle;
   } */

   /**
    *
    * @param localName
    * @return true if the localName is an attribute of a source object, false otherwise.
    */
   public static boolean isSubSource(String localName) {
      return localName.equals("ABBR")
            || Gedcom.isTITL(localName)
            || Gedcom.isAUTH(localName)
            || Gedcom.isPUBL(localName)
            || localName.equals("TEXT")
            || Gedcom.isREPO(localName)
            || Gedcom.isCALN(localName)
            || localName.equals("NOTE")
            || localName.equals("TYPE")
            || localName.equals("_TYPE")
            || Gedcom.isMEDI(localName)
            || localName.equals("_MEDI")
            || localName.equals("LOCA")
            || localName.equals("PLAC")
            || localName.equals("DATE")
            || localName.equals("OBJE")
            || localName.equals("URL")
            || localName.equals("PERI");
   }
}
