package org.werelate.gedcom;

import org.apache.log4j.Logger;
import org.lm.gedml.GedcomParser;
import org.werelate.util.Counter;
import org.xml.sax.*;
import org.xml.sax.helpers.DefaultHandler;
import org.werelate.util.PlaceUtils;
import org.werelate.util.Utils;

import javax.xml.xpath.XPathExpressionException;
import java.io.*;
import java.util.*;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

/**
 * Represents the Gedcom file and all of the
 * necessary data structures inside of it.
 * <p/>
 * This class handles the messages produced by the
 * GedcomParser object from the GedML project, and builds up
 * the data strucures as it handles the calls.
 * <p/>
 * Post processing is done on the data structures which
 * this class produces.
 */
public class Gedcom implements ContentHandler {
   private static final Logger logger = Logger.getLogger("org.werelate.gedcom.Gedcom");
   private static final int CUTOFF_DAY = 1700 * 365;
   private static final int TRUSTED_CUTOFF_DAY = 1550 * 365;

   private String userName = null;
   private boolean isTrustedUploader = false;

   private boolean isInvalid = false;
   public void setInvalid() {
      isInvalid = true;
   }

   public boolean isInvalid() {
      return isInvalid;
   }

   /**
    * @return the tree id associated with this GEDCOM
    */
   public int getTreeID() {
      return treeID;
   }

   private int treeID = -1;


   private void setUserName(String userName) {
      this.userName = userName;
   }

   private void setTreeID(int treeID) {
      this.treeID = treeID;
   }

   public boolean isTrustedUploader() {
      return isTrustedUploader;
   }

   private void setIsTrustedUploader(boolean isTrustedUploader) {
      this.isTrustedUploader = isTrustedUploader;
   }
   
   public int getCutoffDay() {
      return (isTrustedUploader ? TRUSTED_CUTOFF_DAY : CUTOFF_DAY);
   }

   // This says whether we're ignoring
   // the tag at the top of the stack
   //  and all subtags underneath this tag.
   // This allows us to ignore certain tags
   // and recover from unrecognized tag errors
   private boolean isIgnoring = false;


   private boolean isIgnoring() {
      return isIgnoring;
   }

   private void setIgnoring(boolean ignoring) {
      isIgnoring = ignoring;
   }

   /**
    * @return the user name associated with this
    *         gedcom
    */
   public String getUserName() {
      return userName;
   }

   private Stack<Tag> tagStack = new Stack<Tag>();

   private Map<String, Person> people = new TreeMap<String, Person>();

   /**
    * @return Map ID -> people of all people in this GEDCOM
    */
   public Map<String, Person> getPeople() {
      return people;
   }

   private Person currPerson = null;

   private Map<String, Family> families = new TreeMap<String, Family>();

   /**
    * @return Map ID -> family of all families associated with this GEDCOM
    */
   public Map<String, Family> getFamilies() {
      return families;
   }

   private Family currFamily = null;

   /**
    * Maps ID -> Source for all of the sources found in the gedcom.
    */
   private Map<String, Source> sources = new TreeMap<String, Source>();
   private Map<Source, String> source2id = new HashMap<Source, String>();

   /**
    * @return Map GEDCOM ID -> source of all souces in this GEDCOM
    */
   public Map<String, Source> getSources() {
      return sources;
   }

   private Source currSource = null;
   private Name currName = null;

   private Map<String, Repository> repos = new HashMap<String, Repository>();

   private Map<String, Repository> getRepositories() {
      return repos;
   }

   private void setRepos(Map<String, Repository> repos) {
      this.repos = repos;
   }

   private Map<String, Note> notes = new HashMap<String, Note>();

   /**
    * @return Map ID -> top level note, for all notes in this GEDCOM
    */
   public Map<String, Note> getNotes() {
      return notes;
   }

   private Map<String, String> todos = new HashMap<String, String>();

   /**
    * @return Map ID -> To-Do of all TODOs in this GEDCOM
    */
   public Map<String, String> getTodos() {
      return todos;
   }

   private Map<String, Image> images = new HashMap<String, Image>();

   /**
    * @return Map ID -> top level image, for all top level images in this GEDCOM
    */
   public Map<String, Image> getImages() {
      return images;
   }

   private Repository currRepo = null;
   private Event currEvent = null;
   private Citation currCitation = null;
   private Family.Child currChild = null;
   private Image currImage = null;
   private Name.Title currTitle = null;
   private Data currData = null;
   private Note currNote = null;
   private String pedi = null;
   private boolean isPreferredSubPerson = false;

   public void setDocumentLocator(Locator locator) {
      //To change body of implemented methods use File | Settings | File Templates.
   }

   public void startDocument() throws SAXException {
      //To change body of implemented methods use File | Settings | File Templates.
   }

   public void endDocument() throws SAXException {
      //To change body of implemented methods use File | Settings | File Templates.
   }

   public void startPrefixMapping(String prefix, String uri) throws SAXException {
      //To change body of implemented methods use File | Settings | File Templates.
   }

   public void endPrefixMapping(String prefix) throws SAXException {
      //To change body of implemented methods use File | Settings | File Templates.
   }

   private boolean unknownTag = false;

   /**
    * @return whether unknown tags were found in this GEDCOM
    */
   public boolean isUnknownTag() {
      return unknownTag;
   }

   // If we have found a tag that is not recognized, the tag is
   // still pushed down on the stack so that we can recognize when
   // we leave the tag that is being ignored. Of course, a warning
   // is printed to the logger, and we set unknownTag = true, which
   // means that there are unknown tags in this GEDCOM
   private void pushUnknown(String parentName, String childName) {
      String message = getUserName() + ':' + getFN() + ": " + "Line " + getLineNumber() +
                        " Unexpected tag inside " + parentName + ": " + childName + ", ignoring";
      if (ignoreUnexpectedTags)
      {
         logger.info(message);
      } else
      {
         logger.warn(message);
         this.incrementWarnings();
      }
      unknownTag = true;
      finishIgnore(childName);
   }

   // Called when we recognize a tag, but we wish to ignore it by
   // putting the contents of the tag in the text of the object
   // in which the ignored tag was found. In this case, we also
   // print out an "info" to the logger.
   private void pushIgnore(String parentName, String childName) {
      ignoreInfo(parentName, childName);
      finishIgnore(childName);
   }

   private void ignoreInfo(String parentName, String childName) {
      logger.info(getUserName() + ':' + getFN() + ": " + "Line " + getLineNumber() +
            ": Ignoring tag inside " + parentName + ": " + childName);
   }

   // Means that we print out an info; however, in this case, we don't print out
   // the contents of the tag to text.
   private void pushNoTextJustIgnore(String parentName, String childName) {
      ignoreInfo(parentName, childName);
      setSilentlyIgnoring(true);
      finishIgnore(childName);
   }

   private int ignoreLevel = -1;
   // This finishes the setup of the ignored tag
   // after we have decided to ignore it.
   private void finishIgnore(String childName) {
      setIgnoring(true);
      Tag newTag = new Tag(childName, null, getLineNumber());

      if (!this.isSilentlyIgnoring()) {
         childName = translateIgnoredTag(childName);
         newTag.append(childName + ": ");
      }
      ignoreLevel = tagStack.size();
      tagStack.push(newTag);
   }

   // Called when we don't want to print out an info or
   // put any of the text of the tag in the text of the parent element
   private void pushSilentIgnore(String parentName, String childName) {
      logger.debug(getUserName() + ':' + getFN() + ": " + "Line " + getLineNumber() +
            ": Ignoring tag inside " + parentName + ": " + childName);
      setSilentlyIgnoring(true);
      finishIgnore(childName);
   }

   /**
    * generates a log string to be used when printing out
    * infos and warnings. It prints out the username, filename,
    * and the message.
    */
   public String logStr(String msg) {
      return getUserName() + ':' + getFN() + ": " + msg;
   }

   /**
    * Prints out only a line statement
    */
   public String lineStatement() {
      return "Line: " + getLineNumber() + ": ";
   }

   /**
    * @param warning to be printed
    */
   public void warn(String warning) {
      this.getLineNumber();
      incrementWarnings();
      logger.warn(logStr(warning));
   }

   /**
    * @param info to be printed along with a line number
    */
   public void infoLine(String info) {
      logger.info(lineStatement() + logStr(info));
   }

   boolean isInIndiSchema = false;
   boolean isInFamSchema = false;
   private Schema schema = new Schema();
   private String currSchemaKey = null;
   private String currSchemaValue = null;
   private boolean primaryChildOf = false;
   private AlternateName currAltName = null;

   private static boolean isCONT(String tag)
   {
      return tag.equals("CONT") || tag.equals("CONTINUED");
   }

   private static boolean isCONC(String tag)
   {
      return tag.equals("CONC") || tag.equals("CONCATENATION");
   }

   private static boolean isCONTCONC(String tag)
   {
      return isCONC(tag) || isCONT(tag);
   }

   private static boolean isINDI(String tag)
   {
      return tag.equals("INDI") || tag.equals("INDIVIDUAL");
   }

   private static boolean isFAM(String tag)
   {
      return tag.equals("FAM") || tag.equals("FAMILY");
   }

   private static boolean isALIA(String tag)
   {
      return tag.equals("ALIA") || tag.equals("ALIAS");
   }

   private static boolean isFAMC(String tag)
   {
      return tag.equals("FAMC") || tag.equals("FAMILY_CHILD");
   }

   public static boolean isSOUR(String tag)
   {
      return tag.equals("SOUR") || tag.equals("SOURCE");
   }

   public static boolean isADDR(String tag)
   {
      return tag.equals("ADDR") || tag.equals("ADDRESS");
   }

   public static boolean isFAMS(String tag)
   {
      return tag.equals("FAMS") || tag.equals("FAMILY_SPOUSE");
   }

   public static boolean isPHON(String tag)
   {
      return tag.equals("PHON") || tag.equals("PHONE");
   }

   public static boolean isTITL(String tag)
   {
      return tag.equals("TITL") || tag.equals("TITLE");
   }

   public static boolean isHUSB(String tag)
   {
      return tag.equals("HUSB") || tag.equals("HUSBAND");
   }

   public static boolean isCHIL(String tag)
   {
      return tag.equals("CHIL") || tag.equals("CHILD");
   }

