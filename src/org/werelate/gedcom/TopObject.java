package org.werelate.gedcom;

import org.werelate.util.Utils;
import org.werelate.util.PlaceUtils;
import org.apache.log4j.Logger;

import java.io.PrintWriter;
import java.util.List;
import java.util.ArrayList;
import java.util.Collection;

/**
 * Created by IntelliJ IDEA.
 * User: npowell
 * Date: Jan 15, 2007
 * Time: 2:17:00 PM
 * Represents any object that appears at the top level
 * of a GEDCOM file. Includes Notes, repositories, Person, Family
 * EventContainer derives from this class
 */
public abstract class TopObject {
   private String id = null;
   private String reservedTitle = null;
   private List<Note.Citation> noteCitations = new ArrayList<Note.Citation>();

   /**
    *
    * @param gedcom
    * @return the note textual objects via their citation references
    * @throws Gedcom.PostProcessException
    */
   public List<Note> getNotesFromCitations(Gedcom gedcom) throws Gedcom.PostProcessException {
      Collection<Note.Citation> noteCitations = this.noteCitations;

      return Note.Citation.getNotesFromCitations(gedcom, noteCitations);
   }


   public List <Note.Citation> getNoteCitations() {
      return noteCitations;
   }

   public void addNoteCitation(String id)
   {
      noteCitations.add(new Note.Citation(id));
   }

   public void addNoteCitations(List<Note.Citation> newNoteCitations)
   {
      noteCitations.addAll(newNoteCitations);
   }

   private String uid = null;

   public String getUid() {
      return uid;
   }

   public void setUid(String uid) {
      this.uid = uid;
   }

   public void setId(String id) {
      this.id = id;
   }

   public String getID ()
   {
      return id;
   }

   public void setID(String id) {
      this.id = id;
   }

   private String matches = null;
   public void setMatches(String matches)
   {
      this.matches = matches;
   }

   private List <String> problems = new ArrayList<String>();
   public void addProblem(String problem)
   {
      problems.add(problem);
   }

   protected abstract int getNamespace();

   private static final Logger logger =
         Logger.getLogger("org.werelate.gedcom.TopObject");
   /**
    * Starts printing out the page, including the start of the
    * page tag
    * @param out to print the start of the tag out to.
    * @param gedcom
    * @throws Uploader.PrintException
    */
   protected void startPage (PrintWriter out, Gedcom gedcom, boolean primary, boolean exclude, boolean living, boolean beforeCutoff) throws Uploader.PrintException
   {      
      out.print("<page namespace=\"" + getNamespace() +
               "\" id=\"" + getID() + '\"' +
               " tree_id=\"" + gedcom.getTreeID() + '\"');
      if (!Utils.isEmpty(getUid()))
      {
         out.print(" uid=\"" + Utils.encodeXML(getUid()) + '\"');
      }
      if (primary)
      {
         out.print(" primary=\"1\"");
      }
      if (exclude)
      {
         out.print(" exclude=\"true\"");
      }
      if (living)
      {
         out.print(" living=\"true\"");
      }
      if (beforeCutoff)
      {
         out.print(" beforeCutoff=\"true\"");
      }
      if (!PlaceUtils.isEmpty(matches))
      {
         out.print(" potentialMatches=\"" + matches + "\"");
      }
      if (problems.size() > 0)
      {
         String problemsString = problems.get(0);
         for (int i = 1; i < problems.size(); i++)
         {
            problemsString += '|' + problems.get(i);
         }
         out.print(" problems=\"" + Utils.encodeXML(problemsString) + "\"");
      }
      out.print(">\n");
   }
}
