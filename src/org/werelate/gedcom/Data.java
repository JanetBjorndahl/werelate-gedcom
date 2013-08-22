package org.werelate.gedcom;

/**
 * Created by IntelliJ IDEA.
 * User: npowell
 * Date: Jan 20, 2007
 * Time: 3:56:11 PM
 * Represents the GEDCOM Data tag
 */
public class Data {
   private String date = null;
   private String text = null;

   public String getText() {
      return text;
   }

   public void setText(String text) {
      this.text = text;
   }

   public String getDate() {
      return date;
   }

   public void setDate(String date) {
      this.date = date;
   }
}
