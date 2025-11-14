package org.werelate.gedcom;

import org.werelate.util.Utils;
import org.werelate.util.PlaceUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;

/**
 * Created by IntelliJ IDEA.
 * User: npowell
 * Date: Jan 15, 2007
 * Time: 11:37:12 AM
 * Reprents a note that has an ID.
 * This class mostly exists to separate out
 * the printing of this note
 */
public class Note implements ReferencedObject {
   private static final Logger logger = LogManager.getLogger("org.werelate.gedcom.Note");

   private Collection <org.werelate.gedcom.Citation> sourceCitations = new ArrayList<org.werelate.gedcom.Citation>();

   public static String cleanNoteText(String note) {
      if (note != null) {
         note = note.replace("== Sources ==\n<references />","");        // WikiTree gedcom (simple case when there aren't additional sources defined)
         note = note.trim();
         if (note.contains("{geni:place_name}") || note.contains("{geni:county}")) {
            note = "";
         }
         else if (note.startsWith("{geni:about_me}")) {
            note = note.substring(note.indexOf('}')+1).trim();
         }
      }
      return note;
   }

   public void addCitation(org.werelate.gedcom.Citation newCit)
   {
      sourceCitations.add(newCit);
   }

   public Collection<org.werelate.gedcom.Citation> getSourceCitations() {
      return sourceCitations;
   }

   /**
    * Occasionally there are unusual tags inside a NOTE tag.
    * If they were ignored and put into the ignored bucket, we
    * need to be sure to append it to this Note
    * @param ignoredBucket
    */
   public void eatIgnoredBucket(Collection<String> ignoredBucket) {
      for (String s : ignoredBucket)
      {
         appendNote(s);
      }
      ignoredBucket.clear();
   }

   /**
    * Sometimes a note is cited via an ID number in a GEDCOM
    * This Note.Citation class contains the note citation, which
    * we use later on to retrieve the actual note text
    */
   public static class Citation {
      String id = null;

      public String getId() {
         return id;
      }

      public void setId(String id) {
         this.id = id;
      }

      public Citation(String id) {
         this.setId(id);
      }

      public static List<Note> getNotesFromCitations(Gedcom gedcom, Collection<Citation> noteCitations)
            throws Gedcom.PostProcessException
      {
         Map<String, Note> map = gedcom.getNotes();
         List <Note> rval = new ArrayList<Note>();
         for (Citation cit : noteCitations)
         {
            Note note = map.get(cit.getId());
            if(note != null)
            {
               rval.add(note);
            } else
            {
               //throw new Gedcom.PostProcessException ("Note citation with invalid ID", gedcom, cit.getId());
               logger.info(gedcom.logStr("Note citation with invalid id: " + cit.getId()));
            }
         }
         return rval;
      }
   }
   // The actual text of the note
   private String note = null;
   // Only if this NOTE is a top leve NOTE and therefore
   // has an id should this value be non-null
   private String id = null;


   public boolean equals(Object obj)
   {
      Note other = (Note) obj;

      if (Utils.nullCheckEquals(note, other.note) &&
            Utils.nullCheckEquals(id, other.id))
      {
         // Now let's check all of the citations, and
         // make sure they are equal as well.

         if (sourceCitations.size() != other.sourceCitations.size())
         {
            return false;
         }

         for (org.werelate.gedcom.Citation cit : sourceCitations)
         {
            boolean foundEqual = false;
            for (org.werelate.gedcom.Citation otherCitation : other.sourceCitations)
            {
               if (cit.equals(otherCitation))
               {
                  foundEqual = true;
               }
            }
            if (!foundEqual)
            {
               return false;
            }
         }

         return true;
      } else
      {
         return false;
      }
   }

   /**
    *
    * @return the note id, if applicable
    */
   public String getId() {
      return id;
   }

   /**
    *
    * @param id the id of the NOTE object
    */
   public void setId(String id) {
      this.id = id;
   }

   // Creates a Note by setting the text of the note immediately
   public Note (String note)
   {
      setNote(note);
   }

   public Note() {

   }

   /**
    *
    * @return text of the note
    */
   public String getNote() {
      return note;
   }

   /**
    * Sets the text of the note
    * @param note
    */
   public void setNote(String note) {
      this.note = cleanNoteText(note);
   }

   public String getKey() {
      if (PlaceUtils.isEmpty(getNote()))
      {
         return getId();
      } else
      {
         return getNote();
      }
   }

   /**
    * Prepends the newTitle to the text of the note
    * In some GEDCOMs, there is a TITLE element of
    * a note
    * @param newTitle
    */
   public void prependTitle(String newTitle)
   {
      setNote(Uploader.append(newTitle, getNote(), "\n\n"));
   }

   /**
    * Appends newNote to the text of the current note
    * @param newNote
    */
   public void appendNote(String newNote)
   {
      setNote(Uploader.append(getNote(), newNote, "\n\n"));
   }

   // This is the citation ID that
   // might appear on a person or
   // family page. For example, "S1"
   private String upperID = null;

   public String getUpperID() {
      return upperID;
   }

   /**
    *
    * @param upperID to be used for printing out the ID of the note, such as "N1", "N2", etc.
    */
   public void setUpperID(String upperID) {
      this.upperID = upperID;
   }

   /**
    * Prints the note tag properly
    * @param buf to print the note out to
    * @param gedcom
    * @param ec EventContainer which contains this note
    * @throws Uploader.PrintException
    */
   public void print(StringBuffer buf, Gedcom gedcom, EventContainer ec) throws Uploader.PrintException {
      if (!Utils.isEmpty(getUpperID()))
      {
         GedcomElementWriter ew = new GedcomElementWriter("note");

         ew.put("id", getUpperID());
         if (!Utils.isEmpty(getNote()))
         {
            ew.setSubText(Utils.replaceHTMLFormatting(getNote()).trim());
         }
         ew.write(buf);
      } else
      {
         throw new Uploader.PrintException ("Error printing note with no valid upperID", gedcom,ec.getID());
      }
   }
}