   public static boolean isREPO(String tag)
   {
      return tag.equals("REPO") || tag.equals("REPOSITORY");
   }

   public static boolean isCALN(String tag)
   {
      return tag.equals("CALN") || tag.equals("CALL_NUMBER");
   }

   public static boolean isMEDI(String tag)
   {
      return tag.equals("MEDI") || tag.equals("MEDIA");
   }

   public static boolean isAUTH(String tag)
   {
      return tag.equals("AUTH") || tag.equals("AUTHOR");
   }

   public static boolean isPUBL(String tag)
   {
      return tag.equals("PUBL") || tag.equals("PUBLICATION");
   }

   public static boolean isADOP(String tag)
   {
      return tag.equals("ADOP") || tag.equals("ADOPTION");
   }

   public static final String NO_TAG_PRESENT_LOCALNAME = "WERELATE_NO_TAG_PRESENT";

   // Tags that we want to ignore yet still copy to
   // the text of the Person page
   private static Set <String> SILENT_IGNORE = new HashSet<String>(Arrays.asList(
           "ADDR", "ADDRESS", "ALIV", "_EMAF", "PHON", "PHONE", "EMAIL", "_EMAIL", "FAX", "EMAL",
           "RIN", "MRIN", // can't include _UID here because that tag is needed in endElement; it's excluded somewhere but I don't know how
           "CHAN", "_SCRAPBOOK", "_NONE", "DESI", "ANCI", "_TAG4", "_PHOT", "_UPD", "_TAG", "_NEW",
           "_PRIM_CUTOUT", "_POSITION", "_PHOTO_RIN", "_FILESIZE", "_CUTOUT"
   ));
   private static boolean shouldSilentlyIgnore(String localName) {
      return SILENT_IGNORE.contains(localName);
   }

   /**
    * Standard implementation of the ContentHandler.startElement
    *
    * @param uri
    * @param localName
    * @param qName
    * @param atts
    * @throws SAXException
    */
   public void startElement(String uri, String localName, String qName, Attributes atts) throws SAXException {
      String id = getID(atts);

      if (isIgnoring()) {
         // do nothing, because we want to
         // ignore all child tags of the ignored
         // tag (which is currently at the top of the
         // stack).

         // However, if we're not silently ignoring the tag,
         // but doing a regular ignore, we want to print out the contents
         // to the text of the XML.
         Tag peekTag = tagStack.get(ignoreLevel);

         // The reason we don't want to append anything to the current
         // tag if the locaName = CONT or CONC is because those tags
         // are already appended to the top tags content by the
         // method "characters"
         if (!(this.isSilentlyIgnoring()
               || isCONTCONC(localName))) {
            if (!PlaceUtils.isEmpty(peekTag.getContent()) && !peekTag.getContent().endsWith("\n")) {
               peekTag.append("\n");
            }
            // Gets human readable form of the tag
            localName = translateIgnoredTag(localName);
            peekTag.append(localName + ": ");
         } else if (isCONT(localName)) {
            peekTag.append("\n");
         }
         Tag newTag = new Tag(localName, id, getLineNumber());
         tagStack.push(newTag);
      } else if (PlaceUtils.isEmpty(localName)) {
         // Then the tag name is empty
         this.pushSilentIgnore("Empty tag is child", localName);
      } else if (tagStack.size() == 0) {
         // Then we're at a top level object,
         // such as INDI, FAM, SOUR, etc.
         startTopLevel(localName, id);
      } else {
         // peekName is the name of the tag at the top of the
         // stack.
         String peekName = tagStack.peek().getName();
         // This is the new tag that will be pushed onto the stack,
         // unless there's a problem
         Tag newTag = new Tag(localName, id, getLineNumber());
         if (isCONTCONC(peekName)) {
            // If there is a tag inside a CONT or CONC field, we cause
            // it to be treated as if the parent is the parent of
            // the CONT or CONC field.
            peekName = tagStack.get(tagStack.size() - 2).getName();
         }
         if (isCONTCONC(localName)) {
            // We always accept these two tags as a continuation of
            // any field
            tagStack.push(newTag);
         } else if (isSOUR(localName))
         {
            tagStack.push(newTag);
            startCitation(id);
         } else if (localName.equals("NOTE"))
         {
            tagStack.push(newTag);
         }
         else if (isINDI(peekName)) {
            if (isInIndiSchema) {
               // This means that we're inside of a schema which defines tag labels,
               // not a regular INDI tag.

               // All of the schema labeled tags
               // start with a "_"
               if (localName.startsWith("_")) {
                  tagStack.push(newTag);
                  currSchemaKey = localName;
               } else {
                  pushUnknown(peekName, localName);
               }
            }
            // isOtherEvent are event types which will end up with type="Other"
            // in the tag
            else if (Event.isEvent(localName) || Event.isOtherEvent(localName)) {
               tagStack.push(newTag);
               currEvent = new Event(localName, this);
            } else if (Person.isAttribute(localName)) {
               tagStack.push(newTag);
               if (localName.equals("NAME")) {
                  currName = new Name();
               } else if (localName.equals("OBJE")) {
                  currImage = new Image();
               } else if (isTITL(localName)) {
                  currTitle = new Name.Title();
               } else if (isALIA(localName)
                     || localName.equals("NICK")) {
                  currAltName = new AlternateName("Alt Name");
               }
            } else if (schema.contains(peekName, localName)) {
               tagStack.push(newTag);
               currEvent = new Event(this);
               currEvent.setOther(schema.get(peekName, localName));
            } else if (shouldSilentlyIgnore(localName)) {
               pushSilentIgnore(peekName, localName);
            } else if (Person.shouldIgnore(localName)) {
               pushIgnore(peekName, localName);
            } else {
               pushUnknown(peekName, localName);
            }
         } else if (isFAM(peekName)) {
            if (isInFamSchema) {
               // This means that we're inside of a schema,
               // not a regular FAM tag.
               if (localName.startsWith("_")) {
                  tagStack.push(newTag);
                  currSchemaKey = localName;
               } else {
                  pushUnknown(peekName, localName);
               }
            } else if (Event.isEvent(localName) || Event.isOtherEvent(localName)) {
               tagStack.push(newTag);
               currEvent = new Event(localName, this);
            } else if (Family.isAttribute(localName)) {
               tagStack.push(newTag);
               if (isHUSB(localName) ||
                     localName.equals("WIFE")) {
                  // isPreferredSubPerson might
                  // later be changed to true
                  // if there is a "PRIMARY"
                  // sub tag
                  isPreferredSubPerson = false;
               }
               if (isCHIL(localName)) {
                  // isPreferredSubPerson might
                  // later be changed to true
                  // if there is a "PRIMARY"
                  // sub tag
                  isPreferredSubPerson = false;
                  currChild = new Family.Child();
               } else if (localName.equals("OBJE")) {
                  currImage = new Image();
               }
            } else if (schema.contains(peekName, localName)) {
               tagStack.push(newTag);
               currEvent = new Event(this);
               currEvent.setOther(schema.get(peekName, localName));
            } else if (shouldSilentlyIgnore(localName)) {
               pushSilentIgnore(peekName, localName);
            } else if (Family.shouldIgnore(localName)) {
               pushIgnore(peekName, localName);
            } else {
               pushUnknown(peekName, localName);
            }
         } else if (peekName.equals(currSchemaKey)) {
            if (localName.equals("LABL")) {
               tagStack.push(newTag);
            } else {
               pushUnknown(peekName, localName);
            }
         } else if (isSOUR(peekName)) {
            if (tagStack.size() > 1) {
               // This means we're in a citation,
               // not a top-level source tag.
               if (localName.equals("PAGE") ||
                     localName.equals("TEXT") ||
                     localName.equals("DATE"))
               {
                  tagStack.push(newTag);
               } else if (localName.equals("DATA")) {
                  tagStack.push(newTag);
                  currData = new Data();
               } else if (localName.equals("OBJE")) {
                  tagStack.push(newTag);
                  currImage = new Image();
               } else if (isSubNoteTitleText(localName)) {
                  tagStack.push(newTag);
               } else if (localName.equals("QUAY")) {
                  tagStack.push(newTag);
               } else if (localName.equals("_VERI") ||
                     localName.equals("REFN")) {
                  pushIgnore(peekName, localName);
               } else if (localName.equals("_RIN")) {
                  pushSilentIgnore(peekName, localName);
               } else {
                  pushUnknown("SOUR citation tag", localName);
               }
            } else {
               // We're in a top-level source tag
               if (Source.isSubSource(localName)) {
                  if (isREPO(localName)) {
                     currRepo = new Repository();
                  } else if (localName.equals("OBJE")) {
                     currImage = new Image();
                  }
                  tagStack.push(newTag);
               } else if (localName.equals("_ITALIC") ||
                     localName.equals("_PAREN") ||
                     localName.equals("CHAN") ||
                     localName.equals("_MASTER") ||
                     localName.equals("RIN") ||
                     localName.equals("UID") ||
                     localName.equals("_UID") ||
                     localName.equals("_QUOTED")) {
                  pushSilentIgnore("SOUR top-level ", localName);
               } else if (Source.shouldIgnore(localName)) {
                  pushIgnore("SOUR top-level ", localName);
               } else {
                  pushUnknown("SOUR regular source", localName);
               }
            }
         } else if (isREPO(peekName)) {
            if (isSubRepo(localName)) {
               tagStack.push(newTag);
            } else if (localName.equals("_EMAIL") ||
                  localName.equals("_URL") ||
                  localName.equals("WWW")) {
               pushIgnore(peekName, localName);
            } else {
               pushUnknown(peekName, localName);
            }
         } else if (peekName.equals("HEAD")) {
            if (localName.equals("_SCHEMA") || localName.equals("SCHEMA")) {
               tagStack.push(newTag);
            } else {
               pushSilentIgnore("HEAD", localName);
            }
         } else if (peekName.equals("_TODO")) {
            pushIgnore(peekName, localName);
         } else if (peekName.equals("_SCHEMA") || peekName.equals("SCHEMA")) {
            if (isINDI(localName)) {
               isInIndiSchema = true;
               tagStack.push(newTag);
            } else if (isFAM(localName)) {
               isInFamSchema = true;
               tagStack.push(newTag);
            } else {
               pushUnknown(peekName, localName);
            }
         } else if (peekName.equals("NAME")) {
            if (isSubName(localName)) {
               tagStack.push(newTag);
               if (localName.equals("NICK")
                     || localName.equals("_AKA")
                     || localName.equals("_AKAN")) {
                  currAltName = new AlternateName("Alt Name");
               }
            } else if (localName.equals("TYPE"))
            {
               pushIgnore(peekName, localName);
            } else if (localName.equals("SPFX"))
            {
               pushSilentIgnore(peekName, localName);
            }
            else {
               pushUnknown(peekName, localName);
            }
         } else if (peekName.equals("ALIA")) {
            if (isSubName(localName))
            {
               tagStack.push(newTag);
            } else
            {
               pushUnknown(peekName, localName);
            }
         }
         else if (peekName.equals("NOTE")) {
            if (isSubNoteTitleText(localName) ||
                  localName.equals("_TITLE")) {
               tagStack.push(newTag);
            } else if (localName.equals("OBJE"))
            {
               tagStack.push(newTag);
               currImage = new Image();
            } else if (localName.equals("CHAN") ||
                  localName.equals("_AREA") ||
                  localName.equals("_ASID"))
            {
               this.pushSilentIgnore(peekName, localName);
            }
            else {
               pushUnknown(peekName, localName);
            }
         } else if (isADDR(peekName)) {
            if (isSubNoteTitleText(localName)
                  || isSubAddress(localName)) {
               tagStack.push(newTag);
            } else if (localName.equals("OBJE"))
            {
               tagStack.push(newTag);
               currImage = new Image();
            }
            else if (localName.equals("_SORT") ||
                  localName.equals("MAP")) {
               pushIgnore(peekName, localName);
            } else {
               pushUnknown(peekName, localName);
            }
         } else if (isTITL(peekName)) {
            if (isSubNoteTitleText(localName)) {
               tagStack.push(newTag);
            } else if (isSOUR(localName)) {
               if (currCitation != null) {
                  throw new RuntimeException("I'm trying to create a citation inside of a citation!");
               }
            } else {
               pushUnknown(peekName, localName);
            }
         } else if (peekName.equals("TEXT")) {
            if (isSubNoteTitleText(localName)) {
               tagStack.push(newTag);
            } else {
               pushUnknown("TEXT", localName);
            }
         } else if (Event.isEvent(peekName) || Event.isOtherEvent(peekName)
               || schema.contains(peekName)) {
            if (Event.isAttribute(localName)) {
               tagStack.push(newTag);
               if (localName.equals("OBJE")) {
                  currImage = new Image();
               }
            } else if (localName.equals("_ALT_BIRTH")) {
               pushNoTextJustIgnore(peekName, localName);
            } else if (Event.silentlyIgnoreAttribute(localName)) {
               pushSilentIgnore(peekName, localName);
            } else if (Event.ignoreAttribute(localName)) {
               pushIgnore(peekName, localName);
            } else {
               pushUnknown("Event \"" + peekName + "\"", localName);
            }
         } else if (isCHIL(peekName)) {
            if (localName.equals("_MREL")
                  || localName.equals("_FREL")
                  || isADOP(localName)
                  || localName.equals("_PREF")
                  || localName.equals("_STAT")) {
               tagStack.push(newTag);
            } else if (localName.equals("CSTA")) {
               this.pushNoTextJustIgnore(peekName, localName);
            } else if (localName.equals("SLGC")) {
               pushSilentIgnore(peekName, localName);
            } else if ((Event.isEvent(localName) && !isADOP(localName))
                  || localName.equals("FOST")) {
               pushIgnore(peekName, localName);
            } else {
               pushUnknown(peekName, localName);
            }
         } else if (peekName.equals("OBJE")) {
            if (Image.isAttribute(localName)) {
               tagStack.push(new Tag(localName, id, getLineNumber()));
               if (localName.equals("DATA")) {
                  currData = new Data();
               } else if (localName.equals("SOUR"))
               {
                  if (currCitation != null)
                  {
                     this.warn("Creating a citation inside of an image, but there is already a currCitation!");
                  } else
                  {
                    this.startCitation(id);
                  }
               }
            } else if (Image.shouldSilentlyIgnore(localName)) {
               pushSilentIgnore("OBJE", localName);
            } else {
               pushUnknown("OBJE", localName);
            }
         } else if (peekName.equals("DATA")) {
            if (localName.equals("DATE")) {
               tagStack.push(newTag);
            } else if (localName.equals("TEXT")) {
               tagStack.push(newTag);
            } else {
               pushUnknown("DATA", localName);
            }
         } else if (peekName.equals("STAT")) {
            if (localName.equals("DATE")) {
               tagStack.push(newTag);
            } else {
               pushUnknown(peekName, localName);
            }
         } else if (isFAMC(peekName)) {
            if (localName.equals("_PRIMARY")) {
               tagStack.push(newTag);
            } else if (localName.equals("PEDI")) {
               tagStack.push(newTag);
            } else if (isADOP(localName)) {
               pushIgnore(peekName, localName);
            } else {
               pushUnknown(peekName, localName);
            }
         } else if (isCALN(peekName)) {
            if (isMEDI(localName)) {
               tagStack.push(newTag);
            } else {
               pushUnknown(peekName, localName);
            }
         } else if (peekName.equals("PEDI")) {
            if (localName.equals("_WIFE") ||
                  localName.equals("_HUSB")) {
               pushNoTextJustIgnore("PEDI", localName);
            } else {
               pushUnknown(peekName, localName);
            }
         } else if (isALIA(peekName)
               || peekName.equals("NICK")) {
            if (localName.equals("TYPE"))
            {
               pushIgnore(peekName, localName);
            }
            else {
               pushUnknown(peekName, localName);
            }
         } else if (peekName.equals("FILE")) {
            if (localName.equals("FORM")) {
               pushSilentIgnore(peekName, localName);
            } else if (isTITL(localName))
            {
               if (currImage != null)
               {
                  tagStack.push(newTag);
               } else
               {
                  pushUnknown(peekName, localName);
               }
            } else
            {
               pushUnknown(peekName, localName);
            }
         } else if (isHUSB(peekName) ||
               peekName.equals("WIFE")) {
            if (localName.equals("_PREF")) {
               tagStack.push(newTag);
            } else {
               pushUnknown(peekName, localName);
            }
         } else if (peekName.equals("DATE"))
         {
            if (localName.equals("SOUR") &&
                  tagStack.size() > 1 &&
                  Event.isEvent(tagStack.elementAt(tagStack.size()-2).getName()))
            {
               this.startCitation(id);
            }
            else if (localName.equals("TIME"))
            {
               pushIgnore(peekName, localName);
            } else
            {
               pushUnknown(peekName, localName);
            }
         } else if (peekName.equals("_STAT"))
         {
            if (localName.equals("DATE"))
            {
               tagStack.push(newTag);
            } else
            {
               pushUnknown(peekName, localName);
            }
         } else if (peekName.equals("TYPE"))
         {
            if(localName.equals("DATE"))
            {
               pushIgnore(peekName, localName);
            } else
            {
               pushUnknown(peekName, localName);
            }
         }
         else if (hasSubText(peekName)) {
            if (isSubNoteTitleText(localName)) {
               tagStack.push(newTag);
            } else {
               pushUnknown(peekName, localName);
            }
         } else {
            pushUnknown(peekName, localName);
         }
      }
   }

