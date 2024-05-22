package org.werelate.gedcom;

import org.werelate.util.PlaceUtils;
import org.werelate.util.SharedUtils;
import org.werelate.util.Utils;
import org.werelate.util.MultiMap;
import org.werelate.util.EventDate;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.werelate.gedcom.Person;

import java.awt.event.WindowFocusListener;
import java.util.*;
import java.io.PrintWriter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by IntelliJ IDEA.
 * User: npowell
 * Date: Nov 9, 2006
 * Time: 1:47:23 PM
 * Represents a family and the ability to print itself out
 * to the XML file
 */
public class Family extends EventContainer{
   private static final Logger logger = LogManager.getLogger("org.werelate.gedcom.Family");

   private static class MinMaxDate {
      int minBirthDay;
      int maxBirthDay;

      MinMaxDate(int minBirthDay, int maxBirthDay) {
         this.minBirthDay = minBirthDay;
         this.maxBirthDay = maxBirthDay;
      }
   }

   private static class DateStd {
      String date;
      Integer stdDate;                          // changed type from String Oct 2021 Janet Bjorndahl
      DateStd(String date, Integer stdDate) {
         this.date = date;
         this.stdDate = stdDate;
      }
   }

   // list of all wives attached to this family object.
   private List <String> wives = new ArrayList <String>();
   // List of all husbands attached to this family object
   private List <String> husbands = new ArrayList <String>();
   // Specifies the GEDCOM ID number of the first husband to be listed.
   private String preferredHusband = null;
   // Specifies the GEDCOM ID number of the first wife to be listed.
   private String preferredWife = null;

   public void removeSpouse(String personID)
   {
      for (int i=0; i < wives.size(); i++)
      {
         if (wives.get(i).equals(personID))
         {
            wives.remove(i);
            return;
         }
      }
      for (int i=0; i < husbands.size(); i++)
      {
         if (husbands.get(i).equals(personID))
         {
            husbands.remove(i);
            return;
         }
      }
   }

   // Specifies the namespace to be printed out to the page xml attribute
   protected int getNamespace() {
      return Utils.FAMILY_NAMESPACE;
   }

   /**
    * Set's the LivingStatus of all parents in this family to being
    * DEAD -- this is sometimes needed to avoid infinite loops when
    * setting the LivingStatus of an entire family, such as in the case
    * where all spouses are status unknown after the first pass.
    * @param gedcom
    * @throws Gedcom.PostProcessException
    */
   public void setAllSpousesDead(Gedcom gedcom) throws Gedcom.PostProcessException {
      for (String personID : getHusbands())
      {
         setPersonToDead(gedcom, personID);
      }
      for (String personID : getWives())
      {
         setPersonToDead(gedcom, personID);
      }
   }

   /**
    *
    * @param gedcom
    * @param myPersonID
    * @return true if there is a spouse of the person specified
    * that is living, false otherwise
    * @throws Gedcom.PostProcessException
    */
   public boolean isSpouseLiving (Gedcom gedcom, String myPersonID) throws Gedcom.PostProcessException {
      for (String personID : getHusbands())
      {
         if(notEqualAndLiving(gedcom, myPersonID, personID))
         {
            return true;
         }
      }
      for (String personID : getWives())
      {
         if (notEqualAndLiving(gedcom, myPersonID, personID))
         {
            return true;
         }
      }
      return false;
   }

   // Utility method for the above isSpouseLiving.
   // Returns true if the personID exists and is not equals to myPersonID
   // and is determined to be living, false otherwise.
   private static boolean notEqualAndLiving(Gedcom gedcom, String myPersonID, String personID) throws Gedcom.PostProcessException
   {
      if (!personID.equals(myPersonID))
      {
         Person person = gedcom.getPeople().get(personID);
         if (person!=null)
         {
            if (person.isLiving())
            {
               return true;
            }
         } else
         {
            throw new Gedcom.PostProcessException("Invalid spouse id when testing if spouse is living",
                  gedcom, personID);
         }
      }
      return false;
   }

   // Sets the person indicated by the personID as dead
   private static void setPersonToDead(Gedcom gedcom, String personID) throws Gedcom.PostProcessException {
      Person person = gedcom.getPeople().get(personID);
      if (person != null)
      {
         person.setLiving(Person.LivingStatus.DEAD);
      } else
      {
         throw new Gedcom.PostProcessException("Invalid person id when setting all spouses to DEAD", gedcom, personID);
      }
   }

