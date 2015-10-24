package org.werelate.gedcom;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Pattern;

/**
 * User: dallan
 * Date: 10/24/15
 */
public class Util {
   private static final String[] CHARACTER_REPLACEMENTS = {
      "æ","ae",
      "ǝ","ae",
      "ǽ","ae",
      "ǣ","ae",
      "Æ","Ae",
      "Ə","Ae",
      "ß","ss",
      "đ","dj",
      "Đ","Dj",
      "ø","oe",
      "œ","oe",
      "Œ","Oe",
      "Ø","Oe",
      "þ","th",
      "Þ","Th",
      "ĳ","y",
      "Ĳ","Y",
      "á","a",
      "à","a",
      "â","a",
      "ä","a",
      "å","a",
      "ą","a",
      "ã","a",
      "ā","a",
      "ă","a",
      "ǎ","a",
      "ȃ","a",
      "ǻ","a",
      "ȁ","a",
      "Ƌ","a",
      "ƌ","a",
      "ȧ","a",
      "Ã","A",
      "Ą","A",
      "Á","A",
      "Ä","A",
      "Å","A",
      "À","A",
      "Â","A",
      "Ā","A",
      "Ă","A",
      "Ǻ","A",
      "ĉ","c",
      "ć","c",
      "č","c",
      "ç","c",
      "ċ","c",
      "Ĉ","C",
      "Č","C",
      "Ć","C",
      "Ç","C",
      "ð","d",
      "ď","d",
      "Ď","D",
      "Ð","D",
      "Ɖ","D",
      "ê","e",
      "é","e",
      "ë","e",
      "è","e",
      "ę","e",
      "ė","e",
      "ě","e",
      "ē","e",
      "ĕ","e",
      "ȅ","e",
      "Ė","E",
      "Ę","E",
      "Ê","E",
      "Ë","E",
      "É","E",
      "È","E",
      "Ě","E",
      "Ē","E",
      "Ĕ","E",
      "ƒ","f",
      "ſ","f",
      "ğ","g",
      "ģ","g",
      "ǧ","g",
      "ġ","g",
      "Ğ","G",
      "Ĝ","G",
      "Ģ","G",
      "Ġ","G",
      "Ɠ","G",
      "ĥ","h",
      "Ħ","H",
      "í","i",
      "і","i",
      "ī","i",
      "ı","i",
      "ï","i",
      "î","i",
      "ì","i",
      "ĭ","i",
      "ĩ","i",
      "ǐ","i",
      "į","i",
      "Í","I",
      "İ","I",
      "Î","I",
      "Ì","I",
      "Ï","I",
      "І","I",
      "Ĩ","I",
      "Ī","I",
      "ј","j",
      "ĵ","j",
      "Ј","J",
      "Ĵ","J",
      "ķ","k",
      "Ķ","K",
      "ĸ","K",
      "ł","l",
      "ŀ","l",
      "ľ","l",
      "ļ","l",
      "ĺ","l",
      "Ļ","L",
      "Ľ","L",
      "Ŀ","L",
      "Ĺ","L",
      "Ł","L",
      "ñ","n",
      "ņ","n",
      "ń","n",
      "ň","n",
      "ŋ","n",
      "ǹ","n",
      "Ň","N",
      "Ń","N",
      "Ñ","N",
      "Ŋ","N",
      "Ņ","N",
      "ô","o",
      "ö","o",
      "ò","o",
      "õ","o",
      "ó","o",
      "ő","o",
      "ơ","o",
      "ǒ","o",
      "ŏ","o",
      "ǿ","o",
      "ȍ","o",
      "ō","o",
      "ȯ","o",
      "ǫ","o",
      "Ó","O",
      "Ő","O",
      "Ô","O",
      "Ö","O",
      "Ò","O",
      "Õ","O",
      "Ŏ","O",
      "Ō","O",
      "Ơ","O",
      "Ƿ","P",
      "ƽ","q",
      "Ƽ","Q",
      "ř","r",
      "ŕ","r",
      "ŗ","r",
      "Ř","R",
      "Ʀ","R",
      "Ȓ","R",
      "Ŗ","R",
      "Ŕ","R",
      "š","s",
      "ś","s",
      "ş","s",
      "ŝ","s",
      "ș","s",
      "Ş","S",
      "Š","S",
      "Ś","S",
      "Ș","S",
      "Ŝ","S",
      "ť","t",
      "ţ","t",
      "ŧ","t",
      "ț","t",
      "Ť","T",
      "Ŧ","T",
      "Ţ","T",
      "Ț","T",
      "ũ","u",
      "ú","u",
      "ü","u",
      "ư","u",
      "û","u",
      "ů","u",
      "ù","u",
      "ű","u",
      "ū","u",
      "µ","u",
      "ǔ","u",
      "ŭ","u",
      "ȕ","u",
      "Ū","U",
      "Ű","U",
      "Ù","U",
      "Ú","U",
      "Ü","U",
      "Û","U",
      "Ũ","U",
      "Ư","U",
      "Ů","U",
      "Ǖ","U",
      "Ʊ","U",
      "ŵ","w",
      "Ŵ","W",
      "ÿ","y",
      "Ŷ","Y",
      "Ÿ","Y",
      "ý","y",
      "ȝ","y",
      "Ȝ","Y",
      "Ý","Y",
      "ž","z",
      "ź","z",
      "ż","z",
      "Ź","Z",
      "Ž","Z",
      "Ż","Z"
   };