   // Creates a new citation object and sets the id
   private void startCitation(String id) {
      currCitation = new Citation();
      currCitation.setId(id);
   }

   // Returns whether the tag defined by the
   // peekName can have subText elements such as
   // "NOTE" or "ABBR" elements
   private boolean hasSubText(String peekName) {
      return peekName.equals("OCCU")
            || isPUBL(peekName)
            || isAUTH(peekName)
            || peekName.equals("ABBR")
            || peekName.equals("PLAC")
            || peekName.equals("PAGE");
   }

   /**
    * Translates the given localName tag label into
    * a more human-readable format
    *
    * @param localName GEDCOM tag name to be translated
    * @return human readable version of the tagName
    */
   public String translateIgnoredTag(String localName) {
      if (Tag.IGNORED_TAG_LABELS.containsKey(localName)) {
         localName = Tag.IGNORED_TAG_LABELS.get(localName);
      }
      return localName;
   }

   private boolean isSubRepo(String localName) {
      return localName.equals("NAME") ||
            isADDR(localName) ||
            isPHON(localName) ||
            localName.equals("NOTE") ||
            isCALN(localName);
   }

   private boolean isSubNoteTitleText(String localName) {
      return localName.equals("NOTE")
            || localName.equals("ABBR");
   }

   private boolean isSubName(String localName) {
      return localName.equals("GIVN")
            || localName.equals("SURN")
            || localName.equals("NSFX")
            || localName.equals("NPFX")
            || localName.equals("NICK")
            || localName.equals("_MARNM")
            || localName.equals("_MAR")
            || localName.equals("_AKA")
            || localName.equals("_AKAN")
            || isSOUR(localName)
            || localName.equals("NOTE")
            || localName.equals("DATE");
   }

   private boolean isSubCitation(String localName) {
      return localName.equals("PAGE")
            || localName.equals("QUAY");
   }


   // Called to start a GEDCOM top-level object
   private void startTopLevel(String localName, String id) throws SAXException {
      if (((isINDI(localName) ||
            isFAM(localName) ||
            isSOUR(localName) ||
            isREPO(localName) ||
            localName.equals("NOTE")) && id != null) ||
            localName.equals("_TODO") ||
            localName.equals("OBJE") ||
            localName.equals("HEAD"))
      {
         tagStack.push(new Tag(localName, id, getLineNumber()));
         if (isINDI(localName)) {
            currPerson = new Person();
         } else if (isFAM(localName)) {
            currFamily = new Family();
         } else if (isSOUR(localName)) {
            currSource = new Source();
         } else if (isREPO(localName)) {
            currRepo = new Repository();
         } else if (localName.equals("NOTE")) {
            currNote = new Note();
         } else if (localName.equals("OBJE")) {
            currImage = new Image();
         }
      } else if (!localName.toUpperCase().equals("GED"))
      {
         this.pushSilentIgnore(null, localName);
      }
   }