   /**
    *
    * @param gedcom
    * @return true if all of the parents in this GEDCOM have LivingStatus.UNKNOWN
    * @throws Uploader.PrintException
    * @throws Gedcom.PostProcessException
    */
   public boolean allSpousesStatusUnknown(Gedcom gedcom) throws Uploader.PrintException, Gedcom.PostProcessException {
      if (!allStatus(gedcom, getHusbands(), Person.LivingStatus.UNKNOWN))
      {
         return false;
      } else
      {
         return allStatus(gedcom, getWives(), Person.LivingStatus.UNKNOWN);
      }
   }

   // Returns true if all of the people indicated by the IDs in personList have the
   // LivingStatus indicated by "ls". Returns false otherwise.
   private static boolean allStatus(Gedcom gedcom, Collection <String> personList, Person.LivingStatus ls)
         throws Gedcom.PostProcessException
   {
      for (String personID : personList)
      {
         Person person = gedcom.getPeople().get(personID);
         if (person != null)
         {
            if (person.getLiving() != ls)
            {
               return false;
            }
         } else
         {
            gedcom.warn("Person referred to while checking if all have a " +
                  "certain living status in family is not valid: " + personID);
         }
      }
      return true;
   }

   private boolean shouldAlwaysPrint = false;

   public void setShouldAlwaysPrint() {
      this.shouldAlwaysPrint = true;
   }

   public boolean hasLivingParent(Gedcom gedcom) throws Uploader.PrintException
   {
      return hasLiving(gedcom, getHusbands(), "husband") ||
            hasLiving(gedcom, getWives(), "wife");
   }

   /**
    *
    * @param gedcom
    * @return true if there is at least one dead person in family, false otherwise
    */
   public boolean shouldPrint (Gedcom gedcom)
   {
      int cntIncluded = countIncluded(gedcom, getHusbands(), "husband") +
                        countIncluded(gedcom, getWives(), "wife") +
                        countIncluded(gedcom, Family.getChildIDs(getChildren()), "child");
      return (shouldAlwaysPrint ||
              (!isBeforeCutoff(gedcom) &&
                  (cntIncluded > 1 || (cntIncluded > 0 && getEvents().size() > 0))));
   }

   // Extracts the GEDCOM childIDs from the collection of Child objects
   public static Collection <String> getChildIDs (Collection <Child> childList) {
      List <String> children = new ArrayList <String>();
      for (Child chil : childList)
      {
         children.add(chil.getId());
      }
      return children;
   }

   // Returns the number of family members that will not be excluded in the collection
   private int countIncluded(Gedcom gedcom, Collection <String> personList, String typeString) {
      int cnt = 0;
      for (String personID : personList)
      {
         Person person = gedcom.getPeople().get(personID);
         if (person != null)
         {
            if (person.isIncluded(gedcom))
            {
               cnt++;
            }
         } else
         {
            logger.info(gedcom.logStr("Invalid ID for " + typeString + ": " + personID));
         }
      }
      return cnt;
   }

   public boolean isBeforeCutoff(Gedcom gedcom) {
      Collection<String> members = new ArrayList<String>();
      members.addAll(getHusbands());
      members.addAll(getWives());
      members.addAll(Family.getChildIDs(getChildren()));

      for (String personID : members) {
         Person person = gedcom.getPeople().get(personID);
         if (person != null && !person.isBornBeforeCutoff()) {
            return false;
         }
      }
      return true;
   }

   // Returns true if any of the people indicated by the list of GEDCOM IDs in personList
   // have LivingStatus == Living.
   // Otherwise this returns false
   private boolean hasLiving(Gedcom gedcom, Collection <String> personList, String typeString)
      throws Uploader.PrintException
   {
      for (String personID : personList)
      {
         Person person = gedcom.getPeople().get(personID);
         if (person != null)
         {
            if (person.isLiving())
            {
               return true;
            }
         } else
         {
            logger.info(gedcom.logStr("Invalid ID for " + typeString + ": " + personID));
         }
      }
      return false;
   }

   private void checkSpouseDates(Person spouse, boolean isHusband, int minMarriageDay, int maxMarriageDay) {
      String spousePronoun = isHusband ? "husband" : "wife";
      int spouseMinBirthDay = new EventDate(spouse.getBirthDate()).getMinDay();                        // method replaced Oct 2021 by Janet Bjorndahl
      int spouseMaxBirthDay = new EventDate(spouse.getBirthDate()).getMaxDay();                        // method replaced Oct 2021 by Janet Bjorndahl
      if (spouseMaxBirthDay > 0 && minMarriageDay > 0 && minMarriageDay - spouseMaxBirthDay > 100 * 365) {
         addProblem("2Marriage is after " + spousePronoun + " is 100 years old");
      }
      else if (spouseMaxBirthDay > 0 && minMarriageDay > 0 && minMarriageDay - spouseMaxBirthDay > 70 * 365) {
         addProblem("0Marriage is after " + spousePronoun + " is 70 years old");
      }
      if (spouseMinBirthDay > 0 && maxMarriageDay > 0 && maxMarriageDay - spouseMinBirthDay < 12 * 365) {
         addProblem("2Marriage is before " + spousePronoun + " is 12 years old");
      }

      int spouseMaxDeathDay = new EventDate(spouse.getDeathDate()).getMaxDay();                        // method replaced Oct 2021 by Janet Bjorndahl
      if (spouseMaxDeathDay > 0 && minMarriageDay > 0 && minMarriageDay > spouseMaxDeathDay) {
         addProblem("2Marriage occurs after the death of " + spousePronoun);
      }
   }

