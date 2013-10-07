package org.werelate.gedcom;


import org.apache.commons.cli.*;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.params.HttpMethodParams;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.log4j.Logger;
import org.werelate.util.*;
import org.w3c.dom.Node;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import org.xml.sax.InputSource;
import org.xml.sax.SAXParseException;

import java.sql.*;
import java.util.*;
import java.util.Date;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.io.*;
import java.text.SimpleDateFormat;

import javax.xml.xpath.*;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.transform.TransformerException;

/**
 * Handles the interface between the Gedcom parsing
 * and transformation code and the WeRelate interfaces,
 * such as the database, place search engine, etc.
 * It is also the class that has overall
 * responsibility for uploading GEDCOM files
 */
public class Uploader {
   private static final Logger logger =
         Logger.getLogger("org.werelate.gedcom.Upload");
   // The source directory for Gedcoms
   private String gedcomDir = null;
   private String placeServer = null;
   private String matchServer = null;
   private boolean unitTesting = false;
   private boolean stubMatching = false;

   /**
    * Returns whether the Uploader class is in unit testing mode.
    * If this variable is set to be true,
    * then we use the stubbed-out version of the
    * ID reservation code. Also, if we're in
    * unit testing mode, then we do not try to
    * generate pages
    * after generating the xml file
    * @return whether we're in unit
    */
   public boolean isUnitTesting() {
      return unitTesting;
   }

   /**
    * Sets whether the Uploader is in unit testing mode.
    * If this variable is set to be true,
    * then we use the stubbed-out version of the
    * ID reservation code. Also, if we're in
    * unit testing mode, then we do not try to
    * generate pages after generating the xml file.
    * @param unitTesting specifies whether we're in unit testing mode
    */
   public void setUnitTesting(boolean unitTesting) {
      this.unitTesting = unitTesting;
   }

   /**
    * Sets whether to stub out the matching functionality.
    * @param stubMatching
    */
   public void setStubMatching (boolean stubMatching) {
      this.stubMatching = stubMatching;
   }

   private boolean ignoreUnexpectedTags = false;

   public boolean isIgnoreUnexpectedTags() {
      return ignoreUnexpectedTags;
   }

   public void setIgnoreUnexpectedTags(boolean ignoreUnexpectedTags) {
      this.ignoreUnexpectedTags = ignoreUnexpectedTags;
   }

   /**
    * GEDCOM is terminally rejected
    */
   public static final int STATUS_REJECTED = 0;
   /**
    * GEDCOM has been reviewed and is ready to
    * be processed.
    */
   public static final int STATUS_UPLOADED = 1;
   /**
    * GEDCOM is currently being processed
    */
   public static final int STATUS_PROCESSING = 2;
   /**
    * Pages for GEDCOM have been generated, and they
    * are ready for the user.
    */
   public static final int STATUS_READY = 3;
   /**
    * User has downloaded the pages from a family tree
    * into a GEDCOM. Not yet implemented.
    */
   public static final int STATUS_DOWNLOADED = 4;

   /**
    * The GEDCOM was parsed successfully, but some
    * pages need to be reviewed before the person,
    * family, and source pages are generated.
    */
   public static final int STATUS_REVIEW = 5;

   /**
    * The user has reviewed the pages, and the
    * pages are ready to be created / generated.
    */
   public static final int STATUS_CREATE_PAGES = 6;
   /**
    * Pages are currently being updated and
    * generated (posted) to WeRelate.
    */
   public static final int STATUS_GENERATING = 7;
   /**
    * Reserved
    */
   public static final int STATUS_ADMIN_REVIEW = 8;
   /**
    * Try again to import the GEDCOM, only this time
    * ignore the case where the current file is overlapping
    * a previously imported file.
    */
   public static final int STATUS_IGNORE_OVERLAP = 10;
   /**
    * Queue for GEDCOMs where the xml should not be
    * recreated, but the pages should be regenerated
    * from the XML.
    */
   public static final int STATUS_REGENERATE = 11;

   /**
    * Hold status
    */
   public static final int STATUS_HOLD = 19;

   /**
    * GEDCOM was rejected for one of the following reasons:
    * - GEDCOM was spam or invalid
    * - GEDCOM had new, unrecognized tags
    * - Uploader encountered an internal error while processing the GEDCOM
    */
   public static final int STATUS_ERROR = 100;
   /**
    * User has been notified of the rejection of the GEDCOM.
    */
   public static final int STATUS_OVERLAP_DETECTED = 101;
   /**
    * GEDCOM uploader detected that the file uploaded
    * is not a GEDCOM
    */
   public static final int STATUS_NOT_GEDCOM = 102;
   /**
    * There was some problem while generating the pages -- i.e. --
    * reading the xml file and sending the pages to the wiki page
    * generation api.
    */
   public static final int STATUS_GENERATE_FAILED = 105;

   // Contains the properties parsed from the java properties file
   private Properties properties = null;
   private UserTalker userTalker = null;

   /**
    * Sets up all of the initial variables so that
    * we can loop through the GEDCOMs that are ready to
    * be processed.
    * @param properties java properties file
    * @throws ClassNotFoundException
    * @throws InstantiationException
    * @throws IllegalAccessException
    * @throws SQLException
    * @throws IOException
    */
   public Uploader(Properties properties)
         throws ClassNotFoundException, InstantiationException, IllegalAccessException,
         SQLException, IOException, XPathFactoryConfigurationException
   {
      this.properties = properties;
      Class.forName("com.mysql.jdbc.Driver").newInstance();
      dbConnect();
      xml_output = properties.getProperty("xml_output");
      xml_inprocess = properties.getProperty("xml_inprocess");
      PageEdit.SetWerelateAgent(properties);
      encodeXML = true;
      // The directory which contain the source gedcoms
      gedcomDir = properties.getProperty("gedcom_dir");
      // The hostname of the server the pages will be uploaded to
      wikiServer = properties.getProperty("wiki_server");
      placeServer = properties.getProperty("place_server");
      matchServer = properties.getProperty("match_server");
      PageEdit.setWERELATE_URL("http://" + wikiServer);
      // Creates a new userTalker to send messages to users. The sysop user
      // specified here will receive notifications when there are problems with the GEDCOMs.
      userTalker = new UserTalker(properties);

      // The minimum match score to use as a threshold for the setting the potential
      // match attributes of the family elements
      minimumMatchScore = Float.parseFloat(properties.getProperty("match_threshold",  Float.toString(minimumMatchScore)));
      medievalMatchScore = Float.parseFloat(properties.getProperty("medieval_match_threshold", Float.toString(medievalMatchScore)));
   }

   // Connect to the wikidb
   private void dbConnect() throws SQLException {
      String userName = properties.getProperty("db_username");
      String password = properties.getProperty("db_passwd");
      String url = properties.getProperty("db_url");
      conn = DriverManager.getConnection(url, userName, password);
      initializePreparedStatements();
      logger.info("Database connection established");
   }

   // gedID is updated to the id of the gedcom
   // that we're currently processing
   private int gedID = -1;
   // The status of the GEDCOM we're going to process.
   private int gedStatus = -1;
   // The default country of the GEDCOM we're going to process
   private String defaultCountry = "";

   // The hostname of the server to which we're uploading the
   // pages
   private String wikiServer = null;

   private static String printQueryPart(String fieldName, String name)
   {
      if (!PlaceUtils.isEmpty(name))
      {
         return fieldName + ":\"" + Utils.removeQuotesEtc(name) + "\" ";
      } else
      {
         return "";
      }
   }

   private static boolean bornBefore1600(List<String> personIds, Gedcom gedcom)
   {
      for (String personId : personIds)
      {
         Person spouse = gedcom.getPeople().get(personId);
         if (spouse != null)
         {
            String birthDate = spouse.getBirthDate();
            if (birthDate != null && Utils.dateIsBefore1600AD(birthDate)) {
               return true;
            }
         }
      }
      return false;
   }

   private static String printPersonFields(String personType, List<String> personIds, Gedcom gedcom, boolean isMedieval)
   {
      String query = "";
      for (String personId : personIds)
      {
         Person spouse = gedcom.getPeople().get(personId);
         if (spouse != null && spouse.getName() != null)
         {
            if (!Utils.isEmpty(spouse.getName().getGiven()) && !spouse.getName().getGiven().toLowerCase().equals("unknown"))
            {
               query += printQueryPart(personType + "Givenname", spouse.getName().getFirstGiven());
            }
            if (!Utils.isEmpty(spouse.getName().getSurname()) && !spouse.getName().getSurname().toLowerCase().equals("unknown"))
            {
               query += printQueryPart(personType + "Surname", spouse.getName().getSurname());
            }
            boolean foundBirth = false;
            boolean foundDeath = false;
            for (Event event : spouse.getEvents())
            {
               if (!foundBirth && event.getType() == Event.Type.birth)
               {
                  foundBirth = true;
                  query += printQueryPart(personType + "BirthDate", event.getAttribute("DATE"));
               } else if (isMedieval && !foundDeath && event.getType() == Event.Type.death)
               {
                  foundDeath = true;
                  query += printQueryPart(personType + "DeathDate", event.getAttribute("DATE"));
               }
            }
            if (!foundBirth || (isMedieval && !foundDeath))
            {
               for (Event event : spouse.getEvents())
               {
                  if (!foundBirth && (event.getType() == Event.Type.christening || event.getType() == Event.Type.Baptism))
                  {
                     foundBirth = true;
                     query += printQueryPart(personType + "BirthDate", event.getAttribute("DATE"));
                  } else if (isMedieval && !foundDeath && event.getType() == Event.Type.burial)
                  {
                     foundDeath = true;
                     query += printQueryPart(personType + "DeathDate", event.getAttribute("DATE"));
                  }
               }
            }
         }
      }
      return query;
   }

