package org.werelate.gedcom;

import org.werelate.util.Utils;
import org.werelate.util.PlaceUtils;
import org.werelate.util.ElementWriter;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.Collection;
import java.io.PrintWriter;
import java.io.FileWriter;
import java.io.IOException;

import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathConstants;

/**
 * Created by IntelliJ IDEA.
 * User: npowell
 * Date: Jan 19, 2007
 * Time: 6:30:51 PM
 * Represents the Name of a person
 */
public class Name extends ReferenceContainer {
   private String given =null;
   private String surname = null;
   private String suffix = null;
   private String prefix = null;
   private String description = null;

   public void addNoteCitations(ReferenceContainer container) {
      for (Note.Citation nc : container.getNoteCitations())
      {
         addNoteCitation(nc);
      }
   }

   /**
    *
    * @param note to be added
    */
   public void addNote(Note note)
   {
      throw new GedcomElementWriter.ElementWriterException("Shouldn't add note to name.");
   }

   /**
    * Used separately when parsing a GEDCOM,
    * because sometimes a Title tag appears directly
    * within an INDI tag as opposed to within the name.
    * These titles are later copied over to the appropriate
    * Name object before printing.
    */
   public static class Title extends ReferenceContainer {
      String title = null;
      //Citation cit = null;

      protected String getParentName() throws Gedcom.PostProcessException {
         return null; // We should never have Name as parent to an image
      }

      public String getTitle() {
         return title;
      }

      public void setTitle(String title) {
         this.title = title;
      }
   }

   public void eatIgnoredBucket(Collection<String> ignoredBucket) {
      String newNoteString = "";
      for (String s : ignoredBucket)
      {
         newNoteString = Uploader.append(newNoteString, s, "\n");
      }
      this.addNote(null, newNoteString);
      ignoredBucket.clear();
   }

   // Used for determining the parent to an image.
   // Since we never have images inside of NAME tags, this returns null
   protected String getParentName() throws Gedcom.PostProcessException {
      return null; // We should have Name as a parent to an image
   }

   /**
    * Copy constructor of Name
    * @param other Name to be copied from
    */
   public Name(Name other)
   {
      super(other);
      setGiven(other.getGiven());
      setSurname(other.getSurname());
      setSuffix(other.getSuffix());
      setPrefix(other.getPrefix());
      setDescription(other.getDescription());
      setTagName(other.getTagName());
      setType(other.getType());
   }

   /**
    * default constructor
    */
   public Name() {

   }

   private static final XPath xpe = Uploader.xpe;
   private static XPathExpression nameExpression;
   private static XPathExpression givenExpression;
   private static XPathExpression surnameExpression;
   private static XPathExpression titlePrefixExpression;
   private static XPathExpression titleSuffixExpression;
   static
   {
      try
      {
         nameExpression = xpe.compile("person/name");
         givenExpression = xpe.compile("@given");
         surnameExpression = xpe.compile("@surname");
         titlePrefixExpression = xpe.compile("@title_prefix");
         titleSuffixExpression = xpe.compile("@title_suffix");
      } catch (XPathExpressionException e)
      {

      }
   }

   public void parseFromPersonXML(Document personContentXML, boolean living) throws XPathExpressionException
   {
      Node name = (Node) nameExpression.evaluate(personContentXML, XPathConstants.NODE);
      if (living) {
         setGiven("Living");
      }
      if (name != null) {
         if (!living) {
            setGiven((String) givenExpression.evaluate(name, XPathConstants.STRING));
            setPrefix((String) titlePrefixExpression.evaluate(name, XPathConstants.STRING));
            setSuffix((String) titleSuffixExpression.evaluate(name, XPathConstants.STRING));
         }
         setSurname((String) surnameExpression.evaluate(name, XPathConstants.STRING));
      }
   }

   /**
    *
    * @return text which will be put in the Name's description field
    */
   public String getDescription() {
      return description;
   }

   /**
    *
    * @return the name suffix
    */
   public String getSuffix() {
      return suffix;
   }

   /**
    *
    * @return the given names
    */
   public String getGiven() {
      return given;
   }

   /**
    *
    * @return the surname
    */
   public String getSurname() {
      return surname;
   }