   // Returns the id, if it exists, in the
   // attributes passed into the startElement function.
   private String getID(Attributes atts) {
      String id = null;
      for (int i = 0; i < atts.getLength(); i++) {
         if (atts.getLocalName(i).equals("ID") || atts.getLocalName(i).equals("REF")) {
            if (!PlaceUtils.isEmpty(atts.getValue(i))) {
               id = atts.getValue(i);
            }
            break;
         }
      }
      return id;
   }

   boolean silentlyIgnoring = false;

   private boolean isSilentlyIgnoring() {
      return silentlyIgnoring;
   }

   private void setSilentlyIgnoring(boolean silentlyIgnoring) {
      this.silentlyIgnoring = silentlyIgnoring;
   }

   // Ends the current source citation in the case that it is contained within
   // a ReferenceContainer object
   private void endCurrCitation(ReferenceContainer destination, String content, List<String> ignoredBucket)
   {
		if (currCitation != null) {
			currCitation.appendText(content);
			currCitation.eatIgnoredBucket(ignoredBucket);
			if (destination != null)
			{
				destination.addCitation(currCitation);
			} else
			{
				this.warn(this.lineStatement() + "Destination while adding citation is null");
			}
			currCitation = null;
		}
   }

   // Ends the current source citation in the case that it is contained within
   // an EventContainer object
   private void endCurrCitation(EventContainer destination, String content, List<String> ignoredBucket) {
		if (currCitation != null) {
			currCitation.appendText(content);
			currCitation.eatIgnoredBucket(ignoredBucket);
			destination.addCitation(currCitation);
			currCitation = null;
		}
   }

   // Implementation of the standard ContentHandler.endElement method
   public void endElement(String uri, String localName, String qName) throws SAXException {
      Tag popped;

      if (isIgnoring()) {
         Tag tag = tagStack.pop();
         if ((tagStack.size() == ignoreLevel) &&
               localName.equals(tag.getName())) {
            if (!isSilentlyIgnoring()) {
               // The ignored bucket will hopefully be
               // eaten by one of the parent tags
               tagStack.peek().addToIgnoredBucket(tag.getContent());
            }
            setIgnoring(false);
            setSilentlyIgnoring(false);
            ignoreLevel = -1;
         }
      } else if (tagStack.size() > 0 && (popped = tagStack.pop()).getName().equals(localName)) {
         String content = popped.getContent();
         String id = popped.getID();
         List<String> ignoredBucket = popped.getIgnoredBucket();
         int lineNum = popped.getLineNum();

         // We start with the top level tags.
         if (tagStack.size() == 0) {
            if (isINDI(localName)) {
               endINDI(id, ignoredBucket, content);
            } else if (isFAM(localName)) {
               endFAM(id, ignoredBucket, content);
            } else if (isSOUR(localName)) {
               endSOUR(id, ignoredBucket);
            } else if (isREPO(localName)) {
               endREPO(id, ignoredBucket);
            } else if (localName.equals("NOTE")) {
               endNOTE(id, content, ignoredBucket);
            } else if (localName.equals("HEAD")) {
               ignoredBucket.clear();
            } else if (localName.equals("_TODO")) {
               String todoText = "TODO:\n";
               for (String note : ignoredBucket) {
                  todoText += note + '\n';
               }
               ignoredBucket.clear();
               todos.put(id, todoText);
            } else if (localName.equals("OBJE")) {
               images.put(id, currImage);
               ignoredBucket.clear();
               currImage = null;
            }
            if (ignoredBucket.size() > 0) {
               this.warn(this.lineStatement() + " Bucket not emptied after top-level object");
               ignoredBucket.clear();
            }
         } else {
            try {
               String peek = tagStack.peek().getName();
               if (isCONTCONC(peek))
               {
                  // This causes us to end the current tag
                  // as if its parent is the parent of the
                  // CONC or CONT tag.
                  peek = tagStack.get(tagStack.size() - 2).getName();
               }
               if (isCONT(localName)) {
                  tagStack.peek().append('\n' + content);
               } else if (isCONC(localName)) {
                  tagStack.peek().append(content);
               } else if (isFAM(peek)) {
                  if (localName.equals(currSchemaKey)) {
                     endSchemaEntry(peek);
                  } else {
                     endSubFAM(peek, popped);
                  }
               } else if (isINDI(peek)) {
                  if (localName.equals(currSchemaKey)) {
                     endSchemaEntry(peek);
                  } else {
                     endSubINDI(peek, popped);
                  }
               } else if (peek.equals("HEAD")) {
                  if (localName.equals("_SCHEMA") || localName.equals("SCHEMA")) {
                     //System.out.println("End of schema");
                     // Do nothing
                  } else {
                     warnEnd(peek, popped);
                  }
               } else if (peek.equals("_SCHEMA") || peek.equals("SCHEMA")) {
                  if (isINDI(localName)) {
                     isInIndiSchema = false;
                  } else if (isFAM(localName)) {
                     isInFamSchema = false;
                  } else {
                     warnEnd(peek, popped);
                  }
               } else if (peek.equals(currSchemaKey)) {
                  if (localName.equals("LABL")) {
                     currSchemaValue = content;
                  } else {
                     warnEnd(currSchemaKey, popped);
                  }
               } else if (isCHIL(peek)) {
                  endSubCHIL(peek, popped);
               } else if (peek.equals("_FREL") || peek.equals("_MREL"))
               {
                  if(isSOUR(localName))
                  {
                     Tag tag = tagStack.elementAt(tagStack.size()-2);
                     if (isCHIL(tag.getName()))
                     {
                        endCurrCitation(currFamily, content, ignoredBucket);
                     } else
                     {
                        warnEnd(peek, popped);
                     }
                  } else
                  {
                     warnEnd(peek, popped);
                  }
               }
               else if (Event.isEvent(peek) || Event.isOtherEvent(peek)
                     || schema.contains(peek))
               {
                  endSubEvent(popped);
               } else if (peek.equals("NOTE") ||
                     peek.equals("TEXT")) {
                  endSubNoteText(popped);
               } else if (hasSubText(peek)) {
                  if (this.isSubNoteTitleText(localName)) {
                     tagStack.peek().append(content, " ");
                  } else {
                     warnEnd(peek, popped);
                  }
               } else if (isADDR(peek)) {
                  endSubADDR(peek, popped);
               } else if (isTITL(peek)) {
                  endSubTITL(peek, popped);
               } else if (peek.equals("NAME")) {
                  endSubNAME(currName, popped);
               } else if (peek.equals("ALIA")) {
                  endSubNAME(this.currAltName, popped);
               } else if (isSOUR(peek)) {
                  endSubSOUR(peek, popped);
               } else if (isREPO(peek)) {
                  endSubREPO(peek, popped);
               } else if (peek.equals("OBJE")) {
                  endSubOBJE(popped);
               } else if (peek.equals("DATA")) {
                  endSubDATA(popped);
               } else if (peek.equals("STAT")) {
                  if (localName.equals("DATE")) {
                     if (currEvent != null) {
                        currEvent.setAttribute("STATDATE", content);
                     } else {
                        warnEnd(peek, popped);
                     }
                  } else {
                     warnEnd(peek, popped);
                  }
               } else if (isFAMC(peek)) {
                  if (localName.equals("_PRIMARY")) {
                     if (content.trim().toLowerCase().equals("y")) {
                        // primary child of family will
                        // be listed first
                        this.primaryChildOf = true;
                     } /*else
                     {
                        this.primaryChildOf = false;
                     }   not needed
                     */
                  } else if (localName.equals("PEDI")) {
                     // Pedigree is typically something
                     // like "birth" or "apopted", meaning
                     // "birth" or "adopted" parents
                     this.pedi = content;
                  } else {
                     warnEnd(peek, popped);
                  }
               } else if (isCALN(peek)) {
                  if (isMEDI(localName)) {
                     // We should be in a top-level
                     // source, if we're not, there
                     // is a problem
                     if (currSource != null) {
                        currSource.appendType(content);
                     } else {
                        warnEnd("SOMETHING.CALN", popped);
                     }
                  } else {
                     warnEnd(peek, popped);
                  }
               } else if (isALIA(peek)
                     || peek.equals("NICK")) {
                  if (isSOUR(localName)) {
                     if (currCitation != null) {
                        endCurrCitation(currAltName, content, ignoredBucket);
                     } else {
                        warnEnd("NO currCitation in ALIA", popped);
                     }
                  }
                  else {
                     warnEnd(peek, popped);
                  }
               } else if (isHUSB(peek) ||
                     peek.equals("WIFE")) {
                  if (localName.equals("_PREF")) {
                     if (content.equals("Y")) {
                        isPreferredSubPerson = true;
                     }
                  }
               } else if (peek.equals("FILE"))
               {
                  if (isTITL(localName))
                  {
                     if (currImage != null)
                     {
                        currImage.appendCaption(content);
                     } else
                     {
                        warnEnd(peek, popped);
                     }
                  } else
                  {
                     warnEnd(peek, popped);
                  }
               } else if (peek.equals("_STAT"))
               {
                  if (localName.equals("DATE"))
                  {
                     tagStack.peek().append("Date: " + content);
                  } else
                  {
                     warnEnd(peek, popped);
                  }
               } else if (peek.equals("DATE"))
               {
                  if (localName.equals("SOUR"))
                  {
                     this.endCurrCitation(currEvent, content, ignoredBucket);
                  } else
                  {
                     warnEnd(peek, popped);
                  }
               }
               else {
                  warnEnd(peek, popped);
                  /*warn("Line " + lineNum +
                        ": Unrecognized parent tag \"" + peek +
                        "\" while ending element \"" + localName + '\"');*/
               }
            } catch (EmptyStackException e) {
               logger.error("There was an empty stack when trying to end element: " + localName);
               throw e;
            } finally {
               if (tagStack.size() > 0) {
                  tagStack.peek().addAllIgnoredBucket(ignoredBucket);
               }
            }
         }
      }
   }

