package org.werelate.gedcom;

import org.werelate.util.Utils;
import org.werelate.util.LineReader;
import org.werelate.util.EventDate;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;
import java.io.PrintWriter;
import java.io.InputStream;

/**
 * Created by IntelliJ IDEA.
 * User: npowell
 * Date: Nov 9, 2006
 * Time: 1:46:40 PM
 * Represents an Event/Fact
 * Note that this class is derived from a Citation container,
 * because and Event can refer to notes, images, and source citations
 */
public class Event extends ReferenceContainer implements Comparable {
   private static final Logger logger = LogManager.getLogger("org.werelate.gedcom.Event");
   /**
    *
    * @return the parent name to go into an image tag
    * @throws Gedcom.PostProcessException
    */
   protected String getParentName () throws Gedcom.PostProcessException {
      String rval = toString();
      // ToString does not return a non-empty string
      // for LDS ordinances. Therefore, we must also include these:
      if (rval == null)
      {
         switch (getType())
         {
            case lds_baptism:
               rval = "BAPL";
               break;
            case lds_child_sealing:
               rval = "SLGC";
               break;
            case lds_spouse_sealing:
               rval = "SLGS";
               break;
            case lds_endowment:
               rval = "ENDL";
               break;
            default:
               throw new Gedcom.PostProcessException("Unknown parent name for event.", null, null);
         }
      }
      return rval;
   }

   // Value of the GEDCOM restricted field
   private boolean isPrivate = false;
   public boolean isPrivate() {
      return isPrivate;
   }
   public void setPrivate(boolean aPrivate) {
      isPrivate = aPrivate;
   }

   /**
    *
    * @param restriction value of the GEDCOM RESN tag
    * Possible values include: privacy, locked, and confidential.
    * If the value is privacy or confidnetial, then we mark this
    * event as private. If the event is private, then the EventContainer
    * object drops this object on the floor.
    */
   public void setRestriction (String restriction)
   {
      restriction = restriction.toLowerCase();
      if (restriction.equals("confidential") || restriction.equals("privacy"))
      {
         setPrivate(true);
      }
   }

   // Content which comes from the regular
   // GEDCOM content
   private String content = null;

   // GEDCOM this event is associated with
   private Gedcom gedcom = null;

   /**
    *
    * @return content from the regular GEDCOM content of the event
    */
   public String getContent() {
      return content;
   }

   /**
    *
    * @param content of the event
    * @param id of the event, if there is one.
    * If there is an ID, then that means that it
    * is a note citation, so we add it to the list
    * of this event's note citations
    */
   public void setContent(String content, String id) {
      if (!Utils.isEmpty(id))
      {
         addNoteCitation(new Note.Citation(id));
      }
      this.content = content;
   }

   /**
    *
    * @param newContent content to be appended to the current content.
    * Note: the delimeter is a space
    */
   public void appendContent(String newContent)
   {
      this.content = Uploader.append(this.content, newContent, " ");
   }

   /**
    * Prints this event as a set of attributes. This is typically used
    * in the family husband, wife, child tags
    * @param gedcom that contains this event
    * @param ew to set the attributes to.
    * @throws Uploader.PrintException
    */
   public void printAsAttribute(Gedcom gedcom, GedcomElementWriter ew) throws Uploader.PrintException{
      if (getAttributeTypeName() != null)
      {
         ew.put(getAttributeTypeName() + "date",  getAttribute("DATE"));
         ew.put(getAttributeTypeName() + "place", getPlace(gedcom));
      }
   }

   private static Set <String> TEMPLE_CODES = null;

   /**
    *
    * @param is input steam containing a formatted list
    * of temple codes, temple descriptions.
    * (This is comma deliminated).
    */
   public static void setTempleCodes(InputStream is)
   {
      if (is != null)
      {
         TEMPLE_CODES = new HashSet<String>();
         LineReader lr = new LineReader(is);
         for (String line : lr.getLines())
         {
            String [] tokens = line.split("\\s*,\\s*");
            TEMPLE_CODES.add(tokens[0].trim().toUpperCase());
         }
      }
   }
   private boolean isLDSEvent() {
      return getType() == Type.lds_baptism ||
            getType() == Type.lds_endowment ||
            getType() == Type.lds_spouse_sealing ||
            getType() == Type.lds_child_sealing;
   }

   // Get's the event's place
   private String getPlace(Gedcom gedcom)
   {
      // We move the temple code from the PLAC
      // to the TEMP attribute, if we need to.
      if (TEMPLE_CODES != null && isLDSEvent() &&
            !Utils.isEmpty(getAttribute("PLAC")) &&
            Utils.isEmpty(getAttribute("TEMP")))
      {
         String [] tokens = getAttribute("PLAC").split("\\W+");
         for (String token : tokens) {
            token = token.toUpperCase();
            if (TEMPLE_CODES.contains(token)) {
               setAttribute("TEMP", token);
               atts.remove("PLAC");
               break;
            }
         }
      }
      try
      {
         // Will of course return null if we have moved the temple
         // code from the PLAC attribute to the TEMP attribute

         // place2standard has a map from all place names in the
         // gedcom to the WeRelate standardized place names         

         // We are no longer doing this in the first phase, so,
         // we will just return the non-standardized place.
         //return gedcom.getPlace2standard().get(getAttribute("PLAC"));

         return getAttribute("PLAC");
      } catch (NullPointerException e)
      {         
         throw e;
      }
   }