   private float minimumMatchScore = 3.15f;
   private float medievalMatchScore = 2.35f;

   private static final DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
   public static DocumentBuilder db;
   static
   {
         try {
            //Using factory get an instance of document builder
            db = dbf.newDocumentBuilder();
         }catch(ParserConfigurationException pce) {
            pce.printStackTrace();
         }
   }

   // Following is specific to Saxon: should be in a properties file

   public static XPath xpe;
   private static XPathFactory xpf;
   private static XPathExpression findMatches;
   private static XPathExpression scoreExpression;
   private static XPathExpression titleExpression;
   private static XPathExpression lstExpression;
   private static XPathExpression queryExpression;
   private static XPathExpression placeTitleExpression;
   private static XPathExpression errorExpression;

   static
   {
      try
      {
         xpf = XPathFactory.newInstance();
         xpe = xpf.newXPath();
         findMatches = xpe.compile("/response/result/doc");
         scoreExpression = xpe.compile("float[@name='score']");
         titleExpression = xpe.compile("str[@name='TitleStored']");
         lstExpression = xpe.compile("/response/arr/lst");
         queryExpression = xpe.compile("./str[@name='q']");
         placeTitleExpression = xpe.compile("./str[@name='PlaceTitle']");
         errorExpression = xpe.compile("./str[@name='error']");
      } catch (Exception e)
      {
         throw new RuntimeException(e);
      }
   }

   // Pattern used to parse the primary person update response.
   private static final Pattern pPrimaryResponse = Pattern.compile("<updateTreePrimary\\s+status=\"([^\"]+)\"/>");

   private void updatePrimaryPerson(String title)
   {
      PostMethod m = null;
      try
      {
         String url = getApiUrl();
         m = new PostMethod(url);
         String rsargs = "tree_id=" + treeID + "|primary=" + title;
         uploadNVP[0].setName("rs");
         uploadNVP[0].setValue("wfUpdateTreePrimary");
         uploadNVP[1].setName("action");
         uploadNVP[1].setValue("ajax");
         uploadNVP[2].setName("rsargs");
         uploadNVP[2].setValue(rsargs);         
         m.setRequestBody(uploadNVP);
         if (!executeHttpMethod(m))
         {
            logger.warn("There was an error when executing the method to update the primary person");
            String msg = "No longer can talk to the wiki server while updating the primary person";
            updateGedcom(STATUS_GENERATE_FAILED, gedID, msg);
            userTalker.sendErrorMessage(userName, gedcomName, gedID);
            throw new RuntimeException(msg);
         }
         String response = m.getResponseBodyAsString();
         Matcher mPrimaryResponse = pPrimaryResponse.matcher(response);
         if (mPrimaryResponse.find())
         {
            int status = Integer.parseInt(mPrimaryResponse.group(1));
            if (status != 0)
            {
               // Throw an exception about a non-zero status.
               // I think status -2 means you're not logged in.
               throw new RuntimeException("Unrecognized status after attempting to update the primary person: " + status);
            }
         } else
         {
            throw new RuntimeException("Unrecognized response after attempting to update the primary person: " + response);
         }
      } catch (Exception e)
      {
         throw new RuntimeException(e);
      } finally
      {
         m.releaseConnection();
      }
   }

   private boolean getIsTrustedUploader(String userName, int gedID) throws SQLException, SAXException, IOException {
      boolean isTrusted = false;
      boolean foundResponse = false;
      PostMethod m = null;
      try
      {
         String url = getApiUrl();
         m = new PostMethod(url);
         uploadNVP[0].setName("rs");
         uploadNVP[0].setValue("wfIsTrustedUploader");
         uploadNVP[1].setName("action");
         uploadNVP[1].setValue("ajax");
         uploadNVP[2].setName("rsargs");
         uploadNVP[2].setValue("user_name=" + userName);
         m.setRequestBody(uploadNVP);
         if (!executeHttpMethod(m))
         {
            logger.warn("There was an error when executing the method wfIsTrustedUploader");
            String msg = "No longer can talk to the wiki server while calling wfIsTrustedUploader";
            updateGedcom(STATUS_GENERATE_FAILED, gedID, msg);
            userTalker.sendErrorMessage(userName, gedcomName, gedID);
            throw new RuntimeException(msg);
         }
         String response = m.getResponseBodyAsString();
         m.releaseConnection();

         m = null;
         Matcher mStatus = pResponseStatus.matcher(response);
         if (mStatus.find())
         {
            String status = mStatus.group(2);
            if (!status.equals("0"))
            {
               String msg = "Response status for wfIsTrustedUploader is not 0, it is " + status;
               updateGedcom(Uploader.STATUS_GENERATE_FAILED, gedID, msg);
               userTalker.sendErrorMessage(userName, gedcomName, gedID);
               throw new RuntimeException(msg + '\n' + response);
            }

            // Let's parse the response.
            Document resultDoc = Uploader.db.parse(new InputSource(new StringReader(response)));
            Node node = resultDoc.getFirstChild().getAttributes().getNamedItem("trusted");
            if (node != null) {
               foundResponse = true;
               isTrusted = node.getNodeValue().equalsIgnoreCase("true");
            }
         }

         if (!foundResponse) {
            String msg = "Could not get a response status when calling wfIsTrustedUploader";
            updateGedcom(STATUS_GENERATE_FAILED, gedID, msg);
            userTalker.sendErrorMessage(userName, gedcomName, gedID);
            throw new RuntimeException(msg + ":\n" + response);
         }

      } catch (IOException e)
      {
         updateGedcom(STATUS_GENERATE_FAILED, gedID, e.getMessage());
         userTalker.sendErrorMessage(userName, gedcomName, gedID);
         throw new RuntimeException(e);
      } finally
      {
         if (m != null)
         {
            m.getResponseBodyAsString();
            m.releaseConnection();
         }
      }

      return isTrusted;
   }