   private void checkParentDates(Gedcom gedcom, String parentId, boolean isHusband,
                                 int childMinBirthDay, int childMaxBirthDay, int childMinDeathDay, Name childName) {
      Person parent = gedcom.getPeople().get(parentId);
      if (parent != null)
      {
         // Let's see if the husband was born too early for this parent
         int parentMinBirthDay = new EventDate(parent.getBirthDate()).getMinDay();                        // method replaced in these 4 rows Oct 2021 by Janet Bjorndahl
         int parentMaxBirthDay = new EventDate(parent.getBirthDate()).getMaxDay();
         int parentMinDeathDay = new EventDate(parent.getDeathDate()).getMinDay();
         int parentMaxDeathDay = new EventDate(parent.getDeathDate()).getMaxDay();
         if (childMaxBirthDay > 0 && parentMinBirthDay > 0) {
            if (isHusband && childMaxBirthDay - parentMinBirthDay < 16 * 365) {
               addProblem("2Husband was less than 16 years old when " + childName + " was born");
            }
            else if (!isHusband && childMaxBirthDay - parentMinBirthDay < 12 * 365) {
               addProblem("2Wife was less than 12 years old when " + childName + " was born");
            }
         }
         if (childMinBirthDay > 0 && parentMaxBirthDay > 0) {
            if (isHusband && childMinBirthDay - parentMaxBirthDay > 80 * 365) {
               addProblem("1Husband was more than 80 years old when " + childName + " was born");
            }
            if (isHusband && childMinBirthDay - parentMaxBirthDay > 65 * 365) {
               addProblem("0Husband was more than 65 years old when " + childName + " was born");
            }
            else if (!isHusband && childMinBirthDay - parentMaxBirthDay > 55 * 365) {
               addProblem("1Wife was more than 55 years old when " + childName + " was born");
            }
            else if (!isHusband && childMinBirthDay - parentMaxBirthDay > 45 * 365) {
               addProblem("0Wife was more than 45 years old when " + childName + " was born");
            }
         }
         if (childMinBirthDay > 0 && parentMaxDeathDay > 0) {
            if (isHusband && childMinBirthDay - parentMaxDeathDay > 270) {
               addProblem("2Husband died more than nine months before " + childName + " was born");
            }
            else if (!isHusband && childMinBirthDay - parentMaxDeathDay > 0) {
               addProblem("2Wife died before " + childName + " was born");
            }
         }
         if (childMinDeathDay > 0 && parentMaxBirthDay > 0 && childMinDeathDay - parentMaxBirthDay > 200 * 365) {
            addProblem("2Child died more than 200 years after "+(isHusband ? "father" : "mother")+" was born");
         }
         if (parentMinDeathDay > 0 && childMaxBirthDay > 0 && parentMinDeathDay - childMaxBirthDay > 100 * 365) {
            addProblem("2"+(isHusband ? "Father" : "Mother")+" died more than 100 years after child was born");
         }
      }
   }

   private DateStd getMarriageDateStd() {
      Integer stdMarriageDate = 0;              // changed type from String (set to null) Oct 2021 by Janet Bjorndahl
      String marriageDate = null;

      for (Event event : getEvents())
      {
         if (event.getType() != Event.Type.lds_spouse_sealing) {
            String date = event.getAttribute("DATE");
            if (!Utils.isEmpty(date)) {
               // Edit dates - error if the date cannot be interpreted; otherwise an alert if the date requires signficant reformating. Added Aug 2021 by Janet Bjorndahl
               EventDate eventDate = new EventDate(date, event.eventType());
               if (eventDate.editDate()) {
                  if (eventDate.getSignificantReformat()) {
                     addProblem("0" + event.eventType() + " date automatically reformated from \"" + date + "\" to \"" + eventDate.getFormatedDate() + "\"");
                  }
               }
               else {
                  addProblem("2" + eventDate.getErrorMessage() + ": " + date + " (Please write dates in \"d mmm yyyy\" format, e.g., 5 Jan 1900)");
               }
               if (event.getType() == Event.Type.marriage) {
                  marriageDate = date;
                  stdMarriageDate = eventDate.getDateSortKey();        // method replaced Oct 2021 by Janet Bjorndahl
               }
            }   
         }
      }
      return new DateStd(marriageDate, stdMarriageDate);
   }

