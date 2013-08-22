package org.werelate.util;

import java.io.*;
import java.util.ArrayList;
import java.util.Set;
import java.util.HashSet;

/**
 * Reads a text file into an array of strings, ignoring blank lines and lines that are commented out with a
 * '#' character.
 */
public class LineReader {
    final String[] lines;

    public LineReader(File f) {
        this(f, "utf8");
    }

    public LineReader(File f, String encoding) {
        this( makeInputStream(f), encoding);
    }

    public LineReader(InputStream inputStream) {
        this(inputStream, "utf8");
    }
    public LineReader(InputStream inputStream, String encoding) {
        try {
            BufferedReader in = new BufferedReader(new InputStreamReader(inputStream, encoding));
            ArrayList<String> lineList = new ArrayList<String>();
            String line = in.readLine();
            while( line!=null ) {
                if ( line.length()>0 && !line.startsWith("#"))
                    lineList.add(line);
                line = in.readLine();
            }
            in.close();
            this.lines = (String[]) lineList.toArray(new String[lineList.size()]);


        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static InputStream makeInputStream(File f) {
        try {
            return new FileInputStream(f);
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }
    public String[] getLines() {
        return lines;
    }

   public Set<String> getLinesAsSet() {
      Set<String> lineSet = new HashSet<String>(lines.length);
      for (int i = 0; i < lines.length; i++) {
         lineSet.add(lines[i]);
      }
      return lineSet;
   }
}