   /**
    * This is the main loop for the GEDCOM uploader.
    * This is a summary of what this method does:
    * 1. Gets the next gedcom to be processes
    * 2. If the GEDCOM is valid, then it is processed,
    *  else we wait 60 seconds before looking for another one.
    * 3. We update the status of the GEDCOM to be STATUS_PROCESSING
    * 4. Parse the gedcom file into data structures (when we call the
    *  Gedcom constructor)
    * 5. Decide which people and families are living and therefore
    *  shouldn't be printed out
    * 6. Print the sources, people, and families out to an XML file,
    * 7. If there were warnings while parsing or printing the file,
    *  then we update the status of the GEDCOM to STATUS_REJECTED
    *  and continue to the next GEDCOM
    * 8. We throw away the data structures
    * 9. If there were no warnings for the gedcom, then we upload each
    *  of the pages in the XML file to the wiki
    * 10. Once done successfully generating the pages, we update the
    *  status to be STATUS_READY
    * 11. If there was a problem generating the pages, we update the
    *  status instead to be STATUS_GENERATE_FAILED
    * @throws IOException
    * @throws Uploader.GenerateException
    */
   public void loop() throws IOException, Uploader.GenerateException, InterruptedException, SQLException
   {
      try
      {
         int numPlaces = 0;
         // Sets the gedID and gedStatus and defaultCountry values.
         getNextGedcomID();
         while (gedID > -1) {
            String xmlPath = getXml_output() + '/' + gedID + ".xml";
            if (gedStatus == Uploader.STATUS_REGENERATE && (new File(xmlPath)).exists())
            {
               // Then we will just read in the gedcom from the path
               // instead of reparsing and re-reserving IDs for all
               // of the titles
               updateGedcom(STATUS_GENERATING, gedID, "");
               if (!isUnitTesting())
               {
                  // First we need to reload the XML file.
                  GedcomXML gedXML = new GedcomXML();
                  try
                  {
                     gedXML.parse(xmlPath);
                     readGedcomData(gedXML, true); // re-read gedcom data to set matchedIds and id2ReservedTitle
                     logger.info("Preparing the XML file to be generated");
                     gedXML.prepareForGeneration();
                     logger.info("Updating the family tree with matched titles.");
                     addPagesToFamilyTree(gedXML.getMatchedPagesXML(treeID));
                     logger.info("Generating the updated XML file");
                     uploadXML(gedXML.getPages());
                     String primaryTitle = gedXML.getPrimaryPersonTitle();
                     if (primaryTitle != null)
                     {
                        updatePrimaryPerson(primaryTitle);
                     }
                     logger.info("Done generating the XML file");
                  } catch (GedcomXML.GedcomXMLException e)
                  {
                     logger.warn(e.getMessage());
                     hadException(e);
                  }
               }
            } else if (gedStatus == STATUS_CREATE_PAGES)
            {
               // Let's do each of the steps we need to.
               // First let's read in the xml that we're going to process.
               try
               {
                  updateGedcom(STATUS_GENERATING, gedID, "");
                  GedcomXML gedXml = new GedcomXML();
                  gedXml.parse(getXml_inprocess() + '/' + gedID + ".xml");
                  readGedcomData(gedXml, false);

                  // Now that we're parsed, let's go ahead and reserve
                  // the titles.
                  reserveIDs(gedXml);

                  // Set existing titles in the XML file prior to saving so we can get them in case we need to regenerate
                  gedXml.setExistingTitles();

                  // Makes all citations of a mysource that only has a title simple the contents of
                  // the title as opposed to a link to the (now) excluded mysource
                  // gedXml.fixTitleOnlyMySourceReferences(); -- doesn't seem to work; let's not apply it  (perhaps it's not a good idea after all)

                  // Now let's go ahead and save the xml file.
                  // After we save the XML file, we will go ahead and
                  // generate the pages.
                  logger.info("Saving the updated XML file");
                  gedXml.save(new File(getXml_output() + '/' + gedID + ".xml"));

                  logger.info("Preparing the XML file to be generated");
                  gedXml.prepareForGeneration();
                  if (!isUnitTesting())
                  {
                     logger.info("Updating the family tree with matched titles.");
                     addPagesToFamilyTree(gedXml.getMatchedPagesXML(treeID));
                     logger.info("Generating the updated XML file");
                     uploadXML(gedXml.getPages());
                     String primaryTitle = gedXml.getPrimaryPersonTitle();
                     if (primaryTitle != null)
                     {
                        updatePrimaryPerson(primaryTitle);
                     }
                     logger.info("Done generating the XML file");
                  }
               } catch (GedcomXML.GedcomXMLException e)
               {
                  logger.warn(e.getMessage());
                  hadException(e);
               }
               catch (XPathException e)
               {
                  logger.warn ("XPath Exception: "+e.getMessage());
                  hadException(e);
               }catch (SAXException e)
               {
                  logger.warn ("SAX Exception: "+e.getMessage());
                  hadException(e);
               }
            } else if (gedStatus == STATUS_UPLOADED
                  || gedStatus==STATUS_IGNORE_OVERLAP)
            {
               String gedPath = gedcomDir + '/' + gedID + ".ged";
               if (!(new File(gedPath)).exists()) {
                  // may not have been copied to the gedcom processor server yet; wait until next time
                  break;
               }

               try
               {
                  updateGedcom(STATUS_PROCESSING, gedID, "");
                  String inprocessPath = this.getXml_inprocess() + '/' + gedID + ".xml";
                  StringBuffer placeXMLBuffer = new StringBuffer();
                  boolean isTrustedUploader = getIsTrustedUploader(userName, gedID);
                  Gedcom gedcom = new Gedcom(this, gedPath, userName,
                                             placeServer, defaultCountry, treeID, isTrustedUploader,
                                             isIgnoreUnexpectedTags(), placeXMLBuffer);
                  // If the GEDCOM appears to not be a GEDCOM,
                  if(gedcom.isInvalid())
                  {
                     doGedInvalid();
                  } else
                  {
                     numPlaces += gedcom.getNumPlacesQueried();
                     logger.info("Change living name to unknown");
                     logger.info("Setting isLiving");
                     Person.setLiving(gedcom);
                     // don't do this anymore
                     //gedcom.propagatePrimaryPerson();
                     logger.info("Done setting isLiving");
                     logger.info("Changing names from living to unknown");
                     Person.setUnknownName(gedcom);
                     logger.info("Done changing names");
                     logger.info("Setting isBornBeforeCutoff");
                     Person.setAllBornBeforeCutoff(gedcom);
                     logger.info("Done setting isBornBeforeCutoff");
                     PrintWriter out = new PrintWriter(new OutputStreamWriter(new FileOutputStream(inprocessPath), "UTF-8"));
                     int numCitationOnlySources = 0;
                     for (Source source : gedcom.getSources().values()) {
                        if (!source.shouldPrint(gedcom)) {
                           numCitationOnlySources++;
                        }
                     }
                     out.print("<gedcom citation_only_sources=\""+numCitationOnlySources+"\"");
                     if (gedcom.getPrimaryPerson() != null)
                     {
                        out.print(" primary_person=\"" + gedcom.getPrimaryPerson().getID() + "\"");
                     }
                     out.println(">");
                     out.print(placeXMLBuffer);
                     logger.info("Printing sources");
                     // Print out souces to the xml file
                     for (Map.Entry entry : gedcom.getSources().entrySet()) {
                        // When adding new sources to the source map
                        // (when parsing the GEDCOM),
                        // we check to see if there is another existing
                        // source which already has the same contents as
                        // the source we are about to add.
                        // If there is such an existing source, then
                        // we make the id we are about to add point to the
                        // existing source.
                        //
                        // This has the consequence that when we iterate
                        // through all of the sources in the source map
                        // to print them out,
                        // we need to make sure that the key (id number)
                        // equals the value (Source)'s id number,
                        // so that we only print out each unique source
                        // once.
                        Source source = (Source) entry.getValue();
                        if (entry.getKey().equals(source.getID()))
                        {
                           try
                           {
                              if (source.shouldPrint(gedcom)) {
                                 source.print(gedcom, out, isEncodeXML());
                              }
                           } catch (PrintException e)
                           {
                              logger.warn(e);
                           }
                        }
                     }

                     logger.info("Printing people while auditing for problems");
                     // Print out all of the people to the xml file
                     for(Person person : gedcom.getPeople().values())
                     {
                        try
                        {
                           person.findProblems();
                           person.print(gedcom, out, isEncodeXML());
                        } catch (PrintException e)
                        {
                           logger.warn(e);
                        }
                     }
                     // Print out all of the families to the xml file
                     MultiMap<String, Family> familyNames2Families = new MultiMap<String, Family>();
                     // Let's fill the multimap with the families:
                     logger.info("Finding possible matches for families");
                     for (Family fam : gedcom.getFamilies().values())
                     {
                        if (!stubMatching && fam.shouldPrint(gedcom))
                        {
                           findMatches(fam, gedcom);
                        }
                        familyNames2Families.put(fam.getWikiTitle(gedcom), fam);
                     }
                     logger.info("Printing families while auditing for problems");
                     for(Family fam : gedcom.getFamilies().values())
                     {
                        try
                        {
                           fam.findProblems(familyNames2Families, gedcom);
                           fam.print(gedcom, out, isEncodeXML());
                        } catch (PrintException e)
                        {
                           logger.warn(e);
                        }
                     }
                     out.println("</gedcom>");
                     out.close();
                     // If there are warnings, then we want to update the status
                     // of the GEDCOM and skip the page generation
                     if (gedcom.getNumWarnings() > 0)
                     {
                        logger.warn(gedcom.getUserName() + ':' + gedcom.getFN() + " has "
                              + gedcom.getNumWarnings() + " warning(s).");
                        String reason;
                        if (gedcom.isUnknownTag())
                        {
                           reason = "Unfamiliar tag(s) found in gedcom";
                        } else
                        {
                           reason = "Post-processing exception occurred";
                        }
                        updateGedcom(STATUS_ERROR, gedID, reason);
                        userTalker.sendErrorMessage(userName, gedcomName, gedID);
                     } else
                     {
                        // It's time to use the api to actually update the gedcom

                        // First let's free up the memory taken up by the gedcom object:
                        gedcom = null;

                        // Let's update the GEDCOM's status so that the
                        // user may review it.
                        System.out.print('\n');
                        updateGedcom(Uploader.STATUS_REVIEW, gedID, "");
                        if (!this.isUnitTesting())
                        {
                           userTalker.sendGedcomReviewMessage(userName, gedcomName, gedID);
                        }
                     }
                  }
               } catch (SAXException e)
               {
                  hadException(e);
               }
               catch (Gedcom.PostProcessException e)
               {
                  logger.warn(e.getMessage());
                  //e.printStackTrace();
                  hadException(e);
               } catch (GedcomElementWriter.ElementWriterException e)
               {
                  logger.warn(e.getMessage());
                  hadException(e);
               }
               /*
               // We no longer do this because too many GEDCOMs
               // did not have UIDs.
               catch (Gedcom.UIDException e)
               {
                  updateGedcom(STATUS_REJECTED, gedID, "UID not present for all INDI and FAM objects");
               }*/
            }
            logger.info("Done with gedcom " + gedID);
            getNextGedcomID();
         }
         logger.info("Total number of places queried = " + numPlaces);
      } catch (Gedcom.GedcomException e)
      {
         logger.error("Unhandled Gedcom exception");
         logger.error(e);
      } catch (XPathException e)
      {
         logger.error("Unhandled XPathExpressionException");
         logger.error(e);
      } catch (TransformerException e)
      {
         logger.error("Unhandled TransformerException");
         logger.error(e);
      } catch (SAXException e)
      {
         logger.error("Unhandled SAX Exception");
         logger.error(e);
      }
   }

