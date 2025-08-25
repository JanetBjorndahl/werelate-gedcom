package org.werelate.gedcom;

import org.w3c.dom.*;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.werelate.util.Utils;
import org.werelate.util.PlaceUtils;
import org.werelate.util.MultiMap;
import org.werelate.util.ElementWriter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.xml.xpath.*;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.io.*;

/**
 * Created by IntelliJ IDEA.
 * User: nathan
 * Date: Jan 2, 2009
 * Time: 1:30:09 PM
 * To change this template use File | Settings | File Templates.
 */
public class GedcomXML {
   private static final Logger logger =
         LogManager.getLogger("org.werelate.gedcom.GedcomXML");

   private Map<String, Node> id2Page = new HashMap<String, Node>();
   private Map<String, Document> id2Content = new HashMap<String, Document>();
   private Map<String, String> id2Text = new HashMap<String,String>();
   private Set<String> excludedIds = new HashSet<String>();
   private Set<String> livingIds = new HashSet<String>();
   private Set<String> matchedIds = new HashSet<String>();
   private Set<String> personIds = new HashSet<String>();
   private Set<String> familyIds = new HashSet <String>();
   private Set<String> mysourceIds = new HashSet<String>();
   private Map<String, String> id2Title = new HashMap<String, String>();
   private Map<String, String> id2ReservedTitle = new HashMap<String, String>();
   private Map<String, String> placeText2Id = new HashMap<String, String>();
   private Map<String, String> placeId2Standardized = new HashMap<String, String>();
   private Map<String, String> id2Uid = new HashMap<String,String>();
   private Map<String, Integer> id2Namespace = new HashMap<String, Integer>();
   private MultiMap<String,String> id2ExistingPageTitles = new MultiMap <String, String>();
   private Document doc = null;
   private String primaryPerson = null;


   public class GedcomXMLException extends Exception {
      public GedcomXMLException(String message)
      {
         super(message);
      }
   }

   // This function is called also during page regeneration to populate matchedIds and id2ReservedTitle
   // matchedIds should really be stored in the gedcom and read, but they're not unfortunately
   public void SetData(String resultString, boolean regenerate) throws XPathExpressionException, GedcomXMLException,
         SAXException, IOException
   {
      Document resultDoc = Uploader.db.parse(new InputSource(new StringReader(resultString)));
      Node primaryNode = resultDoc.getFirstChild().getAttributes().getNamedItem("primary");
      if (!regenerate && primaryNode != null)
      {
         primaryPerson = primaryNode.getNodeValue();
         // Let's also update the primary person in the GEDCOM tag.
         Node gedcomPrimary = doc.getFirstChild().getAttributes().getNamedItem("primary_person");
         if (gedcomPrimary != null)
         {
            gedcomPrimary.setNodeValue(primaryPerson);
         } else
         {
            Node primaryAttribute = doc.createAttribute("primary_person");
            primaryAttribute.setNodeValue(primaryPerson);
            doc.getFirstChild().getAttributes().setNamedItem(primaryAttribute);
         }
      }

      NodeList results = (NodeList) resultExpression.evaluate(resultDoc, XPathConstants.NODESET);
      for (int i=0; i < results.getLength(); i++)
      {
         Node result = results.item(i);
         String id = result.getAttributes().getNamedItem("key").getNodeValue();

         // Let's see if we need to update the contents of the page.
         if (!regenerate && !Utils.isEmpty(result.getTextContent()))
         {
            // In case there was already textual content, but it is no longer there:
            id2Text.remove(id);
            // We update the page's contents
            parseContent(result.getTextContent(), id);
         }

         Node excludeNode = result.getAttributes().getNamedItem("exclude");
         if (!regenerate && excludeNode != null && !PlaceUtils.isEmpty(excludeNode.getNodeValue()))
         {
            if (excludeNode.getNodeValue().equals("-1"))
            {
               excludedIds.remove(id);
               Node page = id2Page.get(id);
               if (page.getAttributes().getNamedItem("exclude") !=null)
               {
                  page.getAttributes().removeNamedItem("exclude");
               }
            } else if (excludeNode.getNodeValue().equals("1"))
            {
               excludedIds.add(id);
               // Let's update the page node appropriately.
               if (id2Page.containsKey(id)) {
                  Node page = id2Page.get(id);
                  excludePage(page);
               }
               else if (placeId2Standardized.containsKey(id)) {
                  placeId2Standardized.remove(id);
               }
               else {
                  throw new GedcomXMLException("Excluded Id cannot be found.");
               }
            }
         }
         Node livingNode = result.getAttributes().getNamedItem("living");
         if (!regenerate && livingNode != null && !PlaceUtils.isEmpty(livingNode.getNodeValue()))
         {
            if (livingNode.getNodeValue().equals("-1"))
            {
               removeLiving(id);
            } else if (livingNode.getNodeValue().equals("1"))
            {
               markAsLiving(id);
            }
         }

         Node matchNode = result.getAttributes().getNamedItem("match");
         if (matchNode != null && !matchNode.getNodeValue().equals(""))
         {
            String matchTitle = null;
            boolean match = false;
            // Match title is source instead of mysource
            boolean isSource = false;
            if (!matchNode.getNodeValue().toLowerCase().equals("#nomatch#"))
            {
               match = true;
               matchTitle = matchNode.getNodeValue();
               if (matchTitle.startsWith("Source:"))
               {
                  isSource = true;
               }
               matchTitle = matchTitle.replaceFirst("(Person|Family|MySource|Source|Place):", "");
            }

            if (placeId2Standardized.containsKey(id))
            {
               if (!regenerate) {
                  // We should also update the place node for this id.
                  XPathExpression expression = xpe.compile("/gedcom/place[@key='" + id + "']");
                  Node placeNode = (Node) expression.evaluate(doc, XPathConstants.NODE);
                  if (!match)
                  {
                     matchTitle = placeNode.getAttributes().getNamedItem("text").getNodeValue();
                  }

                  // Let's make sure there is a title.
                  placeId2Standardized.put(id, matchTitle);
                  if (placeNode != null &&
                      placeNode.getAttributes() != null &&
                      placeNode.getAttributes().getNamedItem("title") != null) {
                     placeNode.getAttributes().getNamedItem("title").setNodeValue(matchTitle);
                     Node overrideNode = doc.createAttribute("override");
                     overrideNode.setNodeValue("true");
                     placeNode.getAttributes().setNamedItem(overrideNode);
                  }
               }
            }
            else if (match)
            {
               matchedIds.add(id);
               id2ReservedTitle.put(id, matchTitle);
               if (!regenerate) {
                  setTitle(matchTitle, id2Page.get(id));
                  if (isSource)
                  {
                     mysourceIds.remove(id);
                     setPageNamespace(id, Utils.SOURCE_NAMESPACE, "source");
                  }
               }
            }
            else // if not a match.
            {
               matchedIds.remove(id);
               id2ReservedTitle.remove(id);
               if (!regenerate) {
                  Node titleNode = id2Page.get(id).getAttributes().getNamedItem("title");
                  if (titleNode != null)
                  {
                     removeNode(titleNode);
                  }

                  if (id2Namespace.get(id) == Utils.SOURCE_NAMESPACE)
                  {
                     // Let's change the namespace back to a MySource, if the
                     // namespace is currently a source.
                     mysourceIds.add(id);
                     setPageNamespace(id, Utils.MYSOURCE_NAMESPACE, "mysource");
                  }
               }
            }
         }
      }

      if (!regenerate) {
         rerunFamilyLiving();
         recalculateFamilyPersonReferences();
      }
   }

