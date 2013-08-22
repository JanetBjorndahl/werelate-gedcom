package org.werelate.util;

import java.util.*;
import java.io.PrintWriter;
import java.io.PrintStream;

/**
 * General class to collect counts 
 */
public class CountsCollector {
   HashMap hm;

   public int size() {
       return hm.size();
   }

   private class ValueComparator implements Comparator {
      public int compare(Object o1, Object o2) {
         Map.Entry me1 = (Map.Entry)o1;
         Map.Entry me2 = (Map.Entry)o2;
         if (((Counter)me1.getValue()).count < ((Counter)me2.getValue()).count ||
             (((Counter)me1.getValue()).count == ((Counter)me2.getValue()).count &&
              ((String)me1.getKey()).compareTo((String)me2.getKey()) < 0)) {
            return 1;
         }
         else {
            return -1;
         }
      }

      public boolean equals(Object o1, Object o2) {
         Map.Entry me1 = (Map.Entry)o1;
         Map.Entry me2 = (Map.Entry)o2;
         return (((Counter)me1.getValue()).count == ((Counter)me2.getValue()).count &&
                 ((String)me1.getKey()).equals((String)me2.getKey()));
      }
   }

   private class KeyComparator implements Comparator {
      public int compare(Object o1, Object o2) {
         Map.Entry me1 = (Map.Entry)o1;
         Map.Entry me2 = (Map.Entry)o2;
         return (((String)me1.getKey()).compareTo((String)me2.getKey()));
      }

      public boolean equals(Object o1, Object o2) {
         Map.Entry me1 = (Map.Entry)o1;
         Map.Entry me2 = (Map.Entry)o2;
         return (((String)me1.getKey()).equals((String)me2.getKey()));
      }
   }

   public CountsCollector() {
      hm = new HashMap();
   }

   public void add(String key) {
      add(key, 1);
   }

   public void add(String key, int count) {
      if (key != null) {
         Counter c = (Counter)hm.get(key);
         if (c == null) {
            c = new Counter();
            hm.put(key, c);
         }
         c.count += count;
      }
   }

   public void remove(String key) {
      hm.remove(key);
   }

   public int getCount(String key) {
      Counter c = (Counter)hm.get(key);
      if (c == null) {
         return 0;
      }
      else {
         return c.count;
      }
   }

   public Set getKeys() {
      return hm.keySet();
   }

   public void addAll(Set keys) {
      Iterator iter = keys.iterator();
      while (iter.hasNext()) {
         add((String)iter.next());
      }
   }

   /**
    * Returns the collection sorted and filtered
    * @param byKey if true, sort by key; otherwise sort descending by count
    * @param minCount only include items >= minCount
    */
   public SortedSet getSortedSet(boolean byKey, int minCount) {
   	  Comparator comp;
      if (byKey) {
         comp = new KeyComparator();
      }
      else {
         comp = new ValueComparator();
      }
   	  SortedSet ss = new TreeSet(comp);
   	  Iterator iter = hm.entrySet().iterator();
   	  while (iter.hasNext()) {
   	  	Map.Entry entry = (Map.Entry)iter.next();
   	  	if (((Counter)entry.getValue()).count >= minCount) {
   	  		ss.add(entry);
   	  	}
   	  }
   	  return ss;
   }
   
   /**
    * Write the stats in sorted order
    * @param byKey if true, sort by key value; otherwise sort by count
    * @param minCount write out only those entries having count >= minCount
    * @param writer PrintWriter to write to
    */
   public void writeSorted(boolean byKey, int minCount, PrintWriter writer) {
      // add all entries in the hash map appearing at least minCount times into the sorted set
      SortedSet ss = getSortedSet(byKey, minCount);
      Iterator iter = ss.iterator();
      while (iter.hasNext()) {
         Map.Entry entry = (Map.Entry)iter.next();
         writer.println((String)entry.getKey() + "\t" + entry.getValue());
      }
      writer.flush();
   }
}
