package org.werelate.gedcom;

import org.werelate.util.Utils;
import org.apache.log4j.Logger;

import java.util.*;

/**
 * Created by IntelliJ IDEA.
 * User: npowell
 * Date: Jan 20, 2007
 * Time: 7:31:06 PM
 * Anything that itself contains citation references.
 * In other words, anything can contain an attribute
 * such as notes="N1, N2", images="I1", sources="S1".
 * Derived classes include Name, Citation, and Event
 */
abstract public class ReferenceContainer {
   private static final Logger logger = Logger.getLogger("org.werelate.gedcom.ReferenceContainer");

   public void clearNotes() {
      this.notes.clear();
   }
   // Copies
   protected ReferenceContainer(ReferenceContainer other)
   {
      this.images = other.images;
      this.citations = other.citations;
      this.noteCitations = other.noteCitations;
      this.notes = other.notes;
   }

   protected ReferenceContainer()
   {

   }

   private List<Image> images = new ArrayList<Image>();
   private ArrayList<Note> notes = new ArrayList<Note>();
   // Notes accessed via ID number as opposed to being in text
   private ArrayList<Note.Citation> noteCitations = new ArrayList <Note.Citation>();

   /**
    *
    * @return all the Note.Citations for this object
    */
   public ArrayList<Note.Citation> getNoteCitations() {
      return noteCitations;
   }

   /**
    * @param noteCit to add
    */
   public void addNoteCitation(Note.Citation noteCit)
   {
      noteCitations.add(noteCit);
   }

   /**
    *
    * @return all regular notes for this object, NOT including note citations
    */
   public ArrayList<Note> getNotes() {
      return notes;
   }

   /**
    *
    * @param note to be added
    */
   public void addNote(Note note)
   {
      if (!Utils.isEmpty(note.getNote()))
      {
         notes.add(note);
      }
   }

   /**
    * For adding a note without specifying whether it is
    * an in-text or id referenced note. This method makes
    * that determination via the id and content values
    * @param id of the tag, if present
    * @param content of the tag, if present
    */
   public void addNote(String id, String content)
   {
      if (!Utils.isEmpty(id))
      {
         addNoteCitation(new Note.Citation(id));
      }
      else if (Gedcom.isID(content))
      {
         addNoteCitation(new Note.Citation(content));
      } else
      {
         addNote(new Note(content));
      }
   }

   /**
    *
    * @param newNotes in-text note collection to be added to object
    */
   public void addNotes(Collection<Note> newNotes)
   {
      for (Note note : newNotes)
      {
         addNote(note);
      }
   }
   // Retrieves the inline image references through the
   // image reference ids.
   private List <Image> getImagesFromCitations(Gedcom gedcom) {
      List <Image> rval = new ArrayList <Image>();
      for (String imageID : imageCitations)
      {
         Image image = gedcom.getImages().get(imageID);
         if (image != null)
         {
            rval.add(image);
         } else
         {
            logger.info(gedcom.logStr("Invalid reference to OBJE: " + imageID));
         }
      }
      return rval;
   }
   /**
    *
    * @return list of all image references attached to this object
    */
   public List<Image> getImages(Gedcom  gedcom) {
      List <Image> rval = new ArrayList<Image>();
      rval.addAll(images);
      rval.addAll(getImagesFromCitations(gedcom));
      return rval;
   }
   /**
    * This method decides whether this is an
    * inline image reference or a reference to an
    * image reference via a GEDCOM id number
    *
    * If it is a reference to an image reference
    * we add the id to the list of imageCitations
    * @param gedcom
    * @param image object to be added
    * @param id of the image citation, if it exists
    */
   public void addImage(Gedcom gedcom, Image image, String id) {
      if (Utils.isEmpty(image.getGedcom_file_name()))
      {
         if (Utils.isEmpty(id))
         {
            gedcom.infoLine("OBJE tag contains neither an id nor a filename. We will ignore it.");
         } else
         {
            imageCitations.add(id);
         }
      } else
      {
         images.add(image);
      }
   }
   private Set<String> imageCitations = new HashSet<String>();

   private ArrayList<Citation> citations = new ArrayList<Citation>();

   /**
    *
    * @param citation - source citation to be added
    */
   public void addCitation(Citation citation)
   {
      if (citation != null)
      {
         citations.add(citation);
      }
   }

   /**
    *
    * @param newCitations list of citations to add to object
    */
   public void addCitations(Collection <Citation> newCitations)
   {
      citations.addAll(newCitations);
   }

   /**
    *
    * @return list of all source citations attached to this object
    */
   public ArrayList<Citation> getCitations()
   {
      return citations;
   }

   /**
    * Prints to buf and imageBuf the source citations, notes, and images, that
    * are in this reference container
    * @param sourceBuffer to print source citations to
    * @param imageBuf to print image tags to
    * @param noteBuffer to print notes out to
    * @param ec in which these references occur.
    * @param gedcom
    * @throws Uploader.PrintException
    * @throws Gedcom.PostProcessException
    */
   public void printCitsNotesImages(StringBuffer sourceBuffer,
                                    StringBuffer noteBuffer,
                                    EventContainer ec, Gedcom gedcom)
         throws Uploader.PrintException, Gedcom.PostProcessException
   {
      // add referenced @ID@ notes
      addNotes(Note.Citation.getNotesFromCitations(gedcom, getNoteCitations()));
      // add citations from notes
      for (Note note : getNotes()) {
         addCitations(note.getSourceCitations());
      }
      // print citations
      for (Citation cit : getCitations())
      {
         ec.printCitation(cit, sourceBuffer, noteBuffer, gedcom);
      }
      // print notes
      for (Note note : getNotes())
      {
         Note printedNote;
         if ((printedNote = ec.getPrintedNote(note)) != null)
         {
            note.setUpperID(printedNote.getUpperID());
         } else
         {
            String sNoteNum = ec.getNextNoteString();
            note.setUpperID(sNoteNum);
            note.print(noteBuffer, gedcom, ec);
            ec.addPrintedNote(note);
         }
      }
   }

   // Returns the name to be used for specifying the parent of an image
   // tag, because the image tag is being put inside of the data element
   protected abstract String getParentName() throws Gedcom.PostProcessException;
   protected boolean isEmpty() {
      return images.size() == 0 && notes.size() == 0 && citations.size() == 0;
   }

   /**
    * This method merely prints out the reference numbers
    * as a tag attribute for the notes and source citations
    * contained in this reference container
    * @param ew
    * @throws Uploader.PrintException
    */
   public void printReferences(GedcomElementWriter ew) throws Uploader.PrintException {
      if (getCitations().size() > 0)
      {
         String sources = getCitations().get(0).getUpperID();
         Set <String> addedSources = new HashSet<String>();
         addedSources.add(sources);
         for (int i=1; i < getCitations().size(); i++)
         {
            String sourceId = getCitations().get(i).getUpperID();

            // We want to make sure that we don't repeat
            // any source ids.
            if (!addedSources.contains(sourceId))
            {
               sources += ", " + sourceId;
               addedSources.add(sourceId);
            }
         }
         ew.put("sources", sources);
      }

      if (getNotes().size() > 0)
      {
         String notes = getNotes().get(0).getUpperID();
         Set<String> addedNotes = new HashSet<String> ();
         for (int i=1; i < getNotes().size(); i++)
         {
            String noteId  = getNotes().get(i).getUpperID();
            if (!addedNotes.contains(noteId))
            {
               notes += ", " + noteId;
               addedNotes.add(noteId);
            }
         }
         ew.put("notes", notes);
      }
   }
}