   private void setPageNamespace(String id, int namespace, String tagName) {
      // Let's change the namespace of the page element
      Node page = id2Page.get(id);
      page.getAttributes().getNamedItem("namespace").setNodeValue(Integer.toString(namespace));
      id2Namespace.put(id, namespace);
   }

   public String getMatchedPagesXML(int treeID)
   {
      StringBuffer buffer = new StringBuffer();
      buffer.append("<pages tree_id=\"" + treeID + "\">\n");
      for (String id : matchedIds)
      {
         buffer.append("<page namespace=\"" + id2Namespace.get(id) +
               "\" title=\"" + Utils.encodeXML(id2ReservedTitle.get(id)) + "\" uid=\"" + id2Uid.get(id) + "\"/>\n");
      }
      buffer.append("</pages>");
      return buffer.toString();
   }

   private void markAsLiving(String id) {
      livingIds.add(id);
      Node page = id2Page.get(id);
      Node livingAttribute = doc.createAttribute("living");
      livingAttribute.setNodeValue("true");
      page.getAttributes().setNamedItem(livingAttribute);
   }

   private void removeLiving(String id) {
      livingIds.remove(id);
      Node page = id2Page.get(id);
      if (page.getAttributes().getNamedItem("living") != null)
      {
         page.getAttributes().removeNamedItem("living");
      }
   }

   private void excludePage(Node page) {
      Node excludeAttribute = doc.createAttribute("exclude");
      excludeAttribute.setNodeValue("true");
      page.getAttributes().setNamedItem(excludeAttribute);
   }

   public Collection<Node> getPages()
   {
      return id2Page.values();
   }

   // Check to see if any families that are
   // not already marked living, should be marked
   // living because a husband or wife is now living
   private void rerunFamilyLiving() throws XPathExpressionException
   {
      for (String id : familyIds)
      {
         // Let's first determine if any of the
         // parents are currently living.
         Node content = id2Content.get(id);
         NodeList spouseIds = (NodeList) spouseIdExpression.evaluate(content, XPathConstants.NODESET);

         boolean hasLivingSpouse = false;
         for (int i=0; i < spouseIds.getLength(); i++)
         {
            String spouseId = spouseIds.item(i).getNodeValue();
            if (livingIds.contains(spouseId))
            {
               hasLivingSpouse = true;
               break;
            }
         }

         if (hasLivingSpouse && !livingIds.contains(id))
         {
            // The family was not marked as living, but
            // we now need to change it to a living state.
            markAsLiving(id);
         } else if (!hasLivingSpouse && livingIds.contains(id))
         {
            // Then we need to change the state of the family
            // so that it is no longer marked as living.
            removeLiving(id);
         }
      }
   }

   private static XPathExpression pageExpression;
   private static XPathExpression placeExpression;
   public static XPathExpression namespaceExpression;
   public static XPathExpression nameExpression;
   public static XPathExpression idExpression;
   public static XPathExpression excludedExpression;
   public static XPathExpression livingExpression;
   private static XPathExpression contentExpression;
   private static XPathExpression husbandIdExpression;
   private static XPathExpression wifeIdExpression;
   private static XPathExpression spouseIdExpression;
   private static XPathExpression sourceTitleExpression;
   private static XPathExpression familyReferenceExpression;
   private static XPathExpression personReferenceExpression;
   private static XPathExpression personReferenceAttributeExpression;
   private static XPathExpression childOfFamilyExpression;
   private static XPathExpression sourceCitationExpression;
   private static XPathExpression sourceIdExpression;
   private static XPathExpression sourcesExpression;
   private static XPathExpression placeAttributeExpression;
   private static XPathExpression resultExpression;
   private static XPathExpression eventExpression;
   private static XPathExpression altNameExpression;
   private static XPathExpression noteExpression;
   private static XPathExpression uidExpression;
   private static final XPath xpe = Uploader.xpe;