   private String description = null;

   /**
    *
    * @param s to be appended to the Event description
    */
   public void appendDescription(String s)
   {
      s = Note.cleanNoteText(s);
      if (!Utils.isEmpty(s)) {
         description = Uploader.append(description, s, " ");
      }
   }

   /**
    *
    * @param s append a description, only this time, using
    * a new line as a delimiter. Note: we might be able to
    * take this out, because I think we may have decided to
    * replace all newlines with spaces for all attributes.
    */
   public void appendNewLineDescription (String s)
   {
      s = Note.cleanNoteText(s);
      if (!Utils.isEmpty(s)) {
         description = Uploader.append(description, s, "\n");
      }
   }

   /**
    *
    * @param s to be prepended to the description field
    */
   public void prependDescription (String s)
   {
      s = Note.cleanNoteText(s);
      if (!Utils.isEmpty(s))
      {
         if (Utils.isEmpty(description))
         {
            description = s;
         } else
         {
            description = s + ' ' + description;
         }
      }
   }

   /**
    *
    * @return the description of the event
    */
   public String getDescription() {
      return description;
   }

   /**
    * Prints the Event as an XML tag, with references
    * @param gedcom
    * @return the String version of the XML tag for the event
    * @throws Uploader.PrintException
    */
   public String printTag(Gedcom gedcom) throws Uploader.PrintException 
   {
      String outText = "";
      if (!Utils.isEmpty(toString()) &&
            (this.atts.size() > 0
            || !Utils.isEmpty(getContent())
            || !Utils.isEmpty(getDescription())
            || !super.isEmpty()))
      {
         GedcomElementWriter ew = new GedcomElementWriter("event_fact");
         formatTag(ew, gedcom);
         printReferences(ew);
         outText = ew.write();
      }
      return outText;
   }

   /**
    * Prepares the Event as an XML tag, without references
    * @param gedcom
    * @return the String version of the XML tag for the event
    */
    public String prepareTag(Gedcom gedcom) 
   {
      String outText = "";
      if (!Utils.isEmpty(toString()) &&
            (this.atts.size() > 0
            || !Utils.isEmpty(getContent())
            || !Utils.isEmpty(getDescription())
            || !super.isEmpty()))
      {
         GedcomElementWriter ew = new GedcomElementWriter("event_fact");
         formatTag(ew, gedcom);
         outText = ew.write();
      }
      return outText;
   }

   /**
    * Formats the Event as an XML tag
    * @param ew the GedcomElementWriter to use for the tag
    * @param gedcom
    */
   public void formatTag(GedcomElementWriter ew, Gedcom gedcom) 
   {
      String typeAtt = getAttribute("TYPE");
      String normalizedTypeAtt = null;
      if (typeAtt != null)
      {            
         normalizedTypeAtt = EVEN_TYPES_MAP.get(typeAtt.trim().toLowerCase());
      }
      if (normalizedTypeAtt != null)
      {
         ew.put("type", normalizedTypeAtt);
      } else
      {
         ew.put("type", toString());
      }
      if (getAttribute("DATE") != null)                                          // date formatting for the XML added Aug 2021 by Janet Bjorndahl
      {
         EventDate eventDate = new EventDate(getAttribute("DATE"), normalizedTypeAtt != null ? normalizedTypeAtt : toString());              
         ew.put("date", eventDate.formatDate());
      } else 
      {
         ew.put("date", null);
      }                                     

      /*if (this.getType() == Event.Type.ReferenceNumber && Gedcom.isID(getContent().trim()))
      {
         // Let's see if we can retrieve the note with this ID number.
         Note note = gedcom.getNotes().get(getContent().trim());
         if (note!=null)
         {
            this.content = note.getNote();
         }
      }*/
      appendToDescription("CAUS", gedcom);
      appendToDescription("STAT", gedcom);
      appendToDescription("STATDATE", gedcom);
      appendToDescription("AGE", gedcom);
      appendDescription(getContent());
      if (normalizedTypeAtt == null
            && !Utils.isEmpty(typeAtt)
            && (Utils.isEmpty(getDescription())
            ||  !getDescription().contains(typeAtt)))
      {
         appendDescription(typeAtt);
      }
      appendNewLineDescription(getAttribute("ADDR"));
      // This moves content of the place field
      // to the description field if it really belongs there
      String place = getPlace(gedcom);
      if (  this.getType() == Event.Type.Other &&
            Utils.isEmpty(getDescription()) &&
            place != null &&
            !place.contains(",") &&
            !gedcom.getStandardizedPlaceSet().contains(place)
            )
      {
         appendDescription(place);
      } /* else if ( this.getType() == Event.Type.SocSecNo &&                no longer needed, since Soc Sec No not being imported, changed Apr 2025 by Janet Bjorndahl
            Utils.isEmpty(getDescription()) &&
            place != null &&
            place.trim().matches("\\d\\d\\d-?\\d\\d-?\\d\\d\\d\\d"))
      {
         appendDescription(place);
      }  */
      else
      {
         ew.put("place", place);
      }
      ew.put("desc", getDescription());
   }

