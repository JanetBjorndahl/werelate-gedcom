package org.werelate.gedcom;

import org.werelate.util.Utils;

import java.util.List;
import java.util.ArrayList;
import java.util.Collection;

/**
 * Created by IntelliJ IDEA.
 * User: npowell
 * Date: Jan 10, 2007
 * Time: 12:09:28 PM
 * Represents a GEDCOM REPO tag; this information is
 * copied over to the Source object before it is printed out
 * as part of the Source.
 */
public class Repository extends TopObject {
   private String address = null;

   public String getName() {
      return name;
   }

   public void setName(String name) {
      this.name = Utils.setVal(this.name, name);
   }

   public void appendName(String newName) {
      this.name = Uploader.append(this.name, newName, "");
   }

   private String name = null;

   public String getAddress() {
      return address;
   }

   public void appendToAddress(String address) {
      this.address = Uploader.append(this.address, address, " ");
   }

   private String callNum = null;

   public String getCallNum() {
      return callNum;
   }

   public void setCallNum(String callNum) {
      this.callNum = callNum;
   }

   List <String> notes = new ArrayList<String> ();
   /**
    *
    * @param note to be added
    */
   public void addNote(String note) {
      notes.add(note);
   }

   public void addNotes(Collection<String> notes)
   {
      this.notes.addAll(notes);
   }

   public List <String> getNotes () {
      return notes;
   }

   protected int getNamespace() {
      return 0;
   }

   public void eatIgnoredBucket(Collection<String> ignoredBucket) {
      addNotes(ignoredBucket);
      ignoredBucket.clear();
   }

   public List <Image> images = new ArrayList<Image>();
   public void addImage(Image image)
   {
      images.add(image);
   }
}