   static
   {
      try
      {
         pageExpression = xpe.compile("/gedcom/page");
         placeExpression = xpe.compile("/gedcom/place");
         namespaceExpression = xpe.compile("@namespace");
         idExpression = xpe.compile("@id");
         uidExpression = xpe.compile("@uid");
         excludedExpression = xpe.compile("@exclude");
         livingExpression = xpe.compile("@living");
         contentExpression = xpe.compile("content");
         husbandIdExpression = xpe.compile("/family/husband[1]/@id");
         wifeIdExpression = xpe.compile("/family/wife[1]/@id");
         spouseIdExpression = xpe.compile("/family/husband/@id | /family/wife/@id");
         sourceTitleExpression = xpe.compile("/mysource/title | /source/title");
         familyReferenceExpression = xpe.compile("person/child_of_family | person/spouse_of_family");
         personReferenceExpression = xpe.compile("family/husband | family/wife | family/child");
         personReferenceAttributeExpression = xpe.compile("@child_of_family | @spouse_of_family");
         childOfFamilyExpression = xpe.compile("person/child_of_family");
         sourceCitationExpression = xpe.compile("person/source_citation | family/source_citation");
         sourceIdExpression = xpe.compile("@source_id");
         sourcesExpression = xpe.compile("person/name[@sources] | person/alt_name[@sources] | person/event_fact[@sources] | person/note[@sources] | family/name[@sources] | family/alt_name[@sources] | family/event_fact[@sources] | family/note[@sources]");
         placeAttributeExpression = xpe.compile("//@place | //@birthplace | //@deathplace | //@chrplace | //@burialplace");
         resultExpression = xpe.compile("/readGedcomData/result");
         eventExpression = xpe.compile("person/event_fact | family/event_fact");
         nameExpression = xpe.compile("person/name");
         altNameExpression = xpe.compile("person/alt_name");
         noteExpression = xpe.compile("person/note | family/note");
         //textExpression = xpe.compile("@text");
         //titleExpression = xpe.compile("@title");
      } catch (XPathExpressionException e)
      {
         throw new RuntimeException(e);
      }
   }

   private static final String stringXmlSection = "(<(mysource|person|family)>.*?</(mysource|person|family)>)|(<(mysource|person|family)/>)";
   public static final Pattern pPageContentXml = Pattern.compile(stringXmlSection, Pattern.DOTALL);

   /**
    * Should only be called after the titles have all been reserved, and the
    * person's page has been uploaded.
    *
    * @return the primary person for this GEDCOM, returns null if that person is undefined.
    * If a title has not yet been reserved for the person, then null will be returned as well
    */
   public String getPrimaryPersonTitle()
   {
      if (primaryPerson != null)
      {
         if(id2ReservedTitle.containsKey(primaryPerson))
         {
            return id2ReservedTitle.get(primaryPerson);
         }
      }
      return null;
   }

   public void parse(String filename) throws XPathException, XPathExpressionException,
         SAXException, IOException, GedcomXMLException
   {
      doc = Uploader.db.parse(new InputSource(new InputStreamReader(new FileInputStream(filename), "UTF-8")));
      Node primaryPersonNode = doc.getFirstChild().getAttributes().getNamedItem("primary_person");
      if (primaryPersonNode != null)
      {
         primaryPerson = primaryPersonNode.getNodeValue();
      }

      // First let's get the standardized places.
      NodeList places = (NodeList) placeExpression.evaluate(doc, XPathConstants.NODESET);
      for (int i = 0; i < places.getLength(); i++)
      {
         Node place = places.item(i);
         String id = place.getAttributes().getNamedItem("key").getNodeValue();
         String text = place.getAttributes().getNamedItem("text").getNodeValue();
         placeText2Id.put(text, id);
         Node titleNode = place.getAttributes().getNamedItem("title");  // can be null if the place standardizes to the empty string
         placeId2Standardized.put(id, titleNode == null ? text : titleNode.getNodeValue());
      }

      // We're going to go through all of the top level objects, and
      // we're going to add those objects to appropriate maps.
      NodeList pages = (NodeList) pageExpression.evaluate(doc, XPathConstants.NODESET);
      //reserveIds(pages);
      // We're going to load all of the pages into a set of maps:

      for (int i= 0; i < pages.getLength(); i++)
      {
         Node page = pages.item(i);
         int namespace = Integer.parseInt((String) namespaceExpression.evaluate(page, XPathConstants.STRING));
         String id = (String) idExpression.evaluate(page, XPathConstants.STRING);
         id2Page.put(id, page);
         String uid = (String) uidExpression.evaluate(page, XPathConstants.STRING);
         if (!PlaceUtils.isEmpty(uid))
         {
            id2Uid.put(id, uid);
         }
         id2Namespace.put(id, namespace);

         // Now depending on the namespace, we need to generate the
         // appropriate wiki title:
         switch (namespace)
         {
            case Utils.PERSON_NAMESPACE:
               personIds.add(id);
               break;
            case Utils.FAMILY_NAMESPACE:
               familyIds.add(id);
               break;
            case Utils.MYSOURCE_NAMESPACE:
               mysourceIds.add(id);
               break;
            default:
               break;
         }

         String excluded = (String) excludedExpression.evaluate(page, XPathConstants.STRING);
         if (!PlaceUtils.isEmpty(excluded) && excluded.trim().toLowerCase().equals("true"))
         {
            excludedIds.add(id);
         }
         String living = (String) livingExpression.evaluate(page, XPathConstants.STRING);
         if (!PlaceUtils.isEmpty(living) && living.trim().toLowerCase().equals("true"))
         {
            livingIds.add(id);
         }

         // We need to load the content node info:
         // First let's get the XML portion:
         String content = (String) contentExpression.evaluate(page, XPathConstants.STRING);
         parseContent(content, id);
      }
   }

   private void parseContent(String content, String id)
         throws SAXException, IOException, GedcomXMLException
   {
      Matcher mXmlContent = pPageContentXml.matcher(content);
      if (mXmlContent.find())
      {
         String xmlContentString = mXmlContent.group(0);
         String text = content.substring(mXmlContent.end()).trim();
         // Now let's parse this into an Xml node:
         Document contentDocument = Uploader.db.parse(new InputSource(new StringReader(xmlContentString)));
         id2Content.put(id, contentDocument);
         id2Text.put(id, text);
      } else
      {
         throw new GedcomXMLException("Could not find content section for id="+id);
      }
   }