   private void appendToDescription(String attToAppend, Gedcom gedcom) {
      if (!Utils.isEmpty(getAttribute(attToAppend)))
      {
         appendDescription(gedcom.translateIgnoredTag(attToAppend) + ": " + getAttribute(attToAppend));
      }
   }

   private void printLDSAttributes(Gedcom gedcom, String containerID, GedcomElementWriter ew) throws Uploader.PrintException {
      ew.put("date", getAttribute("DATE"));
      ew.put("place", getPlace(gedcom));
      ew.put("temple", getAttribute("TEMP"));
      ew.put("stat", getAttribute("STAT"));
      ew.put("statdate", getAttribute("STATDATE"));
      if (this.getType() == Type.lds_child_sealing)
      {
         String famID = getAttribute("FAMC");
         if (!Utils.isEmpty(famID))
         {
            Family fam = gedcom.getFamilies().get(famID);
            if (fam != null)
            {
               ew.put("family_id", fam.getID());
            } else
            {
               logger.info(gedcom.logStr("Family ID \"" + famID +
                     "\" referred to in FAMC of SLGC of person is not valid"));
            }
         }
      }

      if (getNotes().size() > 0)
      {
         String notes = getNotes().get(0).getNote();
         for (int i=1; i < getNotes().size(); i++)
         {
            notes += ' ' + getNotes().get(i).getNote();
         }
         ew.put("note", notes);
      }
   }

   /**
    * Prints souce citations and notes
    * for when this event is an LDS event,
    * which is assumed by this method as
    * being the case when it is called.
    * @param gedcom
    * @param ec the EventContainer around this Event
    * @param buf to print the source citations and notes to
    * @throws Uploader.PrintException
    * @throws Gedcom.PostProcessException
    */
   public void printLDSSourceCitations(Gedcom gedcom, EventContainer ec, StringBuffer buf)
         throws Uploader.PrintException, Gedcom.PostProcessException
   {
      for (Citation cit : this.getCitations())
      {
         // Let's append any existing notes to the text.
         if (!Utils.isEmpty(cit.getPage()))
         {
            cit.appendText("Page: " + cit.getPage());
         }
         if (!Utils.isEmpty(cit.getDate()))
         {
            cit.appendText("Date: " + cit.getDate());
         }

         for (Note note : cit.getNotes())
         {
            cit.appendText(note.getNote());
         }
         GedcomElementWriter ew = new GedcomElementWriter("source_citation");
         ew.put("id", cit.getUpperID());
         ew.put("text", cit.getText());

         if (!Utils.isEmpty(cit.getId()))
         {
            Source source = gedcom.getSources().get(cit.getId());
            if (source != null)
            {
               ew.put("id", source.getID());
               cit.printReferences(ew);
            } else
            {
               throw new Uploader.PrintException ("Invalid source id \"" + cit.getId() +
                     "\" found in INDI \"" + ec.getID() + "\"", gedcom, ec.getID());
            }
         }
         ew.write(buf);
      }
   }
   // This map controls the ordering of the events
   // when they are printed in person and family pages.
   private static final Map <Type, Integer> type2rank = new HashMap <Type, Integer>();
   static
   {
      type2rank.put(Type.birth, 1);
      type2rank.put(Type.marriage, 2);
      type2rank.put(Type.christening, 3);
      type2rank.put(Type.death, 4);
      type2rank.put(Type.burial, 5);
   }

   /**
    * Controls the ordering of the events
    * @param o
    * @return the ordering int
    */
   public int compareTo(Object o) {
      Event other = (Event) o;

      if (type2rank.get(getType()) != null)
      {
         if (type2rank.get(other.getType()) == null)
         {
            return -1;
         } else
         {
            int rval = type2rank.get(getType()).compareTo(type2rank.get(other.getType()));
            if (rval != 0)
            {
               return rval;
            }
         }
      } else
      {
         if (type2rank.get(other.getType()) != null)
         {
            return 1;
         }
      }

      int rval = compareAtts(other);
      if (rval != 0)
      {
         return rval;
      }

      if (getType() != other.getType())
      {
         return getType().compareTo(other.getType());
      }

      // Let's try comparing the descriptions
      if(getDescription() != null)
      {
         if (!getDescription().equals(other.getDescription()))
         {
            return getDescription().compareTo(other.getDescription());
         }
      } else if (other.getDescription() != null)
      {
         if (!other.getDescription().equals(getDescription()))
         {
            return other.getDescription().compareTo(getDescription());
         }
      }

      // Let's try comparing the content
      if (getContent() != null)
      {
         if (!getContent().equals(other.getContent()))
         {
            return getContent().compareTo(other.getContent());
         }
      } else if (other.getContent() != null)
      {
         if (!other.getContent().equals(getContent()))
         {
            return other.getContent().compareTo(getContent());
         }
      }

      if (!(getCitations().size() == other.getCitations().size()))
      {
         return getCitations().size() - other.getCitations().size();
      }

      // The following line of code reports an event as being the same as itself (although it doesn't report all events, for an unknown reason).
//      gedcom.infoLine("Two events -- \"" + toString() + "\" and \"" + other.toString() + "\" are identical");
      return 0;
   }