   private static final HashMap<Character,String> CHARACTER_MAPPINGS = new HashMap<Character,String>();
   static {
      for (int i = 0; i < CHARACTER_REPLACEMENTS.length; i+=2) {
         CHARACTER_MAPPINGS.put(CHARACTER_REPLACEMENTS[i].charAt(0), CHARACTER_REPLACEMENTS[i+1]);
      }
   }

   public static boolean isAscii(String in) {
      for (int i = 0; i < in.length(); i++) {
         if (in.charAt(i) > 127) {
            return false;
         }
      }
      return true;
   }

   public static String romanize(String s) {
      if (s == null) {
         return "";
      }
      if (isAscii(s)) {
         return s;
      }

      StringBuilder buf = new StringBuilder();
      for (int i = 0; i < s.length(); i++) {
         char c = s.charAt(i);
         String replacement;
         if ((int)c > 127 && (replacement = CHARACTER_MAPPINGS.get(c)) != null) {
            buf.append(replacement);
         }
         else {
            buf.append(c);
         }
      }
      return buf.toString();
   }

   private static final String[][] ABBREVS = {
      {"Alabama", "AL"},
      {"Alaska", "AK"},
      {"Arizona", "AZ"},
      {"Arkansas", "AR"},
      {"California", "CA"},
      {"Colorado", "CO"},
      {"Connecticut", "CT"},
      {"Delaware", "DE"},
      {"District of Columbia", "DC", "D.C."},
      {"Florida", "FL"},
      {"Georgia", "GA"},
      {"Hawaii", "HI"},
      {"Idaho", "ID"},
      {"Illinois", "IL"},
      {"Indiana", "IN"},
      {"Iowa", "IA"},
      {"Kansas", "KS"},
      {"Kentucky", "KY"},
      {"Louisiana", "LA"},
      {"Maine", "ME"},
      {"Maryland", "MD"},
      {"Massachusetts", "MA"},
      {"Michigan", "MI"},
      {"Minnesota", "MN"},
      {"Mississippi", "MS"},
      {"Missouri", "MO"},
      {"Montana", "MT"},
      {"Nebraska", "NE"},
      {"Nevada", "NV"},
      {"New Hampshire", "NH", "N.H."},
      {"New Jersey", "NJ", "N.J."},
      {"New Mexico", "NM", "N.M."},
      {"New York", "NY", "N.Y."},
      {"North Carolina", "NC", "N.C."},
      {"North Dakota", "ND", "N.D."},
      {"Ohio", "OH"},
      {"Oklahoma", "OK"},
      {"Oregon", "OR"},
      {"Pennsylvania", "PA"},
      {"Rhode Island", "RI", "R.I."},
      {"South Carolina", "SC", "S.C."},
      {"South Dakota", "SD", "S.D."},
      {"Tennessee", "TN"},
      {"Texas", "TX"},
      {"Utah", "UT"},
      {"Vermont", "VT"},
      {"Virginia", "VA"},
      {"Washington", "WA"},
      {"West Virginia", "WV", "WVa", "W.V.", "W.Va."},
      {"Wisconsin", "WI"},
      {"Wyoming", "WY"},
      {"United States", "US", "U.S.", "U. S.", "USA", "U.S.A."},
      {"Township", "Twp"},
      {"County", "Co"},
           {""}
   };