   // Reformats sufficient (minimal) gedcom data into XML format for editing using FamilyDQAnalysis.
   public String prepareDataForAnalysis(Gedcom gedcom) 
         throws Uploader.PrintException, Gedcom.PostProcessException
   {
      StringBuffer buf = new StringBuffer();
      StringBuffer sourceBuffer = new StringBuffer();
      StringBuffer noteBuffer = new StringBuffer();
      StringBuffer bodyText = new StringBuffer();
      buf.append("<family>\n");
      printFamilyMembers(buf, bodyText, gedcom);
      printEvents(gedcom, buf, sourceBuffer, noteBuffer);
      buf.append("</family>");
      return buf.toString();
   }

   private static final Set<String> LIVING_EVENT_WORDS = Person.LIVING_EVENT_WORDS;

   // Determines if this family has events indicating the GEDCOM generator designated 
   // one or more family members as living.
   public boolean hasLivingEvents() {
      for (Event event : getEvents())
      {
         String date = event.getAttribute("DATE");
         if (!Utils.isEmpty(date))
         {
            date = date.trim().toLowerCase();
            if (LIVING_EVENT_WORDS.contains(date))
            {
               return true;
            }
         }
      }
      return false;
   }

   public void findProblems(MultiMap<String, Family> familyNames2Family, Gedcom gedcom)
         throws Gedcom.PostProcessException
   {
      // Check to see if there is a possibly duplicate family:
      String wikiTitle = getWikiTitle(gedcom);
      Set<Family> possibleDuplicates = familyNames2Family.get(wikiTitle);
      Person thisHusband = getFirstHusband() == null ? null : gedcom.getPeople().get(getFirstHusband());
      Person thisWife = getFirstWife() == null ? null : gedcom.getPeople().get(getFirstWife());
      if (thisHusband != null && thisWife != null && thisHusband.getName() != null && thisWife.getName() != null) {
         for (Family possibleDuplicate : possibleDuplicates)
         {
            // let's see if the given and surnames match.
            if (this != possibleDuplicate)
            {
               Person otherHusband =  possibleDuplicate.getFirstHusband() == null ? null :
                                       gedcom.getPeople().get(possibleDuplicate.getFirstHusband());
               Person otherWife = possibleDuplicate.getFirstWife() == null ? null :
                                 gedcom.getPeople().get(possibleDuplicate.getFirstWife());
               if (otherHusband != null && otherHusband.getName() != null &&
                   otherWife != null && otherWife.getName() != null &&
                   thisHusband.getName().givenSurnameEquals(otherHusband.getName()) &&
                   thisWife.getName().givenSurnameEquals(otherWife.getName()))
               {
                  Integer stdMarriage = getMarriageDateStd().stdDate;                  // type changed from String on this and next line Oct 2021 by Janet Bjorndahl
                  Integer stdOther = possibleDuplicate.getMarriageDateStd().stdDate;
                  if (stdMarriage != 0 && stdOther != 0 && stdMarriage.equals(stdOther)) {    // changed Oct 2021 by Janet Bjorndahl
                     addProblem("3This family duplicates another family in your gedcom");
                  }
                  else {
                     addProblem("2This family may duplicate another family in your gedcom");
                  }
                  break;
               }
            }
         }
      }

      // Check to see if the husband and wife have the same surname:
      for (String husbandId : getHusbands())
      {
         Person husband = gedcom.getPeople().get(husbandId);
         if (husband != null && husband.getName() != null)
         {
            String husbandSurname = husband.getName().getSurname();
            String husbandGivenname = husband.getName().getFirstGiven();
            String husbandGivenRegex = null;
            if (!SharedUtils.isEmpty(husbandGivenname)) {
               husbandGivenRegex = ".*\\b" + husbandGivenname.toLowerCase() + "\\b.*";
            }
            for (String wifeId : getWives())
            {
               Person wife = gedcom.getPeople().get(wifeId);
               if (wife != null && wife.getName() != null)
               {
                  String wifeSurname = wife.getName().getSurname();
                  String wifeGivenname = wife.getName().getGiven();
                  if (husbandSurname != null && wifeSurname != null && husbandSurname.equalsIgnoreCase(wifeSurname)) {
                     addProblem("0Husband and wife have the same surname");
                  }
                  if (husbandGivenRegex != null &&
                          ((wifeGivenname != null && wifeGivenname.toLowerCase().matches(husbandGivenRegex)) ||
                           (wifeSurname != null && wifeSurname.toLowerCase().matches(husbandGivenRegex)))) {
                     addProblem("0Husband's given name is part of the wife's name");
                  }
               }
            }
         }
      }

      // Let's gather pertinent dates
      DateStd marriage = getMarriageDateStd();

      // check problems with marriage date
      int minMarriageDay = 0;
      int maxMarriageDay = 0;
      if (marriage.stdDate != 0) {                                                 // changed Oct 2021 by Janet Bjorndahl
         minMarriageDay = new EventDate(marriage.date).getMinDay();                // method replaced in these 2 lines Oct 2021 by Janet Bjorndahl
         maxMarriageDay = new EventDate(marriage.date).getMaxDay();

         for (String husbandId : getHusbands())
         {
            Person husband = gedcom.getPeople().get(husbandId);
            if(husband != null)
            {
               checkSpouseDates(husband, true, minMarriageDay, maxMarriageDay);
            }
         }

         for (String wifeId : getWives())
         {
            Person wife = gedcom.getPeople().get(wifeId);
            if (wife!=null)
            {
               checkSpouseDates(wife, false, minMarriageDay, maxMarriageDay);
            }
         }
      }

      // Now let's go through all of the child-birth related problems:
      List<MinMaxDate> childBirthDates = new ArrayList<MinMaxDate>();
      for (Child childObject : getChildren())
      {
         Person child = gedcom.getPeople().get(childObject.getId());
         if (child != null)
         {
            if (!"".equals(child.getBirthDate()) || !"".equals(child.getDeathDate())) {     // method on these 4 lines changed Oct 2021 by Janet Bjorndahl
               int childMinBirthDay = new EventDate(child.getBirthDate()).getMinDay();       
               int childMaxBirthDay = new EventDate(child.getBirthDate()).getMaxDay();
               int childMinDeathDay = new EventDate(child.getDeathDate()).getMinDay();

               if (childMinBirthDay > 0 && childMaxBirthDay > 0) {
                  if (child.getBirthDate(false) == null) {
                     childMinBirthDay -= 5 * 365; // the date we're using must be a christening date, so assume child could have been born up to 5 years earlier
                  }
                  childBirthDates.add(new MinMaxDate(childMinBirthDay, childMaxBirthDay));
               }

               // check for marriage date / child birth problems
               if (childMaxBirthDay > 0 && minMarriageDay > 0 && minMarriageDay - childMaxBirthDay > 5 * 365 ) {
                  addProblem("1Birth of " + child.getName() + " occurred more than 5 years before marriage");
               }
               else if (childMaxBirthDay > 0 && minMarriageDay > 0 && childMaxBirthDay < minMarriageDay) {
                  addProblem("0Birth of " + child.getName() + " occurred before marriage");
               }
               if (childMinBirthDay > 0 && maxMarriageDay > 0 && childMinBirthDay - maxMarriageDay > 50 * 365) {
                  addProblem("2Birth of " + child.getName() + " occurred over 50 years after marriage");
               }
               if (childMinBirthDay > 0 && maxMarriageDay > 0 && childMinBirthDay - maxMarriageDay > 35 * 365) {
                  addProblem("1Birth of " + child.getName() + " occurred over 35 years after marriage");
               }

               // Now let's check and see if the child was born abnormally early or late.
               for (String husbandId : getHusbands())
               {
                  checkParentDates(gedcom, husbandId, true, childMinBirthDay, childMaxBirthDay, childMinDeathDay, child.getName());
               }

               for (String wifeId : getWives())
               {
                  checkParentDates(gedcom, wifeId, false, childMinBirthDay, childMaxBirthDay, childMinDeathDay, child.getName());
               }
            }
         }
      }

      // Now let's see if there are any child birth events which occur less than 9 months apart
      OUTER2:
      for (MinMaxDate minMaxDate : childBirthDates) {
         for (MinMaxDate otherMinMaxDate : childBirthDates) {
            if (minMaxDate.minBirthDay != otherMinMaxDate.minBirthDay ||
                minMaxDate.maxBirthDay != otherMinMaxDate.maxBirthDay) { // dates must be different
               if (minMaxDate.maxBirthDay - otherMinMaxDate.minBirthDay < 250 &&   // allow for 3 weeks premature
                   otherMinMaxDate.maxBirthDay - minMaxDate.minBirthDay < 250) {
                  addProblem("1Child births less than nine months apart");
                  break OUTER2;
               }
            }
         }
      }
   }

