package org.werelate.gedcom;

/**
 * Created by IntelliJ IDEA.
 * User: npowell
 * Date: Jan 22, 2007
 * Time: 12:52:36 PM
 * Interface to define referenced objects in order to cut down on some
 * duplicate code
 */
public interface ReferencedObject {
   /**
    *
    * @return the note or citation id, such as "N1", "N2", "S1", "S2", etc.
    */
   public String getUpperID();

   /**
    *
    * @param upperID
    */
   public void setUpperID(String upperID);
}