   private void findMatches(Family fam, Gedcom gedcom)
         throws GenerateException
   {
      // First let's find potential matches for this
      // family:
      String response;
      String url = "http://" + matchServer + "/search";
      GetMethod m = new GetMethod(url);
      uploadNVP[0].setName("fl");
      uploadNVP[0].setValue("TitleStored,score");
      uploadNVP[1].setName("rows");
      uploadNVP[1].setValue("3");
      uploadNVP[2].setName("wt");
      uploadNVP[2].setValue("xml");
      uploadNVP[3].setName("hl");
      uploadNVP[3].setValue("false");
      uploadNVP[4].setName("q");
      String query = "";

      // We need to see if either husband or wife
      // has a medieval birth date.
      boolean isMedieval = bornBefore1600(fam.getHusbands(), gedcom) ||
            bornBefore1600(fam.getWives(), gedcom);

      query += printPersonFields("Husband", fam.getHusbands(), gedcom, isMedieval);
      query += printPersonFields("Wife", fam.getWives(), gedcom, isMedieval);
      // Let's put the marriage information in there.
      for (Event event : fam.getEvents())
      {
         if (event.getType() == Event.Type.marriage)
         {
            String date = event.getAttribute("DATE");
            String place = event.getAttribute("PLAC");
            if (!PlaceUtils.isEmpty(date))
            {
               query += printQueryPart("MarriageDate", date);
            }
            if (!PlaceUtils.isEmpty(place))
            {
               query += printQueryPart("MarriagePlace", place);
            }
         }
      }

      // First let's see what the max score was.
      // If it was not above the threshold, then we stop, because
      // none of the matches are close enough.

      // Build the source document. This is outside the scope of the XPath API, and
      // is therefore Saxon-specific.

      try
      {
         if (!Utils.isEmpty(query))
         {            
            uploadNVP[4].setValue(query);
            m.setQueryString(uploadNVP);

            if (executeHttpMethod(m))
            {
               Document doc = db.parse(m.getResponseBodyAsStream());
               NodeList matches = (NodeList)findMatches.evaluate(doc, XPathConstants.NODESET);
               if (matches != null && matches.getLength() > 0)
               {
                  String matchesString = "";
                  for (int i = 0; i < matches.getLength(); i++)
                  {
                     Node docNode = matches.item(i);
                     float score = Float.parseFloat((String) scoreExpression.evaluate(docNode, XPathConstants.STRING));
                     String matchTitle = (String) titleExpression.evaluate(docNode, XPathConstants.STRING);
                     if ((isMedieval && score > medievalMatchScore) || (!isMedieval && score > minimumMatchScore))
                     {
                        if (matchesString.length() > 0)
                        {
                           matchesString += '|';
                        }
                        matchesString += matchTitle;
                     }
                  }
                  fam.setMatches(matchesString);
               }
            } else
            {
               String msg = "No longer can talk to wiki server when searching for matches";
                  throw new GenerateException(msg);
            }
         }
      } catch (SAXParseException e)
      {
         // Let's print out what we got back from the server:
         try
         {
            System.out.println("Query:\n");
            System.out.println(query);
            System.out.println("Response:\n");
            System.out.println(m.getResponseBodyAsString());
         } catch (IOException e2)
         {

         }
         throw new RuntimeException(e);
      }
      catch (Exception e)
      {
         throw new RuntimeException(e);
      } finally
      {
         m.releaseConnection();
      }
   }

   private void hadException(Exception e) throws SQLException {
      updateGedcom(STATUS_ERROR, gedID, e.getMessage());
      if (!isUnitTesting())
      {
         userTalker.sendErrorMessage(userName, gedcomName, gedID);
      }
   }

   private void doGedInvalid() throws SQLException {
      updateGedcom(STATUS_NOT_GEDCOM, gedID, "File does not appear to be a GEDCOM");
      if (!isUnitTesting())
      {
         userTalker.sendNotGedcom(userName, gedcomName);
      }
   }

   // NameValuePair array used for reserving ids.
   private static NameValuePair [] uploadNVP = new NameValuePair[5];
   static
   {
      for (int i=0; i < uploadNVP.length; i++)
      {
         uploadNVP[i] = new NameValuePair();
      }
   }

   /**
    * Special exception class used for the generate phase of the
    * upload
    */
   public static class GenerateException extends Exception {
      public GenerateException(String msg)
      {
         super(msg);
      }
   }

   /**
    *
    * @param url of the machine to request the reservations from
    * @param request the request string to send to the reservation machine
    * @return the response received by the reservation machine
    * @throws GenerateException
    * @throws IOException
    */
   public String sendIndexNumbersRequest (String url, String request)
         throws GenerateException, IOException
   {
      String response;
      PostMethod m = new PostMethod(url);
      uploadNVP[0].setName("rs");
      uploadNVP[0].setValue("wfReserveIndexNumbers");
      uploadNVP[1].setName("action");
      uploadNVP[1].setValue("ajax");
      uploadNVP[2].setName("rsargs");
      uploadNVP[2].setValue(request);
      m.setRequestBody(uploadNVP);

      if (executeHttpMethod(m))
      {
         response = m.getResponseBodyAsString();
         m.releaseConnection();
         return response;
      } else
      {
         String msg = "No longer can talk to wiki server when requesting title index numbers";
            throw new GenerateException(msg);
      }
   }
   // Pattern used to parse the pIndexResponses
   private static final Pattern pIndexResponse = Pattern.compile("<page[^>]+namespace=\"([^\"]+)\"[^>]+" +
         "title=\"([^\"]+)\"[^>]+titleix=\"([^\"]+)\"[^>]*/>");

   private static final int MAX_NUM_RESERVATIONS = 5000;
   // Class defined mostly so that we can insure that
   // only a limited number of reservation requests are
   // sent each time.
   public static class RequestsList {
      // List of StringBuffers which contain reservation
      // requests which we'll send to the reservation xml api
      // one by one. Each buffer may contain at most MAX_NUM_RESERVATIONS
      // reservations.
      Collection <StringBuffer> buffers = new ArrayList <StringBuffer>();
      // The current StringBuffer that we're appending to.
      private StringBuffer currBuffer = new StringBuffer();
      // The number of times that the currentBuffer has been accessed,
      // which should translate to how many lines have been appended to the
      // current buffer.
      private int numAccesses = 0;
      /**
       * This is called in order to append a line to it.
       * The reason it is done this way instead of passing in the
       * line to be appended is because this stringBuffer is passed
       * into an element writer to write to.
       * @return the current buffer that may be written to.
       */
      public StringBuffer getCurrentBuffer()
      {
         if (numAccesses == MAX_NUM_RESERVATIONS)
         {
            resetCurrent();
         } else
         {
            numAccesses++;
         }
         return currBuffer;
      }
      // Stores the currBuffer in the list of buffers
      // that we have and creates a new one which
      // may be appended to.
      private void resetCurrent() {
         buffers.add(currBuffer);
         currBuffer = new StringBuffer();
         numAccesses = 0;
      }

      /**
       * Returns a list of buffers, each with at most
       * MAX_NUM_RESERVATIONS lines
       * @return a list of buffers containing reservations
       */
      public Collection<StringBuffer> getBuffers() {
         resetCurrent();
         return buffers;
      }

      /**
       *
       * @return the number of requests total in the list
       */
      public int size() {
         int rval = 0;
         for (StringBuffer buf : this.buffers)
         {
            rval+= MAX_NUM_RESERVATIONS;
         }
         rval += numAccesses;
         return rval;
      }
   }

   /**
    * Returns reservations garnered from the reservation API
    * @param requestsList reservation requests
    * @return Map ReservationRequest -> reservations matching the reservation request.
    *   We must have a Queue of reservations for each reservationRequests, because
    *   multiple people and families might have the same names, and we want to
    *   assign such reservations to them one by one.
    * @throws SQLException
    * @throws IOException
    * @throws GenerateException thrown because of a problem interacting with the reservation API
    */
   private Map <ReservationRequest, Queue <String>> getReservations (RequestsList requestsList)
         throws SQLException, IOException, GenerateException
   {
      try
      {
         Map <ReservationRequest, Queue <String>> rval = new HashMap<ReservationRequest, Queue <String>>();
         if (requestsList.size() > 0) {
            for (StringBuffer requests : requestsList.getBuffers())
            {
               String responseString = sendIndexNumbersRequest("http://" + wikiServer + "/w/index.php",
                     "<gedcom>\n" + requests.toString() + "</gedcom>");
               Matcher m = pIndexResponse.matcher(responseString);
               boolean foundResponse = false;
               while (m.find())
               {
                  foundResponse = true;
                  String namespace = m.group(1);
                  String title = Utils.unencodeXML(m.group(2));
                  String titleix = Utils.unencodeXML(m.group(3));
                  ReservationRequest rr = new ReservationRequest(Integer.parseInt(namespace), title);
                  // If the map already contains the reservation request,
                  // we add the reservation to the queue for that reservation request
                  if (rval.containsKey(rr))
                  {
                     Queue <String> queue = rval.get(rr);
                     queue.add(titleix);
                  }
                  // If the map does not already contain the reservation request,
                  // we put the reservation request into the map with a value
                  // set as a new Queue with the current reservation as its
                  // initial value.
                  else
                  {
                     Queue <String> queue = new LinkedList <String>();
                     queue.add(titleix);
                     rval.put(rr, queue);
                  }
               }
               if (!foundResponse)
               {
                  // If we didn't get a valid response, seomthing is wrong,
                  // so we should stop reserving and uploading for now
                  // by throwing a Generate exception, which will stop the
                  // whole process.
                  String msg = "Invalid response to my reservation request. Response = ";
                  String exceptResponse;
                  if (responseString.length() > 250)
                  {
                     exceptResponse = responseString.substring(0, 250);
                  } else
                  {
                     exceptResponse = responseString;
                  }
                  throw new GenerateException(msg + ":\n" + exceptResponse);
               }
            }
         }
         return rval;
      } catch (GenerateException e)
      {
         this.updateGedcom(Uploader.STATUS_ERROR, gedID, e.getMessage());
         if (!isUnitTesting()) {
            userTalker.sendErrorMessage(userName, gedcomName, gedID);
         }
         throw e;
      }
   }   