   /**
    *
    * @return the surname of the title
    */
   public String getTitleSurname() {
      if (!Utils.isEmpty(getSurname()))
      {
         return cleanName(getSurname().trim());
      } else
      {
         return null;
      }
   }

   public String getPrefix() {
      return prefix;
   }

   public void setDescription(String description) {
      this.description = description;
   }

   public void setPrefix(String prefix) {
      this.prefix = prefix;
   }

   /**
    * Only sets the given name if
    * the current version is empty or null
    *
    * If the passed in given name is rejected,
    * then we return false. Otherwise, we
    * return true
    * @param given
    */
   public boolean setGiven(String given) {
      if (Utils.isEmpty(getGiven()))
      {
         if (given != null)
         {
            given = given.trim();
            if (!Utils.isEmpty(given))
            {
               this.given = clean(eatPrefix(given));
               return true;
            }
         }
      }
      return false;
   }

   public void clearGiven() {
      given = null;
   }

   /**
    * Only sets the surname if the
    * current surname is empty or null
    *
    * If the passed in surname is rejected,
    * then we return false. Otherwise, we
    * return true
    * @param surname
    */
   public boolean setSurname(String surname) {
      if (Utils.isEmpty(getSurname()) &&
            !Utils.isEmpty(surname))
      {
         surname = stripSlashes(surname);
         if (surname != null)
         {
            surname = surname.trim();
            if (!Utils.isEmpty(surname))
            {
               this.surname = eatPrefixSuffix(surname);
               return true;
            }
         }
      }
      return false;
   }

   public void clearSurname() {
      surname = null;
   }

   private String stripSlashes(String val) {
      if (Name.surroundedBySlash(val) && val.length() > 1)
      {
         val =val.substring(1, val.length()-1);
      }
      return val;
   }

   public void setSuffix(String suffix) {
      this.suffix = suffix;
   }

   public  void appendPrefix(String newPrefix)
   {
      setPrefix(Uploader.append(getPrefix(), newPrefix, " "));
   }

   private void appendSuffix(String newSuffix)
   {
      setSuffix(Uploader.append(getSuffix(), newSuffix, " "));
   }
   private static final String postamble = "(\\.|\\b))(\\s*[,\\-])*\\s*";
   private static final Pattern pPrefix = Pattern.compile(
         "\\b((Mrs?|Ms|Miss|Sir|Dr|Capt|Captain|Sgt|Sergeant|Private|Prvt|Pvt|Lieutenant|Rev|Reverend|Esq|Esquire)" + postamble,
         Pattern.CASE_INSENSITIVE);
   private static final Pattern pSuffix = Pattern.compile(
         "\\b((Jr|Sr|I{1,3}V?|V|VI{1,3}|I{1,2}X|X|M\\.?D|Ph\\.?D|R\\.?N|Esq|Esquire)" + postamble,
         Pattern.CASE_INSENSITIVE);
   private static final Pattern pSlashSurname = Pattern.compile(
         "^([^/]*?)\\s*/([^/]*)/(\\s*,)?\\s*([^/]*)"
   );

   public AlternateName setName(String name, Gedcom gedcom) {
      name = name.trim();
      AlternateName rval = null;
      if (!Utils.isEmpty(name))
      {
         // We're going to do some preprocessing in order to
         // extract prefixes and suffixes.
         StringBuffer buf;
         Matcher m;

         // Now we're going to try to extract any surname
         // which happens to be surrounded by "//".
         // If we can, then we will assume that it
         // is the surname, and the rest is the given name.
         m = pSlashSurname.matcher(name);
         if (m.find())
         {
            String newSurname = this.eatPrefixSuffix(m.group(2));
            name = clean(this.eatPrefix(m.group(1))) + (Utils.isEmpty(name = this.eatPrefixSuffix(m.group(4))) ? "" : ' ' + name);
            // Name is now the given name
            rval = specialSet(name, newSurname, rval);
         } else
         {
            // Parse out the prefixes and the suffixes:
            name = eatPrefixSuffix(name);
            // First I want to check for the case of
            // having the surname first, followed by a
            // comma, followed by the given name.
            String [] split = name.split("\\s*,\\s*");
            if (split.length > 1)
            {
               if (split.length > 2)
               {
                  int firstCommaLoc = name.indexOf(',');
                  rval = specialSet(name.substring(firstCommaLoc + 1).trim(),
                        name.substring(0, firstCommaLoc).trim(), rval);
               } else
               {
                  // We will assume that the words before the comma
                  // is the surname, and the words after, the given.
                  rval = specialSet(split[1].trim(), split[0].trim(), rval);
               }
            } else
            {
               // We are going to split based on whitespace.
               split = name.split("\\s+");
               if (split.length > 1)
               {
                  // We will assume the last word is the
                  // surname.
                  String newSurname = split[split.length-1];
                  String given = split[0];
                  for (int i = 1; i < split.length -1; i++)
                  {
                     given += ' ' + split[i];
                  }
                  rval = specialSet(given, newSurname, rval);
               } else
               {
                  // If the word is all caps, assume that
                  // it is the surname.
                  if (name.toUpperCase().equals(name))
                  {
                     rval = specialSet(null, name, rval);
                  } else
                  {
                     // Assume that the name is a given name.
                     rval = specialSet(name,null, rval);
                  }
               }
            }
         }
      }
      return rval;
   }

