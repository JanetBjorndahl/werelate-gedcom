package org.lm.gedml;

import java.util.*;
import java.io.*;
import java.net.URL;

import org.xml.sax.*;
import org.xml.sax.helpers.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.werelate.gedcom.Gedcom;
import org.werelate.util.Utils;

/**
 * org.lm.gedml.GedcomParser
 * <p/>
 * This class is designed to look like a SAX2-compliant XML parser; however,
 * it takes GEDCOM as its input rather than XML.
 * The events sent to the ContentHandler reflect the GEDCOM input "as is";
 * there is no validation or conversion of tags.
 *
 * @author mhkay@iclway.co.uk
 * @version 22 March 2006 - revised by lmonson.com to support string inlining, a few sax feature settings and additional encodings
 */
public class GedcomParser implements XMLReader, Locator {
   private static final List ACCEPTED_TRUE_SAX_FEATURES = Arrays.asList(new String[]{
         "http://xml.org/sax/features/namespace-prefixes",
         // OK to support, since non are produced from gedcom
         "http://xml.org/sax/features/external-general-entities",
         // OK to support, since non are produced from gedcom
         "http://xml.org/sax/features/external-parameter-entities",
         // OK to support, since non are produced from gedcom
         "http://xml.org/sax/features/string-interning",
   });

   private ContentHandler contentHandler;
   private ErrorHandler errorHandler;
   private AttributesImpl emptyAttList = new AttributesImpl();
   private AttributesImpl attList = new AttributesImpl();
   private EntityResolver entityResolver = null;

   private String systemId;
   private int lineNr;

   Gedcom gedcom = null;

   /**
    * Set the ContentHandler
    *
    * @param handler User-supplied content handler
    */
   public void setContentHandler(ContentHandler handler) {
      gedcom = (Gedcom) handler;
      contentHandler = handler;
   }

   /**
    * Get the ContentHandler
    */

   public ContentHandler getContentHandler() {
      return contentHandler;
   }

   /**
    * Set the entityResolver.
    * This call has no effect, because entities are not used in GEDCOM files.
    */

   public void setEntityResolver(EntityResolver er) {
      entityResolver = er;

   }

   /**
    * Get the entityResolver
    */

   public EntityResolver getEntityResolver() {
      return entityResolver;
   }

   /**
    * Set the DTDHandler
    * This call has no effect, because DTDs are not used in GEDCOM files.
    */

   public void setDTDHandler(DTDHandler dh) {
   }

   /**
    * Get the DTDHandler
    */

   public DTDHandler getDTDHandler() {
      return null;
   }

   /**
    * Set the error handler
    *
    * @param eh A user-supplied error handler
    */

   public void setErrorHandler(ErrorHandler eh) {
      errorHandler = eh;
   }

   /**
    * Get the error handler
    */

   public ErrorHandler getErrorHandler() {
      return errorHandler;
   }

   /**
    * Set the locale.
    * This call has no effect: locales are not supported.
    */

   public void setLocale(Locale locale) {
   }

   public static String getCharEncoding(InputStream is) throws IOException {
      BufferedReader in = new BufferedReader(new InputStreamReader(is));

      return getCharEncoding(in);
   }

