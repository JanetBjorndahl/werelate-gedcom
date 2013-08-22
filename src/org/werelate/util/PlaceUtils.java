/**
 * Copyright (C) 2005-2006 Foundation for On-Line Genealogy (folg.org)

 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.

 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.

 * See gpl.txt in the root project folder or
 * www.gnu.org/copyleft/gpl.html
 */
package org.werelate.util;

import com.ibm.icu.text.Normalizer;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

import org.apache.commons.codec.language.DoubleMetaphone;

public class PlaceUtils {
   // Google romanizes the following
//   private static final char[] SPECIAL_CHARS =            {'´', 'ß',  'ј', 'ð', 'æ',  'ł', 'đ',  'ø',  'ŀ', 'і', 'þ',  'ı', 'œ',  'ĳ',
//                                                                      'Ј', 'Ð', 'Æ',  'Ł', 'Đ',  'Ø',  'Ŀ', 'І', 'Þ',       'Œ',  'Ĳ'};
   private static final char[] SPECIAL_CHARS =              {180, 223, 1112, 240, 230,  322, 273,  248,  320,1110, 254,  305, 339,  307,
                                                                       1032, 208, 198,  321, 272,  216,  319,1030, 222,       338,  306};
   private static final String[] SPECIAL_TRANSLITERATIONS = {"'", "ss", "j", "d", "ae", "l", "dj", "oe", "l", "i", "th", "i", "oe", "y",
                                                                        "J", "D", "Ae", "L", "Dj", "Oe", "L", "I", "Th",      "Oe", "Y"};
   private static final HashMap SPECIAL_MAPPINGS = new HashMap();
   static {
      for (int i = 0; i < SPECIAL_CHARS.length; i++) {
         SPECIAL_MAPPINGS.put(new Character(SPECIAL_CHARS[i]), SPECIAL_TRANSLITERATIONS[i]);
      }
   }

   /**
    * Convert non-roman letters in the specified string to their roman (a-zA-Z) equivalents.
    * For example, strip accents from characters, and expand ligatures.  This function attempts to mimic
    * the character conversion that Google does.
    * @param in
    * @return boolean
    */
   public static String romanize(String in) {
      if (in == null) {
         return "";
      }
      if (isAscii(in)) {
         return in;
      }

      char[] srcChar = new char[1];
      char[] destChars = new char[8];
      StringBuffer buf = new StringBuffer();
      for (int i = 0; i < in.length(); i++) {
         // decompose the character
         srcChar[0] = in.charAt(i);
         Normalizer.decompose(srcChar, destChars, false, 0);
         char c = destChars[0];

         if ((int)c >= 0x0300 && (int)c <= 0x036f) {
            // ignore combining diacritics
         }
         else if ((int)c >= 127) {
            String transliteration = (String)SPECIAL_MAPPINGS.get(new Character(c));
            if (transliteration != null) {
               buf.append(transliteration);
            }
            else {
               buf.append(c);
            }
         }
         else {
            buf.append(c);
         }
      }
      return buf.toString();
   }

   private static final String[][] HTML_ENTITIES = {
      {"nbsp","160"},
      {"iexcl","161"},
      {"cent","162"},
      {"pound","163"},
      {"curren","164"},
      {"yen","165"},
      {"brvbar","166"},
      {"sect","167"},
      {"uml","168"},
      {"copy","169"},
      {"ordf","170"},
      {"laquo","171"},
      {"not","172"},
      {"shy","173"},
      {"reg","174"},
      {"macr","175"},
      {"deg","176"},
      {"plusmn","177"},
      {"sup2","178"},
      {"sup3","179"},
      {"acute","180"},
      {"micro","181"},
      {"para","182"},
      {"middot","183"},
      {"cedil","184"},
      {"sup1","185"},
      {"ordm","186"},
      {"raquo","187"},
      {"frac14","188"},
      {"frac12","189"},
      {"frac34","190"},
      {"iquest","191"},
      {"Agrave","192"},
      {"Aacute","193"},
      {"Acirc","194"},
      {"Atilde","195"},
      {"Auml","196"},
      {"Aring","197"},
      {"AElig","198"},
      {"Ccedil","199"},
      {"Egrave","200"},
      {"Eacute","201"},
      {"Ecirc","202"},
      {"Euml","203"},
      {"Igrave","204"},
      {"Iacute","205"},
      {"Icirc","206"},
      {"Iuml","207"},
      {"ETH","208"},
      {"Ntilde","209"},
      {"Ograve","210"},
      {"Oacute","211"},
      {"Ocirc","212"},
      {"Otilde","213"},
      {"Ouml","214"},
      {"times","215"},
      {"Oslash","216"},
      {"Ugrave","217"},
      {"Uacute","218"},
      {"Ucirc","219"},
      {"Uuml","220"},
      {"Yacute","221"},
      {"THORN","222"},
      {"szlig","223"},
      {"agrave","224"},
      {"aacute","225"},
      {"acirc","226"},
      {"atilde","227"},
      {"auml","228"},
      {"aring","229"},
      {"aelig","230"},
      {"ccedil","231"},
      {"egrave","232"},
      {"eacute","233"},
      {"ecirc","234"},
      {"euml","235"},
      {"igrave","236"},
      {"iacute","237"},
      {"icirc","238"},
      {"iuml","239"},
      {"eth","240"},
      {"ntilde","241"},
      {"ograve","242"},
      {"oacute","243"},
      {"ocirc","244"},
      {"otilde","245"},
      {"ouml","246"},
      {"divide","247"},
      {"oslash","248"},
      {"ugrave","249"},
      {"uacute","250"},
      {"ucirc","251"},
      {"uuml","252"},
      {"yacute","253"},
      {"thorn","254"},
      {"yuml","255"},
   };

