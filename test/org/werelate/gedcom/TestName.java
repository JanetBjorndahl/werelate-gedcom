package org.werelate.gedcom;

import junit.framework.TestCase;
import org.werelate.util.Utils;

/**
 * Created by IntelliJ IDEA.
 * User: npowell
 * Date: Jan 30, 2007
 * Time: 10:28:11 AM
 * To change this template use File | Settings | File Templates.
 */
public class TestName extends TestCase {
   Name name = null;
   protected void setUp() throws Exception {
      super.setUp();
      Name.NameLogger.instantiate("/home/npowell/logs/gedcom_names.log");
      name = new Name();
   }

   public void testFixCapitalization() {
      assertEquals("McPearson", Utils.capitalizeTitleCase("mcpearson", true, true));
      assertEquals("MacPearson", Utils.capitalizeTitleCase("macpearson", true, true));
      assertEquals("McFarland", Utils.capitalizeTitleCase("MCFARLAND", true, true));
      assertEquals("M", Utils.capitalizeTitleCase("m", true, true));
      assertEquals("", Utils.capitalizeTitleCase("", true, true));
      assertEquals(null, Utils.capitalizeTitleCase(null, true, true));
      assertEquals("mCFarland", Utils.capitalizeTitleCase("mCFarland", true, true));
      assertEquals("Blane", Utils.capitalizeTitleCase("blane", true, true));
      assertEquals("Blane", Utils.capitalizeTitleCase("BLANE", true, true));
      assertEquals("Blane", Utils.capitalizeTitleCase("Blane", true, true));
      assertEquals("O'Reilly", Utils.capitalizeTitleCase("O'REILLY", true, true));
      assertEquals("Death Of Nel", Utils.capitalizeTitleCase("death of nel", true, true));
      assertEquals("St.John", Utils.capitalizeTitleCase("st.john", true, true));
      assertEquals("Sally-Forth", Utils.capitalizeTitleCase("SALLY-FORTH", true, true));
      assertEquals("Nathaniel Edward", Utils.capitalizeTitleCase("nathaniel EDWARD ", true, true));
   }

   public void testKingName () {
      name.setName("Ethelred \"The /Redeless\"/, II,King Of England", null);
      verify(null, "Ethelred \"The King Of England", "Redeless\"", "II");
   }

   public void testPrefixSuffixPunctuation () {
      name.setName("William Galloway /Ice/, Sr.", null);
      verify(null, "William Galloway", "Ice", "Sr.");
   }

   public void testQuoteName() {
      name.setName("Eleanor \"Maggie\" /Fort/", null);
      verify(null, "Eleanor \"Maggie\"", "Fort", null);
   }

   public void testCommaName () {
      name.setName("Powell, Nathaniel Edward", null);
      verify(null, "Nathaniel Edward", "Powell", null);
   }

   public void testRegularName () {
      name.setName("Mr. nathaniel EDWARD POWELL Jr", null);
      verify("Mr.", "nathaniel EDWARD", "POWELL", "Jr");
   }

   public void testCapsName () {
      name.setName("Nathaniel Edward POWELL", null);
      verify(null, "Nathaniel Edward", "POWELL", null);
   }

   private void verify (String prefix, String given, String surname, String suffix)
   {
      assertEquals(prefix, name.getPrefix());
      assertEquals(given, name.getGiven());
      assertEquals(surname, name.getSurname());
      assertEquals(suffix, name.getSuffix());
   }
}
