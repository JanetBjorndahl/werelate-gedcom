package org.werelate.gedcom;

import org.werelate.util.ElementWriter;

import java.util.Map;
import java.util.HashMap;

/**
 * Created by IntelliJ IDEA.
 * User: npowell
 * Date: Feb 21, 2007
 * Time: 10:30:59 AM
 * Used for printing out XML tags in a uniform
 * fashion. Used in many different parts of the GEDCOM
 * xml printing code.
 */
public class GedcomElementWriter extends ElementWriter {
   public GedcomElementWriter (String tagName) {
      super(tagName);
   }
   private static final String [][][] ATTRIBUTE_ORDER = {
         {{"name", "alt_name"}, {"type", "given", "surname", "title_prefix", "title_suffix", "sources", "notes"}},
         {{"event_fact"}, {"type", "date", "place", "desc", "sources", "images", "notes"}},
         {{"source_citation"}, {"id", "title", "source_id", "record_name", "page", "quality", "date", "notes", "images", "text"}},
         {{"image"}, {"id", "filename", "gedcom_filename", "caption", "primary", "parent"}},
         {{"husband", "wife", "child"},
               {"id", "title", "given", "surname",
               "title_prefix", "title_suffix", "birthdate", "birthplace",
               "chrdate", "chrplace", "deathdate", "deathplace",
               "burialdate", "burialplace", "child_of_family"}}
   };
   private static final Map<String, Map<String, Integer>> attributeOrderMap =
         new HashMap<String, Map<String, Integer>> ();
   protected Map <String ,Map<String, Integer>> getAttributeOrderMap () {
      return attributeOrderMap;
   }
   static {
      for (int i=0; i < ATTRIBUTE_ORDER.length; i++)
      {
         String [][] elementOrdering = ATTRIBUTE_ORDER[i];
         if (elementOrdering.length != 2)
         {
            throw new RuntimeException("An elements attribute ordering must have two elements: " +
                  "an array of the element names this ordering applies to and an array of the " +
                  "attribute orderings.");
         } else
         {
            String[] elementNames = elementOrdering[0];
            String[] attributeOrder = elementOrdering[1];
            Map <String,Integer> attributeOrderMap = new HashMap<String,Integer>();
            for (int j = 0; j < attributeOrder.length; j++)
            {
               attributeOrderMap.put(attributeOrder[j], j);
            }
            for(String elementName : elementNames)
            {
               GedcomElementWriter.attributeOrderMap.put(elementName, attributeOrderMap);
            }
         }
      }
   }
}