   public String eatPrefixSuffix(String name) {
      name = eatPrefix(name);
      name = eatSuffix(name);
      return clean(name);
   }

   private static String clean(String s)
   {
      return s.replaceAll("\\(\\)|\\{\\}|\\[\\]|<>", "").trim();
   }

   private String eatSuffix(String name) {

      StringBuffer buf;
      buf = new StringBuffer();
      Matcher m = pSuffix.matcher(name);
      while (m.find())
      {
         appendSuffix(m.group(1));
         m.appendReplacement(buf, "");
      }
      m.appendTail(buf);
      name = buf.toString();
      return name;
   }

   private String eatPrefix(String name) {
      StringBuffer buf = new StringBuffer();
      Matcher m = pPrefix.matcher(name);
      while (m.find())
      {
         appendPrefix(m.group(1));
         m.appendReplacement(buf, "");
      }
      m.appendTail(buf);
      name = buf.toString();
      return name;
   }

   private boolean specialStrEquals(String a, String b)
   {
      if (a != null)
      {
         return a.trim().toLowerCase().equals(b.trim().toLowerCase());
      } else
      {
         return true;
      }
   }

   private boolean containsAllTokens(String toTokenize)
   {
      String [] split = toTokenize.split("\\s+");
      for (String token : split)
      {
         if (!((getGiven() != null && getGiven().contains(token)) ||
               (getSurname() != null && getSurname().contains(token)) ||
               (getPrefix() != null && getPrefix().contains(token)) ||
               (getSuffix() != null && getSuffix().contains(token))))
         {
            return false;
         }
      }
      return true;
   }

   // Used to set the parts of the name in the case that we're parsing out
   // the whole name from the contents of a NAME object, (not from the GIVN or SURN
   // fields, but in the name content).
   //
   // This is primarily used when the already have just a given name set in this
   // name, and we want to move it to an alternate name for the person while setting
   // the newGiven and the newSurname to be the values for this name
   private AlternateName specialSet(String newGiven, String newSurname, AlternateName rval) {
      if (Utils.isEmpty(getSurname()) && !Utils.isEmpty(getGiven()) &&
               !Utils.isEmpty(newGiven) && !Utils.isEmpty(newSurname) &&
               !newGiven.trim().equals(getGiven().trim()))
      {
         if (rval == null)
         {
            rval = new AlternateName();
         }
         rval.setGiven(getGiven());
         this.given = newGiven;
         setSurname(newSurname);
      } else
      {
         if (newGiven != null &&
               !setGiven(newGiven) &&
               !specialStrEquals(getGiven(), newGiven) &&
               !containsAllTokens(newGiven))
         {
            if (rval == null)
            {
               rval = new AlternateName();
            }
            rval.setGiven(newGiven);
         }

         if (newSurname != null &&
               !setSurname(newSurname) &&
               !specialStrEquals(getSurname(), stripSlashes(newSurname)) &&
               !containsAllTokens(newSurname))
         {
            if(rval == null)
            {
               rval = new AlternateName();
            }
            rval.setSurname(newSurname);
         }
      }
      return rval;
      /*if (!setGiven(newGiven))
      {
         if (!setSurname(newSurname))
         {
            boolean surnameEquals = specialStrEquals(getSurname(), stripSlashes(newSurname));
            boolean givenEquals = specialStrEquals(getGiven(), newGiven);

            if (!givenEquals || !surnameEquals)
            {
               if (rval == null)
               {
                  rval = new AlternateName();
               }
               if(!givenEquals)
               {
                  rval.setGiven(newGiven);
               }
               if (!surnameEquals)
               {
                  rval.setSurname(newSurname);
               }
            }
         }
         // If there is already a given name without a surname, and the given name
         // is different then the new one we're about to set, then we will assume
         // that it is a nickname for the person, and we will move the current
         // given name to an alternate name before we set the new given name.
         else
      } else
      {
         if (!setSurname(newSurname) && !newSurname.equals(getSurname()))
         {
            if (rval == null)
            {
               rval = new AlternateName();
            }
            rval.setSurname(surname);
         }
      }
      return rval;*/
   }