   private static String getCharEncoding(BufferedReader in) throws IOException {
      // We will only try to read the first 100 lines of
      // the file attempting to get the char encoding.
      String line;
      String rval = null;
      boolean foundGeneratorName = false;
      String generatorName = null;
      boolean foundGeneratorVersion = false;
      String generatorVersion = null;
      for (int i = 0; i < 100 && rval == null; i++) {
         line = in.readLine();
         if (line != null) {
            String [] split = line.split("\\s+");

            if (split.length > 1 && Utils.isEmpty(split[0]))
            {
               String [] newSplit = new String [split.length -1];
               for (int j=1; j < split.length; j++)
               {
                  newSplit[j-1] = split[j];
               }
               split = newSplit;
            }
            if (!foundGeneratorName &&
                  split.length >= 3 &&
                  split[0].equals("1") &&
                  split[1].equals("SOUR"))
            {
               generatorName = split[2];
               foundGeneratorName = true;
            } else if (foundGeneratorName &&
                  !foundGeneratorVersion &&
                  split.length >= 3 &&
                  split[0].equals("2") &&
                  split[1].equals("VER"))
            {
               generatorVersion = split[2];
               foundGeneratorVersion = true;
               if (generatorName.equals("Geni.com") && !generatorVersion.equals("1.0"))
               {
                  throw new IOException("We need to check on this version of GEDCOM from Geni.com: "
                        + generatorVersion);
               }
            }
            else if (split.length >= 3 &&
                  split[0].equals("1") &&
                  (split[1].equals("CHAR") || split[1].equals("CHARACTER")))
            {
               // We need to concatenate together the rest of the splits:
               rval = getGedValue(split);
               if (rval.equals("ASCII") &&
                       foundGeneratorName && generatorName.equals("GeneWeb")) {
                    rval = "ANSI";
               }
               else if (rval.equals("ASCII"))
               {
                  line = in.readLine();
                  if (line != null)
                  {
                     split= line.split("\\s+");
                     if (split[0].equals("2") &&
                           split[1].equals("VERS"))
                     {
                        String version;
                        version = getGedValue(split);
                        if (version.trim().equals("MacOS Roman"))
                        {
                           rval = "MACINTOSH";
                        }
                     }
                  }
               } else if (rval.equals("ATARIST_ASCII"))
               {
                  rval = "ASCII";
               }
               else if (rval.equals("UNICODE")
                     && foundGeneratorName && generatorName.equals("Geni.com"))
               {
                  // Geni.com VER 1.0 has mislabled UTF-8 as "UNICODE."
                  rval = "UTF-8";
               } else if (rval.equals("UNICODE")
                     && foundGeneratorName && generatorName.equals("GENJ"))
               {
                  rval = "UTF-8";
               }
               else if (rval.equals("ANSEL")
                     && foundGeneratorName && generatorName.equals("Geni.com"))
               {
                  // Geni.com VER 1.0 has mislabled UTF-8 as "ANSEL"
                  rval = "UTF-8";
               }
               else if (rval.equals("MACROMAN")) {
                  rval = "MACINTOSH";
               }
               else if (rval.equalsIgnoreCase("UTF-16BE")) {
                  rval = "UTF-16";
               }

               break;
            }
         }
      }

      in.close();
      return rval;
   }

   private static String getGedValue(String[] split) {
      String version;
      version = split[2];
      for (int j = 3; j < split.length; j++)
      {
         version += ' ' + split[j];
      }
      return version;
   }

   private static Logger logger = LogManager.getLogger(GedcomParser.class);

   public static BufferedReader getBufferedReader(String systemId) throws IOException, SAXException {
      InputStream in = (new URL(systemId)).openStream();
      String charEncoding = conditionalToUpper(getCharEncoding(in));
      in.close();
      InputStreamReader reader = null;

      if (charEncoding == null) {
         // Let's try again with a UTF-16 reader.
         reader = new InputStreamReader((new URL(systemId)).openStream(), "UTF-16");
         BufferedReader br = new BufferedReader(reader);
         charEncoding = conditionalToUpper(getCharEncoding(br));
         br.close();
         reader.close();

         if (charEncoding != null && (charEncoding.equals("UNICODE") || charEncoding.equals("UTF-16"))) {
            reader = new InputStreamReader((new URL(systemId)).openStream(), "UTF-16");
            int cnt = 0;
            while (reader.read() != '0') {
               cnt++;
            }
            reader.close();
            reader = new InputStreamReader((new URL(systemId)).openStream(), "UTF-16");
            for (int i = 0; i < cnt; i++) {
               reader.read();
            }
            return new BufferedReader(reader);
         }
      }

      // skip over junk at the beginning of the file
      in = (new URL(systemId)).openStream();
      int cnt = 0;
      int myChar;
      while ((myChar = in.read()) != '0' && myChar != -1) {
         cnt++;
      }
      in.close();
      in = (new URL(systemId)).openStream();
      for (int i = 0; i < cnt; i++) {
         in.read();
      }

      if (charEncoding == null ||
            charEncoding.equals("ANSEL")) {
         reader = new AnselInputStreamReader(in);
      } else if (charEncoding.equals("ASCII")) {
         reader = new InputStreamReader(in, "ASCII");
      } else if (charEncoding.equals("ANSI")) {
         //reader = new InputStreamReader(in, "ISO-8859-1");
         reader = new InputStreamReader(in, "Cp1252");
         //reader = new InputStreamReader(in, "ASCII");
      } else if (charEncoding.equals("IBM WINDOWS")) {
         //reader = new InputStreamReader(in, "ISO-8859-1");
         reader = new InputStreamReader(in, "Cp1252");
		} else if (charEncoding.equals("WINDOWS-1250")) {
			reader = new InputStreamReader(in, "Cp1250");
		} else if (charEncoding.equals("WINDOWS-1251")) {
			reader = new InputStreamReader(in, "Cp1251");
		}
		/*else if (charEncoding.equals("WINDOWS-1254")) {
         reader = new InputStreamReader(in, "Cp1254");
      	} else if (charEncoding.equals("WINDOWS-874")) {
         reader = new InputStreamReader(in, "Cp874");
      	}*/
		else if (charEncoding.equals("IBMPC") || charEncoding.equals("IBM DOS")) {
         reader = new InputStreamReader(in, "Cp850");
      } else if (charEncoding.equals("UNICODE")) {
         reader = new InputStreamReader((new URL(systemId)).openStream(), "UTF-16");
         BufferedReader br = new BufferedReader(reader);
         String line = br.readLine();
         logger.warn("Reading in the file marked as UNICODE using UTF-16");
      } else if (charEncoding.equals("UTF-8")) {
         // TODO: Expand this section to others for the macintosh,
         // TODO: including MacRoman (which was the standard character
         // TODO: encoding for the classic mac systems but is now obsolete
         // TODO: MacOS X and above systems use UTF-8 and -16. See
         // TODO: http://en.wikipedia.org/wiki/Mac_OS#"Classic"_Mac_OS_technologies
         reader = new InputStreamReader(in, "UTF-8");
      } else if (charEncoding.equals("MACINTOSH") || charEncoding.equals("MACROMAN")) {
         reader = new InputStreamReader(in, "x-MacRoman");
      } else {
         throw new SAXException("Unrecognized encoding in gedcom file: " + charEncoding);
      }
      return new BufferedReader(reader);
   }