   // Compares the attributes in the two events
   // with each other to determine ordering.
   private int compareAtts(Event other)
   {
      if (!Utils.isEmpty(getAttribute("DATE")))
      {
         String thisDate = getAttribute("DATE");
         String otherDate = other.getAttribute("DATE");
         Integer thisStdDate = new EventDate(thisDate).getDateSortKey();                 // method replaced Oct 2021 by Janet Bjorndahl
         Integer otherStdDate = new EventDate(otherDate).getDateSortKey();               // method replaced Oct 2021 by Janet Bjorndahl
         if (!thisStdDate.equals(otherStdDate))
         {
            return thisStdDate.compareTo(otherStdDate);
         }
      }
      for (String att : Event.STRING_ATTRIBUTES)
      {
         // We still this comparison for the date, if we have not
         // already returned.
         int rval = compareAtt(att, other);
         if (rval != 0)
         {
            return rval;
         }
      }
      return 0;
   }

   // Compares a selected attribute in this Event
   // with the same attribute in the other Event
   private int compareAtt(String att, Event other) {
      if (!Utils.isEmpty(getAttribute(att)) || !Utils.isEmpty(other.getAttribute(att)))
      {
         String thisType = getAttribute(att);
         String otherType = other.getAttribute(att);
         if (thisType == null)
         {
            thisType = "";
         }
         if (otherType == null)
         {
            otherType = "";
         }
         return thisType.compareTo(otherType);
      } else
      {
         return 0;
      }
   }

   /**
    * Prints out the LDS Event to the PrintWriter out
    * @param tagName of the LDS Event
    * @param gedcom
    * @param out to print the LDSEvent
    * @param ec Event container around this LDSEvent
    * @throws Uploader.PrintException
    * @throws Gedcom.PostProcessException
    */
   public void printWholeLDSEvent(String tagName, Gedcom gedcom, PrintWriter out, EventContainer ec)
         throws Uploader.PrintException, Gedcom.PostProcessException {
      GedcomElementWriter ew = new GedcomElementWriter(tagName);
      printLDSAttributes(gedcom, ec.getID(), ew);
      if (getCitations().size() > 0)
      {
         StringBuffer buf = new StringBuffer();
         printLDSSourceCitations(gedcom, ec, buf);
         ew.setSubXML('\n' + buf.toString());
      }
      out.print(ew.write());
   }

   /**
    * Appends to the content of the event the text from any ignored
    * GEDCOM tags inside this event
    * @param ignoredBucket containing a list of text from ignored subtags
    */
   public void eatIgnoredBucket(Collection <String> ignoredBucket) {
      for (String s : ignoredBucket)
      {
         appendContent(s);
      }
      ignoredBucket.clear();
   }

   // Types of WeRelate events
   public static enum Type {birth, alt_birth, christening, alt_christening,
                     death, alt_death, burial, alt_burial,
                     lds_baptism, lds_confirmation,
                     lds_child_sealing, lds_spouse_sealing,
                     lds_endowment, lds_blessing, lds_ordination,
                     /*Directly from person page list: */
                     Adoption, AncestralFileNumber, Baptism,
                     BarMitzvah,BatMitzvah,
                     Blessing,Caste,CauseOfDeath,Census,Citizenship,Confirmation,
                     Cremation,Degree,Education,Emigration,Employment,
                     Excommunication,FirstCommunion,Funeral,Graduation,
                     Illness,Immigration,Living,Medical,Military,
                     Mission,Namesake,Nationality,Naturalization,
                     Obituary,Occupation,Ordination,Pension,
                     PhysicalDescription,Probate,Property,
                     ReferenceNumber,Religion,Residence,Retirement,
                     SocSecNo,Stillborn,Will, marriage, alt_marriage,
                     unknown, divorce,
                     Annulment,DivorceFiling,Engagement,MarriageBanns,
                     MarriageContract,MarriageLicense,MarriageNotice,
                     MarriageSettlement,Separation,
                     Other, DNA                                            // DNA added Aug 2021 by Janet Bjorndahl
   }
   private Type type;

   public Type getType() {
      return type;
   }