   // This method goes trough the xml page objects
   //  and adds their wikiTitle to
   // the list of reservation requests to be sent to the
   // reservation server.
   public void addRequests(Uploader.RequestsList requestsList, String username)
         throws XPathExpressionException
   {
      GedcomElementWriter ew = new GedcomElementWriter("page");
      // First let's generate all of the titles for the people.
      for (String id : personIds)
      {
         if (!excludedIds.contains(id))
         {
            Document content = id2Content.get(id);
            Name name = new Name();
            name.parseFromPersonXML(content, livingIds.contains(id));
            String title = Utils.prepareWikiTitle(Person.getWikiTitle(name));
            id2Title.put(id, title);
            if (!matchedIds.contains(id))
            {
               ew.put("namespace", Integer.toString(Utils.PERSON_NAMESPACE));
               ew.put("title", title);
               ew.write(requestsList.getCurrentBuffer());
               ew.clear();
            }
         }
      }

      // Now let's take care of the families.
      for (String id : familyIds)
      {
         if (!excludedIds.contains(id) && !matchedIds.contains(id))
         {
            Document content = id2Content.get(id);

            ew.put("namespace", Integer.toString(Utils.FAMILY_NAMESPACE));

            // Now we need to get the ids of the husband and wife.
            String husbandTitle, wifeTitle;

            String husbandId = (String)husbandIdExpression.evaluate(content, XPathConstants.STRING);
            if (PlaceUtils.isEmpty(husbandId) || PlaceUtils.isEmpty(husbandTitle = id2Title.get(husbandId)))
            {
               husbandTitle = "Unknown";
            }
            String wifeId = (String) wifeIdExpression.evaluate(content, XPathConstants.STRING);
            if (PlaceUtils.isEmpty(wifeId) || PlaceUtils.isEmpty(wifeTitle = id2Title.get(wifeId)))
            {
               wifeTitle = "Unknown";
            }
            String title = Utils.prepareWikiTitle(husbandTitle + " and " + wifeTitle);
            id2Title.put(id, title);
            ew.put("title", title);
            ew.write(requestsList.getCurrentBuffer());
            ew.clear();
         }
      }

      // Rather than add reservation requests, let's just reserve
      // titles for the mysources at the GEDCOM level.
      Set <String> mysourceReservedTitle = new HashSet<String>();
      for (String id : mysourceIds)
      {
         if (!excludedIds.contains(id) && !matchedIds.contains(id))
         {
            Document content = id2Content.get(id);
            // Now we need to get the source title out.
            String title = username + '/' + sourceTitleExpression.evaluate(content, XPathConstants.STRING);
            title = Utils.prepareWikiTitle(title);
            id2Title.put(id, title);
            String reservedTitle = title;
            int i = 1;
            while (mysourceReservedTitle.contains(reservedTitle))
            {
               reservedTitle = title + " (" + i + ")";
               i++;
            }
            mysourceReservedTitle.add(reservedTitle);
            id2ReservedTitle.put(id, reservedTitle);
         }
      }
   }

   public void setReservedTitles (Map <Uploader.ReservationRequest, Queue<String>> reservations)
         throws XPathExpressionException, GedcomXMLException
   {
      for (Map.Entry<String, Node> entry : id2Page.entrySet())
      {
         String id = entry.getKey();
         Node page = entry.getValue();
         if (matchedIds.contains(id))
         {
            setTitle(id2ReservedTitle.get(id), page);
         }
         else if (!excludedIds.contains(id))
         {
            // Let's get the namespace:
            int namespace = Integer.parseInt((String) namespaceExpression.evaluate(page, XPathConstants.STRING));
            String title = id2Title.get(id);

            if (!PlaceUtils.isEmpty(title))
            {
               String reservedTitle;
               if (id2ReservedTitle.containsKey(id))
               {
                  reservedTitle = id2ReservedTitle.get(id);
               } else
               {
                  Uploader.ReservationRequest rr = new Uploader.ReservationRequest(namespace, title);
                  reservedTitle = reservations.get(rr).remove();
                  if (!Utils.isEmpty(reservedTitle))
                  {
                     id2ReservedTitle.put(id, reservedTitle);
                  }
               }
               if (!Utils.isEmpty(reservedTitle))
               {
                  setTitle(reservedTitle, page);
               } else
               {
                  throw new GedcomXMLException("Could not find a reserved title for page id: " + id);
               }
            } else
            {
               throw new GedcomXMLException("No title in map for id: " + id);
            }
         }
         // Let's get rid of the "potentialMatches" attribute.
         if (page.getAttributes().getNamedItem("potentialMatches") != null)
         {
            page.getAttributes().removeNamedItem("potentialMatches");
         }
         // let's get rid of the "problems" tag
         if (page.getAttributes().getNamedItem("problems") != null)
         {
            page.getAttributes().removeNamedItem("problems");
         }
      }
   }

   private void setTitle(String reservedTitle, Node page) {
      // Then let's set the reserved title in the XML.
      Node titleAttribute = doc.createAttribute("title");
      titleAttribute.setNodeValue(reservedTitle);
      page.getAttributes().setNamedItem(titleAttribute);
   }

   private static void removeNode(Node node)
   {
      node.getParentNode().removeChild(node);
   }

   // place2displayName maps standardized gedcom place names to refined display names.
   private Map<String, String> place2displayName = new HashMap<String, String>();

   public String getPlace2displayName(String text) {
      return place2displayName.get(text);
   }

   private Uploader uploader = null;

   public void updateContent (String placeServer) throws XPathExpressionException, IOException
   {
      updateCollectionContent(personIds, familyReferenceExpression, "Family", null, null);
      updateCollectionContent(familyIds, personReferenceExpression, "Person", personReferenceAttributeExpression, "Family");

      // Now let's update all of the places referenced.
      // The first step ensures that each place has the correct Place page titlr
      // based on any changes the user made to place matching.
      // It also accumulates a list of all places to get standard display names for.
      Set <String> placeNames = new HashSet <String>();
      for (Map.Entry<String, Document> entry : id2Content.entrySet())
      {
         NodeList placeReferences = (NodeList) placeAttributeExpression.evaluate(entry.getValue(),
               XPathConstants.NODESET);
         for (int i=0; i < placeReferences.getLength(); i++)
         {
            Node placeNode = placeReferences.item(i);
            String text = placeNode.getNodeValue();
            String id = placeText2Id.get(text);
            if (id != null)
            {
               String placeTitle = placeId2Standardized.get(id);
               if (placeTitle != null && !placeTitle.equals(text))
               {
                  placeNode.setTextContent(placeTitle + '|' + text);
               }
            }
            placeNames.add(placeNode.getNodeValue().replace('|','^'));     // use ^ as a stand-in for the pipe
         }
      }

      // The second step is to refine display names. This can't be done before
      // the first step, since the first step matches on the place text
      // as it was in the gedcom file.
      uploader.getPlaceDisplayNames(placeServer, placeNames, place2displayName);
      for (Map.Entry<String, Document> entry : id2Content.entrySet())
      {
         NodeList placeReferences = (NodeList) placeAttributeExpression.evaluate(entry.getValue(),
               XPathConstants.NODESET);
         for (int i=0; i < placeReferences.getLength(); i++)
         {
            Node placeNode = placeReferences.item(i);
            String text = placeNode.getNodeValue();
            String displayName = getPlace2displayName(text);
            if (displayName != null) {
               placeNode.setTextContent(displayName);
            }
         }
      }
   }