   private void endSubNoteText(Tag pop) {
      if (this.isSubNoteTitleText(pop.getName())) {
         tagStack.peek().append(pop.getContent());
      } else if (pop.getName().equals("_TITLE")) {
         // We want to bold the title
         tagStack.peek().prepend("'''" + pop.getContent() + "'''", "\n\n");
      } else if (isSOUR(pop.getName())) {
         // We're going to go down the stack looking
         // for an event or a person tag
         // so that we can properly attach the
         // citation, since we cannot attach a
         // source directly to a NOTE or TEXT element
         boolean ateCitation = false;
         for (Tag tag : tagStack) {
            if (Event.isEvent(tag.getName()) ||
                  Event.isOtherEvent(tag.getName())) {
               endCurrCitation(currEvent, pop.getContent(), pop.getIgnoredBucket());
               ateCitation = true;
               break;
            } else if (isINDI(tag.getName())) {
               endCurrCitation(currPerson, pop.getContent(), pop.getIgnoredBucket());
               ateCitation = true;
               break;
            } else if (isFAM(tag.getName())) {
               endCurrCitation(currFamily, pop.getContent(), pop.getIgnoredBucket());
               ateCitation = true;
               break;
            }
         }

         if (!ateCitation) {
            // This is probably because we're a top level note,
            // so we need to add ourselves to the top level note
            // object.
            currCitation.appendText(pop.getContent());
            currCitation.eatIgnoredBucket(pop.getIgnoredBucket());
            if (currNote != null) {
               currNote.addCitation(currCitation);
            }
            else {
               infoLine("null currNote; throwing away citation: " + pop.getName() + " = " + pop.getContent());
            }
            currCitation = null;
            //warnEnd(lineNum, peek, localName);
         }
      } else if (pop.getName().equals("OBJE"))
      {
         Tag noteParentTag = tagStack.get(tagStack.size() -2);
         if (noteParentTag != null)
         {
            String parentTag = noteParentTag.getName();

            if (isINDI(parentTag))
            {
               currPerson.addImage(this, currImage, pop.getID());
            } else if (isFAM(parentTag))
            {
               currFamily.addImage(this, currImage, pop.getID());
            } else if (isSOUR(parentTag))
            {
               if (tagStack.size() > 2)
               {
                  if (PlaceUtils.isEmpty(pop.getID()))
                  {
                     currCitation.addImage(this, currImage, pop.getID());
                  } else
                  {
                     incrementWarnings();
                     logger.warn(logStr(lineStatement() + "Image citation inside of a souce citation, something we don't yet handle."));
                  }
               } else
               {
                  currSource.addImage(this, currImage, pop.getID());
               }
            } else if (Event.isEvent(parentTag))
            {
               if (PlaceUtils.isEmpty(pop.getID()))
               {
                  currEvent.addImage(this, currImage, pop.getID());
               } else
               {
                  incrementWarnings();
                  logger.warn(logStr(lineStatement() + "Image citation inside of an event, which is something we don't yet support."));
               }
            } else
            {
               infoLine("Image inside of a tag we don't support yet for images: " + parentTag);
            }
         }
      }
      else {
         warnEnd("NOTE or TEXT", pop);
      }
   }

   private void endSchemaEntry(String topTag) {
      schema.put(topTag, currSchemaKey, currSchemaValue);
      currSchemaKey = null;
      currSchemaValue = null;
   }

   private void endREPO(String id, List<String> ignoredBucket) throws RuntimeException {
      if (PlaceUtils.isEmpty(id)) {
         throw new RuntimeException("No ID number for REPO!");
      } else {
         currRepo.eatIgnoredBucket(ignoredBucket);
         repos.put(id, currRepo);
         currRepo = null;
      }
   }

   private void endNOTE(String id, String content, List<String> ignoredBucket) {
      if (PlaceUtils.isEmpty(id)) {
         warn("Must have ID for top level note");
      } else {
         currNote.setId(id);
         currNote.setNote(content);
         currNote.eatIgnoredBucket(ignoredBucket);
         notes.put(id, currNote);
         currNote = null;
      }
   }

   private void endSOUR(String id, List<String> ignoredBucket) throws RuntimeException {
      if (PlaceUtils.isEmpty(id)) {
         throw new RuntimeException("No ID number for SOUR!");
      } else {
         currSource.eatIgnoredBucket(ignoredBucket);
         // We check to see if there is another existing
         // source which already has the same contents as
         // the source we are about to add.
         // If there is such an existing source, then
         // we make the id we are about to add point to the
         // existing source.
         //
         // This has the consequence that when we iterate
         // through all of the sources in the source map
         // to print them out,
         // we need to make sure that the key (id number)
         // equals the value (Source)'s id number,
         // so that we only print out each unique source
         // once.
         if (source2id.containsKey(currSource))
         {
            sources.put(id, sources.get(source2id.get(currSource)));
         } else
         {
            currSource.setID(id);
            sources.put(id, currSource);
            source2id.put(currSource, id);
         }
         currSource = null;
      }
   }

   private void endFAM(String id, List<String> ignoredBucket, String textContent) throws RuntimeException {
      if (PlaceUtils.isEmpty(id)) {
         throw new RuntimeException("No ID number for Family!");
      } /*else if (PlaceUtils.isEmpty(currFamily.getUid()))
      {
         throw new UIDException("No UID", this, id);
      } */
      else {
         currFamily.setID(id);
         if(!PlaceUtils.isEmpty(textContent))
         {
            currFamily.addNote(textContent);
         }
         currFamily.eatIgnoredBucket(ignoredBucket);
         families.put(id, currFamily);
         currFamily = null;
      }
   }

   // Was used for enforcing that all uploaded GEDCOMs have
   // UIDs in all people and families. Deprecated
   public static class UIDException extends Gedcom.GedcomException {
      public UIDException(String m, Gedcom g, String id) {
         super(m, g, id);
      }
   }

   private Person primaryPerson = null;

   public Person getPrimaryPerson() {
      return primaryPerson;
   }

   private void setPrimaryPerson(Person primaryPerson) {
      this.primaryPerson = primaryPerson;
   }

   /**
    * This gets the primary person,
    * (which was the first person parsed from the GEDCOM),
    * and recursively propagates a special parameter which
    * specifies that living ancestors of the primary person
    * should always be printed.
    */
   public void propagatePrimaryPerson() // no longer used
   {
      if (getPrimaryPerson() != null)
      {
         getPrimaryPerson().setShouldAlwaysPrint(this, 4); // force root + 4 generations to print even if living
      }
   }

   private void endINDI(String id, List<String> ignoredBucket, String textContent) {
      if (PlaceUtils.isEmpty(id)) {
         warn("No ID number for individual!");
      } /*else if (PlaceUtils.isEmpty(currPerson.getUid()))
      {
         throw new UIDException("No UID", this, id);
      }*/
      else {
         currPerson.setID(id);
         if (!PlaceUtils.isEmpty(textContent))
         {
            currPerson.addNote(textContent);
         }
         currPerson.eatIgnoredBucket(ignoredBucket);
         currPerson.end(this);
         if (people.size() == 0)
         {
            // Then this is the
            // first person to be
            // added. We will mark them
            // as the primary person.
            currPerson.setPrimary();
            setPrimaryPerson(currPerson);
         }
         people.put(id, currPerson);
         currPerson = null;
      }
   }

   private void endSubDATA(Tag pop) {
      if (pop.getName().equals("DATE")) {
         currData.setDate(pop.getContent());
      } else if (pop.getName().equals("TEXT")) {
         currData.setText(pop.getContent());
      } else {
         warnEnd("DATA", pop);
      }
   }

   private void endSubOBJE(Tag pop) {
      if (pop.getName().equals("FILE") || pop.getName().equals("_FILE")) {
         currImage.setGedcom_file_name(pop.getContent());
      } else if (pop.getName().equals("NOTE") || pop.getName().equals("_NOTE")) {
         currImage.addNote(pop.getID(), pop.getContent());
      } else if (pop.getName().equals("_PRIM") || pop.getName().equals("_PRIMARY")) {
         if (pop.getContent().trim().equals("Y") || pop.getContent().trim().equals("YES")) {
            currImage.setPrimary(true);
         } else if (pop.getContent().trim().equals("N") || pop.getContent().trim().equals("NO")) {
            currImage.setPrimary(false);
         } else {
            warn("Line " + pop.getLineNum() +
                  ": Unrecognized value of _PRIM. Must be 'Y' or 'N': " + pop.getName());
         }
      } else if (pop.getName().equals("SOUR"))
      {
         this.endCurrCitation(currImage, pop.getContent(), pop.getIgnoredBucket());
      }
      else if (isTITL(pop.getName()))
      {
         currImage.setCaption(pop.getContent());
      } else if (pop.getName().equals("_DATE"))
      {
          currImage.appendCaption(pop.getContent());
      }
      else {
         warnEnd("OBJE", pop);
      }
   }

   private void endSubCHIL(String peekName, Tag pop) throws RuntimeException {
      if (isADOP(pop.getName())) {
         currChild.setAdopted(true);
      } else if (Event.isEvent(pop.getName())) {
         throw new RuntimeException("Event inside child which was not ignored: " + pop.getName());
         //currEvent = null;
      } else if (pop.getName().equals("_MREL") ||
            pop.getName().equals("_FREL")) {
         if (!pop.getContent().equals("Natural") &&
               !pop.getContent().equals("Unknown")) {
            logger.info(getUserName() + ':' + getFN() + ": " + "Line " + getLineNumber() +
                  ": Is not \"Natural\" or \"Unknown\": " + pop.getName() + ": CHIL");
         }
      } else if (pop.getName().equals("_PREF")) {
         if (pop.getContent().equals("Y")) {
            isPreferredSubPerson = true;
         }
      } else if (pop.getName().equals("_STAT")) {
         currChild.setStatus(pop.getContent());
      } else {
         warnEnd(peekName, pop);
      }
   }

   private void endSubREPO(String peekName, Tag pop) throws RuntimeException {
      if (pop.getName().equals("NAME")) {
         currRepo.setName(pop.getContent());
      } else if (isADDR(pop.getName()) ||
            isPHON(pop.getName())) {
         currRepo.appendToAddress(pop.getContent());
      } else if (pop.getName().equals("NOTE")) {
         if (!PlaceUtils.isEmpty(pop.getID())) {
            currRepo.addNoteCitation(pop.getID());
         } else if (isID(pop.getContent())) {
            currRepo.addNoteCitation(pop.getContent());
         } else {
            currRepo.addNote(pop.getContent());
         }
      } else if (isCALN(pop.getName())) {
         if (currSource != null) {
            currSource.setCallNum(pop.getContent());
         } else if (currRepo != null) {
            currRepo.setCallNum(pop.getContent());
         } else {
            warnEnd("REPO but with no currRepo created", pop);
         }
      } else {
         warnEnd(peekName, pop);
      }
   }