   private static String [] EVEN_TYPES = {
         "Marriage Bond",
         "Appraisal", "Distribution List", "Emancipation", "Escape or Runaway",
         "Estate Inventory", "Estate Settlement", "First Appearance", "Freedmans Bureau",
         "Hired Away", "Homestead", "Household List", "Plantation Journal", "Purchase",
         "Recapture", "Relocation", "Sale", "Slave List",
         "Citizenship", "Employment", "Funeral", "Illness", "Living", "Obituary", "Pension", "Stillborn",    // These labels added Aug 2021 by Janet Bjorndahl 
         "Marriage Notice", "Separation", "Degree"                         // Some desktop software doesn't use specific GEDCOM tags for Separation and Degree
   };
   private static Map <String, String> EVEN_TYPES_MAP = new HashMap<String, String>();
   static
   {
      for (String s: EVEN_TYPES)
      {
         EVEN_TYPES_MAP.put(s.toLowerCase(), s);
      }
   }

   // Translates the enum WeRelate event type
   // to a string
   public String toString() {
      switch(getType())
      {
         case birth:
            return "Birth";
         case alt_birth:
            return "Alt Birth";
         case christening:
            return "Christening";
         case alt_christening:
            return "Alt Christening";
         case death:
            return "Death";
         case alt_death:
            return "Alt Death";
         case burial:
            return "Burial";
         case alt_burial:
            return "Alt Burial";
         case Adoption:
            return "Adoption";
         case AncestralFileNumber:
            return "Ancestral File Number";
         case Baptism:
            return "Baptism";
         case BarMitzvah:
            return "Bar Mitzvah";
         case BatMitzvah:
            return "Bat Mitzvah";
         case Blessing:
            return "Blessing";
         case Caste:
            return "Caste";
         case CauseOfDeath:
            return "Cause of Death";
         case Census:
            return "Census";
         case Citizenship:
            return "Citizenship";
         case Confirmation:
            return "Confirmation";
         case Cremation:
            return "Cremation";
         case Degree:
            return "Degree";
         case DNA:                          // added Aug 2021
            return "DNA";            
         case Education:
            return "Education";
         case Emigration:
            return "Emigration";
         case Employment:
            return "Employment";
         case Excommunication:
            return "Excommunication";
         case FirstCommunion:
            return "First Communion";
         case Funeral:
            return "Funeral";
         case Graduation:
            return "Graduation";
         case Illness:
            return "Illness";
         case Immigration:
            return "Immigration";
         case Living:
            return "Living";
         case Medical:
            return "Medical";
         case Military:
            return "Military";
         case Mission:
            return "Mission";
         case Namesake:
            return "Namesake";
         case Nationality:
            return "Nationality";
         case Naturalization:
            return "Natualization";
         case Obituary:
            return "Obituary";
         case Occupation:
            return "Occupation";
         case Ordination:
            return "Ordination";
         case Pension:
            return "Pension";
         case PhysicalDescription:
            return "Physical Description";
         case Probate:
            return "Probate";
         case Property:
            return "Property";
         case ReferenceNumber:
            return "Reference Number";
         case Religion:
            return "Religion";
         case Residence:
            return "Residence";
         case Retirement:
            return "Retirement";
         case Stillborn:
            return "Stillborn";
         case Will:
            return "Will";
         case marriage:
            return "Marriage";
         case alt_marriage:
            return "Alt Marriage";
         case divorce:
            return "Divorce";
         case Annulment:
            return "Annulment";
         case DivorceFiling:
            return "Divorce Filing";
         case Engagement:
            return "Engagement";
         case MarriageBanns:
            return "Marriage Banns";
         case MarriageContract:
            return "Marriage Contract";
         case MarriageLicense:
            return "Marriage License";
         case MarriageNotice:
            return "Marriage Notice";
         case MarriageSettlement:
            return "Marriage Settlement";
         case Separation:
            return "Separation";
         case Other:
            return "Other";
         case lds_baptism:
         case lds_child_sealing:
         case lds_spouse_sealing:
         case lds_endowment:
         case SocSecNo:              // no longer imported, changed Apr 2025 by Janet Bjorndahl (if added again, reinstate relevant code in formatTag)
//            return "Soc Sec No";
         case unknown:
         default:
            return null;
      }
   }
   
   // Returns WeRelate event type, for both standard and custom types 
   public String eventType() {
      String typeAtt = getAttribute("TYPE");
      if (typeAtt != null) 
      {
         String customEventType = EVEN_TYPES_MAP.get(typeAtt.trim().toLowerCase());
         if (customEventType != null)
         {
            return customEventType;
         } 
      }
      return toString();
   }

   /**
    *
    * @param type of event to set this to
    */
   public void setType(Type type) {
      this.type = type;
   }

   // Sets the type of this event to "Other", with the
   // label prepended to the description field of the event
   public void setOther(String label)
   {
      setType(Type.Other);
      prependDescription(label);
   }

