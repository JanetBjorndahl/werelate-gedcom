package org.werelate.util;

import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.MalformedURLException;
import java.net.URLDecoder;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.*;
import java.io.Reader;
import java.io.IOException;
import java.io.PrintWriter;

import org.apache.log4j.Logger;
import org.apache.commons.codec.language.DoubleMetaphone;
import com.ibm.icu.text.Normalizer;
import org.werelate.gedcom.Name;

/**
 * Created by Dallan Quass
 * Date: Oct 22, 2005
 */
public class Utils {
   private static Logger logger = Logger.getLogger(Utils.class);

   /** max wiki title length */
   public static final int MAX_TITLE_LEN = 150;

   /** XML Namespace URI */
   public static final String XML_URI = "http://www.w3.org/XML/1998/namespace";

   /** Directory for the wiki's */
   public static final String WIKI_DIR = "/wiki/";

   /** Suffix for wiki page titles */
   public static final String WIKI_TITLE_SUFFIX = " - Genealogy";

   private static final String [][] XML_CHARS = {
      {"&", "&amp;"},
      {"<", "&lt;"},
      {">", "&gt;"},
      {"\"", "&quot;"},
      {"'", "&apos;"},
   };
   private static final Pattern CANONICALIZE_PATTERN = Pattern.compile("^(.{3,5}://)?(www\\d*\\.)?" + "(.*?)" +
           "(/((index|default|(main([_-]?(page|frame))?))([_-]?e(n(g(lish)?)?)?)?(\\.(aspx?|[jps]?html?|jsp|cfm|tcl|cgi|php[345]?))?)?)?" + "(#.*)?$");
//   public static final Pattern pCOUNTY_WORDS = Pattern.compile(
//         "\\s*\\b(County|Subdivision|Borough|Parish|Census|and|Borough|District|Regional|Municipality|Township|Metropolitan|Area|Territory|United|Counties|Judicial|Division)$",
//         Pattern.CASE_INSENSITIVE
//   );

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
         char c = in.charAt(i);
         if ((int)c >= 32 && (int)c < 127) {
            buf.append(c);
         }
         else if (c == 'ä') {
            buf.append("ae");
         }
         else if (c == 'ö') {
            buf.append("oe");
         }
         else if (c == 'ü') {
            buf.append("ue");
         }
         else {
            // decompose the character
            srcChar[0] = c;
            Normalizer.decompose(srcChar, destChars, false, 0);
            c = destChars[0];

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

   public static final int PERSON_NAMESPACE = 108;
   public static final int FAMILY_NAMESPACE = 110;
   public static final int MYSOURCE_NAMESPACE = 112;
   public static final int SOURCE_NAMESPACE = 104;

   public static String removeNoise(String s, boolean removeSpaces) {
      // keep in sync with PlaceSearcher.php
      return s.replaceAll("(\\s|-|\\.|')+", removeSpaces ? "" : " ");
   }

   public static String removeQuotesEtc(String s) {
      // keep in sync with PlaceSearcher.php
      return s.replaceAll("[\\-#%&\"()\\[\\]{}~:^\\\\+;=?|*]+", " ");
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
   public static String encodeXML(String text) {
      for (int i=0; i < XML_CHARS.length; i++) {
         text = text.replace(XML_CHARS[i][0], XML_CHARS[i][1]);
      }
      return text;
   }

   public static String unencodeXML(String text) {
      for (int i=XML_CHARS.length-1; i >= 0; i--) {
         text = text.replace(XML_CHARS[i][1], XML_CHARS[i][0]);
      }
      return text;
   }

   /**
    * One entry for each US state.  The first element of each entry is the full state name, the second element is the
    * two-letter abbreviation, the following elements (if any) are alternate abbreviations.
    *
    * Note that if you plan to create regular expressions out of these strings you'll need to escape the .'s in the abbreviations.
    */
   public static final String[][] US_STATES_WITH_ABBREVS = {
      {"Alabama", "AL", "Ala."},
      {"Alaska", "AK"},
      {"Arizona", "AZ", "Ariz."},
      {"Arkansas", "AR", "Ark."},
      {"California", "CA", "Cal.", "Calif."},
      {"Colorado", "CO", "Colo."},
      {"Connecticut", "CT", "Conn."},
      {"Delaware", "DE", "Del."},
      {"District of Columbia", "DC", "D.C."},
      {"Florida", "FL", "Fla."},
      {"Georgia", "GA"},
      {"Hawaii", "HI"},
      {"Idaho", "ID"},
      {"Illinois", "IL", "Ill."},
      {"Indiana", "IN", "Ind."},
      {"Iowa", "IA"},
      {"Kansas", "KS", "Kan."},
      {"Kentucky", "KY", "Ken."},
      {"Louisiana", "LA"},
      {"Maine", "ME"},
      {"Maryland", "MD", "Mary."},
      {"Massachusetts", "MA", "Mass."},
      {"Michigan", "MI", "Mich."},
      {"Minnesota", "MN", "Minn."},
      {"Mississippi", "MS", "Miss."},
      {"Missouri", "MO"},
      {"Montana", "MT", "Mont."},
      {"Nebraska", "NE", "Neb."},
      {"Nevada", "NV", "Nev."},
      {"New Hampshire", "NH", "N.H."},
      {"New Jersey", "NJ", "N.J."},
      {"New Mexico", "NM", "N.M."},
      {"New York", "NY", "N.Y."},
      {"North Carolina", "NC", "N.C."},
      {"North Dakota", "ND", "N.D."},
      {"Ohio", "OH"},
      {"Oklahoma", "OK", "Okla."},
      {"Oregon", "OR", "Ore."},
      {"Pennsylvania", "PA"},
      {"Rhode Island", "RI", "R.I."},
      {"South Carolina", "SC", "S.C."},
      {"South Dakota", "SD", "S.D."},
      {"Tennessee", "TN", "Tenn."},
      {"Texas", "TX", "Tex."},
      {"Utah", "UT"},
      {"Vermont", "VT"},
      {"Virginia", "VA"},
      {"Washington", "WA", "Wash."},
      {"West Virginia", "WV", "W.V.", "W.Va."},
      {"Wisconsin", "WI", "Wisc.", "Wis."},
      {"Wyoming", "WY", "Wyo."}
   };
   /**
    * Map US state abbreviations to the full state name.
    */
   public static Map<String,String> US_STATE_ABBREVS = new HashMap<String,String>();
   static {
      for (int i = 0; i < US_STATES_WITH_ABBREVS.length; i++) {
         for (int j = 1; j < US_STATES_WITH_ABBREVS[i].length; j++) {
            US_STATE_ABBREVS.put(US_STATES_WITH_ABBREVS[i][j], US_STATES_WITH_ABBREVS[i][0]);
         }
      }
   }

   public static String readReader(Reader reader) throws IOException {
      char[] chars = new char[256];
      StringBuffer buf = new StringBuffer();
      int len = reader.read(chars);
      while (len != -1) {
         buf.append(chars, 0, len);
         len = reader.read(chars);
      }
      return buf.toString();
   }

   public static String prepareWikiTitle(String title) {
      return prepareWikiTitle(title, MAX_TITLE_LEN);
   }

   /**
    * Convert a string into a form that can be used for a wiki title
    */
   public static String prepareWikiTitle(String title, int maxTitleLen) {
      try {
         try {
            title = URLDecoder.decode(title, "UTF-8");
         }
         catch (IllegalArgumentException e) {
            // ignore
         }
         title = title.replace("http://", "").replace("https://", "").replaceAll("</?(i|b|u|strong|em)>", "").
                 replace('<','(').replace('[','(').replace('{','(').replace('>',')').replace(']',')').replace('}', ')').
                       replaceAll("[#?+_|=&%]", " ").replaceAll("\\s+", " ").replaceAll("//+", "/").trim();
         while (title.length() > 0 && (title.charAt(0) == '.' || title.charAt(0) == '/')) {
            title = title.substring(1);
         }
         if (title.length() > maxTitleLen) {
            title = title.substring(0, maxTitleLen);
         }
         StringBuffer dest = new StringBuffer();
         for (int i = 0; i < title.length(); i++) {
            char c = title.charAt(i);
            // omit control characters, unicode unknown character
            if ((int)c >= 32 && c != 0xFFFD &&
               !(c == 0x007F) &&
               // omit Hebrew characters (right-to-left)
               !(c >= 0x0590 && c <= 0x05FF) && !(c >= 0xFB00 && c <= 0xFB4F) &&
               // omit Arabic characters (right-to-left)
               !(c >= 0x0600 && c <= 0x06FF) && !(c >= 0x0750 && c <= 0x077F) && !(c >= 0xFB50 && c <= 0xFC3F) && !(c >= 0xFE70 && c <= 0xFEFF)
            ) {
               dest.append(c);
            }
         }
         title = dest.toString();
         return uppercaseFirstLetter(title);
      } catch (UnsupportedEncodingException e) {
         throw new RuntimeException("Unsupported encoding: UTF-8");
      }
   }

   /**
    * Uppercase the first letter (only) of the specified string
    * @param in
    * @return
    */
   public static String uppercaseFirstLetter(String in) {
      if (in.length() > 0 && Character.isLowerCase(in.charAt(0))) {
         return in.substring(0,1).toUpperCase() + in.substring(1);
      }
      return in;
   }

   /**
    * Uppercase the first letter and any letter after space ' . -
    * @param name
    * @return
    */
   public static String toMixedCase(String name) {
      StringBuffer buf = new StringBuffer();
      for (int i = 0; i < name.length(); i++) {
         if (i == 0 || name.charAt(i-1) == ' ' || name.charAt(i-1) == '\'' || name.charAt(i-1) == '.' || name.charAt(i-1) == '-') {
            buf.append(name.substring(i, i+1).toUpperCase());
         }
         else {
            buf.append(name.substring(i, i+1).toLowerCase());
         }
      }
      return buf.toString();
   }

   /**
    * Translate \ to \\ and $ to \$, in preparation for using the specified string in a regexp replacement
    * @param text
    * @return
    */
   public static String protectDollarSlash(String text) {
      return text.replaceAll("\\\\", "\\\\\\\\").replaceAll("\\$", "\\\\\\$");
   }

    /**
     * Protects special regular expression metacharacters so that
     * string literals may be used in regular expressions.
     * @param text
     * @return fixed text
     */
   public static String protectRegular (String text) {
        text = text.replace("\\", "\\\\");
        text = text.replace("|", "\\|");
        text = text.replace("{", "\\{");
        text = text.replace("}", "\\}");
        text = text.replace("[", "\\[");
        text = text.replace("]", "\\]");
        text = text.replace("(", "\\(");
        text = text.replace(")", "\\)");
        text = text.replace(".", "\\.");
        text = text.replace("?", "\\?");
        text = text.replace("*", "\\*");
        text = text.replace("+", "\\+");
        text = text.replace("^", "\\^");
        text = text.replace("$", "\\$");
        return text;
   }

   // Returns the . components of a host string reversed, with a trailing . at the end
   public static String reverseHost(String host) {
      StringBuffer buf = new StringBuffer();
      int startPos = 0;
      int dotPos = host.indexOf('.', startPos);
      while (dotPos >= 0) {
         buf.insert(0, host.substring(startPos, dotPos+1));
         startPos = dotPos+1;
         dotPos = host.indexOf('.', startPos);
      }
      buf.insert(0, host.substring(startPos)+".");
      return buf.toString();
   }

   /**
    * Given a URL, return the Site value to index.
    * This is reverseHost(host)
    * @param urlString
    * @return
    */
   public static String constructSiteFromUrl(String urlString) {
      try {
         // default to http protocol if none given
         if (urlString.indexOf("://") < 0) {
            urlString = "http://" + urlString;
         }
         URL url = new URL(urlString);
         String host = url.getHost().toLowerCase();
         StringBuffer buf = new StringBuffer();
         buf.append(reverseHost(host));
         return buf.toString();
      }
      catch (MalformedURLException e) {
         int pos = urlString.indexOf("://");
         if (pos >= 0) {
            urlString = urlString.substring(pos+3);
         }
         pos = urlString.indexOf('/');
         if (pos >= 0) {
            urlString = urlString.substring(0, pos);
         }
         pos = urlString.indexOf('?');
         if (pos >= 0) {
            urlString = urlString.substring(0, pos);
         }
         return reverseHost(urlString.toLowerCase());
      }
   }

   public static String canonHost(String url) throws MalformedURLException {
       url = canonicalizeUrl(url);
       if (!url.startsWith("http://") && !url.startsWith("https://"))
       {
         url = "http://" + url;
       }
       url = (new URL(url)).getHost();
       return url;
   }

    public static void sleep(int miliseconds) {
        try
        {
            Thread.sleep(miliseconds);
        } catch (InterruptedException e)
        {
            logger.warn(e);
        }
    }

   private static final String SPACES = "                ";

   public static String tabsToSpaces(String s, int tabStops) {
      if (tabStops > SPACES.length()) {
         throw new IllegalArgumentException("tabsToSpaces: tabStops parm too large: " + tabStops);
      }
      StringBuilder buf = new StringBuilder();
      for (int i = 0; i < s.length(); i++) {
         char c = s.charAt(i);
         if (c == '\t') {
            buf.append(SPACES.substring(0, tabStops - (buf.length() % tabStops)));
         }
         else {
            buf.append(c);
         }
      }
      return buf.toString();
   }

   // Copied from a web forum.
   public static String reverse(String s) {
       char[] carray=s.toCharArray();
       char[] carray2=new char[carray.length];
       for(int i=carray.length-1, j=0; i>=0; i--, j++) {
           carray2[j]=carray[i];
       }
       return new String(carray2);
   }

   public static void println(PrintWriter p, String text)
   {
      if (p != null)
      {
         p.println(text);
         p.flush();
      }
   }

   public static void println(StringBuffer p, String text)
   {
      if (p != null)
      {
         p.append(text).append('\n');
      }
   }

   public static String printLink(String url)
   {
      return "<left><a href=\"" + url + "\">" + url + "</a></left><br>";
   }

   /**
    * Given a site returned by constructSiteFromUrl, return the host value to display in search results
    * @param site
    * @return
    */
   public static String constructHostFromSite(String site) {
      StringBuilder b = new StringBuilder();
      int oldPos = site.length() - 1;
      int pos = site.lastIndexOf('.', oldPos - 1);
      while (pos >= 0) {
         b.append(site.substring(pos+1,oldPos));
         b.append('.');
         oldPos = pos;
         pos = site.lastIndexOf('.', oldPos - 1);
      }
      b.append(site.substring(0, oldPos));

      return b.toString();
   }

   public static String canonicalizeUrl(String url) {
      Matcher m = CANONICALIZE_PATTERN.matcher(url.toLowerCase().trim());
      return m.replaceFirst("$3");
   }

   public static String setVal(String orig, String name) {
      if (name != null)
      {
         name = name.trim();
         if (!Utils.isEmpty(name))
         {
            return name;
         } else
         {
            return orig;
         }
      } else
      {
         return orig;
      }
   }

   private static Pattern pAlphaNumRegExp =  Pattern.compile("\\d+|[^0-9\\s'~!@#$%^&*()_+\\-={}|:'<>?;,/\"\\[\\]\\.\\\\]+");

   public static final boolean isAllNumeric(final String s) {
     if (s == null) return true;
     final char[] numbers = s.toCharArray();
     for (int x = 0; x < numbers.length; x++) {
       final char c = numbers[x];
       if ((c >= '0') && (c <= '9')) continue;
       return false; // invalid
     }
     return true; // valid
   }

   public static final boolean isPartNumeric(final String s) {
     if (s == null) return false;
     final char[] numbers = s.toCharArray();
     for (int x = 0; x < numbers.length; x++) {
       final char c = numbers[x];
       if ((c >= '0') && (c <= '9')) return true;
     }
     return false;
   }

   private static int parseInt(String field)
   {
      if (isAllNumeric(field))
      {
         try {
            return Integer.parseInt(field);
         }
         catch (NumberFormatException e) {
            // ignore
         }
      }
      return 0;
   }

   public static final Pattern pEarlyYear = Pattern.compile("(\\d{3,4})\\s*(B\\.?\\s*C)?", Pattern.CASE_INSENSITIVE);

   public static boolean dateIsBefore700AD(String date)
   {
      return dateIsBeforeXAD(date, 700);
   }

   public static boolean dateIsBefore1600AD(String date)
   {
      return dateIsBeforeXAD(date, 1600);
   }

   public static boolean dateIsBeforeXAD(String date, int x)
   {
      if (Utils.isEmpty(date)) return false;
      Matcher m = pEarlyYear.matcher(date);
      boolean found = false;
      while (m.find()) {
         if (m.group(2) != null || Integer.parseInt(m.group(1)) < x) {
            found = true;
         }
         else {
            return false;
         }
      }
      return found;
   }

   public static String replaceHTMLFormatting(String bodyString)
   {
      return bodyString.replaceAll("«/?b»", "'''").replaceAll("«tab»", "    ");
   }

   public static String replaceHTMLFormatting(StringBuffer bodyText) {
      return replaceHTMLFormatting(bodyText.toString());
   }

   /**
    * For every set of entries
    * a->b and b->c, entries become:
    * a->c and b->c
    */
   public static <T> void fixDoupleMappings   (Map <T, T> map)
   {
      for (Map.Entry e : map.entrySet())
      {
         T key = (T) e.getKey();
         T value = (T) e.getValue();
         while (map.containsKey(value))
         {
            value = map.get(value);
            map.put(key, value);
         }
      }
   }

   public static final Pattern pRedirect = Pattern.compile("#REDIRECT:?\\s*\\[\\s*\\[\\s*(.*?)\\s*\\]\\s*\\]",
            Pattern.CASE_INSENSITIVE);

   public static void prependParagraphBreak(StringBuffer bodyText) {
      if(!Utils.isEmpty(bodyText.toString()))
      {
         bodyText.append("\n\n");;
      }
   }

   public static final String TYPE_WORDS =
        "Arrondissement|Autonomous|Avtonomnyy|Avtonomaya|Avtonomnaya|" +
        "Borough|" +
        "Canton|Commune|Comunidad|County|Council|" +
        "Department|District|Distrito|Division|" +
        "Federal|Governorate|Krai|Kray|Kraj|kommun|landskommun|London borough|" +
        "Metropolitan|Municipal|Municipality|" +
        "of|Oblast|Oblast'|Okrug|" +
        "Partido|Parish|Prefecture|Province|" +
        "Raion|Rayon|Regional|Republic|Rural|" +  // not royal
        "Sheng|State|Subprefecture|stad|" +
        "Urban|Voivodship";

   public static final Pattern pTYPE_SUFFIX_WORDS = Pattern.compile("\\s*\\b(" + TYPE_WORDS + ")\\s*$", Pattern.CASE_INSENSITIVE);

   public static final Pattern pTYPE_PREFIX_WORDS = Pattern.compile("^\\s*(" + TYPE_WORDS + ")\\b", Pattern.CASE_INSENSITIVE);

   public static final Pattern pPARENTHESIZED_TYPE = Pattern.compile(
         "\\s*\\((Arrondissement|autonomous county|autonomous community|autonomous province|autonomous republic|" +
           "Bezirk|Bezirke|Bistum|borough|" +
           "Canton|Comuna|commune|concelho|county borough|county|" + // not country
           "department|Departmento|Departamento|département|District|District Council|distrito|district municipality|" +
           "former county|former province|former parish|" +
           "general region|Gerichtsbezirk|Gerichtsbez\\.|governorate|grafschaft|guberniya|historical region|independent city|island|" + // not härad
           "Kanton|Kerulet|Kerület|kreis|krai|kraj|kray|kommun|landskommun|" +
           "municipality|Municipio|mun\\.|national district|oblast|okres|okrug|" +
           "parish|partido|prefecture|principal area|province|province or territory|Provincia|" +
           "region|región|raion|rayon|région|regione|regional county municipality|Regierungsbezirk|rural municipality|" +
           "sahar|savinao|shire|stad|state|territory|union territory|unitary authority|urban district|uyezd|voivodship)\\)\\s*$",
         Pattern.CASE_INSENSITIVE
   );

//   public static final Pattern pPAR_TYPE = Pattern.compile(
//           "\\s*\\(.+\\)\\s*$",
//           Pattern.CASE_INSENSITIVE
//     );

   public static final Pattern pCITY_TYPE = Pattern.compile(
         "\\s*\\((cdp|city|city or town|community|" +
           "inhabited place|kab\\.|kota\\.|" +
           "town|village|town or village)\\)\\s*$",
         Pattern.CASE_INSENSITIVE
   );

   private static String applyTypePattern(String preferredName, Pattern p) {
      boolean cont = true;
      while (cont)
      {
         Matcher m = p.matcher(preferredName);
         StringBuffer buf = new StringBuffer();
         if (m.find())
         {
            m.appendReplacement(buf, "");
            m.appendTail(buf);
            preferredName = buf.toString().trim();
         } else
         {
            cont = false;
         }
      }
      return preferredName;
   }

   public static String removeTypeWords(String preferredName, boolean removeCityTypes) {
      Matcher m;
      preferredName = preferredName.trim();

      m = pPARENTHESIZED_TYPE.matcher(preferredName);
      preferredName = m.replaceFirst("").trim();
      if (removeCityTypes) {
         m = pCITY_TYPE.matcher(preferredName);
         preferredName = m.replaceFirst("").trim();
      }
      preferredName = applyTypePattern(preferredName, pTYPE_PREFIX_WORDS);
      preferredName = applyTypePattern(preferredName, pTYPE_SUFFIX_WORDS);
      return preferredName;
   }

   public static boolean nullCheckEquals(String a, String b)
   {
      if (a == null)
      {
         if (b == null)
         {
            return true;
         } else
         {
            return false;
         }
      } else
      {
         if (b == null)
         {
            return false;
         } else
         {
            return a.equals(b);
         }
      }
   }

   private static final String[] UPPERCASE_WORDS_ARRAY = {"I","II","III","IV","V","VI","VII","VIII","IX","X"};
   private static final Set<String> UPPERCASE_WORDS = new HashSet<String>();
   static {
      for (String word : UPPERCASE_WORDS_ARRAY) UPPERCASE_WORDS.add(word);
   }
   private static final String[] LOWERCASE_WORDS_ARRAY = {
      "a","an","and","at","but","by","for","from","in","into",
      "nor","of","on","or","over","the","to","upon","vs","with",
      "against", "as", "before", "between", "during", "under", "versus", "within", "through", "up",
      // french
      "à", "apres", "après", "avec", "contre", "dans", "dès", "devant", "dévant", "durant", "de", "avant", "des",
      "du", "et", "es", "jusque", "le", "les", "par", "passe", "passé", "pendant","pour", "pres", "près", "la",
      "sans", "suivant", "sur", "vers", "un", "une",
      // spanish
      "con", "depuis", "durante", "ante", "antes", "contra", "bajo",
      "en", "entre", "mediante", "para", "pero", "por", "sobre", "el", "o", "y",
      // dutch
      "aan", "als", "bij", "eer", "min", "na", "naar", "om", "op", "rond", "te", "ter", "tot", "uit", "voor",
      // german
      "auf", "gegenuber", "gegenüber", "gemäss", "gemass", "hinter", "neben",
      "über", "uber", "unter", "vor", "zwischen", "die", "das", "ein", "der",
      "ans", "aufs", "beim", "für", "fürs", "im", "ins", "vom", "zum", "am",
      // website extensions
      "com", "net", "org",
   };
   public static final Set<String> LOWERCASE_WORDS = new HashSet<String>();
   static {
      for (String word : LOWERCASE_WORDS_ARRAY) LOWERCASE_WORDS.add(word);
   }
   private static final String[] NAME_WORDS_ARRAY = {
           "a", "à", "aan", "af", "auf",
           "bei", "ben", "bij",
           "contra",
           "da", "das", "de", "dei", "del", "della", "dem", "den", "der", "des", "di", "die", "do", "don", "du",
           "ein", "el", "en",
           "het",
           "im", "in",
           "la", "le", "les", "los",
           "met",
           "o", "of", "op",
           "'s", "s'", "sur",
           "'t", "te", "ten", "ter", "tho", "thoe", "to", "toe", "tot",
           "uit",
           "van", "ver", "von", "voor", "vor",
           "y",
           "z", "zum", "zur"
   };
   private static final Set<String> NAME_WORDS = new HashSet<String>();
   static {
      for (String word : NAME_WORDS_ARRAY) NAME_WORDS.add(word);
   }
   public static final Pattern WORD_DELIM_REGEX = Pattern.compile("([ `~!@#$%&_=:;<>,./{}()?+*|\"\\-\\[\\]\\\\]+|[^ `~!@#$%&_=:;<>,./{}()?+*|\"\\-\\[\\]\\\\]+)");

   // keep in sync with wiki/StructuredData.captitalizeTitleCase and wikidata/Util.capitalizeTitleCase
   public static String capitalizeTitleCase(String s, boolean isName, boolean mustCap) {
      StringBuilder result = new StringBuilder();
      if (s != null) {
         Matcher m = WORD_DELIM_REGEX.matcher(s);
         while (m.find()) {
            String word = m.group(0);
            String ucWord = word.toUpperCase();
            String lcWord = word.toLowerCase();
            if (isName && word.length() > 1 && word.equals(ucWord)) { // turn all-uppercase names into all-lowercase
               // can't split on apostrophes due to polynesian names
               if (word.length() > 3 && (word.startsWith("MC") || word.startsWith("O'"))) {
                  word = word.substring(0,1)+lcWord.substring(1,2)+word.substring(2,3)+lcWord.substring(3);
               }
               else {
                  word = lcWord;
               }
            }
            if (UPPERCASE_WORDS.contains(ucWord) ||    // upper -> upper
                word.equals(ucWord)) { // acronym or initial
               result.append(ucWord);
            }
            else if (!mustCap && NAME_WORDS.contains(lcWord)) {  // if word is a name-word, keep as-is
               result.append(word);
            }
            else if (!isName && !mustCap && LOWERCASE_WORDS.contains(lcWord)) { // upper/lower -> lower
               result.append(lcWord);
            }
            else if (word.equals(lcWord)) { // lower -> mixed
               result.append(word.substring(0,1).toUpperCase());
               result.append(word.substring(1).toLowerCase());
            }
            else { // mixed -> mixed
               result.append(word);
            }
            word = word.trim();
            mustCap = !isName && (word.equals(":") || word.equals("?") || word.equals("!"));
         }
      }
      return result.toString();
   }

   public static String capitalizeName(String name, boolean mustCap) {
      // remove .'s from name
      return capitalizeTitleCase(name, true, mustCap).replaceAll("[. ]+", " ").trim();
   }
}