   private void updateCollectionContent(Set<String> collection, XPathExpression expression,
                                        String referenceNamespacePrefix, XPathExpression attributeExpression,
                                        String attributeNamespacePrefix) throws XPathExpressionException
   {
      // Now that we've reserved the IDs, we need
      // to replace all places that refer to them
      // with the appropriate reserved titles
      //
      // At the same time, if the reference is to
      // an excluded page, then we need to remove
      // the reference.
      for (String pageId : collection)
      {
         Document content = id2Content.get(pageId);

         NodeList references = (NodeList) expression.evaluate(content, XPathConstants.NODESET);
         for (int i= 0; i < references.getLength(); i++)
         {
            Node reference = references.item(i);
            String id = (String) idExpression.evaluate(reference, XPathConstants.STRING);
            if (id2ReservedTitle.containsKey(id))
            {
               Node titleNode = content.createAttribute("title");
               String title = id2ReservedTitle.get(id);
               if (matchedIds.contains(id))
               {
                  id2ExistingPageTitles.put(pageId, referenceNamespacePrefix + ":" + title);
               }
               titleNode.setNodeValue(title);
               reference.getAttributes().setNamedItem(titleNode);
            }
            if (attributeExpression != null)
            {
               NodeList attributes = (NodeList) attributeExpression.evaluate(reference, XPathConstants.NODESET);
               for (int j=0; j < attributes.getLength(); j++)
               {
                  Node attribute = attributes.item(j);
                  String refId = attribute.getNodeValue();
                  if (excludedIds.contains(refId)) {
                     // remove this attribute (the hard way)
                     NamedNodeMap attrs = reference.getAttributes();
                     attrs.removeNamedItem(attribute.getNodeName());
                  }
                  else if (id2ReservedTitle.containsKey(refId))
                  {
                     String title = id2ReservedTitle.get(refId);
                     if (matchedIds.contains(refId))
                     {
                        id2ExistingPageTitles.put(pageId, attributeNamespacePrefix + ":" + title);
                     }
                     attribute.setNodeValue(title);
                  }
               }
            }
         }
         updateSourceCitations(content);
      }
   }

   public void setExistingTitles() {
      for (Map.Entry<String, Node> entry : id2Page.entrySet())
      {
         String id = entry.getKey();
         if (!excludedIds.contains(id))
         {
            Node page = entry.getValue();

            // If this page has any existing titles, let's add them to the page
            // attribute.
            if (id2ExistingPageTitles.containsKey(id))
            {
               String existingTitlesString = "";
               for (String existingTitle : id2ExistingPageTitles.get(id))
               {

                  if (existingTitlesString.length() > 0)
                  {
                     existingTitlesString += '|';
                  }
                  existingTitlesString += existingTitle;
               }
               Node existingTitlesNode = doc.createAttribute("existing_titles");
               existingTitlesNode.setNodeValue(existingTitlesString);
               page.getAttributes().setNamedItem(existingTitlesNode);
            }
         }
      }
   }

   // TODO: This function doesn't seem to work; let's not apply it right now (perhaps it's not a good idea after all)
   //  -- didn't create a mysource with text
   //  -- removed title-only mysource ref's instead of turning them into title-only refs
   public void fixTitleOnlyMySourceReferences()
   {
      Map <String,String> titleOnlyMySource2Title = new HashMap<String,String>();
      for (Map.Entry<String, Document> entry : id2Content.entrySet())
      {
         String id = entry.getKey();
         if (this.id2Namespace.get(id) == Utils.MYSOURCE_NAMESPACE)
         {
            Document content = entry.getValue();

            // Now we need to determine if the mysource contains only a title
            // attribute.
            NodeList mysourceNodeChildren = content.getFirstChild().getChildNodes();
            boolean foundNonTitle = false;
            String foundTitle = null;
            for (int i =0; i < mysourceNodeChildren.getLength(); i++)
            {
               Node child = mysourceNodeChildren.item(i);
               if (child.getNodeType() == Node.ELEMENT_NODE)
               {
                  if (child.getNodeName().equals("title"))
                  {
                     foundTitle = child.getTextContent();
                  }
                  else
                  {
                     foundNonTitle = true;
                     break;
                  }
               }

            }

            if (foundTitle != null && !foundNonTitle)
            {
               excludePage(id2Page.get(id));
               excludedIds.add(id);
               titleOnlyMySource2Title.put(id, foundTitle);
            }
         }
      }

      // Ok. Now that we've found all of the applicable mysources, we just
      // need to fix all of the references.

      for (Map.Entry<String, Document> entry : id2Content.entrySet())
      {
         String id = entry.getKey();
         int namespace = id2Namespace.get(id);
         if (namespace == Utils.FAMILY_NAMESPACE ||
               namespace == Utils.PERSON_NAMESPACE)
         {
            Document content = entry.getValue();
            Node personFamilyNode = content.getFirstChild();
            NodeList childList = personFamilyNode.getChildNodes();
            for (int i= 0; i < childList.getLength(); i++)
            {
               Node node = childList.item(i);
               if (node.getNodeName().equals("source_citation"))
               {
                  // Now let's see if the title matches a mysource
                  String citationId = node.getAttributes().getNamedItem("source_id").getNodeValue();
                  if (titleOnlyMySource2Title.containsKey(citationId))
                  {
                     // Then we need to change the title by taking out the namespace reference,
                     // and just leaving the title as-is.
                     Node newTitleAttribute = content.createAttribute("title");
                     newTitleAttribute.setNodeValue(titleOnlyMySource2Title.get(citationId));
                     node.getAttributes().setNamedItem(newTitleAttribute);
                  }
               }
            }
         }
      }
   }