   // Simulates the actions of the reservation server by assuming that
   // the names for this GEDCOM have never been previously reserved.
   // This is used for the unit testing mode.
   // The output of this method is the "reservedNames" parameter.
   // "values" must be a collection of EventContainer
   // "namespace" of course is the namespace of the EventContainers in "values
   private void stubReserveNames(Gedcom gedcom, Map<Integer, Set<String>> reservedNames, int namespace, Object values)
         throws Gedcom.PostProcessException
   {
      Collection <EventContainer> ecc = (Collection <EventContainer>) values;
      for (EventContainer ec : ecc)
      {
         Set<String> setReservedNames;
         if (reservedNames.containsKey(namespace))
         {
            setReservedNames = reservedNames.get(namespace);
         } else
         {
            setReservedNames = new TreeSet<String>();
            reservedNames.put(namespace, setReservedNames);
         }
         int i = 1;
         String candidateName;
         do
         {
            candidateName = ec.getWikiTitle(gedcom) + " (" + i + ')';
            i++;
         } while(setReservedNames.contains(candidateName));
         setReservedNames.add(candidateName);

         //ec.setReservedTitle(candidateName);
      }
   }
   // Method responsible for reserving IDs for all sources, people, and families
   // in the parameter gedcom object

   private void reserveIDs (GedcomXML gedXml) throws XPathExpressionException,
         SQLException, IOException, GenerateException, GedcomXML.GedcomXMLException
   {
      logger.info("Reserving IDs for people, families and sources in Gedcom");
      RequestsList requestsList = new RequestsList();
      // Add to the requestsList all the wikiTitles of all of the families and people in the
      // gedcom
      gedXml.addRequests(requestsList, userName);

      if (requestsList.size() == 0)
      {
         logger.warn("There are no reservations to be made. There may be a problem with the GEDCOM.");
      }
      // From the reservation server ...
      Map <ReservationRequest, Queue<String>> reservations = getReservations(requestsList);
      // Now have the people and families eat the reservations garnered from the
      // reservation server
      gedXml.setReservedTitles(reservations);
      logger.info("Done reserving titles for people and families in Gedcom");
      logger.info("Updating all gedcom ID references with the reserved titles");
      gedXml.updateContent();
      logger.info("Done updating with reserved titles");
   }

   private String getApiUrl()
   {
      return "http://" + wikiServer + "/w/index.php";
   }

   // Is also called for regenerate in order to set id2ExistingPageTitles
   private void readGedcomData(GedcomXML gedXml, boolean regenerate) throws SQLException, IOException,
         GedcomXML.GedcomXMLException, IOException,
         XPathExpressionException, SAXException
   {
      PostMethod m = null;
      try
      {
         String url = getApiUrl();
         m = new PostMethod(url);
         uploadNVP[0].setName("rs");
         uploadNVP[0].setValue("wfReadGedcomData");
         uploadNVP[1].setName("action");
         uploadNVP[1].setValue("ajax");
         uploadNVP[2].setName("rsargs");
         uploadNVP[2].setValue("gedcom_id=" + Integer.toString(gedID));
         m.setRequestBody(uploadNVP);
         if (!executeHttpMethod(m))
         {
            logger.warn("There was an error when executing the method to read GEDCOM data");
            String msg = "No longer can talk to the wiki server while reading GEDCOM data";
            updateGedcom(STATUS_GENERATE_FAILED, gedID, msg);
            userTalker.sendErrorMessage(userName, gedcomName, gedID);
            throw new RuntimeException(msg);
         }
         String response = m.getResponseBodyAsString();
         m.releaseConnection();

         m = null;
         Matcher mStatus = pResponseStatus.matcher(response);
         if (mStatus.find())
         {
            //logger.warn("Found status: " + mStatus.group(2));
            // If the command line argument statusToProcess is set to
            // STATUS_GENERATE_FAILED, then that means that we should
            // ignore status(-5) errors received from the generate API
            String status = mStatus.group(2);
            if (!status.equals("0"))
            {
               String msg = "Response status for wfReadGedcomData is not 0, it is " + status;
               updateGedcom(Uploader.STATUS_GENERATE_FAILED, gedID, msg);
               userTalker.sendErrorMessage(userName, gedcomName, gedID);
               throw new RuntimeException(msg + '\n' + response);
            }
         } else
         {
            String msg = "Could not get a response status when generating page";
            updateGedcom(STATUS_GENERATE_FAILED, gedID, msg);
            userTalker.sendErrorMessage(userName, gedcomName, gedID);
            throw new RuntimeException(msg + ":\n" + response);
         }

         // Let's parse the response.
         gedXml.SetData(response, regenerate);
      } catch (IOException e)
      {
         updateGedcom(STATUS_GENERATE_FAILED, gedID, e.getMessage());
         userTalker.sendErrorMessage(userName, gedcomName, gedID);
         throw new RuntimeException(e);
      } finally
      {
         if (m != null)
         {
            m.getResponseBodyAsString();
            m.releaseConnection();
         }
      }
   }

   private static final Pattern pResponseStatus = Pattern.compile("<(add|generate|reserve|readGedcomData|updateTreePrimary|trustedUploader)[^>]+status=\"([^\">]+)\"");
   // This next method is responsible for sending the
   // contents of the gedcom's xml file to the wikiserver
   // generate API, page by page.
   private void uploadXML (Collection<Node> pages) throws SQLException, IOException, TransformerException
   {
      BufferedReader in = null;
      PostMethod m = null;
      try
      {

         logger.info("Generating pages for " + gedID);
         String pageString = "";
         String url = getApiUrl();
         int i=0;
         for (Node page : pages)
         {
            if (page.getAttributes().getNamedItem("exclude") == null ||
                  !page.getAttributes().getNamedItem("exclude").getNodeValue().equals("true"))
            {
               m = new PostMethod(url);
               uploadNVP[0].setName("rs");
               uploadNVP[0].setValue("wfGenerateFamilyTreePage");
               uploadNVP[1].setName("action");
               uploadNVP[1].setValue("ajax");
               uploadNVP[2].setName("rsargs");
               String xmlText = GedcomXML.serializeNode(page);
               uploadNVP[2].setValue(xmlText);
               m.setRequestBody(uploadNVP);

               if (!executeHttpMethod(m))
               {
                  logger.warn("There was an error when executing method.");
                  String msg = "No longer can talk to wiki server.";
                  updateGedcom(STATUS_GENERATE_FAILED, gedID, msg);
                  userTalker.sendErrorMessage(userName, gedcomName, gedID);
                  throw new RuntimeException(msg);
               }
               //logger.warn("Done executing method.");
               String response = m.getResponseBodyAsString();
               m.releaseConnection();
               // Just to make sure that we don't
               // try to release it again in the
               // finally clause, we set m = null
               m = null;
               Matcher mStatus = pResponseStatus.matcher(response);
               if (mStatus.find())
               {
                  //logger.warn("Found status: " + mStatus.group(2));
                  // If the command line argument statusToProcess is set to
                  // STATUS_GENERATE_FAILED, then that means that we should
                  // ignore status(-5) errors received from the generate API
                  String status = mStatus.group(2);
                  if (!status.equals("0") && !(status.equals("-5") &&
                        (gedStatus == Uploader.STATUS_REGENERATE)))
                  {
                     String msg = "Response status for wfGenerateFamilyTreePage is not 0, it is " + status;
                     updateGedcom(STATUS_GENERATE_FAILED, gedID, msg);
                     userTalker.sendErrorMessage(userName, gedcomName, gedID);
                     logger.warn("Received status "+status+" for page with title: \"" +
                           page.getAttributes().getNamedItem("title").getTextContent() + "\"");
                     throw new RuntimeException(msg + '\n' + page);
                  } else if (status.equals("-5"))
                  {
                     logger.warn("Received status -5 for page with title: \"" +
                           page.getAttributes().getNamedItem("title").getTextContent() + "\"");
                  }
               } else
               {
                  String msg = "Could not get a response status when generating page";
                  updateGedcom(STATUS_GENERATE_FAILED, gedID, msg);
                  userTalker.sendErrorMessage(userName, gedcomName, gedID);
                  throw new RuntimeException(msg + ":\n" + page);
               }
               i++;
               if (i % 25 == 0)
               {
                  logger.info("Generated 25 pages time="+(new SimpleDateFormat("HH:mm:ss")).format(new Date()));
                  System.out.print('.');
               }
               Utils.sleep(100); // pause just a little
            }
         }
         System.out.print('\n');
         updateGedcom(STATUS_READY, gedID, "");
         userTalker.sendSuccessfulMessage(userName, gedcomName, treeName);
      } catch (FileNotFoundException e)
      {
         updateGedcom(STATUS_GENERATE_FAILED, gedID, e.getMessage());
         userTalker.sendErrorMessage(userName, gedcomName, gedID);
         throw new RuntimeException(e);
      } catch (IOException e)
      {
         updateGedcom(STATUS_GENERATE_FAILED, gedID, e.getMessage());
         userTalker.sendErrorMessage(userName, gedcomName, gedID);
         throw new RuntimeException(e);
      } finally
      {
         if (in != null)
         {
            in.close();
         }
         if (m != null)
         {
            m.getResponseBodyAsString();
            m.releaseConnection();
         }
      }
   }

