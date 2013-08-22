package org.werelate.gedcom;

import junit.framework.TestCase;

/**
 * Created by IntelliJ IDEA.
 * User: npowell
 * Date: Feb 7, 2007
 * Time: 6:29:42 PM
 * To change this template use File | Settings | File Templates.
 */
public class TestPerson extends TestCase {
   public void testCutOffSurname() {
      assertEquals("-nathan e powell", Person.cutOffSurname("-nathan e powell"));
      assertEquals("blah-nathan e", Person.cutOffSurname("blah-nathan e powell"));
   }
   public void testIsDateThatOld() {
      assertTrue(Person.isDateThatOld("February 22, 1897", 110));
      assertTrue(Person.isDateThatOld("February 23, 1897", 110));
      assertTrue(!Person.isDateThatOld("February 24, 1897", 110));
   }
}