   public void prepareForGeneration()
         throws XPathExpressionException, TransformerException, GedcomXMLException
   {
      for (Map.Entry<String, Node> entry : id2Page.entrySet())
      {
         String id = entry.getKey();
         if (!excludedIds.contains(id))
         {
            Node page = entry.getValue();
            page.getAttributes().removeNamedItem("id");

            if (matchedIds.contains(id))
            {
               // Then we will basically tell the generator to
               // exclude this page from generation.
               excludePage(page);
            }
            // We need to update the content of this page.
            Document content = id2Content.get(id);

            if (livingIds.contains(id))
            {
               // Then we need to get rid of all events:
               NodeList events = (NodeList) eventExpression.evaluate(content, XPathConstants.NODESET);
               for (int i=0; i < events.getLength(); i++)
               {
                  removeNode(events.item(i));
               }

               // If this is a person, then we also need to get rid of the
               // name, etc.
               NodeList names = (NodeList) nameExpression.evaluate(content, XPathConstants.NODESET);
               if (names.getLength() > 0)
               {
                  // Let's get the surname.
                  String surname = null;
                  Node name;
                  for (int i=0; i < names.getLength() -1; i++)
                  {
                     name = names.item(i);
                     if (Utils.isEmpty(surname) && name.getAttributes().getNamedItem("surname") != null)
                     {
                        surname = name.getAttributes().getNamedItem("surname").getNodeValue();
                     }
                     removeNode(names.item(i));
                  }
                  name = names.item(names.getLength() -1);
                  if (Utils.isEmpty(surname) && name.getAttributes().getNamedItem("surname") != null)
                  {
                     surname = name.getAttributes().getNamedItem("surname").getNodeValue();
                  }
                  // Let's replace the node.
                  Node replacement = content.createElement("name");
                  Node givenNode = content.createAttribute("given");
                  givenNode.setNodeValue("Living");
                  replacement.getAttributes().setNamedItem(givenNode);
                  if (!Utils.isEmpty(surname))
                  {
                     Node surnameNode = content.createAttribute("surname");
                     surnameNode.setNodeValue(surname);
                     replacement.getAttributes().setNamedItem(surnameNode);
                  }
                  name.getParentNode().replaceChild(replacement, name);
               }

               removeNodes(content, altNameExpression);
               removeNodes(content, sourceCitationExpression);
               removeNodes(content, noteExpression);
            }

            // Let's remove all of the ids for person/family references
            if (content.getFirstChild().getNodeName().equals("person"))
            {
               removeIdReferences((NodeList) familyReferenceExpression.evaluate(content, XPathConstants.NODESET));
            } else if (content.getFirstChild().getNodeName().equals("family"))
            {
               NodeList personReferences = (NodeList) personReferenceExpression.evaluate(content, XPathConstants.NODESET);
               // Ok -- if the person referenced is living, then we need to remove everything except for
               // the title.
               for (int i=0; i < personReferences.getLength(); i++)
               {
                  Node personReference = personReferences.item(i);
                  // let's get the title, because that's the only thing we're going to
                  // preserve.
                  String refId = personReference.getAttributes().getNamedItem("id").getNodeValue();

                  if (excludedIds.contains(refId))
                  {
                     // We need to remove the family reference.
                     removeNode(personReference);
                  } else if (livingIds.contains(refId))
                  {
                     // Then let's remove everything except for the title.
                     int j = 0;
                     while (j < personReference.getAttributes().getLength())
                     {
                        String nodeName = personReference.getAttributes().item(j).getNodeName();
                        if (nodeName.equals("title"))
                        {
                           j++;
                        }else
                        {
                           personReference.getAttributes().removeNamedItem(personReference.getAttributes().item(j).getNodeName());
                        }
                     }
                     // Now let's add in the given = "Living"
                     Node givenNode = content.createAttribute("given");
                     givenNode.setNodeValue("Living");
                     personReference.getAttributes().setNamedItem(givenNode);
                  } else
                  {
                     personReference.getAttributes().removeNamedItem("id");
                  }
               }
            } else if (content.getFirstChild().getNodeName().equals("mysource") ||
                  content.getFirstChild().getNodeName().equals("source"))
            {
               // Let's get rid of the title nodes for all of the
               // source (mysource) top-level objects
               Node titleNode = (Node) sourceTitleExpression.evaluate(content, XPathConstants.NODE);
               if (titleNode != null)
               {
                  removeNode(titleNode);
               }
            }

            if (content.getFirstChild().getNodeName().equals("person") ||
                  content.getFirstChild().getNodeName().equals("family"))
            {
               // Let's remove mysource ids from source citations, and remove
               // any source citations and references to them if the mysource has
               // been excluded.
               NodeList sourceCitations = (NodeList) sourceCitationExpression.evaluate(content, XPathConstants.NODESET);
               // Let's redo the existingIds in the form of a stack:
               Set<String> removedIds = new HashSet<String>();
               // map from the old (higher numbered) ids to the new ones, because
               // we are moving some higher ids to replace numbered ones which are
               // lower.
               Map<String, String> high2Low = new HashMap<String, String>();

               // First let's remove the nodes that point to sources which are excluded.

               // First removed specifies the first removed / moved sourceCitation id
               // which hasn't already been replaced by a subsequent id.
               // -1 indicates that there are no previous citations which
               // haven't already been filled in by a subsequent id.
               int firstRemoved = -1;
               boolean removedCitation = false;
               for (int i=0; i < sourceCitations.getLength(); i++)
               {
                  Node sourceCitation = sourceCitations.item(i);
                  String citationId = (String) idExpression.evaluate(sourceCitation, XPathConstants.STRING);
                  String sourceId = (String) sourceIdExpression.evaluate(sourceCitation, XPathConstants.STRING);
                  if (excludedIds.contains(sourceId) ||
                        // If this source has nothing but a citation id, let's just go ahead and remove it.
                        (!Utils.isEmpty(citationId) && sourceCitation.getAttributes().getLength() <= 1 && Utils.isEmpty(sourceCitation.getTextContent())))
                  {
                     // We need to remove the citation, and all references to it.
                     removeNode(sourceCitation);
                     removedIds.add(citationId);
                     if (firstRemoved == -1)
                     {
                        firstRemoved = Integer.parseInt(citationId.substring(1));
                     }
                     removedCitation = true;
                  } else
                  {
                     if (!Utils.isEmpty(sourceId))
                     {
                        sourceCitation.getAttributes().removeNamedItem("source_id");
                     }

                     // Ok. This ID has not been removed, so let's move the id
                     // back to fill in any gaps immediately before it.
                     if (firstRemoved > 0)
                     {
                        // let's move this id to fill in the gap.
                        String firstRemovedId = "S" + firstRemoved;
                        sourceCitation.getAttributes().getNamedItem("id").setNodeValue(firstRemovedId);
                        high2Low.put(citationId, firstRemovedId);
                        firstRemoved++;
                     }
                  }
               }

               if (removedCitation)
               {
                  // Now we need to find all references to citation ids, and replace or remove them when necessary.
                  NodeList sourcesAttributeParents = (NodeList) sourcesExpression.evaluate(content, XPathConstants.NODESET);
                  for (int j=0; j < sourcesAttributeParents.getLength(); j++)
                  {
                     Node sourcesAttributeParent = sourcesAttributeParents.item(j);
                     Node sourcesAttribute = sourcesAttributeParent.getAttributes().getNamedItem("sources");
                     // We need to find all references to the citationId, and remove
                     // the reference.
                     // probably the simplest way of doing that is to split up the
                     // list, remove all references to the item, and then print the
                     // list back out.
                     if (!Utils.isEmpty(sourcesAttribute.getNodeValue()))
                     {
                        String [] split = sourcesAttribute.getNodeValue().split("\\s*,\\s*");
                        List <String> newIds = new ArrayList<String>();
                        for (String item : split)
                        {
                           if (!removedIds.contains(item))
                           {
                              if (high2Low.containsKey(item))
                              {
                                 newIds.add(high2Low.get(item));
                              } else
                              {
                                 newIds.add(item);
                              }
                           }
                        }

                        // Now let's print it back out:
                        if (newIds.size() > 0)
                        {
                           String newCitationList = newIds.get(0);
                           for (int i = 1; i < newIds.size(); i++)
                           {
                              newCitationList += ", " + newIds.get(i);
                           }
                           sourcesAttribute.setNodeValue(newCitationList);
                        } else
                        {
                           sourcesAttributeParent.getAttributes().removeNamedItem("sources");
                        }
                     }
                  }
               }
            }
         }
      }

      updatePageContent();
   }