   // This next method is responsible for sending the
   // contents of the gedcom's xml file to the wikiserver
   // generate API, page by page.
   private void addPagesToFamilyTree (String pagesXML) throws SQLException, IOException, TransformerException
   {
      BufferedReader in = null;
      PostMethod m = null;
      try
      {

         logger.info("Adding pages to family tree for " + gedID);
         String url = getApiUrl();
         m = new PostMethod(url);
         uploadNVP[0].setName("rs");
         uploadNVP[0].setValue("wfAddPagesToTree");
         uploadNVP[1].setName("action");
         uploadNVP[1].setValue("ajax");
         uploadNVP[2].setName("rsargs");
         uploadNVP[2].setValue(pagesXML);
         m.setRequestBody(uploadNVP);

         //logger.warn("Executing Method.");
         if (!executeHttpMethod(m))
         {
            logger.warn("There was an error when executing method.");
            String msg = "No longer can talk to wiki server.";
            updateGedcom(STATUS_GENERATE_FAILED, gedID, msg);
            userTalker.sendErrorMessage(userName, gedcomName, gedID);
            throw new RuntimeException(msg);
         }
         //logger.warn("Done executing method.");
         String response = m.getResponseBodyAsString();
         m.releaseConnection();
         // Just to make sure that we don't
         // try to release it again in the
         // finally clause, we set m = null
         m = null;
         Matcher mStatus = pResponseStatus.matcher(response);
         if (mStatus.find())
         {
            //logger.warn("Found status: " + mStatus.group(2));
            // If the command line argument statusToProcess is set to
            // STATUS_GENERATE_FAILED, then that means that we should
            // ignore status(-5) errors received from the generate API
            String status = mStatus.group(2);
            if (!status.equals("0"))
            {
               String msg = "Response status for wfAddPagesToTree is not 0, it is " + status;
               updateGedcom(STATUS_GENERATE_FAILED, gedID, msg);
               userTalker.sendErrorMessage(userName, gedcomName, gedID);
               throw new RuntimeException(msg + '\n' + pagesXML);
            }
         } else
         {
            String msg = "Could not get a response status when adding pages to tree";
            updateGedcom(STATUS_GENERATE_FAILED, gedID, msg);
            userTalker.sendErrorMessage(userName, gedcomName, gedID);
            throw new RuntimeException(msg + ":\n" + pagesXML);
         }
      } catch (IOException e)
      {
         updateGedcom(STATUS_GENERATE_FAILED, gedID, e.getMessage());
         userTalker.sendErrorMessage(userName, gedcomName, gedID);
         throw new RuntimeException(e);
      } finally
      {
         if (in != null)
         {
            in.close();
         }
         if (m != null)
         {
            m.getResponseBodyAsString();
            m.releaseConnection();
         }
      }
   }

   // This method helps us to retry if there is a
   // problem so that we can stay connected to the wikiServer
   private boolean executeHttpMethod(HttpMethod m) {
      boolean executed = false;
      for (int i=0; i < 5; i++)
      {
         try
         {
            if (!userTalker.isLoggedIn())
            {
               userTalker.setLogin();
            }
            if (userTalker.isLoggedIn())
            {
               //System.out.println("Before");
               userTalker.getClient().executeMethod(m);
               //System.out.println("After");
               Matcher mStatus = pResponseStatus.matcher(m.getResponseBodyAsString());
               if (mStatus.find() && mStatus.group(2).equals("-2"))
               {
                  if (userTalker.setLogin())
                  {
                     userTalker.getClient().executeMethod(m);
                  }
               }
               if (userTalker.isLoggedIn())
               {
                  executed = true;
                  break;
               }
            }
         } catch(HttpException e) {
            logger.warn("HttpException exception while attempting to execute HttpMethod in Uploader.");
            logger.warn(e.getMessage());
            userTalker.setLoggedIn(false);
         } catch (IOException e){
            logger.warn("IO exception while attempting to execute HttpMethod ");
            logger.warn(e.getMessage());
            userTalker.setLoggedIn(false);
         }
         m.releaseConnection();
         userTalker.resetClient();
         Utils.sleep(60000);
      }
      return executed;
   }
   /**
    * Was used for underscoring a title so that
    * we could reserve title ids using the older
    * database method
    * @param title
    * @return DB key form of the title passed in
    * @deprecated
    */
   public @Deprecated static String getDBKey(String title) {
      return title.replaceAll(" ", "_");
      //return title;
   }
   // ReservationRequest class used mostly
   // to combine the namespace and wikifiedTitle
   // of a person or family into a Map key.
   // It might have been easier to just append them
   // together into a string, but this is already done.
   public static class ReservationRequest {
      int namespace;
      String wikifiedTitle;

      public ReservationRequest(int namespace, String wikifiedTitle)
      {
         setNamespace(namespace);
         setWikifiedTitle(wikifiedTitle);
      }

      public int getNamespace() {
         return namespace;
      }

      public void setNamespace(int namespace) {
         this.namespace = namespace;
      }

      public String getWikifiedTitle() {
         return wikifiedTitle;
      }

      public void setWikifiedTitle(String wikifiedTitle) {
         this.wikifiedTitle = wikifiedTitle;
      }

      public int hashCode() {
         int result;
         result = namespace;
         result = 29 * result + (wikifiedTitle != null ? wikifiedTitle.hashCode() : 0);
         return result;
         //return toStr().hashCode();
      }

      public boolean equals(Object o) {
         if (this == o) return true;
         if (o == null || getClass() != o.getClass()) return false;

         final ReservationRequest that = (ReservationRequest) o;

         if (namespace != that.namespace) return false;
         return !(wikifiedTitle != null ? !wikifiedTitle.equals(that.wikifiedTitle) : that.wikifiedTitle != null);
      }
   }

   // Prepared statements used for accessing and updating the
   // familytree_gedcom table in the wikidb
   protected PreparedStatement sUpdateFamilyTreeGedcom = null,
                     sSelectGedcomUploaded = null,
                     sSelectFamilyTreeGedcom = null,
                     sSelectGedcomIDOfUser = null;

   private void initializePreparedStatements() throws SQLException {

      sUpdateFamilyTreeGedcom = conn.prepareStatement("UPDATE familytree_gedcom " +
                                                      "SET fg_status = ?, fg_status_date = ?, " +
                                                      "fg_status_reason = ? WHERE fg_id = ?");
      sSelectGedcomUploaded = conn.prepareStatement("SELECT fg_id, fg_status, ft_user, ft_tree_id, fg_gedcom_filename, ft_name, fg_default_country" +
                                                      " FROM familytree_gedcom " +
                                                      "INNER JOIN familytree ON fg_tree_id = ft_tree_id " +
                                                      "WHERE fg_status = " +
                                                      STATUS_UPLOADED +
                                                      " ORDER BY fg_status_date " +
                                                      "LIMIT 1");

      sSelectFamilyTreeGedcom = conn.prepareStatement("SELECT fg_id, fg_status, ft_user, ft_tree_id, fg_gedcom_filename, ft_name, fg_default_country" +
                                                      " FROM familytree_gedcom " +
                                                      "INNER JOIN familytree ON fg_tree_id = ft_tree_id " +
                                                      "WHERE fg_status = " +
                                                      STATUS_UPLOADED + " OR fg_status = " +
                                                      STATUS_CREATE_PAGES + " OR fg_status = " +
                                                      STATUS_REGENERATE + " OR fg_status = " +
                                                      STATUS_IGNORE_OVERLAP +
                                                      " ORDER BY fg_status_date " +
                                                      "LIMIT 1");
      sSelectGedcomIDOfUser = conn.prepareStatement(
               "SELECT fg_id, fg_status, fg_gedcom_filename from familytree_gedcom  " +
               "INNER JOIN familytree ON fg_tree_id = ft_tree_id " +
               "WHERE ft_user = ? AND (fg_status = " + Uploader.STATUS_READY +
                     " OR fg_status = " + Uploader.STATUS_DOWNLOADED + ")");
   }

   /**
    * Used to execute queries against the wikidb.
    * This method is useful mostly because it helps
    * us keep the connection active.
    * @param query to execute on the wikidb
    */
   public void executeQuery(String query)
   {
      // We'll assume that we're already connected
      boolean connected = true;
      for (int i=0; i < 5; i++)
      {
         try
         {
            if (!connected)
            {
               dbConnect();
               logger.info("Reconnected to database inside of executeQuery()");
            }
            Statement s = getConn().createStatement();
            s.execute(query);
            s.close();
            return;
         } catch (SQLException e)
         {
            close();
            connected = false;
            // We should reset the connection
            Utils.sleep(60000);
         }
      }
      throw new RuntimeException("I can no longer talk to the wikidb");
   }

   /**
    * Extracts the original query and result from
    * a place search result from Ezekiel
    */
   public static final Pattern pSearchResult = Pattern.compile(
         "<search q=\"([^\"]+?)\"><result><title>([^<]+?)</title>"
   );
   // Used for talking to the Place search engine
   private static NameValuePair [] nvp = new NameValuePair[3];

   static {
      nvp[0] = new NameValuePair();
      nvp[1] = new NameValuePair();
      nvp[2] = new NameValuePair();
   }

   private static Pattern pBeginSearchResults = Pattern.compile("^<response>", Pattern.MULTILINE);
   private static Pattern pEndSearchResults = Pattern.compile("</response>$", Pattern.MULTILINE);

