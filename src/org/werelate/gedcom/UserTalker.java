package org.werelate.gedcom;

import org.werelate.util.PageEdit;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.regex.Pattern;
import java.net.URLEncoder;
import java.io.UnsupportedEncodingException;
import javax.mail.*;
import javax.mail.internet.*;
import java.util.*;
import java.io.*;

/**
 * Created by IntelliJ IDEA.
 * User: npowell
 * Date: Mar 19, 2007
 * Time: 2:41:35 PM
 * Sends rote messages to user's talk pages
 */
public class UserTalker extends PageEdit {
   Logger logger = LogManager.getLogger("org.werelate.gedcom");
   private static final String [] INPUTS = {
      "wpEdittime",
      "wpEditToken"
   };
   protected String [] getINPUTS() {
      return INPUTS;
   }
   private static final Pattern [] INPUT_VALUE_PATTERNS = {
      PageEdit.pWPEditTime,
      PageEdit.pWPEditToken
   };
   protected Pattern [] getINPUT_VALUE_PATTERNS() {
      return INPUT_VALUE_PATTERNS;
   }

   private static final String [] TEXT_AREAS = {
   };
   protected String [] getTEXT_AREAS() {
      return TEXT_AREAS;
   }

   private static final Pattern [] TEXT_AREA_VALUE_PATTERNS = {
   };

   protected Pattern [] getTEXT_AREA_VALUE_PATTERNS() {
      return TEXT_AREA_VALUE_PATTERNS;
   }

   protected String getGETURL(String wrTitle) {
      try
      {
         return WERELATE_URL + "/w/index.php?title=User_talk:" + URLEncoder.encode(wrTitle, "UTF-8") + "&action=edit&section=new";
      } catch (UnsupportedEncodingException e)
      {
         throw new RuntimeException(e);
      }
   }

   protected String getPOSTURL(String wrTitle)  {
      try
      {
         return WERELATE_URL + "/w/index.php?title=User_talk:" + URLEncoder.encode(wrTitle, "UTF-8") + "&action=submit&section=new";
      } catch (UnsupportedEncodingException e)
      {
         throw new RuntimeException(e);
      }
   }
   String sysopEmail = null;
   Properties properties = null;

   public UserTalker(Properties properties)
   {
      super();
      this.properties = properties;
      this.sysopUsername = properties.getProperty("sysop");
      if (sysopUsername == null)
      {
         throw new RuntimeException("sysop is not set in the properties.");
      }
      this.sysopEmail = properties.getProperty("sysop_mail");
      if (sysopEmail == null)
      {
         throw new RuntimeException("sysop_mail is not set in the properties.");
      }
   }

   public void sendOverlapMessage (String userName, String gedcomName)
   {
      sendMessage(userName, gedcomName + " appears to overlap a previously-imported GEDCOM",
            "{{subst:overlapmsg}}\n\n:");
      Wrgedcom (userName, gedcomName);
   }

   public void sendErrorMessage(String userName, String gedcomName, int gedID)
   {
      sendMessage(userName, "Error importing " + gedcomName,
         "{{subst:errormsg}}\n\n:"
      );
      Wrgedcom (userName, gedcomName);
      String message = gedID + " gedcom had an error";
      sendEmail(message, message);
   }

   public void sendNotGedcom (String userName, String fileName)
   {
      sendMessage(userName, fileName + " does not appear to be a GEDCOM",
            "{{subst:notgedcom}}\n\n:");
   }

   public void sendSuccessfulMessage(String userName, String gedcomName, String treeName)
   {
      try
      {
         sendMessage(userName, gedcomName + " Imported Successfully",
               "{{Subst:Successmsg|"+URLEncoder.encode(userName, "UTF-8")+"|"+URLEncoder.encode(treeName, "UTF-8")+"}}\n\n:");
      } catch (UnsupportedEncodingException e)
      {
         throw new RuntimeException(e);
      }
   }

   public void sendGedcomReviewMessage(String userName, String gedcomName, long gedID)
   {
      sendMessage(userName, "Next step: Review your GEDCOM",
            "{{Subst:GedcomReviewMessage|" + gedID + "|"+gedcomName+"}}\n\n:");
   }

   private String sysopUsername;

   public void Wrgedcom(String gedcomOwner, String gedcomName)
   {
      sendMessage("Wrgedcom", "There is a problem importing " + gedcomName,
            "There was a problem importing " + gedcomName +
            ", owned by [[User Talk:" + gedcomOwner + "|" + gedcomOwner + "]]\n -~~~~");
   }

   private void sendMessage(String userName, String summary, String message)
   {

      get(userName);
      // This is the title of the message
      setValue("wpSummary", summary);
      // Body of the message
      setValue("wpTextbox1", message);
      logger.info("About to send message to " + userName + " with summary: " + summary);
      post();
      logger.info("Done sending message to " + userName + " with summary: " + summary);
   }

   private void sendEmail(String subject, String msg)
   {
      if (sysopEmail == null || "".equals(sysopEmail)) {
         return;
      }
      //Here, no Authenticator argument is used (it is null).
      //Authenticators are used to prompt the user for user
      //name and password.
      Session session = Session.getDefaultInstance(properties, null );
      MimeMessage message = new MimeMessage( session );
      try {
         //the "from" address may be set in code, or set in the
         //config file under "mail.from" ; here, the latter style is used
         //message.setFrom( new InternetAddress(aFromEmailAddr) );
         message.addRecipient(
            Message.RecipientType.TO, new InternetAddress(sysopEmail));
         message.setSubject( subject );
         message.setText( msg );
         Transport.send( message );
      } catch (MessagingException ex){
         logger.error("Cannot send email. " + ex);
      }
   }
}
