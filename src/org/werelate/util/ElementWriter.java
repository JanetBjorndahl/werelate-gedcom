package org.werelate.util;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Map;
import java.util.TreeMap;

/**
 * Created by IntelliJ IDEA.
 * User: nathan
 * Date: Oct 27, 2007
 * Time: 10:59:30 AM
 * To change this template use File | Settings | File Templates.
 */
public abstract class ElementWriter {
   private static Logger logger = LogManager.getLogger(ElementWriter.class);

   public static class ElementWriterException extends RuntimeException {
      public ElementWriterException (String msg) {
         super(msg);
      }
   }

   private String tagName = null;
   /**
    *
    * @param tagName tag name to be used to print out the tag
    */
   public ElementWriter(String tagName)
   {
      this.tagName = tagName;
   }
   // This is text that belongs in the
   // body of the tag as opposed to being
   // an attribute
   private String text = null;

   /**
    * Readies the element writer to
    * print a new tag after previously
    * printing another tag. Note, this
    * method must be called if this instance
    * has been previously used to print out
    * a tag, unless of course ALL of the
    * attributes and the text are guaranteed
    * to be replaced.
    */
   public void clear() {
      text = null;
      attributes.clear();
   }

   /**
    *
    * @param text to be put in the body of the tag
    */
   public void setSubText(String text) {
      this.text = text;
   }

   private String subXML = null;

   /**
    *
    * @param subXML sets XML data which is to appear in the body of the current
    * tag. This means that we don't XML encode it when we print it out.
    */
   public void setSubXML(String subXML) {
      this.subXML = subXML;
   }

   protected abstract Map <String, Map<String, Integer>> getAttributeOrderMap ();

   private int compareAtts(String elementName, String att1, String att2)
   {
      Map <String, Integer> attMap = getAttributeOrderMap().get(elementName);
      if (attMap == null)
      {
         return att1.compareTo(att2);
      } else
      {
         try
         {
            return attMap.get(att1).compareTo(attMap.get(att2));
         } catch(NullPointerException e)
         {
            throw new ElementWriterException("Null pointer exception " +
                  "encountered while writing page");
         }
      }
   }
   private class Attribute implements Comparable {
      public String value;
      public Attribute(String value)
      {
         this.value = value;
      }
      public int compareTo(Object o) {
         return compareAtts(tagName, value, ((Attribute) o).value);
      }

      public boolean equals(Object obj) {
         return value.equals(((Attribute)obj).value);
      }
   }
   // Attribute name -> attribute value
   private Map<Attribute, String> attributes = new TreeMap<Attribute, String>();

   public Map <Attribute, String> getAttributes()
   {
      return attributes;
   }
   /**
    * Designates an attribute key value pair to be printed
    * @param key attribute name
    * @param value attribute value
    */
   public void put(String key, String value)
   {
      if (attributes.containsKey(new Attribute(key)))
      {
         logger.info("Duplicate attributes in tag: " + key);
      }
      if (key != null && !Utils.isEmpty(key) && value != null && !Utils.isEmpty(value))
      {
         attributes.put(new Attribute(key), value);
      }
   }

   /**
    *
    * @param buf to print the tag to.
    */
   public void write (StringBuffer buf) {
      buf.append("<");
      buf.append(tagName);

      for (Map.Entry entry : attributes.entrySet())
      {
         buf.append(testAndPrintAtt(((Attribute)entry.getKey()).value, (String) entry.getValue()));
      }
      if (Utils.isEmpty(text) && Utils.isEmpty(subXML))
      {
         buf.append("/>\n");
      } else
      {
         buf.append(">");
         if (!Utils.isEmpty(subXML))
         {
            buf.append('\n').append(subXML);
         }
         if (!Utils.isEmpty(text))
         {
            buf.append(Utils.encodeXML(text));
         }
         buf.append("</").append(tagName).append(">\n");
      }
   }

   /**
    *
    * @return String of the printed out tag
    */
   public String write ()
   {
      StringBuffer buf = new StringBuffer();
      write(buf);
      return buf.toString();
   }

   // Tests to see if the value is empty. If it is not, then it prints
   // out the attribute, otherwise, it returns an empty string
   private static String testAndPrintAtt(String key, String value) {
      if (!Utils.isEmpty(value))
      {
         return printAtt(key, Utils.replaceHTMLFormatting(value.replaceAll("\n", " ")));
      } else
      {
         return "";
      }
   }

   // Prints out the key value pair as an XML key value pair.
   private static String printAtt(String name, String value)
   {
      return ' ' + Utils.encodeXML(name) + "=\"" + Utils.encodeXML(value) + '\"';
   }
}