   private static boolean surroundedBySlash(String name) {
      try
      {
         return name.length() > 0 && name.charAt(0) == '/' && name.length() > 1 &&  name.charAt(name.length() -1) == '/';
      } catch (NullPointerException e)
      {
         throw e;
      }
   }

   /**
    *
    * @return the first given name
    */
   public String getFirstGiven() {
      String name = getGiven();
      if (!Utils.isEmpty(name))
      {
         name = name.trim().split("\\s+")[0];
      }
      return cleanName(name);
   }

   // Characters that should be cleaned up from a name, especially before using
   // it in a wiki title
   private static final String CHAR_CLASS_NAME = "?~!@#$%^\\&*\\.()\\_+=/*<>{}\\[\\];:\"\\\\,/|";
   // These are characters that are allowed in a name. This pattern is useful for
   // letting us know if the name is empty after cleaning it up.
   // Notice that CHAR_CLASS_NAME does not contain these characters.
   private static final Pattern pONLY_NAME_CHARS = Pattern.compile("^[-.'`]*$");
   // A pattern for recognizing patterns that we want to remove from a name
   // when cleaning it up.
   private static final Pattern pNameCharsToStrip = Pattern.compile("[" + CHAR_CLASS_NAME + "]+");
   // This pattern is useful for extracting and removing alternate spellings names like in such names as:
   // Nathan Powel(l)
   private static final Pattern pREMOVE_ALTERNATE_NAME =
            Pattern.compile("(?<=[^" + CHAR_CLASS_NAME + "]+)\\s*([\\[{<(].*?($|[\\]}>)])|[\\\\/,].*$)");

   // Cleans names of alternate names and special characters
   // Keep in sync with SpecialGedcomPage.php
   private static String cleanName(String name) {
      if (!Utils.isEmpty(name))
      {
         // Removes alternate names in qu
         Matcher m = pREMOVE_ALTERNATE_NAME.matcher(name);
         name = m.replaceAll("");
         m = pNameCharsToStrip.matcher(name);
         name = m.replaceAll(" ");
         name = name.replaceAll("\\s+", " ");
         m = pONLY_NAME_CHARS.matcher(name);
         if (m.matches())
         {
            return null;
         } else
         {
            if (Utils.isEmpty(name.trim()))
            {
               return null;
            } else
            {
               return name.trim();
            }
         }
      }  else
      {
         return null;
      }
   }

   /**
    *
    * @return a one string format of name
    */
   public String toString() {
      if (!Utils.isEmpty(getGiven()) ||
            !Utils.isEmpty(getSurname()))
      {
         String rval;
         if (Utils.isEmpty(getGiven()))
         {
            rval = "Unknown";
         } else
         {
            rval = getGiven();
         }
         rval += ' ';
         if (Utils.isEmpty(getSurname()))
         {
            rval += "Unknown";
         } else
         {
            rval += getSurname();
         }
         return rval;
      } else
      {
         return "Unknown";
      }
   }

   // Returns whether the name is all caps or all lower case
   private static boolean hasHomogenousCaps(String name)
   {
      String [] split = name.split("\\s+");
      for (String s : split)
      {
         if (s.equals(s.toLowerCase()) || s.equals(s.toUpperCase()))
         {
            return true;
         }
      }
      return false;
   }

