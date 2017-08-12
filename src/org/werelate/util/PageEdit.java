package org.werelate.util;

import org.apache.commons.httpclient.cookie.CookiePolicy;
import org.apache.log4j.Logger;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;

import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.*;
import java.io.IOException;

/**
 * Created by IntelliJ IDEA.
 * User: npowell
 * Date: Apr 24, 2006
 * Time: 4:33:48 PM
 * <p/>
 * This is an abstract class designed to be the interface through
 * which a Page is edited through the HTTP interface.
 */
public abstract class PageEdit implements PageEditInterface {
   protected static String fromFile = null;

   protected static final Pattern pWPEditTime = Pattern.compile(
         "\\<input type='hidden' value=\"([^\\>]*?)\" name=\"wpEdittime\" /\\>");
   protected static final Pattern pWPEditToken = Pattern.compile(
         "\\<input type='hidden' value=\"([^\\>]*?)\" name=\"wpEditToken\" /\\>");
   protected static final Pattern pTextArea = Pattern.compile(
         "\\<textarea.*?wpTextbox1.*?\\>(.*?)\\</textarea\\>", Pattern.DOTALL);

   /**
    * @param WERELATE_URL the base WeRelate URL if different
    *                     from the default www.werelate.org
    */
   public static void setWERELATE_URL(String WERELATE_URL) {
      PageEdit.WERELATE_URL = WERELATE_URL;
   }

   public static void setLoginRequired(boolean newValue)
   {
      LOGIN_REQUIRED = newValue;
   }

   protected static String WERELATE_URL = null;
   private static Boolean LOGIN_REQUIRED = true;
   private static String WERELATE_AGENT = null;
   private static String AGENT_PASSWD = null;
   private static int RETRY_WAIT_MILIS = 20000;
   private static Logger logger = Logger.getRootLogger();

   public static void SetWerelateAgent(Properties userPasswordProperties)
   {
      WERELATE_AGENT = userPasswordProperties.getProperty("wiki_username");
      AGENT_PASSWD = userPasswordProperties.getProperty("wiki_password");
      WERELATE_URL = "https://"+userPasswordProperties.getProperty("wiki_server");
   }

   public static class BadPageException extends Exception {

   }

   public String getWrText() {
      return wrText;
   }

   //textareas

   // These are implemented in the subclass so that
   // this class' methods can be used for more than one
   // type of edit page.
   protected abstract String [] getINPUTS();

   protected abstract Pattern [] getINPUT_VALUE_PATTERNS();

   protected abstract String [] getTEXT_AREAS();

   protected abstract Pattern [] getTEXT_AREA_VALUE_PATTERNS();

   private String wrText;
   protected String wrTitle;
   private boolean goodPage = true;

   private HttpClient client = null;

   public PageEdit() {
      resetClient();
   }
   
   public void resetClient() {
      client = new HttpClient();
      client.getParams().setParameter("http.protocol.content-charset", "UTF-8");
      client.getParams().setParameter("http.socket.timeout", 600000);
      client.getParams().setParameter("http.connection.timeout", 600000);
      client.getParams().setCookiePolicy(CookiePolicy.RFC_2109);
   }

   public HttpClient getClient() {
      return client;
   }

   // This map holds all the values of all the tags that
   // are parsed out of the HTML file as specified by getINPUT_VALUE_PATTERNS
   // and getTEXT_AREA_VALUE_PATTERNS
   protected Map<String, String> name2Val = new HashMap<String, String>();
   boolean loggedIn = false;

   // This function searches through the wrText (which is
   // really the HTML file to edit the WR article as returned
   // from the HTTP server). It searches for the form fields
   // as specified by argument patterns, and then sets the
   // name2val with the respective names of the fields as the
   // key.
   private boolean searchPatterns(
         String [] inputs, Pattern [] patterns) throws BadPageException {
      Matcher m;
      for (int i = 0; i < inputs.length; i++) {
         m = patterns[i].matcher(wrText);
         if (m.find()) {
            this.setValue(inputs[i], /*m.group(1)*/ Utils.unencodeXML(m.group(1)));
         } else {
            logger.error(inputs[i] + " not found for " + wrTitle);
            logger.error(wrText);
            throw new BadPageException();
         }
      }
      return true;
   }

   protected abstract String getGETURL(String wrTitle);

   /**
    * Get's the page and parses through the values
    * of the form for the title specified.
    *
    * @param title of place that we should get from server.
    * @return true if successful, false otherwise.
    */
   public boolean get(String title) {
      // This wrapper does some retries if we've
      // failed to connect
      for (int i = 0; i < 5; i++) {
         if (getHTTP(title)) {
            return true;
         } else {
            // 200 seconds
            Utils.sleep(RETRY_WAIT_MILIS);
         }
      }
      System.exit(111);
      return false;
   }

   private boolean getHTTP(String title) {
      if (!loggedIn) if (!(loggedIn = login())) return false;
      this.wrTitle = title;
      String url = getGETURL(wrTitle);
      //logger.warn("Encoded URL:\n" + url);
      return getUrl(url);
   }

   private boolean getUrl(String url) {
      HttpMethod m = new GetMethod(url);
      try {
         name2Val.clear();
         if (Utils.isEmpty(fromFile)) {
            client.executeMethod(m);
            wrText = m.getResponseBodyAsString();
            if(wrText.contains("sign in</a> to edit pages.") ||
                  wrText.matches("Sign-in\\s+required\\s+to\\s+edit"))
            {
               if(!(loggedIn = login())) return false;
               client.executeMethod(m);
               wrText = m.getResponseBodyAsString();
            }
         } else
         {
            wrText = fromFile;
         }
         try {
            return searchPatterns();
         } catch (BadPageException e) {
            logger.warn(e);
            logger.info("Re-logging in and retrying");

            goodPage = false;
            return true;
         }
      } catch (HttpException e) {
         logger.error("There was an HttpException when executing this url: " + url);
         logger.error(e);
         loggedIn = false;
         return false;
      } catch (IOException e) {
         logger.error("There was an IOException when executing this url: " + url);
         logger.error(e);
         loggedIn = false;
         return false;
      } finally {
         m.releaseConnection();
      }
   }