   private static void removeNodes(Document content, XPathExpression expression) throws XPathExpressionException {
      NodeList removeNodes = (NodeList) expression.evaluate(content, XPathConstants.NODESET);
      for (int i=0; i < removeNodes.getLength(); i++)
      {
         removeNode(removeNodes.item(i));
      }
   }

   private void removeIdReferences(NodeList references) throws XPathExpressionException {
      for (int i= 0; i < references.getLength(); i++)
      {
         Node reference = references.item(i);
         String referenceId = (String) idExpression.evaluate(reference, XPathConstants.STRING);
         if (excludedIds.contains(referenceId))
         {
            // We need to remove the family reference.
            removeNode(reference);
         } else
         {
            // Id might not be there if the page was updated in phase 2
            // by the user.
            if (reference.getAttributes().getNamedItem("id") !=null)
            {
               reference.getAttributes().removeNamedItem("id");
            }
         }
      }
   }

   private void updateSourceCitations(Document content) throws XPathExpressionException {
      NodeList sourceCitations = (NodeList) sourceCitationExpression.evaluate(content, XPathConstants.NODESET);

      for (int i=0; i < sourceCitations.getLength(); i++)
      {
         Node sourceCitation = sourceCitations.item(i);
         String id = (String) sourceIdExpression.evaluate(sourceCitation, XPathConstants.STRING);

         if (id2ReservedTitle.containsKey(id))
         {
            Node titleNode = content.createAttribute("title");
            int namespace = id2Namespace.get(id);
            if (namespace == Utils.SOURCE_NAMESPACE)
            {
               // Then we need to replace the title with the matched source title:
               titleNode.setNodeValue("Source:" + id2ReservedTitle.get(id));
            } else if (namespace == Utils.MYSOURCE_NAMESPACE)
            {
               titleNode.setNodeValue("MySource:" + id2ReservedTitle.get(id));
            }
            sourceCitation.getAttributes().setNamedItem(titleNode);
         }
      }
   }
   private static TransformerFactory xformFactory = TransformerFactory.newInstance();
   public static Transformer idTransform;
   static
   {
      try
      {
         idTransform = xformFactory.newTransformer();
      } catch (TransformerConfigurationException e)
      {
         throw new RuntimeException (e);
      }
   }
   public void save(File outputFile) throws
         TransformerException, XPathExpressionException,
         FileNotFoundException, IOException,
         GedcomXMLException
   {
      updatePageContent();
      javax.xml.transform.Source input;
      Result output;
      // Ok, now that we've reset the content values of all the nodes,
      // let's go ahead and write the entire document out to a file.
      input = new DOMSource(doc);
      FileOutputStream outputStream = new FileOutputStream(outputFile);
      output = new StreamResult(new PrintWriter(outputStream));
      idTransform.transform(input, output);
      outputStream.close();
   }

   private Transformer updatePageContent()
         throws XPathExpressionException, GedcomXMLException
   {
      for (Map.Entry<String, Node> entry : id2Page.entrySet())
      {
         String id = entry.getKey();
         Node page = entry.getValue();
         // Now that we've serialized the content, let's go ahead
         // and set the pages content.
         Node contentNode = (Node) contentExpression.evaluate(page, XPathConstants.NODE);
         if (id2Text.containsKey(id))
         {
            StringBuffer xmlBuffer = new StringBuffer();
            Node firstChild = id2Content.get(id).getFirstChild();
            ElementWriter ew = new GedcomElementWriter(firstChild.getNodeName());
            StringBuffer subBuffer = new StringBuffer();
            for (int i=0; i < firstChild.getChildNodes().getLength(); i++)
            {
               Node child = firstChild.getChildNodes().item(i);
               if (child.getNodeType() != Node.TEXT_NODE)
               {
                  serializeContent(child, subBuffer);
               }
            }
            ew.setSubXML(subBuffer.toString());
            ew.write(xmlBuffer);
            String additionalText = id2Text.get(id);
            if (additionalText.startsWith("\n"))
            {
               additionalText = additionalText.substring(1);
            }
            if (livingIds.contains(id))
            {
               xmlBuffer.append("<show_sources_images_notes/>");
            } else
            {
               xmlBuffer.append(additionalText.trim());
            }
            contentNode.setTextContent(xmlBuffer.toString().trim());
         } else
         {
            throw new GedcomXMLException("id2Text doesn't contain: " + id);
         }
      }
      return idTransform;
   }