   public static String conditionalToUpper(String charEncoding) {
      if (charEncoding != null)
      {
         charEncoding = charEncoding.toUpperCase();
      }
      return charEncoding;
   }

   /**
    * Parse input from the supplied systemId
    */

   public void parse(String systemId) throws SAXException, IOException {
      //System.out.println("This parser was called!");
      this.systemId = systemId;
      try {
         parse(getBufferedReader(systemId));
      } catch (SAXParseException e) {
         System.out.println("SAX Parse Exception: Line: " + this.getLineNumber());
         System.out.println("Exception error msg: " + e.getMessage());
         e.printStackTrace();
         throw e;
      }
   }

   /**
    * Parse input from the supplied InputSource
    */

   public void parse(InputSource source) throws SAXException, IOException {

      if (contentHandler == null) contentHandler = new DefaultHandler();
      if (errorHandler == null) errorHandler = new DefaultHandler();
      systemId = source.getSystemId();

      if (source.getCharacterStream() != null) {
         parse(new BufferedReader(source.getCharacterStream()));
      } else if (source.getByteStream() != null) {
         parse(new BufferedReader(
               new AnselInputStreamReader(
                     source.getByteStream())));
      } else if (systemId != null) {
         parse(systemId);
      } else {
         throw new SAXException("No input supplied");
      }
   }