   /**
    * Produces a map from the names passed in to WeRelate standardized
    * names as returned by the PlaceSearch server. If the server was
    * not able to find the place name, then there is no entry
    * in the map for it.
    * @param names place names to standardize
    * @throws IOException
    */
   public void getStandardizedPlaceNames(String placeServer, Set<String> names, String defaultCountry,
                                         StringBuffer placeXMLBuffer)
         throws IOException
   {
      if (names.size() > 0)
      {
         boolean started = false;
         logger.info("Creating place standardization query");
         StringBuffer query = new StringBuffer();
         for (String name : names)
         {
            if (started)
            {
               query.append('|');
            } else
            {
               started = true;
            }
            query.append(name);
         }
         logger.info("Done creating place standardization query");
         String url = "http://" + placeServer + "/placestandardize";
         PostMethod m = new PostMethod(url);

         logger.info("Setting request body");
         nvp[0].setName("q");
         nvp[0].setValue(query.toString());
         //nvp[0].setValue("Fort Collins, Colorado");
         nvp[1].setName("defaultCountry");
         nvp[1].setValue(defaultCountry);
         nvp[2].setName("wt");
         nvp[2].setValue("xml");
         m.setRequestBody(nvp);
         HttpMethodParams params = new HttpMethodParams();
         params.setContentCharset("UTF-8");
         params.setHttpElementCharset("UTF-8");
         params.setParameter("http.protocol.content-charset", "UTF-8");
         m.setParams(params);
         m.setRequestHeader("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
         logger.info("Done setting request body");
         logger.info("Executing method");
         if(!executeHttpMethod(m))
         {
            String msg = "Can no longer talk to http server while getting standard names.";
            throw new RuntimeException(msg);
         }

         String charSet = m.getResponseCharSet();
         String s = m.getResponseBodyAsString();

         // Now we need to parse the response as XPaths.

         // First we need to check to make sure that we got a valid response
         // back.
         Matcher mBeginSearchResults = pBeginSearchResults.matcher(s);
         Matcher mEndSearchResults = pEndSearchResults.matcher(s);
         if (!(mBeginSearchResults.find() && mEndSearchResults.find()))

         {
            throw new RuntimeException("Invalid search results returned from the place search server. " +
                  "Please ensure that the url specified for the search server is valid.");
         }

         logger.info("Done executing method, got back valid string");
         logger.info("Parsing place results");
         // Now let's create an input source for our XPaths.
         try
         {
            //System.out.println(s);
            Document doc = db.parse(new InputSource(new StringReader(s)));
            NodeList lstNodes = (NodeList) lstExpression.evaluate(doc, XPathConstants.NODESET);
            for (int i=0; i < lstNodes.getLength(); i++)
            {
               Node node = lstNodes.item(i);
               String q = (String) queryExpression.evaluate(node, XPathConstants.STRING);
               String placeTitle = (String)placeTitleExpression.evaluate(node, XPathConstants.STRING);
               String error = (String) errorExpression.evaluate(node, XPathConstants.STRING);
               ElementWriter ew = new GedcomElementWriter("place");
               ew.put("key", String.format("WRP%03d", i));
               ew.put("text", q);
               ew.put("title", placeTitle);
               Node errorNode = node.getAttributes().getNamedItem("error");
               if (!Utils.isEmpty(error))
               {
                  ew.put("error", error);
               }
               ew.write(placeXMLBuffer);
            }
         } catch (Exception e)
         {
            throw new RuntimeException(e);
         }
         logger.info("Done parsing results");
      }
   }

   /**
    * Defined for use in cases where we have an exception,
    * while we are printing or doing something related
    * to outputting the object to the GEDCOM's xml file
    */
   public static class PrintException extends Gedcom.GedcomException {
      public PrintException (String m, Gedcom g, String id)
      {
         super(m, g, id);
      }

   }

   /**
    * closes the sql prepared statements and
    * the database connection
    */
   protected void close()
   {
      try {
         if (this.sSelectGedcomUploaded != null)
         {
            sSelectGedcomUploaded.close();
            sSelectGedcomUploaded = null;
         }
         if (this.sSelectGedcomIDOfUser != null)
         {
            sSelectGedcomIDOfUser.close();
            sSelectGedcomIDOfUser = null;
         }
         if (this.sSelectFamilyTreeGedcom != null)
         {
            sSelectFamilyTreeGedcom.close();
            sSelectFamilyTreeGedcom = null;
         }
         if (this.sUpdateFamilyTreeGedcom != null)
         {
            sUpdateFamilyTreeGedcom.close();
            sUpdateFamilyTreeGedcom = null;
         }
         if (conn != null)
         {
            conn.close();
            conn = null;
         }
         logger.info("Database connection terminated");
      }
      catch (Exception e)
      {
         logger.warn("There was an exception when terminating database connection");
         logger.warn(e);
      }
   }

   /**
    * Used to print a simple tag of this form:
    * <tag_name>value</tag_name>\n
    * More complicated cases use the GedcomElementWriter
    * @param tagName
    * @param value
    * @return a string of the printed form of the tag
    */
   public static String printTag(String tagName, String value)
   {
      if (Utils.isEmpty(value) || value.trim().equals(""))
      {
         return "";
      } else
      {
         return '<' + tagName + '>' + Utils.encodeXML(value) + "</" + tagName + ">\n";
      }
   }

   /**
    * Used by a variety of methods to do a carefully delimited append to a string
    * @param currVal
    * @param newVal
    * @param delimeter
    * @return currVal appended with newVal separated by the delimeter
    */
   public static String append(String currVal, String newVal, String delimeter)
   {
      if (!Utils.isEmpty(newVal))
      {
         if (Utils.isEmpty(currVal))
         {
            return newVal;
         } else if (currVal.contains(newVal))
         {
            return currVal;
         } else
         {
            return currVal + delimeter + newVal;
         }
      } else
      {
         return currVal;
      }
   }

   /**
    * @return the location of the directory
    *    where the gedcom files are outputted
    */
   public String getXml_output() {
      return xml_output;
   }
   public String getXml_inprocess() {
      return xml_inprocess;
   }
   private String xml_output = null;
   private String xml_inprocess = null;
   /**
    * @return boolean indicating
    * whether to encode the xml
    * inside of the content tags in
    * person, family, and souce tags
    */
   public boolean isEncodeXML() {
      return encodeXML;
   }
   private boolean encodeXML = false;

   // Used to interact with the database.
   private Connection conn = null;
   /**
    *
    * @return connection to wikidb
    */
   public Connection getConn() {
      return conn;
   }

   //User name to access the wikidb
   protected String userName = null;
   // The tree id of the current gedcom
   protected int treeID = -1;
   protected String treeName = null;

   private static int NUM_DB_RETRIES = 5;
   // in milliseconds
   private static int DB_WAIT = 60000;
   // retrieves the next Gedcom ID to be processed
   // from the wikidb
   private String gedcomName = null;

   protected void getNextGedcomID () throws SQLException,
                                                     IOException,
                                                     InterruptedException,
                                                     Gedcom.GedcomException
   {
      // We'll assume that we're already connected
      boolean connected = true;
      gedID = -1;
      for (int i=0; i < NUM_DB_RETRIES; i++)
      {
         try
         {
            if (!connected)
            {
               dbConnect();
            }
            sSelectGedcomUploaded.execute();
            ResultSet rs = sSelectGedcomUploaded.getResultSet();
            if (rs.next())
            {
               setGedcomSettings(rs);
            } else
            {
               sSelectFamilyTreeGedcom.execute();
               rs = sSelectFamilyTreeGedcom.getResultSet();
               if(rs.next())
               {
                  setGedcomSettings(rs);
               }
            }
            return;
         } catch (SQLException e)
         {
            logger.warn("SQL exception while attempting to get the next GEDCOM ID. ");
            logger.warn(e.getMessage());
            e.printStackTrace();
            connected = false;
            Utils.sleep(DB_WAIT);
         }
      }
      throw new RuntimeException("I can no longer talk to the wikidb");
   }

   private void setGedcomSettings(ResultSet rs) throws IOException, SQLException {
      treeName = rsReadString(rs, 6);
      gedcomName = rsReadString(rs, 5);
      defaultCountry = rs.getString(7);
      treeID = rs.getInt(4);
      userName = rsReadString(rs, 3);
      gedStatus = rs.getInt(2);
      gedID = rs.getInt(1);

      if (gedcomName.toLowerCase().trim().endsWith(".pdf"))
      {
         doGedInvalid();
         gedID = -1;
      }
      else if (gedStatus != STATUS_IGNORE_OVERLAP &&
            gedStatus != STATUS_REGENERATE &&
            gedStatus != STATUS_CREATE_PAGES)
      {
         gedID= checkOverlap(gedID);
      }
   }

   private String rsReadString(ResultSet rs, int index) throws IOException, SQLException {
      String rval;
      String line;
      BufferedReader in = new BufferedReader(new InputStreamReader(rs.getAsciiStream(index), "UTF-8"));
      rval = in.readLine();
      while ((line = in.readLine())!= null)
      {
         rval += '\n' + line;
      }
      in.close();
      return rval;
   }

   private static final int OVERLAP_PERCENT_THRESHOLD = 30;

   // Controls whether we should check for overlap
   // with a user's existing GEDCOMs before uploading the
   // GEDCOM
   private boolean shouldCheckOverlap = true;

   /**
    *
    * @return whether the Uploader is set to check for overlap
    */
   public boolean shouldCheckOverlap() {
      return shouldCheckOverlap;
   }

   /**
    *
    * @param shouldCheckOverlap says whether the Uploader should check for overlap with other
    * GEDCOMs belonging to this users before uploading the current GEDCOM
    */
   public void setShouldCheckOverlap(boolean shouldCheckOverlap) {
      this.shouldCheckOverlap = shouldCheckOverlap;
   }

   private static Pattern pIndiStart = Pattern.compile("0\\s+@[^@]+@\\s+INDI");
   private static Pattern pNonIndiStart = Pattern.compile("0\\s+@[^@]+@\\s+(SOUR|SOURCE|REPOSITORY|REPO|FAM|FAMILY)");
   private String currGiven = null, currSurname = null;

   private Set <String> generateNameSet (int idNum)
   {
      Set <String> nameSet = new HashSet <String>();
      try
      {
         BufferedReader in = new BufferedReader (new FileReader(this.gedcomDir + '/' + idNum + ".ged"));
         String line;
         // First we want to get to the first individual:
         skipToIndi(in);
         while((line = in.readLine()) != null)
         {
            line = line.trim();
            if (pNonIndiStart.matcher(line).find())
            {
               // We are potentially ending the
               // current INDI tag.
               addGivnSurn(nameSet);
               // Now let's skip ahead until we
               // get to the next INDI tag.
               skipToIndi(in);
            } else if (pIndiStart.matcher(line).find())
            {
               addGivnSurn(nameSet);
            } else if (line.startsWith("1 NAME "))
            {
               line = line.substring(7);
               if (!Utils.isEmpty(line))
               {
                  nameSet.add(line.trim());
               }
            } else if (line.startsWith("1 GIVN ") ||
                  line.startsWith("2 GIVN "))
            {
               if (Utils.isEmpty(currGiven))
               {
                  if (line.length() > 7)
                  {
                     currGiven = line.substring(7);
                  }
               } else
               {
                  addGivnSurn(nameSet);
               }
            } else if (line.startsWith("1 SURN") ||
                  line.startsWith("2 SURN"))
            {
               if (Utils.isEmpty(currSurname))
               {
                  if (line.length() > 7)
                  {
                     currSurname = line.substring(7);
                  }
               } else
               {
                  addGivnSurn(nameSet);
               }
            }
         }
         addGivnSurn(nameSet);
      } catch (FileNotFoundException e)
      {
         logger.warn("When checking for warnings, a file not found exception was thrown.");
         // do nothing -- we'll just return an empty nameset.
      } finally
      {
         return nameSet;
      }
   }

   private void skipToIndi(BufferedReader in) throws IOException {
      String line;
      while((line = in.readLine()) != null && !pIndiStart.matcher(line).find())
      {

      }
   }

   // Returns the gedID if there is no overlap between the
   // gedcom with the gedID passed in and another GEDCOM
   // already uploaded by the same user.
   // If there is overlap, then -1 is returned, otherwise,
   // the gedID is returned.
   private int checkOverlap(int gedID) throws IOException
   {
      if (shouldCheckOverlap)
      {
         ResultSet rs;
         // Now we need to make sure that the GEDCOM we got does
         // not significantly overlap with another GEDCOM that
         // this user has uploaded. Therefore, let's execute
         // a query that get's us all the file IDs for this user
         logger.debug("Checking overlap between this gedcom (" + gedID +
               ") and other gedcoms uploaded by user.");

         // We'll assume that we're already connected
         boolean connected = true;
         // We will try up to five times in
         // row to conduct the database operations.
         for (int i=0; i < NUM_DB_RETRIES; i++)
         {
            try
            {
               if (!connected)
               {
                  dbConnect();
               }
               psSetString(sSelectGedcomIDOfUser, 1, userName);
               sSelectGedcomIDOfUser.execute();
               rs = sSelectGedcomIDOfUser.getResultSet();
               while (rs.next())
               {
                  int otherId = rs.getInt(1);
                  int otherStatus = rs.getInt(2);
                  String realGedcomName = rsReadString(rs, 3);
                  if (otherId != gedID &&
                      otherStatus >= 1 &&
                      otherStatus <= 99)
                  {
                     int overlapPercent = getOverlapPercentage(otherId, gedID);

                     if (overlapPercent > OVERLAP_PERCENT_THRESHOLD)
                     {
                        updateGedcom(Uploader.STATUS_OVERLAP_DETECTED, gedID, "May overlap with " + realGedcomName);
                        userTalker.sendOverlapMessage(userName, gedcomName);
                        return -2;
                     }
                  }
               }
               return gedID;
            } catch (SQLException e)
            {
               connected = false;
               Utils.sleep(DB_WAIT);
            }
         }
         throw new RuntimeException (
            "Could no longer communicate with database while checking GEDCOM overlap");
      } else
      {
         return gedID;
      }
   }

   public int getOverlapPercentage(int otherId, int gedID) throws IOException {
      Set<String> otherNameSet = this.generateNameSet(otherId);
      Set <String> thisNameSet = this.generateNameSet(gedID);
      int numMatching = 0;
      for (String name : thisNameSet)
      {
         if (otherNameSet.contains(name))
         {
            numMatching++;
         }
      }

      if (thisNameSet.size() > 0)
      {
         int overlapPercent = numMatching*100 / thisNameSet.size();
         return overlapPercent;
      } else
      {
         return 0;
      }
   }

   private void addGivnSurn(Set<String> nameSet) {
      String newName = "";
      if(!Utils.isEmpty(currGiven))
      {
         newName += currGiven.trim();
         if (!Utils.isEmpty(currSurname))
         {
            newName += ' ';
         }
      }
      if (!Utils.isEmpty(currSurname))
      {
         newName += currSurname.trim();
      }
      if (!Utils.isEmpty(newName))
      {
         nameSet.add(newName);
      }
      currGiven = currSurname = null;
   }

   private void psSetString(PreparedStatement ps, int index, String input) throws SQLException, UnsupportedEncodingException {
      ps.setAsciiStream(index, new ByteArrayInputStream(input.getBytes("UTF-8")),input.length());
   }

   // Generates today's date
   private static final SimpleDateFormat format = new SimpleDateFormat("yyyyMMddHHmmss");
   protected static String generateDateString() {
      GregorianCalendar today = new GregorianCalendar();
      java.util.Date date = today.getTime();
      format.setTimeZone(TimeZone.getTimeZone("GMT"));
      return format.format(date);
   }

   // Updates the status of a gedcom in
   // familytree_gedcom
   protected void updateGedcom (int status, int id, String reason) throws SQLException
   {
      // We'll assume that we're already connected
      boolean connected = true;
      for (int i=0; i < NUM_DB_RETRIES; i++)
      {
         try
         {
            if (!connected)
            {
               dbConnect();
            }
            sUpdateFamilyTreeGedcom.setInt(1, status);
            sUpdateFamilyTreeGedcom.setString(2, generateDateString());
            sUpdateFamilyTreeGedcom.setString(3, reason);
            sUpdateFamilyTreeGedcom.setInt(4, id);
            sUpdateFamilyTreeGedcom.execute();
            return;
         } catch (SQLException e)
         {
            connected = false;
            Utils.sleep(DB_WAIT);
         }
      }
      throw new RuntimeException("I can no longer talk to the wikidb");
   }

   /**
    * This is the primary main for the gedcom package.
    * @param args
    * @throws ParseException
    * @throws Exception
    */
   public static void main (String [] args) throws ParseException, Exception
   {
      Options opt = new Options();
      opt.addOption("p", true, "java .properties file");
      opt.addOption("o", false, "If present, we ignore overlaps when uploading GEDCOMs.");
      opt.addOption("u", false, "If set, we ignore unexpected tags without throwing a " +
            "warning and only printing an info to log file");
      opt.addOption("t", false, "We are testing -- stubs out the ID reservation and skips page generation");
      opt.addOption("h", false, "Print out help information");

      BasicParser parser = new BasicParser();
      CommandLine cl = parser.parse(opt, args);
      if (cl.hasOption("h") || !cl.hasOption("p"))
      {
          System.out.println("Robot which uploads gedcom files to werelate.");
          HelpFormatter f = new HelpFormatter();
          f.printHelp("OptionsTip", opt);
      }
      else
      {
         // First we need to read the properties file.
         Uploader cgp = null;
         try {
            Properties properties = new Properties();
            properties.load(new FileInputStream(cl.getOptionValue("p")));
            cgp = new Uploader(properties);
            if(cl.hasOption("t"))
            {
               cgp.setUnitTesting(true);
            } else
            {
               cgp.setUnitTesting(false);
            }

            // don't check overlap anymore
//            if (cl.hasOption("o"))
//            {
               cgp.setShouldCheckOverlap(false);
//            } else
//            {
//               cgp.setShouldCheckOverlap(true);
//            }
            if(cl.hasOption("u"))
            {
               cgp.setIgnoreUnexpectedTags(true);
            }
            //cgp.setShouldStopWhenWithoutGedcom(true);
            cgp.loop();
         }
         catch (Exception e) {
            System.err.println("There was a problem while uploading");
            throw e;
         }
         finally {
            if (cgp != null)
            {
               cgp.close();
            }
         }
      }
   }
}