   /**
    * Prints the family object out to the PrintWriter out.
    * @param gedcom
    * @param out
    * @param encodeXML whether to encode the XML page text
    * @throws Uploader.PrintException
    */
   public void print(Gedcom gedcom, PrintWriter out, boolean encodeXML)
         throws Uploader.PrintException
   {
      startPage(out, gedcom, false, !shouldPrint(gedcom), hasLivingParent(gedcom), isBeforeCutoff(gedcom));
      try
      {         
         StringBuffer buf = new StringBuffer();
         StringBuffer bodyText = new StringBuffer();
         buf.append("<family>\n");
         printFamilyMembers(buf, bodyText, gedcom);

         printNotes(bodyText, getCitations(), gedcom);
         if (!Utils.isEmpty(bodyText.toString()))
         {
            bodyText.append("\n");
         }
         bodyText.append("<show_sources_images_notes/>\n");
         StringBuffer sourceBuffer = new StringBuffer();
         StringBuffer imageBuf = new StringBuffer();
         StringBuffer noteBuffer = new StringBuffer();
         printEvents(gedcom, buf, sourceBuffer, noteBuffer);
         printCitations(sourceBuffer, noteBuffer, gedcom);
         buf.append(sourceBuffer);
         buf.append(noteBuffer);
         buf.append("</family>\n");
         buf.append(Utils.replaceHTMLFormatting(bodyText));
         // Finish printing out the content
         out.print("<content>");
         String outText = buf.toString();
         if (encodeXML)
         {
            //outText = Utils.encodeXML(outText);
            outText = encloseInCDATA(outText);
         }
         out.print(outText);
      } catch (Uploader.PrintException e)
      {
         if (e.getGedcom() == null)
         {
            e.setId(getID());
            e.setGedcom(gedcom);
            gedcom.incrementWarnings();
         }
         logger.warn(e);
      } catch (Gedcom.PostProcessException e)
      {
         logger.warn(e);
      }
      out.println("</content>");
      out.println("</page>");

   }