   private void recalculateFamilyPersonReferences() throws XPathExpressionException
   {
      for (String familyId : familyIds)
      {
         Document content = id2Content.get(familyId);
         NodeList personReferences = (NodeList) personReferenceExpression.evaluate(content, XPathConstants.NODESET);
         for (int i =0; i < personReferences.getLength(); i++)
         {
            Node personReference = personReferences.item(i);
            String id = personReference.getAttributes().getNamedItem("id").getNodeValue();
            // First let's remove all of the attributes.
            int j = 0;
            while (j < personReference.getAttributes().getLength())
            {
               String nodeName = personReference.getAttributes().item(j).getNodeName();
               if (nodeName.equals("id"))
               {
                  j++;
               } else
               {
                  personReference.getAttributes().removeNamedItem(nodeName);
               }
            }

            // now let's add all of the attributes back in that we need to.
            Document personContent = id2Content.get(id);

            // Let's put the name elements:

            Name name = new Name();
            name.parseFromPersonXML(personContent, livingIds.contains(id));
            Node att;
            if (!Utils.isEmpty(name.getPrefix()))
            {
               att = content.createAttribute("title_prefix");
               att.setNodeValue(name.getPrefix());
               personReference.getAttributes().setNamedItem(att);
            }
            if (!Utils.isEmpty(name.getSuffix()))
            {
               att = content.createAttribute("title_suffix");
               att.setNodeValue(name.getSuffix());
               personReference.getAttributes().setNamedItem(att);
            }
            if (!Utils.isEmpty(name.getGiven()))
            {
               att = content.createAttribute("given");
               att.setNodeValue(name.getGiven());
               personReference.getAttributes().setNamedItem(att);
            }
            if (!Utils.isEmpty(name.getSurname()))
            {
               att = content.createAttribute("surname");
               att.setNodeValue(name.getSurname());
               personReference.getAttributes().setNamedItem(att);
            }

            // We need to make sure that they are added in the right order,
            // First let's loop through the events, and get any birth or death dates.
            NodeList events = (NodeList)eventExpression.evaluate(personContent, XPathConstants.NODESET);
            for (j=0; j < events.getLength(); j++)
            {
               Node event = events.item(j);
               String typeStr = event.getAttributes().getNamedItem("type").getNodeValue();
               String typeName = null;

               if (typeStr.equals("Birth"))
               {
                  typeName = "birth";
               } else if (typeStr.equals("Christening"))
               {
                  typeName = "chr";
               } else if (typeStr.equals("Death"))
               {
                  typeName = "death";
               } else if (typeStr.equals("Burial"))
               {
                  typeName = "burial";
               }

               if (typeName!= null)
               {
                  Node dateNode = event.getAttributes().getNamedItem("date");
                  if (dateNode != null)
                  {
                     att = content.createAttribute(typeName + "date");
                     att.setNodeValue(dateNode.getNodeValue());
                     personReference.getAttributes().setNamedItem(att);
                  }
                  Node placeNode = event.getAttributes().getNamedItem("place");
                  if (placeNode != null)
                  {
                     att = content.createAttribute(typeName + "place");
                     att.setNodeValue(placeNode.getNodeValue());
                     personReference.getAttributes().setNamedItem(att);
                  }
               }
            }

            if (!personReference.getNodeName().startsWith("child"))
            {
               Node childOfFamily = (Node) childOfFamilyExpression.evaluate(personContent, XPathConstants.NODE);
               if (childOfFamily != null)
               {
                  att = content.createAttribute("child_of_family");
                  att.setNodeValue(childOfFamily.getAttributes().getNamedItem("id").getNodeValue());
                  personReference.getAttributes().setNamedItem(att);
               }
            }
         }
      }
   }

   public static String serializeNode(Node node) throws TransformerException
   {
      javax.xml.transform.Source input;
      Result output;
      input = new DOMSource(node);
      StringWriter contentStringWriter = new StringWriter();
      output = new StreamResult(contentStringWriter);
      idTransform.transform(input, output);
      String returnValue = contentStringWriter.toString();
      returnValue = returnValue.replaceAll("<\\?xml\\s+version=\"1.0\"\\s+encoding=\"UTF-8\"(\\s+standalone=\"no\")?\\?>", "");
      return returnValue;
   }

   public static void serializeContent(Node node, StringBuffer buf)
   {
      ElementWriter ew = new GedcomElementWriter(node.getNodeName());
      if (node.getAttributes() != null)
      {
         for (int i=0; i < node.getAttributes().getLength(); i++)
         {
            Node attribute = node.getAttributes().item(i);
            ew.put(attribute.getNodeName(), attribute.getNodeValue());
         }
      }
      StringBuffer subBuf = new StringBuffer();
      StringBuffer subText = new StringBuffer();
      for (int i=0; i < node.getChildNodes().getLength(); i++)
      {
         Node childNode = node.getChildNodes().item(i);
         if (childNode.getNodeType() == Node.TEXT_NODE)
         {
            subText.append(childNode.getTextContent().trim());
         } else
         {
            serializeContent(childNode, subBuf);
         }
      }
      if (node.getNodeValue() != null && !node.getNodeValue().trim().equals(""))
      {
         subBuf.append(node.getNodeValue());
      }
      ew.setSubXML(subBuf.toString());
      ew.setSubText(subText.toString());
      ew.write(buf);
   }

   /**
    * Constructor
    * @param uploader uploader object which is controlling this GEDCOM
    */
   public GedcomXML(Uploader uploader)
   {
      this.uploader = uploader;
   }

   /**
    * Alternate constructor for fix scripts
    */ 
   public GedcomXML()
   {
   }

}
