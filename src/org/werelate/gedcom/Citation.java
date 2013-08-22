package org.werelate.gedcom;

import org.werelate.util.Utils;
import org.apache.log4j.Logger;

import java.util.Collection;

/**
 * Created by IntelliJ IDEA.
 * User: npowell
 * Date: Nov 16, 2006
 * Time: 6:17:50 PM
 * Represents source citations
 */
public class Citation extends ReferenceContainer implements ReferencedObject {
   private static final Logger logger = Logger.getLogger("org.werelate.gedcom.ReferencedObject");
   private String id = null;
   private String page = null;
   private String date = null;
   private String text = null;
   private enum Quality {
      Unreliable,
      Questionable,
      Secondary,
      Primary,
      Unknown
   }
   private Quality quality = Quality.Unknown;

   public boolean equals(Object obj)
   {
      Citation other = (Citation) obj;
      if ((Utils.nullCheckEquals(id, other.id)) &&
            Utils.nullCheckEquals(page, other.page) &&
            Utils.nullCheckEquals(date, other.date) &&
            Utils.nullCheckEquals(text, other.text) &&
            quality == other.quality)
      {
         // Let's make sure the notes are the same
         // as well.
         if (getNoteCitations().size() != other.getNoteCitations().size())
         {
            return false;
         }

         for (Note.Citation citation : getNoteCitations())
         {
            boolean foundEqual = false;
            for (Note.Citation otherCitation : other.getNoteCitations())
            {
               if (Utils.nullCheckEquals(citation.getId(),otherCitation.getId()))
               {
                  foundEqual = true;
               }
            }

            // Didn't find an equal citation, so I will return false;
            if (!foundEqual)
            {
               return false;
            }
         }

         if (getNotes().size() != other.getNotes().size())
         {
            return false;
         }

         for (Note note : getNotes())
         {
            boolean foundEqual = false;
            for (Note otherNote : other.getNotes())
            {
               if (note.equals(otherNote))
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
    * Imple
    * @return
    * @throws Gedcom.PostProcessException
    */
   protected String getParentName() throws Gedcom.PostProcessException {
      if (!Utils.isEmpty(getUpperID()))
      {
         return getUpperID();
      } else
      {
         throw new Gedcom.PostProcessException("When getting citation parent name, no valid upperID set!", null, null);
      }
   }

   public String getQualityString() {
      switch(getQuality())
      {
         case Unreliable:
            return "0";
         case Questionable:
            return "1";
         case Secondary:
            return "2";
         case Primary:
            return "3";
         default:
            return null;
      }
   }

   public Quality getQuality() {
      return quality;
   }

   public void setQuality(Quality quality) {
      this.quality = quality;
   }

   public void setQuality(Gedcom gedcom, String qual)
   {
      if (!Utils.isEmpty(qual))
      {
         try
         {
            int num = Integer.parseInt(qual);
            switch(num)
            {
               case 0:
                  setQuality(Quality.Unreliable);
                  break;
               case 1:
                  setQuality(Quality.Questionable);
                  break;
               case 2:
                  setQuality(Quality.Secondary);
                  break;
               case 3:
                  setQuality(Quality.Primary);
                  break;
               default:
                  this.appendText("Quality: " + qual);
            }
         } catch (NumberFormatException e)
         {
            this.appendText("Quality: " + qual);           
         }
      }
   }

   public String getText() {
      return text;
   }

   public void setText(String text) {
      this.text = text;
   }

   public void appendText(String text) {
      if (Utils.isEmpty(getText()))
      {
         setText(text);
      } else if (!Utils.isEmpty(text))
      {
         setText(getText() + ' ' + text);
      }
   }

   public void eatIgnoredBucket(Collection<String> ignoredBucket)
   {
      for (String s : ignoredBucket)
      {
         appendText(s);
      }
      ignoredBucket.clear();
   }

   public String getDate() {
      return date;
   }

   public void setDate(String date) {
      this.date = date;
   }

   public String getId() {
      return id;
   }

   public void setId(String id) {
      this.id = id;
   }   

   public String getPage() {
      return page;
   }

   public void setPage(String page) {
      this.page = page;
   }

   // This is the citation ID that
   // might appear on a person or
   // family page. For example, "S1"
   private String upperID = null;

   public String getUpperID() {
      return upperID;
   }

   public void setUpperID(String upperID) {
      this.upperID = upperID;
   }

   public void print(StringBuffer sourceBuffer, 
                     StringBuffer noteBuffer, Gedcom gedcom, EventContainer ec)
         throws Uploader.PrintException, Gedcom.PostProcessException
   {
      if (!Utils.isEmpty(getUpperID()))
      {
         printCitsNotesImages(sourceBuffer, noteBuffer, ec, gedcom);
         alwaysPrint(sourceBuffer, ec, gedcom);
      } else
      {
         throw new Uploader.PrintException ("Error printing note with no valid upperID", gedcom, ec.getID());
      }
   }

   public void alwaysPrint(StringBuffer buf, EventContainer ec, Gedcom gedcom)
        throws Uploader.PrintException, Gedcom.PostProcessException
   {
      GedcomElementWriter ew = new GedcomElementWriter("source_citation");
      ew.put("id", getUpperID());
      ew.put("page", getPage());
      if (getQuality() != Quality.Unknown)
      {
         ew.put("quality", getQualityString());
      }
      ew.put("date", getDate());

      String text = getText();

      String id = getId();
      if (!Utils.isEmpty(id))
      {
         Source source = gedcom.getSources().get(id);
         if (source != null) {
            if (source.shouldPrint(gedcom)) {
               ew.put("source_id", source.getID());
            }
            else {
               // append source to text if the source isn't going to be printed
               String citationText = source.getAsCitationText(gedcom);
               if (!Utils.isEmpty(text) && !Utils.isEmpty(citationText)) {
                  text += "\n\n";
               }
               text += citationText;
            }
         }
      }

      printReferences(ew);

      if (!Utils.isEmpty(text))
      {
         ew.setSubText(Utils.replaceHTMLFormatting(text));
      }
      ew.write(buf);
   }
}