   /**
    * Parse input from a supplied BufferedReader
    */
   private void parse(BufferedReader reader) throws SAXException, IOException {

      String line, currentLine, token1, token2;
      String level;
      int thislevel;
      int prevlevel;
      String iden, tag, xref, valu, type;
      int cpos1;
      int cpos2;
      int i;

      char[] newlineCharArray = new char[1];
      newlineCharArray[0] = '\n';

      lineNr = 0;
      currentLine = "";

      Stack stack = new Stack();
      stack.push("GED");

      prevlevel = -1;

      contentHandler.setDocumentLocator(this);
      contentHandler.startDocument();
      contentHandler.startElement("", "GED", "GED", emptyAttList);

      try {
         boolean goodLine = false; // Indicates whether we have found a good line so
                                   // far in the file.
         while ((line = reader.readLine()) != null) {

            if (lineNr > 155295 && (lineNr % 1000 == 0))
            {
               System.out.println(lineNr);
            }
            StringBuffer buf = new StringBuffer();
            for (int j=0; j < line.length(); j++)
            {
               char c = line.charAt(j);
               if (c >= 32 || c == 13 || c == 10 || c == 9)
               {
                  buf.append(c);
               }
            }
            line = buf.toString();            

            // We can't be trimming lines
            // because often the spacing at the
            // end of a line is important, such as in
            // NOTE, CONT, and CONC fields
            //line = line.trim();

            lineNr++;
            currentLine = line;

            // parse the GEDCOM line into five fields: level, iden, tag, xref, valu

            if (line.length() > 0) {
               GedcomLine gl = new GedcomLine(line);

               if (!gl.wasAbleToParse())
               {
                  logger.info(gedcom.logStr("Line does not appear to be standard: "
                        + this.getLineNumber()) +
                        " appending content to the last tag started.");
                  contentHandler.characters(line.toCharArray(), 0, line.length());
                  if (lineNr > 20 && goodLine == false)
                  {
                     gedcom.setInvalid();
                     break;
                  }
               } else
               {
                  thislevel = Integer.parseInt(gl.getLevelNum());
                  if (thislevel > prevlevel && !(thislevel == prevlevel + 1)) {
                     stack.push("WERELATE_DUMMY");
                     //throw new SAXException("Level numbers must increase by 1");
                  } else if (thislevel < 0) {
                     throw new SAXException("Level number must not be negative");
                  }

                  if (gl.getTag() != null)
                  {
                     tag = gl.getTag();
                  } else
                  {
                     throw new SAXException("Tag was not found.");
                  }

                  iden = gl.getID();
                  xref = gl.getXRef();
                  valu = gl.getRemainder();

                  // perform validation on the CHAR field (character code)
                  if (tag.equals("CHAR")) {
                     String encoding = conditionalToUpper(valu.trim().intern());
                     if (!encoding.equals("ANSEL") && !encoding.equals("ASCII") && !encoding.equals("ANSI") &&
                           !encoding.equals("UNICODE") && !encoding.equals("UTF-8") &&
                           !encoding.equals("UTF-16BE") && !encoding.equals("UTF-16") &&
                           !encoding.equals("MACINTOSH") && !encoding.equals("MACROMAN") &&
                           !encoding.equals("IBMPC") && !encoding.equals("IBM DOS") &&
                           !encoding.equals("WINDOWS-1250") && !encoding.equals("WINDOWS-1251") &&
                           !encoding.equals("IBM WINDOWS"))
                        throw new SAXException("WARNING: Unknown character set: " + valu);
                  }

                  // insert any necessary closing tags
                  while (thislevel <= prevlevel) {
                     String endtag = (String) stack.pop();
                     contentHandler.endElement("", endtag, endtag);
                     prevlevel--;
                  }
                  if (!tag.equals("TRLR")) {
                     attList.clear();
                     if (!Utils.isEmpty(iden)) attList.addAttribute("", "ID", "ID", "ID", iden);
                     if (!Utils.isEmpty(xref)) attList.addAttribute("", "REF", "REF", "IDREF", xref);
                     contentHandler.startElement("", tag, tag, attList);
                     goodLine = true;
                     stack.push(tag);
                     prevlevel = thislevel;
                  }
                  if (valu != null && valu.length() > 0) {
                     contentHandler.characters(valu.toCharArray(), 0, valu.length());
                  }
               }
               /*
               cpos1 = line.indexOf(' ');
               if (cpos1 < 0) {
                  logger.info(gedcom.logStr("No space in line " + this.getLineNumber()));
               } else {

                  level = firstWord(line);
                  try {
                     thislevel = Integer.parseInt(level);

                     // check the level number

                     if (thislevel > prevlevel && !(thislevel == prevlevel + 1)) {
                        stack.push("WERELATE_DUMMY");
                        //throw new SAXException("Level numbers must increase by 1");
                     } else if (thislevel < 0) {
                        throw new SAXException("Level number must not be negative");
                     }

                     line = remainder(line);
                     token1 = firstWord(line);
                     String nonTrimmedThirdPortion = nonTrimmedRemainder(line);
                     line = remainder(line);

                     if (token1.startsWith("@")) {
                        if (token1.length() == 1 || !token1.endsWith("@"))
                           throw new SAXException("Bad xref_id");

                        iden = token1.substring(1, token1.length() - 1);
                        tag = firstWord(line).intern();
                        line = remainder(line);
                     } else {
                        iden = "";
                        tag = token1.intern();
                     }
                     ;

                     xref = "";
                     if (line.startsWith("@") && (token2 = firstWord(line)) != null &&
                           !(token2.length() == 1 || !token2.endsWith("@"))) {
                        xref = token2.substring(1, token2.length() - 1);
                        line = remainder(line);
                     }
                     ;

                     valu = line;

                     // perform validation on the CHAR field (character code)
                     if (tag.equals("CHAR")) {
                        String encoding = valu.trim().intern();
                        if (!encoding.equals("ANSEL") && !encoding.equals("ASCII") && !encoding.equals("ANSI")
                              && !encoding.equals("UNICODE") && !encoding.equals("UTF-8") &&
                              !encoding.equals("MACINTOSH") && !encoding.equals("IBMPC") &&
                              !encoding.equals("IBM WINDOWS"))
                           throw new SAXException("WARNING: Character set is " + valu +
                                 ": should be ANSEL or ASCII or ANSI or UNICODE");
                     }

                     // insert any necessary closing tags
                     while (thislevel <= prevlevel) {
                        String endtag = (String) stack.pop();
                        contentHandler.endElement("", endtag, endtag);
                        prevlevel--;
                     }

                     if (!tag.equals("TRLR")) {
                        attList.clear();
                        if (!iden.equals("")) attList.addAttribute("", "ID", "ID", "ID", iden);
                        if (!xref.equals("")) attList.addAttribute("", "REF", "REF", "IDREF", xref);
                        contentHandler.startElement("", tag, tag, attList);
                        goodLine = true;
                        stack.push(tag);
                        prevlevel = thislevel;
                     }

                     if (valu.length() > 0) {
                        if (Utils.isEmpty(iden) && Utils.isEmpty(xref)) {
                           contentHandler.characters(nonTrimmedThirdPortion.toCharArray(), 0, nonTrimmedThirdPortion.length());
                        } else {
                           contentHandler.characters(valu.toCharArray(), 0, valu.length());
                        }
                     }
                  } catch (NumberFormatException err) {

                  }
               }*/
            }
         }

         contentHandler.endElement("", "GED", "GED");
         if (gedcom.getPeople().size() == 0)
         {
            gedcom.setInvalid();
         }
         contentHandler.endDocument();
         //System.err.println("Parsing complete: " + lineNr + " lines");

      } catch (SAXException e1) {
         SAXParseException err = new SAXParseException(e1.getMessage(), this);
         errorHandler.fatalError(err);
         throw err;
         //throw e1;
      } catch (EmptyStackException e) {
         SAXParseException err = new SAXParseException(e.getMessage(), this);
         errorHandler.fatalError(err);
         throw err;
      }
      finally {
         reader.close();
      }

   }

