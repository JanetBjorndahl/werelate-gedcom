package org.werelate.gedcom;

import org.werelate.util.Utils;

import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;

/**
 * Created by IntelliJ IDEA.
 * User: npowell
 * Date: Jan 31, 2007
 * Time: 1:44:16 PM
 * Represents an arbitrary GEDCOM tag which is put on
 * a stack in the Gedcom object
 */
public class Tag {
   private String name = null;
   private String content = "";
   private String id = null;
   // Line number that the
   // tag started on.
   private int lineNum = -1;

   /**
    *
    * @return the line number associated with the tag
    */
   public int getLineNum() {
      return lineNum;
   }

   /**
    *
    * @return the GEDCOM ID associated with this tag, if applicable
    */
   public String getID() {
      return id;
   }

   /**
    *
    * @param name TAG name from the GEDCOM
    * @param id of the tag, if applicable
    * @param lineNum of the start of the tag
    */
   public Tag(String name, String id, int lineNum) {
      setName(name);
      this.id = id;
      this.lineNum = lineNum;
   }

   /**
    *
    * @return the GEDCOM tag name
    */
   public String getName() {
      return name;
   }

   /**
    *
    * @param name GEDCOM tag name
    */
   public void setName(String name) {
      this.name = name;
   }

   public void append(String contentToAppend) {
      content += contentToAppend;
   }

   /**
    *
    * @param contentToAppend
    * @param delim delimiter to use when appending
    */
   public void append(String contentToAppend, String delim) {
      content = simpleAppend(content, contentToAppend, delim);
   }

   // Helper function used to append the string
   // This is different than a standard append because it checks
   // to see if the content already contains contentToAppend
   private static String simpleAppend(String content, String contentToAppend, String delim) {
      String result;
      if (!Utils.isEmpty(contentToAppend))
      {
         if (Utils.isEmpty(content) || content.contains(contentToAppend))
         {
            result = contentToAppend;
         } else
         {
            result = content + delim + contentToAppend;
         }
      } else
      {
         result = content;
      }
      return result;
   }

   /**
    *
    * @param contentToAppend
    * @param delim delimiter
    */
   public void prepend(String contentToAppend, String delim) {
      content = simpleAppend(contentToAppend, content, delim);
   }

   /**
    *
    * @return content contained in the tag
    */
   public String getContent() {
      return content;
   }

   /**
    *
    * @param name
    * @return true if the name of this tag is the same as parameter name, false otherwise
    */
   public boolean eq(String name) {
      return this.name.equals(name);
   }

