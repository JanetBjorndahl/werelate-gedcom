package org.werelate.gedcom;

import java.util.Set;
import java.util.HashSet;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: npowell
 * Date: Jan 20, 2007
 * Time: 2:12:52 PM
 * Represents an image tag in the person and family
 */
public class Image extends ReferenceContainer {
   // User's local file name of the image
   private String gedcom_file_name;
   // comes from the gedcom OBJE title
   private String caption;
   // Whether this is a primary image
   private boolean primary = false;

   // Implementation of the ReferenceContainer.getParentName
   protected String getParentName() {
      return null; // Because image should not be a parent to anybody
   }

   /**
    *
    * @return the user's local file name of the image
    */
   public String getGedcom_file_name() {
      return gedcom_file_name;
   }

   /**
    *
    * @param gedcom_file_name sets the user's local file name of the image
    */
   public void setGedcom_file_name(String gedcom_file_name) {
      this.gedcom_file_name = gedcom_file_name;
   }

   /**
    *
    * @return the caption of the image
    */
   public String getCaption() {
      return caption;
   }

   /**
    *
    * @param caption
    */
   public void setCaption(String caption) {
      this.caption = caption;
   }

   /**
    * Appends the caption, space deliminated
    * @param newCaption
    */
   public void appendCaption (String newCaption) {
      this.caption = Uploader.append (this.caption, newCaption, " ");
   }

   /**
    *
    * @return whether the image is primary
    */
   public boolean isPrimary() {
      return primary;
   }

   /**
    *
    * @param primary sets whether the image is primary
    */
   public void setPrimary(boolean primary) {
      this.primary = primary;
   }

   // TAGS which are allowed as attributes of the image
   private static final String [] STRING_ATTRIBUTES = {
         "FILE", "_FILE", "TITL", "TITLE", "NOTE", "_NOTE", "_PRIM", "DATA", "_DATE", "_PRIMARY", "SOUR"
   };
   private static final Set<String> ATTRIBUTES = new HashSet<String>();
   static
   {
      for (String s : STRING_ATTRIBUTES)
      {
         ATTRIBUTES.add(s);
      }
   }

   /**
    *
    * @param localName
    * @return whether the localName is a tag name which is an attribute of an image.
    */
   public static boolean isAttribute(String localName)
   {
      return ATTRIBUTES.contains(localName);
   }

   // TAG names that are silently ignored
   private static final String [] SILENT_IGNORABLES = {
         "FORM", "_SCBK", "_TYPE", "_SSHOW", "_CLIP",
         "_LENGTH", "_CRC", "_PRINT", "_TRANSFORM",
         "ROWS_PER_PAGE", "IMAGES_PER_ROW", "CHAN",
         "_ROTATE", "BLOB", "OBJE", "_SIZE", "_CROP", "_THUM", "_ASID", "_UPD",
         "_PRIM_CUTOUT", "_POSITION", "_PHOTO_RIN", "_FILESIZE", "_CUTOUT"
   };
   private static final Set<String> SET_SILENT_IGNORABLES = new HashSet<String>();
   static
   {
      for (String s : SILENT_IGNORABLES)
      {
         SET_SILENT_IGNORABLES.add(s);
      }
   }

   /**
    *
    * @param localName
    * @return whether the localName is a tag which should be silently ignored
    */
   public static boolean shouldSilentlyIgnore(String localName)
   {
      return SET_SILENT_IGNORABLES.contains(localName);
   }

   // Used for marking the parent object of
   // the image, such as an event or source citation
   private String parent = null;

   public String getParent() {
      return parent;
   }

   public void setParent(String parent) {
      this.parent = parent;
   }

   /**
    * Prints the image as a tag to be put in the object's data element.
    * @param buf to which the image will be printed
    * @param gedcom
    * @param ec - event container that contains the image
    * @throws Uploader.PrintException
    * @throws Gedcom.PostProcessException
    */
   public void print(StringBuffer buf, Gedcom gedcom, EventContainer ec)
         throws Uploader.PrintException, Gedcom.PostProcessException
   {
      if (getParent() != null)
      {
         GedcomElementWriter ew = new GedcomElementWriter("image");
         ew.put("parent", getParent());
         List<Note> notes = super.getNotes();
         notes.addAll(Note.Citation.getNotesFromCitations(gedcom, super.getNoteCitations()));
         for (Note note : notes)
         {
            appendCaption(note.getNote());
         }
         ew.put("caption", getCaption());
         ew.put("gedcom_filename", this.getGedcom_file_name());
         if (isPrimary())
         {
            ew.put("primary", "true");
         }
         ew.write(buf);
      } else
      {
         throw new Uploader.PrintException ("Error printing image with null valid parent", gedcom, ec.getID());
      }
   }
}