   public boolean logOut()
   {
      return getUrl(PageEdit.WERELATE_URL + "/w/index.php?title=Special:Userlogout&returnto=Main_Page");
   }

   public boolean searchPatterns() throws BadPageException {
      boolean rval;
      rval = searchPatterns(getINPUTS(), getINPUT_VALUE_PATTERNS());
      rval = rval && searchPatterns(getTEXT_AREAS(), getTEXT_AREA_VALUE_PATTERNS());
      goodPage = rval;
      return rval;
   }

   public boolean setLogin() {
      if (LOGIN_REQUIRED)
      {
         return setLoggedIn(login());
      } else
      {
         return setLoggedIn(true);
      }
   }

   public boolean login() {
      String url = WERELATE_URL + "/w/index.php?title=Special:Userlogin&action=submitlogin&type=login";
      logger.info("Logging in: " + url);
      PostMethod m = new PostMethod(url);
      m.getParams().setCookiePolicy(CookiePolicy.RFC_2109);
      NameValuePair [] nvp = {
            new NameValuePair("wpName", PageEdit.WERELATE_AGENT),
            new NameValuePair("wpPassword", PageEdit.AGENT_PASSWD),
            new NameValuePair("wpLoginattempt", "Log in")
      };
      m.setRequestBody(nvp);
      try {
         client.executeMethod(m);
         if (m.getStatusCode() == 302) {
            url = m.getResponseHeader("Location").getValue();
            m = new PostMethod(url);
            m.setRequestBody(nvp);
            client.executeMethod(m);
         }
         if (m.getStatusCode() == 200) {
            String returnText = new String(m.getResponseBody());
            if (returnText.indexOf("Login successful") != -1) {
               logger.info("In PageEdit, I was able to login successfully");
               return true;
            } else {
               logger.error("There was a problem logging in. Here is the text:\n\n" + returnText);
               return false;
            }
         } else return false;
      } catch (HttpException e) {
         logger.error("There was an HttpException when attempting to log in");
         logger.error(e);
         return false;
      } catch (IOException e) {
         logger.error("There was an IOException when executing this url: " + url);
         logger.error(e);
         return false;
      } finally {
         m.releaseConnection();
      }
   }

   protected abstract String getPOSTURL(String wrTitle);

   /**
    * Posts the variables in name2Val map
    * to the URL which is gotten from
    * getPOSTURL()
    *
    * @return true if successful, false otherwise
    */
   public boolean post() {
      // This wrapper does some retries if
      // we've failed to connect.
      for (int i = 0; i < 5; i++) {
         if (postHTTP()) {
            return true;
         } else {
            // 200 seconds
            Utils.sleep(RETRY_WAIT_MILIS);
         }
      }
      System.exit(111);
      return false;
   }

   public boolean isLoggedIn() {
      return loggedIn;
   }

   public boolean setLoggedIn(boolean set) {
      return loggedIn = set;
   }

   private boolean postHTTP() {
      if (!loggedIn) if (!(loggedIn = login())) return false;
      String url = getPOSTURL(wrTitle);
      //logger.debug("Encoded URL:\n" + url);
      name2Val.put("wpSave", "Save page");
      PostMethod m = new PostMethod(url);
      //m.setFollowRedirects(true);
      NameValuePair [] nvp = new NameValuePair [this.name2Val.size()];
      Set entries = name2Val.entrySet();
      Iterator i = entries.iterator();
      Map.Entry curr;
      for (int j = 0; i.hasNext(); j++) {
         curr = (Map.Entry) i.next();
         nvp[j] = new NameValuePair((String) curr.getKey(), (String) curr.getValue());
      }

      m.setRequestBody(nvp);
      try {
         client.executeMethod(m);
         wrText = new String(m.getResponseBody());
         if (m.getStatusCode() == 302) {
            url = m.getResponseHeader("Location").getValue();
            m = new PostMethod(url);
            m.setRequestBody(nvp);
            client.executeMethod(m);
            wrText = new String(m.getResponseBody());
            if (!wrText.contains("User talk:"))
            {
               logger.warn("The User talk: title is not present.");
            }
            //logger.warn("This is what I got back from the post message:\n" + wrText);
            //logger.warn("I posted the update of " + wrTitle);
         }
         if (m.getStatusCode() != 200) {
            logger.error("There was an unexpected status "+m.getStatusCode()+"when executing this url: "+url);
         }
         return true;
      } catch (HttpException e) {
         logger.error("There was an HttpException when executing this url: " + url);
         logger.error(e);
         loggedIn = false;
         return false;
      } catch (IOException e) {
         logger.error("There was an IOException when executing this url: " + url);
         logger.error(e);
         loggedIn = false;
         return false;
      } finally {
         m.releaseConnection();
      }
   }

   /**
    * Get's the value of the form variable
    *
    * @param name of the form variable
    * @return value of the "name" form element
    */
   public String getValue(String name) {
      if (goodPage)
         return this.name2Val.get(name);
      else
         return "";
   }

   public void setValue(String key, String value) {
      this.name2Val.put(key, value.trim()/*Utils.unencodeXML(value.trim())*/);
   }
}