   // This pattern helps us take care of cases like MacDonald or McDonald
//   private static final Pattern pFirstLetter = Pattern.compile("(?<=\\bMa?c)\\w");
   // If the name has all caps or all lower case words in the
   // name, it fixes them to have an upper case first letter
   // with a lower case in the rest of the name word
// replaced by Utils.capitalizeTitleCase
//   public static String fixCapitalization(String name) {
//      if (name != null) {
//         String ucName = name.toUpperCase();
//         if (name.equals(ucName)) {
//
//         }
//      }
//         name = Utils.capitalizeTitleCase(name.trim(), true);
//
//      // Now I need to fix the capitalization
//      if (name != null && (name = name.trim()) != null)
//      {
//         String [] split = name.split("\\s+");
//         name = "";
//         for (String subName : split)
//         {
//            if (hasHomogenousCaps(subName))
//            {
//               // Then we need to properly capitalize this name.
//               String lower = subName.toLowerCase();
//               Matcher m = pFirstLetter.matcher(lower);
//               StringBuffer buf = new StringBuffer();
//               while(m.find())
//               {
//                  m.appendReplacement(buf, m.group(0).toUpperCase());
//               }
//               m.appendTail(buf);
//               subName = buf.toString();
//            }
//            if (!Utils.isEmpty(name))
//            {
//               name += ' ';
//            }
//            name += subName;
//         }
//         return name;
//      }
//      return name;
//   }

   // Name to use at the begining of the name tag: <name
   private String tagName = "name";

   // Type of the name
   private String type = null;

   /**
    *
    * @return tag name to be used when printing out
    */
   public String getTagName() {
      return tagName;
   }

   /**
    *
    * @param tagName to be used to print the name tag
    */
   public void setTagName(String tagName) {
      this.tagName = tagName;
   }

   /**
    *
    * @return the type of the name
    */
   public String getType() {
      return type;
   }

   /**
    *
    * @param type of the name, such as "Alt Name" or "Married Name"
    */
   public void setType(String type) {
      this.type = type;
   }

   public boolean givenSurnameEquals(Name other)
   {
      return (!PlaceUtils.isEmpty(getGiven()) && !PlaceUtils.isEmpty(other.getGiven()) &&
            getGiven().equalsIgnoreCase(other.getGiven()) &&
            !PlaceUtils.isEmpty(getSurname()) && !PlaceUtils.isEmpty(other.getSurname()) &&
            getSurname().equalsIgnoreCase(other.getSurname()));
   }

   /**
    * Print the name to a regular tag to be inside of a person object
    * @param buf to print out to
    * @param ec event container we're printing this image out to
    * @param gedcom the gedcom this image tag comes from    
    * @throws Uploader.PrintException
    * @throws Gedcom.PostProcessException
    */
   public void print(StringBuffer buf, StringBuffer sourceBuffer,
                     StringBuffer noteBuffer,
                     EventContainer ec,
                     Gedcom gedcom)
         throws Uploader.PrintException,
         Gedcom.PostProcessException
   {
      if (!Utils.isEmpty(toString()))
      {
         super.printCitsNotesImages(sourceBuffer, noteBuffer, ec, gedcom);
         GedcomElementWriter ew = new GedcomElementWriter(getTagName());
         formatTag(ew, gedcom);
         super.printReferences(ew);
         ew.write(buf);
      }
   }

   /**
    * Prepare the XML tag with the name only, without references
    * @param buf to print out to
    * @param gedcom the gedcom this name comes from    
    */
   public void prepareTag(StringBuffer buf, Gedcom gedcom)
   {
      if (!Utils.isEmpty(toString()))
      {
         GedcomElementWriter ew = new GedcomElementWriter(getTagName());
         formatTag(ew, gedcom);
         ew.write(buf);
      }
   }

   /**
    * Format the name to be placed in a tag
    * @param ew elementwriter to put the formated name in
    * @param gedcom the gedcom this name comes from
    */
   private void formatTag(GedcomElementWriter ew, Gedcom gedcom)
   {
      ew.put("type", getType());
      ew.put("given", getGiven());
      ew.put("surname", getSurname());
      ew.put("title_prefix", getPrefix());
      ew.put("title_suffix", getSuffix());
   }
}
