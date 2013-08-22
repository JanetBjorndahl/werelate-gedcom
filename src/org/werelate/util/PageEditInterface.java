package org.werelate.util;

/**
 * Created by IntelliJ IDEA.
 * User: npowell
 * Date: May 3, 2006
 * Time: 2:41:56 PM
 * This interface is defined so that I can have a
 * dummy version for the unit tests.
 */
public interface PageEditInterface {
    public boolean get(String title);
    public boolean post();
    public String getValue (String name);
    public void setValue(String key, String value);
}