   private void endSubSOUR(String peekName, Tag pop)
         throws RuntimeException {
      if (tagStack.size() > 1) {
         // Then we're in a citation SOUR instead of
         // a top-level SOUR
			if (currCitation != null) {
				if (pop.getName().equals("PAGE")) {
					currCitation.setPage(pop.getContent());
				} else if (pop.getName().equals("NOTE")) {
					currCitation.addNote(pop.getID(), pop.getContent());
				} else if (pop.getName().equals("DATA")) {
					if (!PlaceUtils.isEmpty(currData.getDate())) {
						currCitation.setDate(currData.getDate());
					}
					if (!PlaceUtils.isEmpty(currData.getText())) {
						currCitation.appendText(currData.getText());
					}
					currData = null;
				} else if (pop.getName().equals("TEXT"))
				{
					currCitation.appendText(pop.getContent());
				} else if (pop.getName().equals("DATE")) {
					currCitation.setDate(pop.getContent());
				} else if (pop.getName().equals("OBJE")) {
					currCitation.addImage(this, currImage, pop.getID());
					currImage = null;
				} else if (this.isSubNoteTitleText(pop.getName())) {
					tagStack.peek().append(pop.getContent());
				} else if (pop.getName().equals("QUAY")) {
					currCitation.setQuality(this, pop.getContent());
				} else {
					warnEnd("citation SOUR", pop);
				}
			}
      } else {
         if (pop.getName().equals("ABBR")) {
            currSource.setAbbreviation(pop.getContent());
         } else if (isTITL(pop.getName())) {
            currSource.setTitle(pop.getContent());
         } else if (isAUTH(pop.getName())) {
            currSource.setAuthor(pop.getContent());
         } else if (pop.getName().equals("PERI")) {
            currSource.setPeri(pop.getContent());
         } else if (isPUBL(pop.getName())) {
            currSource.setPubInfo(pop.getContent());
         } else if (pop.getName().equals("TEXT")) {
            currSource.setText(pop.getContent());
         } /*else if (pop.getName().equals("DATE"))
         {
            currSource.setDate(pop.getContent());
         } */
         else if (pop.getName().equals("TYPE")
               || pop.getName().equals("_TYPE")
               || isMEDI(pop.getName())
               || pop.getName().equals("_MEDI")) {
            currSource.appendType(pop.getContent());
         } else if (pop.getName().equals("LOCA")
               || pop.getName().equals("PLAC")) {
            currSource.appendPlace(pop.getContent());
         } else if (isREPO(pop.getName())) {
            if (!PlaceUtils.isEmpty(pop.getID())) {
               currSource.setRepositoryID(pop.getID());
            }
            if (!PlaceUtils.isEmpty(currRepo.getAddress()))
            {
               currSource.appendAddr(currRepo.getAddress());
            }
            if (!PlaceUtils.isEmpty(currRepo.getName()))
            {
               currSource.setRepositoryName(currRepo.getName());
            }
            currSource.addNoteCitations(currRepo.getNoteCitations());
            currSource.addNotes(currRepo.getNotes());
            if (!PlaceUtils.isEmpty(currRepo.getCallNum())) {
               currSource.setCallNum(currRepo.getCallNum());
            }
            currRepo = null;
         } else if (isCALN(pop.getName())) {
            currSource.setCallNum(pop.getContent());
         } else if (pop.getName().equals("NOTE")) {
            if (!PlaceUtils.isEmpty(pop.getID())) {
               currSource.addNoteCitation(pop.getID());
            } else if (!PlaceUtils.isEmpty(pop.getContent())) {
               currSource.addNote(pop.getContent());
            } else {
               logger.info(logStr(pop.getLineNum() + ": Trying to add an empty note to source"));
            }
         } else if (pop.getName().equals("DATE")) {
            currSource.addNote("Date: " + pop.getContent());
         } else if (pop.getName().equals("URL")) {
            currSource.setUrl(pop.getContent());
         } else if (pop.getName().equals("OBJE")) {

            currSource.addImage(this, currImage, pop.getID());
            currImage = null;
         } else {
            warnEnd(peekName, pop);
         }
      }
   }

   private void endSubNAME(Name currName, Tag pop) throws RuntimeException {
      String localName = pop.getName();
      String content = pop.getContent();
      int lineNum = pop.getLineNum();
      List <String> ignoredBucket = pop.getIgnoredBucket();
      String id = pop.getID();
      if (currRepo != null) {
         // This is a repository name, not
         // a normal name.
         warnEnd("NAME", pop);
      } else {
         if (localName.equals("GIVN")) {
            currName.setGiven(content);
         } else if (localName.equals("SURN")) {
            currName.setSurname(content);
         } else if (localName.equals("NSFX")) {
            currName.setSuffix(content);
         } else if (localName.equals("NPFX")) {
            currName.setPrefix(content);
         } else if (localName.equals("NICK")
               ||   localName.equals("_AKA")
               ||   localName.equals("_AKAN")) {
            currAltName.setName(content, this);
            currPerson.addAltName(currAltName);
            currAltName = null;
         } else if (localName.equals("_MARNM")
               ||   localName.equals("_MAR")) {
            currPerson.addMarriedName(content, this);
         } else if (isSOUR(localName)) {
            endCurrCitation(currName, content, ignoredBucket);
         }  else if (localName.equals("NOTE") ||
            localName.equals("INFO"))
         {
            if (!PlaceUtils.isEmpty(id))
            {
               currPerson.addNoteCitation(id);
            }
            else if (!PlaceUtils.isEmpty(content)) {
               currPerson.addNote(content);
            } else {
               // do nothing if the content is empty
            }
         } else if (localName.equals("DATE"))
         {
            currPerson.addNote("Name date: " + content);
         }
         else {
            warnEnd("NAME", pop);
         }
      }
   }

   private void endSubTITL(String peekName, Tag pop)
         throws RuntimeException {
      if (isSOUR(pop.getName())) {
         endCurrCitation(currTitle, pop.getContent(), pop.getIgnoredBucket());
      } else if (this.isSubNoteTitleText(pop.getName()))
      {
         if (currTitle != null)
         {
            currTitle.addNote(pop.getID(), pop.getContent());
         } else if (currSource != null)
         {
            currSource.addNote(pop.getID(), pop.getContent());
         } else
         {
            throw new RuntimeException("The title should not be null here on line: "
                  + getLineNumber());
         }
      }
      else {
         warnEnd(peekName, pop);
      }
   }

   private void endSubADDR(String peekName, Tag pop) throws RuntimeException
   {
      String localName = pop.getName();
      String content = pop.getContent();
      String id = pop.getID();
      // We only append if the address content does
      // not already contain the new line
      if (addressLine(localName)) {

         if (!tagStack.peek().getContent().contains(content)) {
            tagStack.peek().append(content, "\n");
         }
      } else if (addressPart(localName)) {
         // don't attend addressPart because we end up repeating place
//         if (!tagStack.peek().getContent().contains(content)) {
//            tagStack.peek().append(content, " ");
//         }
      } else if (localName.equals("OBJE"))
      {
         String topName = tagStack.get(tagStack.size() -2).getName();
         if (isINDI(topName))
         {
            currPerson.addImage(this, currImage, id);
         } else if (isFAM(topName))
         {
            currFamily.addImage(this, currImage, id);
         } else if (isREPO(topName))
         {
            currRepo.addImage(currImage);
         }
         else
         {
            // Try the root of the stack.
            topName = tagStack.get(0).getName();
            if (isINDI(topName))
            {
               currPerson.addImage(this, currImage, id);
            } else if (isFAM(topName))
            {
               currFamily.addImage(this, currImage, id);
            } else if (isREPO(topName))
            {
               currRepo.addImage(currImage);
            } else
            {
               this.warn(this.lineStatement() + " OBJE inside of ADDR, but not inside INDI or FAM or REPO");
            }
         }
         currImage = null;
      }
      else {
         warnEnd(peekName, pop);
      }
   }

   private boolean addressLine(String localName) {
      return localName.equals("ADR1")
            || localName.equals("ADR2")
            || localName.equals("_NAME")
            || isSubNoteTitleText(localName);
   }

   private boolean addressPart(String localName) {
      return localName.equals("STAE")
            || localName.equals("CITY")
            || localName.equals("POST")
            || localName.equals("CTRY");
   }

   private boolean isSubAddress(String localName) {
      return addressLine(localName) || addressPart(localName);
   }

   private static final Pattern pGedcomID = Pattern.compile(
         "^@[A-Z]\\d+@$"
   );
   public static boolean isID(String candidateID) {
      Matcher m = pGedcomID.matcher(candidateID);
      return m.matches();
   }

   private void endSubEvent(Tag pop) throws RuntimeException {
      String localName = pop.getName();
      String content = pop.getContent();
      String id = pop.getID();
      if (isSOUR(localName)) {
         endCurrCitation(currEvent, content, pop.getIgnoredBucket());
      } else if (localName.equals("NOTE")) {
         currEvent.addNote(id, content);
      } else if (localName.equals("OBJE")) {
         currEvent.addImage(this, currImage, id);
         currImage = null;
      } else if (isFAMC(localName)) {
         currEvent.setAttribute("FAMC", id);
      } else if (Event.isAttribute(localName)) {
         currEvent.setAttribute(localName, content);
      } else {
         warnEnd("Event", pop);
      }
   }

   private void endSubFAM(String peekName, Tag pop) throws RuntimeException {
      String localName = pop.getName();
      String content = pop.getContent();
      String id = pop.getID();
      if (endSubEventContainer(currFamily, localName, content, id, pop.getLineNum(), pop.getIgnoredBucket())) {
         return;
      } else if (isHUSB(localName)) {
         currFamily.addHusband(this, id, isPreferredSubPerson);
      } else if (localName.equals("WIFE")) {
         currFamily.addWife(this, id, isPreferredSubPerson);
      } /*else if (localName.equals("STAT")) {
         currFamily.setStatus(content);
      } */
      else if (isCHIL(localName)) {
         currChild.setId(id);
         currFamily.addChild(this, currChild, isPreferredSubPerson);
         currChild = null;
      } else if (schema.contains(peekName, localName)) {
         currEvent.setContent(content, id);
         currEvent.eatIgnoredBucket(pop.getIgnoredBucket());
         currFamily.addEvent(currEvent);
         currEvent = null;
      } else if (localName.equals("STAT") || localName.equals("_STAT")) {
         if (!content.equals("MARRIED")) {
            currFamily.addNote("Status: " + content);
            logger.info(logStr("In FAM " + localName + " not \"MARRIED\""));
         }
      } else {
         warnEnd(peekName, pop);
      }
   }

