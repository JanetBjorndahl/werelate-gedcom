package org.werelate.util;

/**
 * Created by IntelliJ IDEA.
 * User: dallan
 */
public class Counter {
   int count;

   public Counter() {
      count = 0;
   }

   public void increment() {
      count++;
   }

   public String toString() {
      return String.valueOf(count);
   }
}
