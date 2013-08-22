package org.werelate.gedcom;

import java.util.Map;
import java.util.HashMap;

/**
 * Created by IntelliJ IDEA.
 * User: npowell
 * Date: Jan 31, 2007
 * Time: 1:47:05 PM
 * Represents a GEDCOM schema like the one produced by Family Tree Maker
 */
public class Schema {
   private Map <String, Map<String, String>> parentTag2schema = new HashMap<String, Map<String,String>>();

   // These are the default values of a schema
   public Schema() {
      put("INDI", "_FA1", "");
      put("INDI", "_FA2", "");
      put("INDI", "_FA3", "");
      put("INDI", "_FA4", "");
      put("INDI", "_FA5", "");
      put("INDI", "_FA6", "");
      put("INDI", "_FA7", "");
      put("INDI", "_FA8", "");
      put("INDI", "_FA9", "");
      put("INDI", "_FA10", "");
      put("INDI", "_FA11", "");
      put("INDI", "_FA12", "");
      put("INDI", "_FA13", "");
      put("INDI", "_MREL", "");
      put("INDI", "_FREL", "");
      put("FAM", "_FA1", "");
      put("FAM", "_FA2", "");
      put("FAM", "_MSTAT", "");
      put("FAM", "_MEND", "");
      put("INDIVIDUAL", "_FACT1", "");
      put("INDIVIDUAL", "_FACT2", "");
      put("INDIVIDUAL", "_FACT3", "");
      put("INDIVIDUAL", "_FACT4", "");
      put("INDIVIDUAL", "_FACT5", "");
      put("INDIVIDUAL", "_FACT6", "");
      put("INDIVIDUAL", "_FACT7", "");
      put("INDIVIDUAL", "_FACT8", "");
      put("INDIVIDUAL", "_FACT9", "");
      put("INDIVIDUAL", "_FACT10", "");
      put("INDIVIDUAL", "_FACT11", "");
      put("INDIVIDUAL", "_FACT12", "");
      put("INDIVIDUAL", "_FACT13", "");
      put("INDIVIDUAL", "_MREL", "");
      put("INDIVIDUAL", "_FREL", "");
      put("FAMILY", "_FA1", "");
      put("FAMILY", "_FA2", "");
      put("FAMILY", "_MSTAT", "");
      put("FAMILY", "_MEND", "");
   }

   /**
    *
    * @param topTag -- is usually INDI or FAM. (We may have
    * a different schema in the FAM schema than in the INDI).
    * @param key is the name of the tag
    * @param value is the label to use in the type field of the
    * "Other" event that will be produced
    */
   public void put(String topTag, String key, String value)
   {
      if (parentTag2schema.containsKey(topTag))
      {
         Map <String,String> topTagMap = parentTag2schema.get(topTag);
         topTagMap.put(key, value);
      } else
      {
         Map <String,String> topTagMap = new HashMap<String,String>();
         topTagMap.put(key, value);
         parentTag2schema.put(topTag, topTagMap);
      }
   }

   /**
    *
    * @param topTag is either INDI or FAM
    * @param key TAG name
    * @return returns true if the
    * the key associated with the top tag exists, false otherwise
    */
   public boolean contains(String topTag, String key)
   {
      Map <String, String> topMap = parentTag2schema.get(topTag);
      if (topMap != null)
      {
         return topMap.containsKey(key);
      } else
      {
         return false;
      }
   }

   /**
    *
    * @param topTag
    * @param key TAG name
    * @return returns the label associated with the
    * key in the context of the topTag
    */
   public String get(String topTag, String key)
   {
      Map <String, String> topMap = parentTag2schema.get(topTag);
      if (topMap != null)
      {
         return topMap.get(key);
      } else
      {
         return null;
      }
   }

   /**
    *
    * @param tagName
    * @return true if the tagName exists in any of the
    * top tag contexts, false otherwise
    */
   public boolean contains(String tagName)
   {
      for (Map<String,String> map : parentTag2schema.values())
      {
         if (map.containsKey(tagName))
         {
            return true;
         }
      }
      return false;
   }
}
