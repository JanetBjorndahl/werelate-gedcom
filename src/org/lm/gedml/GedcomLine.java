package org.lm.gedml;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.werelate.util.Utils;

import java.util.regex.Pattern;
import java.util.regex.Matcher;

/**
 * Created by IntelliJ IDEA.
 * User: npowell
 * Date: May 2, 2007
 * Time: 9:59:22 AM
 * To change this template use File | Settings | File Templates.
 */
public class GedcomLine {
   private static final Logger logger = LogManager.getLogger("org.werelate.gedcom.Gedcom");
   private static final String tagRegex = "(\\w+)";
   private static final String idRegex = "(@([^@]+)@)";
   private static final Pattern pGedcomLine = Pattern.compile(
         "^\\s*(\\d)\\s+((" + idRegex + "\\s" + tagRegex +
         ")|(" + tagRegex + "(\\s" + idRegex + ")?))(\\s(.*))?$"
   );

   private static final int LEVEL_NUM_GROUP = 1;
   private static final int BEFORE_ID_GROUP = 5;
   private static final int AFTER_TAG_GROUP = 6;
   private static final int BEFORE_TAG_GROUP = 8;
   private static final int AFTER_ID_GROUP = 11;
   private static final int REMAINDER_GROUP = 13;

   private Matcher m = null;

   public GedcomLine (String line)
   {
      Matcher m = pGedcomLine.matcher(line);
      if(m.find())
      {
         this.m = m;
      } else
      {
         logger.info("Line does not appear to be valid, so we will append to tag above: " + line);
      }
   }

   /**
    *
    * @return true if the line was a standard GEDCOM line and was therefore parsed, false otherwise.
    */
   public boolean wasAbleToParse() {
      return m != null;
   }

   public String getLevelNum () {
      if (m!=null)
      {
         return m.group(LEVEL_NUM_GROUP);
      }
      return null;
   }

   public String getXRef () {
      if (m!=null)
      {
         return m.group(BEFORE_ID_GROUP);
      }
      return null;
   }

   public String getID () {
      if(m!=null)
      {
         String returnValue = m.group(AFTER_ID_GROUP);
         if (Utils.isEmpty(returnValue))
         {
            return returnValue;
         } else
         {
            return returnValue.replaceAll("[|#/]+", "");
         }
      }
      return null;
   }

   public String getTag() {
      if (m!=null)
      {
         String tag = m.group(BEFORE_TAG_GROUP);
         if (tag != null)
         {
            return tag;
         } else
         {
            return m.group(AFTER_TAG_GROUP);
         }
      }
      return null;
   }

   public String getRemainder () {
      if (m!=null)
      {
         return m.group(REMAINDER_GROUP);
      }
      return null;
   }
}