   private void printFamilyMembers(StringBuffer buf, StringBuffer bodyText, Gedcom gedcom)
         throws Uploader.PrintException, Gedcom.PostProcessException
   {
      // The reason we need this string is to make
      // sure that we print out the preferred husband
      // and wife first.
      String subPeople = "";
      String subPersonStr;
      for (String husband : getHusbands())
      {
         subPersonStr = printSubPerson("husband", husband, gedcom, getID(), true, bodyText);
         if (!Utils.isEmpty(preferredHusband) &&
               husband.equals(preferredHusband))
         {
            buf.append(subPersonStr);
         } else
         {
            subPeople += subPersonStr;
         }
      }
      buf.append(subPeople);
      subPeople = "";
      for (String wife : getWives())
      {
         subPersonStr = printSubPerson("wife", wife, gedcom, getID(), true, bodyText);
         if (!Utils.isEmpty(preferredWife) &&
               wife.equals(preferredWife))
         {
            buf.append(subPersonStr);
         } else
         {
            subPeople += subPersonStr;
         }
      }
      buf.append(subPeople);
      subPeople = "";

      // NOTE!! VERY IMPORTANT --
      // Children must be kept all together when
      // they are printed out!
      Set<Person> children = new TreeSet<Person>();
      for (Child child : getChildren())
      {
         if (child.getId() != null)
         {
            Person person = gedcom.getPeople().get(child.getId());
            if (person != null)
            {
               person.setAdopted(child.isAdopted());
               person.setStatus(child.getStatus());
               children.add(person);
            } else
            {
               logger.info(gedcom.logStr("Invalid ID number for child: " + child.getId() + " in family: " + getID()));
            }
         }
      }

      for (Person child : children)
      {
         subPersonStr = printSubPerson("child", child.getID(), gedcom, getID(), false, bodyText);
         buf.append(subPersonStr);
      }
      buf.append(subPeople);

      for (Person child : children)
      {
         if (child.isAdopted())
         {
            addNote(child.getWikiTitle(gedcom) +
                  " was adopted.");
         }

         if (!Utils.isEmpty(child.getStatus()))
         {
            addNote(child.getWikiTitle(gedcom)
                  + "'s status: "
                  + child.getStatus());
         }
      }
   }

   private boolean printAttribute(Person person, Gedcom gedcom, GedcomElementWriter ew, Event.Type eventType) throws Uploader.PrintException {
      for (Event event : person.getEvents()) {
         if (event.getType() == eventType) {
            event.printAsAttribute(gedcom, ew);
            return true;
         }
      }
      return false;
   }

