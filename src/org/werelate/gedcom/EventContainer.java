package org.werelate.gedcom;

import org.werelate.util.Utils;
import org.werelate.util.MultiMap;
import org.apache.log4j.Logger;

import javax.print.PrintException;
import java.util.*;

/**
 * Created by IntelliJ IDEA.
 * User: npowell
 * Date: Jan 15, 2007
 * Time: 2:05:04 PM
 * Contains some functionality related to containing
 * events. Classes that inherit from this one include
 * Person, Family, Source
 */
public abstract class EventContainer extends TopObject {
   private static final Logger logger = Logger.getLogger("org.werelate.gedcom.EventContainer");
   private Set<Event> events = new TreeSet<Event>();

   /**
    *
    * @return the UID, either from the literal "UID" or "_UID" GEDCOM field
    * or from the reference number of the object
    */
   public String getUid() {
      if (!Utils.isEmpty(super.getUid()))
      {
         return super.getUid();
      } else
      {
         String refNum = null;
         for (Event event : getEvents())
         {
            if (event.getType() == Event.Type.ReferenceNumber)
            {
               refNum = event.getContent();
               break;
            }
         }
         if (!Utils.isEmpty(refNum))
         {
            return refNum;
         } else
         {
            return null;
         }
      }
   }
   public Set<Event> getEvents() {
      return events;
   }

   // This maps alternate event types to be used when
   // there are multiple such events existing
   private static final Map <Event.Type, Event.Type> altMap = new HashMap <Event.Type, Event.Type>();
   static
   {
      altMap.put(Event.Type.birth, Event.Type.alt_birth);
      altMap.put(Event.Type.christening, Event.Type.alt_christening);
      altMap.put(Event.Type.death, Event.Type.alt_death);
      altMap.put(Event.Type.burial, Event.Type.alt_burial);
      altMap.put(Event.Type.marriage, Event.Type.alt_marriage);
   }

   // Map that tells us whether we already have one of the above
   // types of events, and therefore if we would need to create
   // an alternate type of the same event instead
   private Map <Event.Type, Boolean> hasMap = new HashMap <Event.Type,Boolean>();
   public EventContainer () {
      for (Event.Type type : altMap.keySet())
      {
         hasMap.put(type, false);
      }
      initializeReferenceNumbers();
   }

   /**
    *
    * @param event to be added to the event container
    */
   public void addEvent(Event event) {
      if (!event.isPrivate())
      {
         if (hasMap.containsKey(event.getType()))
         {
            if (hasMap.get(event.getType()))
            {
               event.setType(altMap.get(event.getType()));
            } else
            {
               hasMap.put(event.getType(), true);
            }
         }
         events.add(event);
      }
   }

   // Encloses the parameter outText in CDATA
   protected static String encloseInCDATA(String outText) {
      // Before we do the enclosing, we want to remove
      // any control characters
      StringBuffer result = new StringBuffer();
      for (char c : outText.toCharArray())
      {
         if (c >= 32 || c == '\n' || c == '\r' || c == '\f' || c == '\t')
         {
            result.append(c);
         }
      }
      outText = Utils.replaceHTMLFormatting(result.toString());
      // Encodes any instances of "<![CDATA[" in the text
      outText = "<![CDATA[" + outText.replaceAll("\\]\\]>", "]]]]><![CDATA[>") + "]]>";
      return outText;
   }

   public void printCitation(Citation cit, StringBuffer sourceBuffer, StringBuffer noteBuffer, Gedcom gedcom) throws Uploader.PrintException, Gedcom.PostProcessException {
      Citation alreadyPrintedCitation;
      if ((alreadyPrintedCitation = getPrintedCitation(cit)) != null)
      {
         cit.setUpperID(alreadyPrintedCitation.getUpperID());
      } else
      {
         String sCitationNum = getNextCitString();
         cit.setUpperID(sCitationNum);
         cit.print(sourceBuffer, noteBuffer,  gedcom, this);
         addPrintedCitation(cit);
      }
   }

   // Used for generating reference numbers
   // for notes and source citations
   private static class ReferenceNumber {
      private int num = 1;
      private char prefix;
      public ReferenceNumber(char prefix)
      {
         this.prefix = prefix;
      }
      public int getNext() {
         return num++;
      }
      public String getNextString() {
         return prefix + Integer.toString(getNext());
      }
   }

   // citNum is the ID number used for printing out citations.
   // Of course, "S" is later prepended to it to form ID numbers
   // like "S1", "S2", etc.
   // Likewise for noteNum, only we prepend an "N" to it.
   // The ReferenceNumber object stores the letter to be
   // used as well as the current number.
   private ReferenceNumber citNum = null,
         noteNum = null;

   /**
    * Initializes the citation and note ID numbers
    * in order to print out this Event container
     */
   public void initializeReferenceNumbers () {
      citNum = new ReferenceNumber('S');
      noteNum = new ReferenceNumber('N');
   }

   /**
    *
    * @return the next souce citation id,
    * such as "S1", "S2", "S3", etc.
    */
   public String getNextCitString() {
      return citNum.getNextString();
   }

   /**
    * @return Return the next note citation id,
    * such as "N1", "N2", "N3", etc.
    */
   public String getNextNoteString() {
      return noteNum.getNextString();
   }

   /**
    * Prints all of the events in this EventContainer, including their
    * notes, source citations, and image citations
    * @param gedcom
    * @param buf to print everything but the image citations
    * @throws Uploader.PrintException
    * @throws Gedcom.PostProcessException
    */
   protected void printEvents(Gedcom gedcom, StringBuffer buf,
                              StringBuffer sourceBuffer,
                              StringBuffer noteBuffer)
         throws Uploader.PrintException, Gedcom.PostProcessException
   {
      for (Event event : getEvents())
      {
         if (!Utils.isEmpty(event.toString()) && !event.getType().equals(Event.Type.ReferenceNumber))
         {
            event.printCitsNotesImages(sourceBuffer, noteBuffer, this, gedcom);
            buf.append(event.printTag(gedcom));
         }
      }
   }