   /**
    * defines the tags' translations in the case that they are being ignored.
    */
   public static final Map<String, String> IGNORED_TAG_LABELS = new HashMap<String, String>();
   static
   {
      IGNORED_TAG_LABELS.put("ABBR", "Abbreviation");
      IGNORED_TAG_LABELS.put("ADDR", "Address");
      IGNORED_TAG_LABELS.put("ADDRESS", "Address");
      IGNORED_TAG_LABELS.put("ADOP", "Adopted");
      IGNORED_TAG_LABELS.put("ADOPTION", "Adopted");
      IGNORED_TAG_LABELS.put("ADR1", "Address 1");
      IGNORED_TAG_LABELS.put("ADR2", "Address 2");
      IGNORED_TAG_LABELS.put("_AKAN", "Alternate name");
      IGNORED_TAG_LABELS.put("AGE", "Age");
      IGNORED_TAG_LABELS.put("_ALT_BIRTH", "Alternate Birth");
      IGNORED_TAG_LABELS.put("ALIV", "Alive");
      IGNORED_TAG_LABELS.put("_BIB", "Bibliography");
      IGNORED_TAG_LABELS.put("_BIBL", "Bibliography version of source");
      IGNORED_TAG_LABELS.put("_BOTTOM", "Bottom");
      IGNORED_TAG_LABELS.put("_CAT", "Category");
      IGNORED_TAG_LABELS.put("CAUS", "Cause");
      IGNORED_TAG_LABELS.put("CAUSE", "Cause");
      IGNORED_TAG_LABELS.put("CEME", "Cemetary");
      IGNORED_TAG_LABELS.put("_CEN", "CENSUS");
      IGNORED_TAG_LABELS.put("CHAN", "Changed");
      IGNORED_TAG_LABELS.put("CHAR", "Character encoding");
      IGNORED_TAG_LABELS.put("CITY", "City");
      IGNORED_TAG_LABELS.put("_CLIP", "Image clip");
      IGNORED_TAG_LABELS.put("CNTC", "Name of contact person");
      IGNORED_TAG_LABELS.put("_CONFIDENCE", "Confidence");
      IGNORED_TAG_LABELS.put("COLO", "Color");
      IGNORED_TAG_LABELS.put("COPR", "Copyright");
      IGNORED_TAG_LABELS.put("CORP", "Corporation");
      IGNORED_TAG_LABELS.put("CREM", "Cremated");
      IGNORED_TAG_LABELS.put("CTRY", "Country");
      IGNORED_TAG_LABELS.put("DATA", "Data");
      IGNORED_TAG_LABELS.put("_DATE2", "Date 2");
      IGNORED_TAG_LABELS.put("DATE", "Date");
      IGNORED_TAG_LABELS.put("DATV", "Date viewed");
      IGNORED_TAG_LABELS.put("DESC", "Description");
      IGNORED_TAG_LABELS.put("_Description2", "Description 2");
      IGNORED_TAG_LABELS.put("DEST", "Destination");
      IGNORED_TAG_LABELS.put("DETA", "Details");
      IGNORED_TAG_LABELS.put("_DETS", "Unknown data");
      IGNORED_TAG_LABELS.put("EDTR", "Editor");
      IGNORED_TAG_LABELS.put("_EMAIL", "E-Mail");
      IGNORED_TAG_LABELS.put("EMAIL", "E-Mail");
      IGNORED_TAG_LABELS.put("EMAL", "E-Mail");
      IGNORED_TAG_LABELS.put("EYES", "Eyes");
      IGNORED_TAG_LABELS.put("FAM", "Family");
      IGNORED_TAG_LABELS.put("FAMILY", "Family");
      IGNORED_TAG_LABELS.put("FAX", "Fax");
      IGNORED_TAG_LABELS.put("FILE", "File");
      IGNORED_TAG_LABELS.put("FILM", "Film");
      IGNORED_TAG_LABELS.put("FILN", "Film number");
      IGNORED_TAG_LABELS.put("FORM", "Image form");
      IGNORED_TAG_LABELS.put("FOST", "Foster child");
      IGNORED_TAG_LABELS.put("GEDC", "Gedcom");
      IGNORED_TAG_LABELS.put("_GODP", "Godparents");            
      IGNORED_TAG_LABELS.put("HAIR", "Hair");
      IGNORED_TAG_LABELS.put("HEAL", "Health");
      IGNORED_TAG_LABELS.put("HEIG", "Height");
      IGNORED_TAG_LABELS.put("_HME", "Home person:");
      IGNORED_TAG_LABELS.put("HM", "Home");
      IGNORED_TAG_LABELS.put("_HTITL", "Husband title");
      IGNORED_TAG_LABELS.put("HUSB", "Husband");
      IGNORED_TAG_LABELS.put("HUSBAND", "Husband");
      IGNORED_TAG_LABELS.put("_HUSB", "Husband");
      IGNORED_TAG_LABELS.put("IDNO", "I.D./E-Mail");
      IGNORED_TAG_LABELS.put("_IFLAGS", "Flags");
      IGNORED_TAG_LABELS.put("INTE", "Interviewed");
      IGNORED_TAG_LABELS.put("INTV", "Interviewer");
      IGNORED_TAG_LABELS.put("_ITALIC", "Italicized");
      IGNORED_TAG_LABELS.put("LANG", "Language");
      IGNORED_TAG_LABELS.put("LATI", "Latitude");
      IGNORED_TAG_LABELS.put("_LEFT", "Left");
      IGNORED_TAG_LABELS.put("LOC", "Location");
      IGNORED_TAG_LABELS.put("_LOCL", "Local");
      IGNORED_TAG_LABELS.put("LONG", "Longitude");
      IGNORED_TAG_LABELS.put("LVG", "Living");
      IGNORED_TAG_LABELS.put("MAP", "Map");
      IGNORED_TAG_LABELS.put("_MASTER", "Master");
      IGNORED_TAG_LABELS.put("_MEN", "Marriage end");
      IGNORED_TAG_LABELS.put("MSTAT", "Marriage status");
      IGNORED_TAG_LABELS.put("_NAME", "Name");
      IGNORED_TAG_LABELS.put("NAME", "Name");
      IGNORED_TAG_LABELS.put("NAMR", "Religious name");
      IGNORED_TAG_LABELS.put("NCHI", "Number of children");
      IGNORED_TAG_LABELS.put("_NMAR", "Number of marriages");
      IGNORED_TAG_LABELS.put("NMR", "Number of marriages");
      IGNORED_TAG_LABELS.put("NOTEImmigrated", "Immigration note");
      IGNORED_TAG_LABELS.put("NOTE", "Note");
      IGNORED_TAG_LABELS.put("OFFI", "Officiator");
      IGNORED_TAG_LABELS.put("_OTHER", "Other");
      IGNORED_TAG_LABELS.put("_OVER", "Override");
      IGNORED_TAG_LABELS.put("OWNR", "Ownder");
      IGNORED_TAG_LABELS.put("PAGE", "Page");
      IGNORED_TAG_LABELS.put("_PAREN", "Parenthesized");
      IGNORED_TAG_LABELS.put("PERI", "Time Period");
      IGNORED_TAG_LABELS.put("PHON", "Phone");
      IGNORED_TAG_LABELS.put("PHONE", "Phone");
      IGNORED_TAG_LABELS.put("PLAC", "Place");
      IGNORED_TAG_LABELS.put("PLACE", "Place");
      IGNORED_TAG_LABELS.put("_PLAC", "Place");
      IGNORED_TAG_LABELS.put("POST", "Postal code");
      IGNORED_TAG_LABELS.put("_PRIMARY", "Primary");
      IGNORED_TAG_LABELS.put("_PRIM", "Primary");
      IGNORED_TAG_LABELS.put("_PRIORITY", "Priority");
      IGNORED_TAG_LABELS.put("PRTY", "Priority");
      IGNORED_TAG_LABELS.put("QUAY", "Quality");
      IGNORED_TAG_LABELS.put("REFN", "Reference number");
      IGNORED_TAG_LABELS.put("REGI", "Registration number");
      IGNORED_TAG_LABELS.put("REPO", "Repository");
      IGNORED_TAG_LABELS.put("REPOSITORY", "Repository");
      IGNORED_TAG_LABELS.put("_RDATE", "Return date");
      IGNORED_TAG_LABELS.put("_RIGHT", "Right");
      IGNORED_TAG_LABELS.put("_RIN", "Record I.D. Number");
      IGNORED_TAG_LABELS.put("_RPT_PHRS", "Report phrase");
      IGNORED_TAG_LABELS.put("_SCBK", "Scrapbook");
      IGNORED_TAG_LABELS.put("_SCRAPBOOK", "Scrapbook");
      IGNORED_TAG_LABELS.put("_SDATE", "Secondary date");
      IGNORED_TAG_LABELS.put("_SEPR", "Separated");
      IGNORED_TAG_LABELS.put("SLGC", "Child sealing");
      IGNORED_TAG_LABELS.put("_SORT", "Sort by");
      IGNORED_TAG_LABELS.put("SOUR", "Source");
      IGNORED_TAG_LABELS.put("SOURCE", "Source");
      IGNORED_TAG_LABELS.put("_SSHOW", "Image slidshow");
      IGNORED_TAG_LABELS.put("STAE", "State");
      IGNORED_TAG_LABELS.put("STATDATE", "Status Date");
      IGNORED_TAG_LABELS.put("_STAT", "Status");
      IGNORED_TAG_LABELS.put("STAT", "Status");
      IGNORED_TAG_LABELS.put("_STIME", "Slide show time");
      IGNORED_TAG_LABELS.put("_SUB", "Subtitle");
      IGNORED_TAG_LABELS.put("SUBM", "Submission");
      IGNORED_TAG_LABELS.put("SUBN", "Submission note");
      IGNORED_TAG_LABELS.put("_SUBQ", "Short version of source");
      IGNORED_TAG_LABELS.put("TEMP", "Temple");
      IGNORED_TAG_LABELS.put("TEXT", "Text");
      IGNORED_TAG_LABELS.put("TIME", "Time");
      IGNORED_TAG_LABELS.put("_TODO", "TODO");
      IGNORED_TAG_LABELS.put("_TOP", "Top");
      IGNORED_TAG_LABELS.put("_TYPE", "Type");
      IGNORED_TAG_LABELS.put("TYPE", "Type");
      IGNORED_TAG_LABELS.put("UMAR", "Unmarried");
      IGNORED_TAG_LABELS.put("_UNITS", "Units");
      IGNORED_TAG_LABELS.put("_URL", "URL");
      IGNORED_TAG_LABELS.put("URL", "URL");
      IGNORED_TAG_LABELS.put("VER", "Version");
      IGNORED_TAG_LABELS.put("_VERI", "Verified");
      IGNORED_TAG_LABELS.put("VERS", "Version");            
      IGNORED_TAG_LABELS.put("VOL", "Volume");
      IGNORED_TAG_LABELS.put("WIFE", "WIFE");
      IGNORED_TAG_LABELS.put("_WIFE", "Wife");
      IGNORED_TAG_LABELS.put("_WTITL", "Wife title");
      IGNORED_TAG_LABELS.put("WWW", "URL");
   }

   // If tags within the current tag were ignored, then
   // the ignoredBucket is filled with the translated version
   // of those tags. This bucket will hopefully then be
   // eaten by something
   private List <String> ignoredBucket = new ArrayList<String>();

   public void addToIgnoredBucket(String newIgnoreStr)
   {
      ignoredBucket.add(newIgnoreStr);
   }

   public void addAllIgnoredBucket(List <String> newBucket)
   {
      ignoredBucket.addAll(newBucket);
   }

   public List <String> getIgnoredBucket() {
      return ignoredBucket;
   }
}