   static class PatternReplacement {
      Pattern pattern;
      String replacement;
      PatternReplacement(Pattern pattern, String replacement) {
         this.pattern = pattern;
         this.replacement = replacement;
      }
   }
   private static final List<PatternReplacement> ABBREV_REPLACEMENTS = new ArrayList<PatternReplacement>();

   static {
      for (String[] abbrev : ABBREVS) {
         String replacement = abbrev[0];
         for (int j = 1; j < abbrev.length; j++) {
            String p = "\\b"+abbrev[j].replace(".", "\\.");
            if (!p.endsWith(".")) {
               p += "\\b";
            }
            Pattern pattern = Pattern.compile(p);
            ABBREV_REPLACEMENTS.add(new PatternReplacement(pattern, replacement));
         }
      }
   }

   private static final String[] CUT_WORDS = {
           "accessed",
           "http://search.ancestry.com/",
           "http://www.ancestry.com/search"
   };

   private static final String[] STOP_WORDS = {
           "church of jesus christ of latter day saints", "familysearch", "ancestry com",
           "a", "an", "and", "are", "as", "at", "be", "by", "et al", "for", "from", "has", "he",
           "in", "is", "it", "its", "of", "on", "or", "that", "the", "to", "was", "were", "will", "with",
           "comp", "compiler", "ed", "editor", "editor in charge", "editor in chief",
           "com", "http", "https", "org", "url", "web", "website", "www",
           "county", "township",
           "available", "ca", "database", "digital", "i0", "images", "inc", "online"   // ca=circa
   };
   private static final Pattern STOP_WORDS_PATTERN;

   static {
      StringBuffer buf = new StringBuffer();
      for (String word : STOP_WORDS) {
         if (buf.length() > 0) {
            buf.append("|");
         }
         buf.append(word);
      }
      STOP_WORDS_PATTERN = Pattern.compile("\\b("+buf.toString()+")\\b");
   }

   public static String cleanGedcomSource(String s) {
      // convert Abbrevs
      for (PatternReplacement pr : ABBREV_REPLACEMENTS) {
         s = pr.pattern.matcher(s).replaceAll(pr.replacement);
      }

      // cut after cut-words
      for (String cut : CUT_WORDS) {
         int pos = s.lastIndexOf(cut);
         if (pos > 0) {
            s = s.substring(0, pos);
         }
      }

      // romanize
      s = romanize(s);
      // remove all single-letters except a A I
      s = s.replaceAll("\\b[b-zB-HJ-Z]\\b", "");
      // lowercase
      s = s.toLowerCase();
      // remove 's convert all non-alpha-numeric to space; convert mutliple spaces to a single space
      s = s.replaceAll("'", "").replaceAll("[^a-z0-9]", " ").replaceAll("\\s+", " ");
      // handle find a grave
      s = s.replaceAll("\\bfind a grave\\b", "findagrave");
      // remove stopwords
      s = STOP_WORDS_PATTERN.matcher(s).replaceAll("");
      // remove all spaces
      s = s.replaceAll("\\s+", "");

      return s;
   }


}