   // Prints the tags of a husband, wife, or child
   private String printSubPerson(String tagName, String id,
                                 Gedcom gedcom, String famID,
                                 boolean printChildOfFamilies,
                                 StringBuffer bodyText)
         throws Gedcom.PostProcessException, Uploader.PrintException
   {
      String outText = "";
      if (id != null)
      {
         Person person = gedcom.getPeople().get(id);
         if (person == null)
         {
            logger.info(gedcom.logStr("Invalid reference to INDI: " + id));
            Utils.prependParagraphBreak(bodyText);
            bodyText.append("Missing reference to ").append(tagName).append(" with GEDCOM ID: ").append(id);
            return "";
         }
         GedcomElementWriter ew = new GedcomElementWriter(tagName);

         ew.put("id", id);
         String value;
         Name name = person.getName();
         if (name != null)
         {
            ew.put("given", name.getGiven());
            ew.put("surname", name.getSurname());
            ew.put("title_prefix", name.getPrefix());
            ew.put("title_suffix", name.getSuffix());
         }

         // Now we need to print out certain events.
         printAttribute(person, gedcom, ew, Event.Type.birth);
         printAttribute(person, gedcom, ew, Event.Type.christening);
         //      printAttribute(person, gedcom, ew, Event.Type.Baptism);
         printAttribute(person, gedcom, ew, Event.Type.death);
         printAttribute(person, gedcom, ew, Event.Type.burial);
         if (printChildOfFamilies)
         {
            String personFamilyID = person.getPrimaryChildOf();
            if (personFamilyID != null)
            {
               Family fam = gedcom.getFamilies().get(personFamilyID);
               if (fam == null) 
               {
                  logger.info("Invalid family ID exists inside of the person: "
                  + person.getID() + " Family ID: " + personFamilyID);
               }
               else if (fam.shouldPrint(gedcom)) 
               {
                  ew.put("child_of_family", personFamilyID);
               }
            }
         }
         outText += ew.write();
      }
      return outText;
   }

   /**
    * Used for printing out the child_of_family tag while printing out
    * a person
    * @param buf
    * @throws Uploader.PrintException
    */
   public void printChildOfFamily(StringBuffer buf) throws Uploader.PrintException {
      GedcomElementWriter ew = new GedcomElementWriter("child_of_family");
      ew.put("id", getID());
      ew.write(buf);
   }

   private static final Set <String> IGNORE_TAGS = new HashSet<String>(Arrays.asList(
           "_SEPR", "_DETS", "PLAC", "NMR", "_MEN", "UMAR"
   ));
   /**
    *
    * @param localName
    * @return whether the localName tag passed in should be ignored
    */
   public static boolean shouldIgnore(String localName) {
      return IGNORE_TAGS.contains(localName);
   }

   /**
    * Object used to hold some information about
    * a child which is found in a few of the GEDCOMs,
    * such as whether the child was adopted or that
    * it had a special status
    */
   public static class Child {
      private String id = null;
      public String getId() {
         return id;
      }

      public void setId(String id) {
         this.id = Utils.setVal(this.id, id);
      }

      private boolean adopted = false;

      public boolean isAdopted() {
         return adopted;
      }

      public void setAdopted(boolean adopted) {
         this.adopted = adopted;
      }

      private String status = null;

      public String getStatus() {
         return status;
      }

      public void setStatus(String status) {
         this.status = status;
      }
   }

   // All children attched to the family
   private Set <Child> children = new HashSet <Child>();
   // The GEDCOM ID number of the child which must be printed first
   // Some GEDCOMs specify this.
   private Set <String> preferredChildren = new HashSet<String>();

   /**
    * Adds a child object to the family
    * @param gedcom
    * @param child objec to be added to the family
    * @param isPreferred
    */
   public void addChild(Gedcom gedcom, Child child, boolean isPreferred)
   {
      children.add(child);

      if (isPreferred)
      {
         preferredChildren.add(child.getId());
      }
   }

   /**
    *
    * @return set of all children attached to the family
    */
   public Set<Child> getChildren() {
      return children;
   }

   /**
    *
    * @return list of all wives attached to the family
    */
   public List <String> getWives() {
      return wives;
   }

   /**
    * Adds indicated wife to the family
    * @param gedcom
    * @param wife to be added to the GEDCOM
    * @param isPreferred -- indicates whether this wife must be listed first
    */
   public void addWife(Gedcom gedcom, String wife, boolean isPreferred) {
      if (!Utils.isEmpty(wife))
      {
         wives.add(wife);
      }
      if (isPreferred)
      {
         if (Utils.isEmpty(preferredWife))
         {
            preferredWife = wife;
         } else
         {
            gedcom.warn("Attempting to add a preferred wife when there already exists a preferred wife");
         }
      }
   }

   public List<String> getSpouses() {
      List<String> spouses = new ArrayList<String>(husbands.size()+wives.size());
      spouses.addAll(husbands);
      spouses.addAll(wives);
      return spouses;
   }

   /**
    *
    * @return List of all husbands attached to this family
    */
   public List <String> getHusbands() {
      return husbands;
   }