   ;

   /**
    * Set a feature
    */

   public void setFeature(String s, boolean b) throws SAXNotRecognizedException {
      if (!b || !ACCEPTED_TRUE_SAX_FEATURES.contains(s))
         throw new SAXNotRecognizedException("Gedcom Parser does not recognize the feature '" + s + "'");
   }

   /**
    * Get a feature
    */

   public boolean getFeature(String s) throws SAXNotRecognizedException {
      if (s.equals("http://xml.org/sax/features/namespaces")) return true;
      if (s.equals("http://xml.org/sax/features/namespace-prefixes")) return false;
      throw new SAXNotRecognizedException("Gedcom Parser does not recognize any features");
   }

   /**
    * Set a property
    */

   public void setProperty(String s, Object b) throws SAXNotRecognizedException {
      throw new SAXNotRecognizedException("Gedcom Parser does not recognize any properties");
   }

   /**
    * Get a property
    */

   public Object getProperty(String s) throws SAXNotRecognizedException {
      throw new SAXNotRecognizedException("Gedcom Parser does not recognize any properties");
   }


   /**
    * Procedure to return the first word in a string
    */
   private static String firstWord(String inp) {
      int i;
      i = inp.indexOf(' ');
      if (i == 0) return firstWord(inp.trim());
      if (i < 0) return inp;
      return inp.substring(0, i).trim();
   }

   ;

   /**
    * Procedure to return the text after the first word in a string
    */

   private static String remainder(String inp) {
      int i;
      i = inp.indexOf(' ');
      if (i == 0) return remainder(inp.trim());
      if (i < 0) return new String("");
      // We cannot trim the line because a space at the
      // end could be very important.
      //return inp.substring(i + 1, inp.length()).trim();
      return inp.substring(i + 1, inp.length());
   }

   ;

   private static String nonTrimmedRemainder(String inp) {
      int i;
      i = inp.indexOf(' ');
      if (i == 0) return nonTrimmedRemainder(inp.trim());
      if (i < 0) return new String("");
      return inp.substring(i + 1, inp.length());
   }

   /**
    * Get the publicId: always null
    */

   public String getPublicId() {
      return null;
   }

   /**
    * Get the system ID
    */

   public String getSystemId() {
      return systemId;
   }

   /**
    * Get the line number
    */

   public int getLineNumber() {
      return lineNr;
   }

   /**
    * Get the column number: always -1
    */

   public int getColumnNumber() {
      return -1;
   }

};