   /**
    *
    * @param tag GEDCOM tag name to be used to determine the type
    * of the event
    */
   public void setType(String tag)
   {
      tag = tag.toUpperCase();
      if (isEvent(tag))
      {
         setType(EVENT_TYPES.get(tag));
      } else if (isOtherEvent(tag))
      {
         setOther(OTHER_TYPES.get(tag));
      }
      else
      {
         throw new RuntimeException("Invalid event tag name in Event.setType: " + tag);
      }
   }

   // Maps the GEDCOM tag name to the WeRelate event type.
   private static final Map<String, Type> EVENT_TYPES = new HashMap<String, Type> ();
   static {
      // LDS temple ordinances
      EVENT_TYPES.put("ENDL", Event.Type.lds_endowment);
      EVENT_TYPES.put("BAPL", Event.Type.lds_baptism);
      EVENT_TYPES.put("SLGC", Event.Type.lds_child_sealing);
      EVENT_TYPES.put("SLGS", Event.Type.lds_spouse_sealing);

      // Other event / fact tag names
      EVENT_TYPES.put("AFN", Event.Type.AncestralFileNumber);
      EVENT_TYPES.put("ADOP", Event.Type.Adoption);
      EVENT_TYPES.put("ADOPTION", Event.Type.Adoption);
      EVENT_TYPES.put("ALT. BIRTH", Type.alt_birth);
      EVENT_TYPES.put("ANUL", Event.Type.Annulment);
      EVENT_TYPES.put("ANNULMENT", Event.Type.Annulment);
      EVENT_TYPES.put("ARRIVAL", Event.Type.Immigration);
      EVENT_TYPES.put("ARVL", Event.Type.Immigration);
      EVENT_TYPES.put("ARRI", Event.Type.Immigration);
      EVENT_TYPES.put("BAPM", Event.Type.Baptism);
      EVENT_TYPES.put("BAPT", Event.Type.Baptism);
      EVENT_TYPES.put("BAPTISM", Event.Type.Baptism);
      EVENT_TYPES.put("CHRA", Event.Type.Baptism);              // added Aug 2021 (CHRA is the tag for Adult Christening)
      EVENT_TYPES.put("BARM", Event.Type.BarMitzvah);
      EVENT_TYPES.put("BASM", Event.Type.BatMitzvah);           // added Aug 2021 (Bas Mitzvah is the same as Bat Mitzvah and used by the GEDCOM standard)
      EVENT_TYPES.put("BATM", Event.Type.BatMitzvah);
      EVENT_TYPES.put("BAR_MITZVAH", Event.Type.BarMitzvah);
      EVENT_TYPES.put("BLES", Event.Type.Blessing);
      EVENT_TYPES.put("BIRT", Event.Type.birth);
      EVENT_TYPES.put("BIRTH", Event.Type.birth);
      EVENT_TYPES.put("BURI", Event.Type.burial);
      EVENT_TYPES.put("BURIAL", Event.Type.burial);
      EVENT_TYPES.put("CAST", Event.Type.Caste);
      EVENT_TYPES.put("CAUS", Event.Type.CauseOfDeath);
      EVENT_TYPES.put("CAUSE", Event.Type.CauseOfDeath);
      EVENT_TYPES.put("CENS", Event.Type.Census);
      EVENT_TYPES.put("CHR",  Event.Type.christening);
      EVENT_TYPES.put("CHRISTENING", Event.Type.christening);
      EVENT_TYPES.put("CONL", Event.Type.Confirmation);
      EVENT_TYPES.put("CONF", Event.Type.Confirmation);
      EVENT_TYPES.put("CREM", Event.Type.Cremation);
      EVENT_TYPES.put("DEAT", Event.Type.death);
      EVENT_TYPES.put("DEATH", Event.Type.death);
      EVENT_TYPES.put("_DEG", Event.Type.Degree);
      EVENT_TYPES.put("_DEGREE", Event.Type.Degree);
      EVENT_TYPES.put("DEPA", Event.Type.Emigration);
      EVENT_TYPES.put("DPRT", Event.Type.Emigration);
      EVENT_TYPES.put("DSCR", Event.Type.PhysicalDescription);
      EVENT_TYPES.put("DIV", Event.Type.divorce);
      EVENT_TYPES.put("DIVORCE", Event.Type.divorce);
      EVENT_TYPES.put("_DIV", Event.Type.divorce);
      EVENT_TYPES.put("DIVF", Event.Type.DivorceFiling);
      EVENT_TYPES.put("DNA", Event.Type.DNA);                     // added Aug 2021
      EVENT_TYPES.put("_DNA", Event.Type.DNA);                    // added Aug 2021
      EVENT_TYPES.put("_EXCM", Event.Type.Excommunication);
      EVENT_TYPES.put("EDUC", Event.Type.Education);
      EVENT_TYPES.put("EDUCATION", Event.Type.Education);
      EVENT_TYPES.put("EMIG", Event.Type.Emigration);
      EVENT_TYPES.put("EMIGRATION", Event.Type.Emigration);
      EVENT_TYPES.put("ENGA", Event.Type.Engagement);
      EVENT_TYPES.put("EVEN", Event.Type.Other);
      EVENT_TYPES.put("EVENT", Event.Type.Other);
      EVENT_TYPES.put("_FAC", Event.Type.Other);
      EVENT_TYPES.put("FCOM", Event.Type.FirstCommunion);
      EVENT_TYPES.put("GRAD", Event.Type.Graduation);
      EVENT_TYPES.put("GRADUATION", Event.Type.Graduation);
      EVENT_TYPES.put("HEAL", Event.Type.Medical);
      EVENT_TYPES.put("_HEIG", Event.Type.PhysicalDescription);
      EVENT_TYPES.put("_HEIGHT", Event.Type.PhysicalDescription);
      EVENT_TYPES.put("IMMI", Event.Type.Immigration);
      EVENT_TYPES.put("IMMIGRATION", Event.Type.Immigration);
      EVENT_TYPES.put("MARB", Event.Type.MarriageBanns);
      EVENT_TYPES.put("MARC", Event.Type.MarriageContract);
      EVENT_TYPES.put("MARL", Event.Type.MarriageLicense);
      EVENT_TYPES.put("MARR", Event.Type.marriage);
      EVENT_TYPES.put("MARRIAGE", Event.Type.marriage);
      EVENT_TYPES.put("MARS", Event.Type.MarriageSettlement);
      EVENT_TYPES.put("_MDCL", Event.Type.Medical);
      EVENT_TYPES.put("_MEDICAL", Event.Type.Medical);
      EVENT_TYPES.put("MILI", Event.Type.Military);
      EVENT_TYPES.put("_MILI", Event.Type.Military);
      EVENT_TYPES.put("_MILT", Event.Type.Military);
      EVENT_TYPES.put("_MILITARY_SERVICE", Event.Type.Military);
      EVENT_TYPES.put("_MISN", Event.Type.Mission);
      EVENT_TYPES.put("_NAMS", Event.Type.Namesake);
      EVENT_TYPES.put("NATI", Event.Type.Nationality);
      EVENT_TYPES.put("NATU", Event.Type.Naturalization);
      EVENT_TYPES.put("NATURALIZATION", Event.Type.Naturalization);
      EVENT_TYPES.put("OCCU", Event.Type.Occupation);
      EVENT_TYPES.put("OCCUPATION", Event.Type.Occupation);
      EVENT_TYPES.put("ORDI", Event.Type.Other);
      EVENT_TYPES.put("ORDN", Event.Type.Ordination);
      EVENT_TYPES.put("PROB", Event.Type.Probate);
      EVENT_TYPES.put("PROP", Event.Type.Property);
      EVENT_TYPES.put("REFE", Event.Type.ReferenceNumber);
      EVENT_TYPES.put("REFN", Event.Type.ReferenceNumber);
      EVENT_TYPES.put("REFERENCE", Event.Type.ReferenceNumber);
      EVENT_TYPES.put("RFN", Event.Type.ReferenceNumber);
      EVENT_TYPES.put("RELI", Event.Type.Religion);
      EVENT_TYPES.put("RELIGION", Event.Type.Religion);
      EVENT_TYPES.put("RESI", Event.Type.Residence);
      EVENT_TYPES.put("RESIDENCE", Event.Type.Residence);
      EVENT_TYPES.put("RETI", Event.Type.Retirement);
      EVENT_TYPES.put("SEPA", Event.Type.Separation);
      EVENT_TYPES.put("_SEPARATED", Event.Type.Separation);
      EVENT_TYPES.put("SSN", Event.Type.SocSecNo);
      EVENT_TYPES.put("SOC_", Event.Type.SocSecNo);
      EVENT_TYPES.put("SOC_SEC_NUMBER", Event.Type.SocSecNo);
      EVENT_TYPES.put("_UNKN", Event.Type.unknown);
      EVENT_TYPES.put("_WEIG", Event.Type.PhysicalDescription);
      EVENT_TYPES.put("_WEIGHT", Event.Type.PhysicalDescription);
      EVENT_TYPES.put("WILL", Event.Type.Will);
   }

