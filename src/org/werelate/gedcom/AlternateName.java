package org.werelate.gedcom;

/**
 * Created by IntelliJ IDEA.
 * User: npowell
 * Date: Jan 19, 2007
 * Time: 7:24:55 PM
 * Extension of the name object to allow for
 * printing the alternate name in the correct form
 */
public class AlternateName extends Name{
   /**
    * Creates a new alt name
    * @param type of the alt name, such as "Married Name" etc
    */
   public AlternateName (String type) {
      setTagName("alt_name");
      setType(type);
   }

   /**
    * Creates an Alternate Name of type "Alt Name"
    */
   public AlternateName () {
      setTagName("alt_name");
      setType("Alt Name");
   }

   /**
    * Creates an Alternate Name of type "Alt Name"
    * @param name copies entries from this parameter
    */
   public AlternateName(Name name) {
      super(name);
      setTagName("alt_name");
      setType("Alt Name");
   }

   /**
    * This method overide is necessary
    * so that we can set a single word in the
    * name string as a given instead of a surname,
    * which is the typical case for an alternate name
    * @param name to set as alternate name
    * @param gedcom where this name came from
    * @return null
    */
   public AlternateName setName(String name, Gedcom gedcom)
   {
      name = name.trim();
      if (name.matches(".*\\s+.*"))
      {
         super.setName(name, gedcom);
      } else
      {
         setGiven(name);
      }
      return null;
   }
}