   private boolean endSubEventContainer(EventContainer ec, String localName, String content, String id,
                                        int lineNum, List<String> ignoredBucket) {
      if (endSubTopObject(ec, localName, content, id, lineNum)) {
         return true;
      } else if ((Event.isEvent(localName) ||
            Event.isOtherEvent(localName))) {
         currEvent.setContent(content, id);
         currEvent.eatIgnoredBucket(ignoredBucket);         
         ec.addEvent(currEvent);
         currEvent = null;
      } else if (isSOUR(localName)) {
         endCurrCitation(ec, content, ignoredBucket);
      }
      // If the note object does not contain
      // an ID, then we just copy the text in
      else if ((localName.equals("NOTE") ||
            localName.equals("INFO") ||
            localName.equals("HIST") ||
            localName.equals("MIL")) &&
            PlaceUtils.isEmpty(id)) {
         if (!PlaceUtils.isEmpty(content)) {
            ec.addNote(content);
         } else {
            // do nothing if the content is empty
         }
      } else if (localName.equals("OBJE")) {
         ec.addImage(this, currImage, id);
         currImage = null;
      } else {
         return false;
      }
      return true;
   }

   private boolean endSubTopObject(TopObject top, String localName, String content, String id, int lineNum) {
      if ((localName.equals("NOTE")
            || localName.equals("_CEN") ||
            localName.equals("HIST") ||
            localName.equals("MIL"))
            && !PlaceUtils.isEmpty(id))
      {
         // We'll have to get the actual note
         // later by using the id
         top.addNoteCitation(id);
      } else if (localName.equals("_UID") || localName.equals("UID")) {
         top.setUid(content);
      } else {
         return false;
      }
      return true;
   }


   private void endSubINDI(String peekName, Tag pop) {
      String content = pop.getContent();
      String localName = pop.getName();
      String id = pop.getID();
      List<String> ignoredBucket = pop.getIgnoredBucket();
      if (endSubEventContainer(currPerson, localName, content, id, pop.getLineNum(), ignoredBucket)) {
         // Do nothing
      } else if (schema.contains(peekName, localName))
      {
         currEvent.setContent(content, id);
         currEvent.eatIgnoredBucket(ignoredBucket);
         currPerson.addEvent(currEvent);
         currEvent = null;
      } else if (isTITL(localName)) {
         currTitle.setTitle(content);
         currPerson.addTitle(currTitle);
         currTitle = null;
      } else if (localName.equals("NAME")) {
         AlternateName rval = currName.setName(content, this);
         currPerson.eatIgnoredBucket(ignoredBucket);
         currPerson.addName(currName);
         if (rval != null) {
            currPerson.addAltName(rval);
         }
         currName = null;
      } else if (localName.equals("NAMR")) {
         currPerson.addReligiousName(content, this);
      } else if (localName.equals("SEX")) {
         currPerson.setGender(content);
      } else if (localName.equals("AFN")) {
         currPerson.setAfn(content);
      } else if (isFAMC(localName)) {
         currPerson.addChildOfFamily(id, primaryChildOf, pedi);
         primaryChildOf = false;
         pedi = null;
      } else if (isFAMS(localName)) {
         currPerson.addSpouseOfFamily(id);
      } else if (isALIA(localName)
            || localName.equals("NICK")) {
         currAltName.setName(content, this);
         //currAltName.eatIgnoredBucket(ignoredBucket);
         currPerson.addNoteNotes(currAltName.getNotes());
         currAltName.clearNotes();
         currPerson.addAltName(currAltName);
         currAltName = null;
      } else if (localName.equals("_TODO")) {
         if (!PlaceUtils.isEmpty(id)) {
            currPerson.addTodo(id);
         }
         else {
            content += "TODO:\n";
            for (String s : ignoredBucket)
            {
               content += s += '\n';
            }
            ignoredBucket.clear();
            currPerson.addNote(content);
         }
      } else {
         warnEnd(peekName, pop);
      }
   }

   public void warnEnd(String parentTag, Tag popped) {
      if (isSOUR(popped.getName()))
      {
         String cont = popped.getContent();
         List <String> ib = popped.getIgnoredBucket();
         for (int currTagPosition = tagStack.size() -1; currTagPosition >= 0; currTagPosition--)
         {
            String name = tagStack.get(currTagPosition).getName();
            if (Event.isEvent(name))
            {
               this.endCurrCitation(currEvent, cont, ib);
               break;
            } else if (currName != null)
            {
               this.endCurrCitation(currName, cont, ib);
               break;
            } else if (currAltName != null)
            {
               this.endCurrCitation(currAltName, cont, ib);
               break;
            }
            else if (isINDI(name))
            {
               this.endCurrCitation(currPerson, cont, ib);
               break;
            } else if (isFAM(name))
            {
               this.endCurrCitation(currFamily, cont, ib);
               break;
            }
         }
         currCitation = null;
      } else if(popped.getName().equals("NOTE"))
      {
         String cont = popped.getContent();
         List <String> ib = popped.getIgnoredBucket();
         for (int currTagPosition = tagStack.size() -1; currTagPosition >= 0; currTagPosition--)
         {
            String name = tagStack.get(currTagPosition).getName();
            if (Event.isEvent(name))
            {
               currEvent.addNote(popped.getID(), popped.getContent());
               break;
            } else if (Gedcom.isSOUR(name))
            {
               currSource.addNote(popped.getID(), popped.getContent());
            }
            /*else if (currName != null)
            {
               currName.addNote(popped.getID(), popped.getContent());
               break;
            } else if (currAltName != null)
            {
               currAltName.addNote(popped.getID(), popped.getContent());
               break;
            }*/ else if (isINDI(name))
            {
               currPerson.addNote(popped.getID(), popped.getContent());
               break;
            } else if (isFAM(name))
            {
               currFamily.addNote(popped.getID(), popped.getContent());
               break;
            }
         }
      }
      else
      {
         warn("Line " + popped.getLineNum() +
               ": Unrecognized tag when ending element inside of "
               + parentTag + ": " + popped.getName());
      }
   }

   public void characters(char ch[], int start, int length) throws SAXException {
      String s = new String(ch);
      if (isIgnoring()) {
         tagStack.get(ignoreLevel).append(s);
      } else if (tagStack.size() > 0) {
         String tagName = tagStack.peek().getName();
         if (Person.isAttribute(tagName)
               || Event.isAttribute(tagName)
               || isSubName(tagName)
               || Family.isAttribute(tagName)
               || Image.isAttribute(tagName)
               || isSubCitation(tagName)
               || Source.isSubSource(tagName)
               || isSubRepo(tagName)
               || isSubAddress(tagName)
               || tagName.equals("LABL")
               || tagName.equals("PEDI")
               || tagName.equals("_PRIMARY")
               || isMEDI(tagName)
               || tagName.equals("_MEDI")
               || tagName.equals("_TITLE")
               || schema.contains(tagName)
               || isCONTCONC(tagName)
               || tagName.equals("_PREF")
               || (tagStack.size() == 1
               && (tagName.equals("INDI")
               || tagName.equals("FAM")))) {
            tagStack.peek().append(s);
         } else {
            warn("Characters for unrecognized tag: " + tagName);
         }
      }
   }

   public void ignorableWhitespace(char ch[], int start, int length) throws SAXException {
      //To change body of implemented methods use File | Settings | File Templates.
   }

   public void processingInstruction(String target, String data) throws SAXException {
      //To change body of implemented methods use File | Settings | File Templates.
   }

   public void skippedEntity(String name) throws SAXException {
      //To change body of implemented methods use File | Settings | File Templates.
   }

   public static class GedcomException extends Exception {
      private Gedcom gedcom;
      private String id;

      public void setId(String id) {
         this.id = id;
      }

      public void setGedcom(Gedcom gedcom) {
         this.gedcom = gedcom;
      }

      public Gedcom getGedcom() {
         return gedcom;
      }

      public GedcomException(String m, Gedcom g, String id) {
         super("ID: " + id + ": " + m);
         gedcom = g;
         if (gedcom != null) {
            gedcom.incrementWarnings();
         }
         this.id = id;
      }

      public String toString() {
         if (gedcom != null && id != null) {
            return gedcom.logStr(this.getMessage());
         } else {
            return this.getMessage();
         }
      }
   }

   public static class PostProcessException extends GedcomException {
      public PostProcessException(String s, Gedcom g, String id) {
         super(s, g, id);
      }
   }

   // Necessary for when we print out the GEDCOM
   // to WeRelate person and family page XML
   private Map<String, String> place2standard = null;

   public Map<String, String> getPlace2standard() {
      return place2standard;
   }

   // Patterns applied to place names to clean them up.
   private static final String [] CLEAN_PLACE_REGEXES = {
         "^\\s*,\\s*",
         "\\s*,\\s*$",
         "^\\s*<\\s*",
         "\\s*>\\s*$",
         "^\\s*\\bof\\b\\s*",
         "\\s*\\bof\\b\\s*$",
         "^\\s*\\.\\s*",
         "\\s*\\.\\s*$",
         "^\\s*\\band\\b\\s*",
         "\\s*\\band\\b\\s*$",
   };
   private static final Pattern [] CLEAN_PLACE_PATTERNS =
         new Pattern [CLEAN_PLACE_REGEXES.length];
   static {
      for (int i = 0; i < CLEAN_PLACE_REGEXES.length; i++) {
         CLEAN_PLACE_PATTERNS[i] = Pattern.compile(CLEAN_PLACE_REGEXES[i], Pattern.CASE_INSENSITIVE);
      }
   }