   // This is used for determining the label to use for the
   // "Other" event type
   private static final Map <String, String> OTHER_TYPES  = new HashMap<String, String>();
   static
   {
      OTHER_TYPES.put("_ELEC", "Election");
      OTHER_TYPES.put("_ELECTED", "Elected");
      OTHER_TYPES.put("_DEATH_OF_SPOUSE", "Death of spouse");
      OTHER_TYPES.put("_NAMESAKE", "Namesake");
   }

   /**
    *
    * @param tag
    * @return whether the tag matches one of the event tag names
    */
   public static boolean isEvent(String tag)
   {
      return EVENT_TYPES.containsKey(tag);
   }

   /**
    *
    * @param tag
    * @return true if the event is one of the "Other" event type tags
    */
   public static boolean isOtherEvent(String tag)
   {
      return OTHER_TYPES.containsKey(tag);
   }

   /**
    * Construct an event if you know the tagName
    * @param tagName
    * @param gedcom
    */
   public Event(String tagName, Gedcom gedcom)
   {
      setType(tagName);
      this.gedcom = gedcom;
   }

   /**
    * If you don't already know the tag name
    * @param gedcom
    */
   public Event(Gedcom gedcom)
   {
      this.gedcom = gedcom;
   }

   //Attributes recognized as part of an event
   private static final String [] STRING_ATTRIBUTES = {
      "DATE", "DAT", "PLAC", "PLACE", "TEMP", "CAUS", "CAUSE",
         "STAT", "STATDATE", "FAMC", "FAMILY_CHILD",
            "TYPE", "ADDR", "ADDRESS", "RESN", "AGE"
   };
   private static final Set<String> STR_ATTS_SET = new HashSet<String>();
   static {
      for (String s : STRING_ATTRIBUTES)
      {
         STR_ATTS_SET.add(s);
      }
   }