   public static final Pattern HTML_ENTITY_PATTERN;
   static {
      StringBuffer buf = new StringBuffer();
      buf.append("&(");
      for (int i = 0; i < HTML_ENTITIES.length; i++) {
         if (i > 0) {
            buf.append("|");
         }
         buf.append(HTML_ENTITIES[i][0]);
      }
      buf.append(");");
      HTML_ENTITY_PATTERN = Pattern.compile(buf.toString());
   }

   public static final Map HTML_ENTITY_MAP = new HashMap<String,String>();
   static {
      char[] chars = new char[1];
      for (int i = 0; i < HTML_ENTITIES.length; i++) {
         chars[0] = (char)Integer.parseInt(HTML_ENTITIES[i][1]);
         HTML_ENTITY_MAP.put(HTML_ENTITIES[i][0], new String(chars));
      }
   }

   public static String translateHtmlCharacterEntities(String in) {
      if (in == null) {
         return in;
      }
      StringBuffer buf = null;
      Matcher m = HTML_ENTITY_PATTERN.matcher(in);
      while (m.find()) {
         if (buf == null) {
            buf = new StringBuffer();
         }
         m.appendReplacement(buf, (String)HTML_ENTITY_MAP.get(m.group(1)));
      }
      if (buf == null) {
         return in;
      }
      else {
         m.appendTail(buf);
         return buf.toString();
      }
   }

   /**
    * Returns whether the specified string is null or has a zero length
    * @param s
    * @return boolean
    */
   public static boolean isEmpty(String s) {
      if (s != null)
      {
         s = s.trim();
      }
      return (s == null || s.length() == 0);
   }

   private static final String[] NAMESPACE_ARRAY = {
      "place",
      "place_talk"      
   };
   public static final Set NAMESPACES = new HashSet<String>();
   static {
      for (int i = 0; i < NAMESPACE_ARRAY.length; i++) {
         NAMESPACES.add(NAMESPACE_ARRAY[i]);
      }
   }

   public static String getNamespace(Set NAMESPACES, String title) {
      String namespace = "";
      int pos = title.indexOf(':');
      if (pos > 0) {
         String ns = title.substring(0, pos).toLowerCase().replace(' ', '_');
         if (NAMESPACES.contains(ns)) {
            namespace = ns;
         }
      }
      return namespace;
   }

   public static String removeNoise(String s, boolean removeSpaces) {
      // keep in sync with PlaceSearcher.php
      return s.replaceAll("(\\s|-|\\.|')+", removeSpaces ? "" : " ");
   }

   /**
    * Returns true if the specified string contains only 7-bit ascii characters
    * @param in
    * @return boolean
    */
   public static boolean isAscii(String in) {
      for (int i = 0; i < in.length(); i++) {
         if (in.charAt(i) > 127) {
            return false;
         }
      }
      return true;
   }

   /**
    * Return the number of occurrences of the specified character in the specified string
    */
   public static int countOccurrences(char ch, String in) {
      int cnt = 0;
      int pos = in.indexOf(ch);
      while (pos >= 0) {
         cnt++;
         pos = in.indexOf(ch, pos+1);
      }
      return cnt;
   }

   /**
    * Tokenize on spaces and double-metaphone encode the specified string
    * @param value
    * @return
    */
   public static String doubleMetaphoneEncode(String value) {
      DoubleMetaphone dm = new DoubleMetaphone();
      dm.setMaxCodeLen(8);
      StringBuilder buf = new StringBuilder();
      String[] pieces = value.split("\\s");
      for (int i = 0; i < pieces.length; i++) {
         if (i > 0) {
            buf.append(' ');
         }
         buf.append(dm.doubleMetaphone(pieces[i]));
      }
      return buf.toString();
   }
}