   // Extracts a map
   // place name -> cleaned place name
   // from the set of events passed in
   private static Set<String> getPlaces(Set<Event> events) {
      //Map<String, String> place2clean = new HashMap<String, String>();
      Set<String> returnValue = new HashSet<String>();

      String place;

      for (Event event : events) {
         place = event.getAttribute("PLAC");
         if (!PlaceUtils.isEmpty(place)) {
            //place2clean.put(place, cleanPlaceName(place));
            returnValue.add(place);
         }
      }

      return returnValue;
   }
   // Cleans the place name by applying a set of
   // regexes over and over again until there
   // are not matches with any of the regexes in the
   // set.
   private static String cleanPlaceName(String name) {
      boolean shouldCont = true;
      while (shouldCont) {
         shouldCont = false;
         for (Pattern p : CLEAN_PLACE_PATTERNS) {
            Matcher m = p.matcher(name);
            if (m.find()) {
               name = m.replaceAll("");
               shouldCont = true;
            }
         }
      }
      return name;
   }

   // Useful just to get an idea
   // of how many places total were
   // queried versus how many we were
   // not able to standardize.
   private int numPlacesQueried = 0;
   public int getNumPlacesQueried() {
      return numPlacesQueried;
   }

   private Uploader uploader = null;

   /**
    * This creates a standardized place map
    * Place Name -> Standardized Name for
    * all places which are cited by the people
    * and family objects in this GEDCOM.
    * @param placeServer
    * @throws IOException
    */
   public void createStandardizedPlaceMap(String placeServer, String defaultCountry, StringBuffer placeXMLBuffer)
         throws IOException {
      /*logger.info("Creating place2cleaned map");
      // We must first create a map
      // place name -> cleaned name
      Map<String, String> place2cleaned = new HashMap<String, String>();
      for (Family fam : getFamilies().values()) {
         place2cleaned.putAll(getPlaces(fam.getEvents()));
      }
      for (Person person : getPeople().values()) {
         place2cleaned.putAll(getPlaces(person.getEvents()));
      }
      logger.info("Done creating place2cleaned map"); */

      Set <String> placeNames = new HashSet <String>();
      for (Family fam : getFamilies().values()) {
         placeNames.addAll(getPlaces(fam.getEvents()));
      }
      for (Person person : getPeople().values()) {
         placeNames.addAll(getPlaces(person.getEvents()));
      }

      // Now we need to create another map
      // place name -> standardized name
      // based on what we get back from the server
      standardizedPlaceSet = new HashSet<String>();
      //if (place2cleaned.size() > 0) {
      if (placeNames.size() > 0) {
         logger.info("Getting standardized names from server");
         //Map<String, String> cleaned2standard =
         //      uploader.getStandardizedPlaceNames(placeServer, new HashSet<String>(place2cleaned.values()));
         uploader.getStandardizedPlaceNames(placeServer, placeNames, defaultCountry, placeXMLBuffer);
         logger.info("Server returned with standardized names");
      }
   }

   // In a Gedcom, individuals point to families, and those families
   // should point back to the individuals, and vice-versa. Well, in
   // some Gedcoms this is not guaranteed to be the case, so this
   // next section of code will go through all of the INDI and FAM
   // references and make sure that each such reference points both ways
   private void postProcess(String placeServer, String defaultCountry, StringBuffer placeXMLBuffer) throws PostProcessException, IOException {
      logger.info("Checking to make sure that person, family references point in both directions");
      for (Person person : getPeople().values()) {
         for (String famID : person.getSpouseOfFamilies()) {
            Family fam = getFamilies().get(famID);
            if (fam != null) {
               if (!fam.hasSpouse(person.getID())) {
                  if (person.getGender() == Person.Gender.male) {
                     fam.addHusband(this, person.getID(), false);
                  } else if (person.getGender() == Person.Gender.female) {
                     fam.addWife(this, person.getID(), false);
                  } else {
                     // Let's see if the family already has a husband or wife.
                     if(!Utils.isEmpty(fam.getFirstHusband()))
                     {
                        // Assume this is a wife.
                        fam.addWife(this, person.getID(), false);
                     } else if (!Utils.isEmpty(fam.getFirstWife()))
                     {
                        fam.addHusband(this, person.getID(), false);
                     } else
                     {
                        throw new PostProcessException("Person refers to a family as a spouse, and the family doesn't refer to person as a spouse." +
                           " The problem is that the person cannot be added to the family because the gender of the person is unknown.", this, fam.getID());
                     }
                  }
               }
            } else {
               logger.info(logStr("Invalid reference to spouse of family: " + famID));
            }
         }

         for (String famID : person.getChildOfFamilies()) {
            Family fam = getFamilies().get(famID);
            if (fam != null) {
               if (!fam.hasChild(person.getID())) {
                  Family.Child newChild = new Family.Child();
                  newChild.setId(person.getID());
                  fam.addChild(this, newChild, false);
               }
            } else {
               logger.info(logStr("Invalid reference to child of family: " + famID));
            }
         }
      }

      // Now we need to do the same for all of the families:

      for (Family fam : getFamilies().values()) {
         List <String> toRemove = new ArrayList <String>();
         for (String personID : fam.getHusbands()) {
            if(!checkSpouseOfFamilies(personID, fam))
            {
               toRemove.add(personID);
            }
         }
         for (String personID : fam.getWives()) {
            if(!checkSpouseOfFamilies(personID, fam))
            {
               toRemove.add(personID);
            }
         }
         for (String personID : toRemove)
         {
            fam.removeSpouse(personID);
         }
         for (Family.Child child : fam.getChildren()) {
            if (child.getId() != null)
            {
               Person person = getPeople().get(child.getId());
               if (person != null) {
                  if (!person.getChildOfFamilies().contains(fam.getID())) {
                     person.addChildOfFamily(fam.getID(), false, null);
                  }
               } else {
                  logger.info(logStr("Person referred to in family does not exist: " + child.getId()));
               }
            }
         }
      }

      logger.info("Done checking double references");
      logger.info("Copying repository information to sources");
      Source source;
      //ArrayList<String> keysToRemove = new ArrayList <String>();
      for (Map.Entry e : getSources().entrySet()) {
         // When adding new sources to the source map
         // (when parsing the GEDCOM),
         // we check to see if there is another existing
         // source which already has the same contents as
         // the source we are about to add.
         // If there is such an existing source, then
         // we make the id we are about to add point to the
         // existing source.
         //
         // This has the consequence that when we iterate
         // through all of the sources in the source map
         // to print them out,
         // we need to make sure that the key (id number)
         // equals the value (Source)'s id number,
         // so that we only print out each unique source
         // once.
         source = (Source) e.getValue();
         if (e.getKey().equals(source.getID()))
         {
            // We should copy over the repository information
            String repoID = source.getRepositoryID();
            if (!PlaceUtils.isEmpty(repoID)) {
               // Get the repository
               Repository repo = getRepositories().get(repoID);
               if (repo != null) {
                  source.appendAddr(repo.getAddress());
                  source.setRepositoryName(repo.getName());
                  if (!PlaceUtils.isEmpty(repo.getCallNum())) {
                     source.setCallNum(repo.getCallNum());
                  }
                  source.addNotes(repo.getNotes());
                  for (Image image : repo.images)
                  {
                     source.addImage(this, image, null);
                  }

               } else {
                  infoLine("Invalid repository id, \"" +
                        repoID + "\" in source: \"" + e.getKey() + "\" " + source.getID());
               }
            }
         }
      }
      // Now that we're done copying over the information from
      // the repositories, we can delete them
      setRepos(null);
      logger.info("Done copying repository information");
      logger.info("Creating standardized place map");
      createStandardizedPlaceMap(placeServer, defaultCountry, placeXMLBuffer);
      logger.info("Done creating standardized place map");
   }

   // Make sure that the person with the specified personID contains
   // the "fam" as a spouse_of_family
   private boolean checkSpouseOfFamilies(String personID, Family fam) throws PostProcessException {
      Person person = getPeople().get(personID);
      if (person != null) {
         if (!person.getSpouseOfFamilies().contains(fam.getID())) {
            person.addSpouseOfFamily(fam.getID());
         }
         return true;
      } else {
         logger.info(logStr("Person referred to in family does not exist: " + personID));
         // We need to remove spouse:
         return false;                       
      }
   }

   /**
    * Used to test the GEDCOM parsing ability, without
    * worrying about printing the XML or generating wiki pages
    */
   /*public static void main(String [] args)
         throws SAXException, IOException, PostProcessException {
      Gedcom gedcom = new Gedcom(args[0], args[1], "ezekiel:8080", Integer.parseInt(args[2]));
      logger.info("Number of people: " + gedcom.getPeople().size());
      logger.info("Number of families: " + gedcom.getFamilies().size());
   }*/

   private GedcomParser gp = null;
   private int numWarnings = 0;

   public int getNumWarnings() {
      return numWarnings;
   }

   public void incrementWarnings() {
      numWarnings++;
   }

   private int getLineNumber() {
      return gp.getLineNumber();
   }

   private String fn = null;

   public String getFN() {
      return fn;
   }

   public void setFN(String filename) {
      fn = filename;
   }

   private boolean ignoreUnexpectedTags = false;

   /**
    * Constructor
    * @param uploader uploader object which is controlling this GEDCOM
    * @param fn file name of GEDCOM
    * @param userName username of person to whom GEDCOM belongs
    * @param placeServer location of the place search server
    * @param treeId tree id of the GEDCOM
    * @throws IOException
    * @throws SAXException
    * @throws PostProcessException
    */
   public Gedcom(Uploader uploader, String fn, String userName, String placeServer, String defaultCountry,
                 int treeId, boolean isTrustedUploader,
                 boolean ignoreUnexpectedTags, StringBuffer placeXMLBuffer)
         throws IOException, SAXException, PostProcessException
   {
      this.uploader = uploader;
      this.ignoreUnexpectedTags = ignoreUnexpectedTags;
      setTreeID(treeId);
      setUserName(userName);
      setIsTrustedUploader(isTrustedUploader);
      gp = new GedcomParser();
      gp.setContentHandler(this);
      gp.setErrorHandler(new DefaultHandler());
      setFN(fn);
      gp.parse("file://" + (new File(fn)).getAbsolutePath());
      if (!isInvalid)
      {
         postProcess(placeServer, defaultCountry, placeXMLBuffer);
      }
   }

   private Set <String> standardizedPlaceSet  = null;

   public Set<String> getStandardizedPlaceSet() {
      return standardizedPlaceSet;
   }
}