   // List of all citations attached to this EventContainer object
   private ArrayList<Citation> citations = new ArrayList<Citation>();

   /**
    *
    * @param cit source citation to add directly to the event container
    */
   public void addCitation(Citation cit)
   {
      citations.add(cit);
   }

   /**
    *
    * @return list of all source citations directly attached to the event container
    */
   public ArrayList <Citation> getCitations() {
      return citations;
   }

   private MultiMap <String, Citation> printedCitations = new MultiMap<String, Citation>();
   public Citation getPrintedCitation(Citation cit)
   {
      return  printedCitations.getEqual(cit.getId(), cit);
   }

   public void addPrintedCitation(Citation cit)
   {
      printedCitations.put(cit.getId(), cit);
   }


   private List <String> notes = new ArrayList<String> ();
   /**
    *
    * @param note to be added
    */
   public void addNote(String note) {
      note = Note.cleanNoteText(note);
      if (!Utils.isEmpty(note))
      {
         notes.add(note);
      }
   }

   public void addNote(String id, String content)
   {
      if (Utils.isEmpty(id)) {
         addNote(content);
      } else {
         addNoteCitation(id);
      }
   }

   private MultiMap <String, Note> printedNotes = new MultiMap<String, Note>();

   public Note getPrintedNote(Note note)
   {
      return printedNotes.getEqual(note.getKey(), note);
   }
   public void addPrintedNote(Note note)
   {
      printedNotes.put(note.getKey(), note);
   }

   /**
    *
    * @param notes collection of notes to be added to
    * the existing list of notes
    */
   public void addNotes(Collection <String> notes)
   {
      for (String note : notes)
      {
         addNote(note);
      }
   }

   public void addNoteNotes(Collection <Note> notes)
   {
      for (Note n : notes)
      {
         addNote(n.getNote());
      }
   }

   public void addNoteNoteCitations(Collection <Note.Citation> noteCits)
   {
      for (Note.Citation nc : noteCits)
      {
         addNoteCitation(nc.getId());
      }
   }

   /**
    *
    * @param ignoredBucket
    */
   public void eatIgnoredBucket(Collection <String> ignoredBucket)
   {
      addNotes(ignoredBucket);
      ignoredBucket.clear();
   }

   /**
    *
    * @param gedcom
    * @param cits list to which citations in any of the notes will be added.
    * This param is essentially another return value.
    * @return list of notes attached to the event container
    * @throws Gedcom.PostProcessException
    */
   public List<String> getNotes(Gedcom gedcom, Collection <Citation> cits)
         throws Gedcom.PostProcessException
   {
      List <Note> noteNotes = super.getNotesFromCitations(gedcom);
      if (noteNotes != null && noteNotes.size() > 0)
      {
         List <String> rval = new ArrayList <String>();
         rval.addAll(notes);
         for (Note note : noteNotes)
         {
            cits.addAll(note.getSourceCitations());
            rval.add(note.getNote());
         }
         return rval;
      } else
      {
         return notes;
      }
   }

   protected List<String> getNotes()
   {
      return notes;
   }

   /**
    * Prints all source citations attached directly to this EventContainer
    * @param sourceBuffer to print all of the source citations to
    * @param noteBuffer to print all notes attached to these citations.
    * @param gedcom
    * @throws Uploader.PrintException
    * @throws Gedcom.PostProcessException
    */
   protected void printCitations (StringBuffer sourceBuffer,
                                  StringBuffer noteBuffer, Gedcom gedcom)
         throws Uploader.PrintException, Gedcom.PostProcessException
   {
      for (Citation cit : getCitations())
      {
         printCitation(cit, sourceBuffer, noteBuffer, gedcom);
      }
   }

   /**
    * Prints all notes directly attached to this EventContainer
    * @param buf to print notes to
    * @param cits list to which citations in any of the notes will be added.
    * This param is essentially another return value.
    * @param gedcom
    * @throws Uploader.PrintException
    * @throws Gedcom.PostProcessException
    */
   protected void printNotes(StringBuffer buf, Collection <Citation> cits, Gedcom gedcom)
         throws Uploader.PrintException, Gedcom.PostProcessException
   {
      for (String note : getNotes(gedcom, cits))
      {
         if (!Utils.isEmpty(buf.toString()))
         {
            buf.append("\n\n");
         }
         buf.append(note);
      }
   }

   // All image citations attached directly to this EventContainer
   private List <Image> images = new ArrayList<Image>();

   // All image citations attached directly to this EventContainer
   public List<Image> getImages() {
      return images;
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
   private Set <String> imageCitations = new HashSet<String>();

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
    * @param gedcom
    * @return title appropriate to be used as the WeRelate wiki title for this EventContainer
    * @throws Gedcom.PostProcessException
    */
   abstract public String getWikiTitle (Gedcom gedcom) throws Gedcom.PostProcessException;

   /**
    *
    * @param gedcom
    * @return whether this EventContainer should be printed. For people,
    * this is based on whether the person is dead or not. For families,
    * it is based on whether the family contains anyone dead. For MySources,
    * it is based on whether there is any information present besides the title.
    * @throws Uploader.PrintException
    * @throws Gedcom.PostProcessException
    */
   //abstract public boolean shouldPrint(Gedcom gedcom) throws Uploader.PrintException, Gedcom.PostProcessException;
}