   /**
    *
    * @param tagName
    * @return whether this tagName is a valid Event attribute
    */
   public static boolean isAttribute (String tagName)
   {
      return STR_ATTS_SET.contains(tagName) ||
            // These three attributes need special
            // handling and are not normal Event attributes
            Gedcom.isSOUR(tagName) ||
            tagName.equals("NOTE") ||
            tagName.equals("OBJE");
   }

   /**
    *
    * @param tagName
    * @return whether we should ignore this tag
    */
   public static boolean ignoreAttribute(String tagName)
   {
      return  Gedcom.isFAMS(tagName)
            || tagName.equals("_DATE2")
            || tagName.equals("CREM")
            || tagName.equals("CEME")
            || tagName.equals("_RPT_PHRS")
            || tagName.equals("_GODP")
            || tagName.equals("CORP")
            || tagName.equals("_OVER")
            || tagName.equals("AGE")
            || tagName.equals("_PLAC")
            || tagName.equals("MSTAT")
            || tagName.equals("OFFI")
            || Gedcom.isHUSB(tagName)
            || tagName.equals("WIFE")
            || tagName.equals("PLACE")
            || tagName.equals("_Description2")
            || tagName.equals("_PRIM")
            || tagName.equals("_HTITL")
            || tagName.equals("_WTITL");
   }

   // Output event types.  These are event types that
   // come in

   /**
    *
    * @param tagName
    * @return whether we should silently ignore the attribute
    * inside of an Event
    */
   public static boolean silentlyIgnoreAttribute(String tagName)
   {
      return tagName.equals("_SCRAPBOOK")
              || tagName.equals("PHON")
              || tagName.equals("PHONE")
              || tagName.equals("RIN")
              || tagName.equals("_SDATE")            // Desktop program's sort date moved from Ignore list to Silently Ignore list Aug 2021 by Janet Bjorndahl
              || tagName.equals("_PRIM_CUTOUT")
              || tagName.equals("_POSITION")
              || tagName.equals("_PHOTO_RIN")
              || tagName.equals("_FILESIZE")
              || tagName.equals("_CUTOUT")
              || tagName.equals("_UID");
   }

   private Map <String, String> atts = new HashMap <String, String> ();
   public void setAttribute(String tagName, String val)
   {
      if (isAttribute(tagName))
      {
         // If the attribute is a
         // restriction attribute,
         // it is not a normal attribute.
         if (tagName.equals("RESN"))
         {
            setRestriction(val);
         } else if (tagName.equals("DAT"))
         {
            atts.put("DATE", val);
         } else if (tagName.equals("PLACE"))
         {
            atts.put("PLAC", val);
         } else if (tagName.equals("FAMILY_CHILD"))
         {
            atts.put("FAMC", val);
         } else if (tagName.equals("CAUSE"))
         {
            atts.put("CAUS", val);
         } else if (tagName.equals("ADDRESS"))
         {
            atts.put("ADDR", val);
         }
         else
         {
            atts.put(tagName, val);
         }
      } else
      {
         throw new RuntimeException ("Attempted to set an attribute in event that doesn't exist!");
      }
   }

   /**
    *
    * @return attribute type name to be appended to the
    * person type in a family husband, wife or child tag.
    * For example, if the type of the Event is "birth", then
    * birth would be prepended (in the Family method) perhaps
    * to "child" to get "childbirth"
    */
   private String getAttributeTypeName()
   {
      return getAttributeTypeName(getType());
   }

   private static String getAttributeTypeName(Event.Type type)
   {
      switch(type)
      {
         case birth:
            return "birth";
         case death:
            return "death";
         case christening:
         case Baptism:
            return "chr";
         case burial:
            return "burial";
         default:
            return null;
      }
   }

   /**
    *
    * @param tagName
    * @return value of the attribute's tagName
    */
   public String getAttribute(String tagName)
   {
      return atts.get(tagName);
   }
}