   /**
    * Adds a husband to the family
    * @param gedcom
    * @param husband to be added to family
    * @param isPreferred indicates whether this husband needs to be listed first
    */
   public void addHusband(Gedcom gedcom, String husband, boolean isPreferred) {
      if (!Utils.isEmpty(husband))
      {
         husbands.add(husband);
      }
      if (isPreferred)
      {
         if (Utils.isEmpty(preferredHusband))
         {
            preferredHusband = husband;
         } else
         {
            gedcom.warn("Attempting to add a preferred husband when there alread exists a preferred husband.");
         }
      }
   }

   /**
    *
    * @return the first husband of the family
    */
   public String getFirstHusband() {
      if (!Utils.isEmpty(preferredHusband))
      {
         return preferredHusband;
      }
      else if (husbands.size() > 0)
      {
         return husbands.get(0);
      } else
      {
         return null;
      }
   }

   /**
    *
    * @return the first wife of the family
    */
   public String getFirstWife() {
      if (!Utils.isEmpty(preferredWife))
      {
         return preferredWife;
      } else if (wives.size() > 0)
      {
         return wives.get(0);
      } else
      {
         return null;
      }
   }

   /**
    *
    * @param spouseID
    * @return whether the family contains a parent with the
    * indicated spouseID
    */
   public boolean hasSpouse(String spouseID)
   {
      for (String wifeID : getWives())
      {
         if (wifeID.equals(spouseID))
         {
            return true;
         }
      }
      for (String husbandID : getHusbands())
      {
         if (husbandID.equals(spouseID))
         {
            return true;
         }
      }
      return false;
   }

   /**
    *
    * @param childID
    * @return whether the family contains child with indicated GEDCOM ID
    */
   public boolean hasChild (String childID)
   {
      for (Child child : getChildren())
      {
         if (child.getId() != null && child.getId().equals(childID))
         {
            return true;
         }
      }
      return false;
   }

   /**
    *
    * @param gedcom
    * @return wiki-appropriate title for the family
    * @throws Gedcom.PostProcessException
    */
   public String getWikiTitle(Gedcom gedcom)
         throws Gedcom.PostProcessException
   {
      String husbandName, wifeName;
      Map <String, Person> people = gedcom.getPeople();
      String unknownString = "Unknown";
      if (Utils.isEmpty(getFirstHusband()))
      {
         husbandName = unknownString;
      } else
      {
         Person husband = people.get(getFirstHusband());
         if (husband != null)
         {
            husbandName = husband.getWikiTitle(gedcom);
         } else
         {
            logger.info(gedcom.logStr("Invalid husband id number: " + getFirstHusband() +
                  " in family: " + getID()));
            husbandName = unknownString;
         }
      }
      if (Utils.isEmpty(getFirstWife()))
      {
         wifeName = unknownString;
      } else
      {
         Person wife = people.get(getFirstWife());
         if (wife != null)
         {
            wifeName = wife.getWikiTitle(gedcom);
         } else
         {
            logger.info(gedcom.logStr("Invalid wife id number: " + getFirstWife() +
                  " in family: " + getID()));
            wifeName = unknownString;
         }
      }
      return husbandName + " and " + wifeName;
   }

   // Possible attribute GEDCOM tags in a family
   private static final String [] ATTRIBUTE_STRINGS = {
         "HUSB", "HUSBAND", "WIFE", "CHIL", "CHILD",
         "UID", "_UID", "NOTE", "SOUR", "SOURCE", "STAT", "_STAT", "OBJE", "INFO"
   };
   private static final Set <String> ATTRIBUTE_SET = new HashSet<String>();
   static
   {
      for (String s : ATTRIBUTE_STRINGS)
      {
         ATTRIBUTE_SET.add(s);
      }
   }
   /**
    *
    * @param tag
    * @return whether the GEDCOM tag name is an attribute of a family
    */
   public static boolean isAttribute (String tag)
   {
      return ATTRIBUTE_SET.contains(tag) || Event.isEvent(tag) || Event.isOtherEvent(tag);
   }

   public Set<Citation> getAllCitations(Gedcom gedcom) throws Gedcom.PostProcessException
   {
      Set<Citation> citations = new HashSet<Citation>();
      citations.addAll(getCitations());
      for (Note note : getNotesFromCitations(gedcom)) citations.addAll(note.getSourceCitations());
      for (Event event : getEvents()) {
         citations.addAll(event.getCitations());
         for (Note note : event.getNotes()) citations.addAll(note.getSourceCitations());
         for (Note note : Note.Citation.getNotesFromCitations(gedcom, event.getNoteCitations())) citations.addAll(note.getSourceCitations());
      }
      return citations;
   }
}
