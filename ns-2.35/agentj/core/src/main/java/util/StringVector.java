
package util;

import java.io.BufferedReader;
import java.io.StringReader;
import java.util.*;

/**
 * StringVector is a class that extends ArrayList in a convenient way
 * to  deal only with strings. It provides
 * has shortcut names for the methods $elementAt^ (=$at^), $firstElement^
 * (=$first^) and $lastElement^ (=$last^).
 *
 * @author      Ian Taylor
 * @created     25 May 2000
 * @version     $Revision: 1.2 $
 * @date        $Date: 2008-06-20 13:35:38 $ modified by $Author: harrison $
 */
public class StringVector extends ArrayList {

    /**
     * Constructs an empty StringVector with the specified storage capacity.
     * @param initialCapacity the initial storage capacity of the vector
     */
    public StringVector(int initialCapacity) {
        super(initialCapacity);
    }

    /**
     * Constructs a StringVector from a String by splitting the String by
     * new line and adding them one by one to the StringVector.
     */
    public StringVector(String str) throws Exception {
        this(10);
        BufferedReader br = new BufferedReader(new StringReader(str));
        String st;
        while ((st = br.readLine()) != null) {
            add(st);
        }
        br.close();
    }

    /**
     * Constructs an empty vector.
     */
    public StringVector() {
        this(10);
    }

    /**
     * Returns the String element at the specified index.
     * @param index the index of the desired element
     * @exception ArrayIndexOutOfBoundsException If an invalid
     * index was given.
     */
    public final String at(int index) {
        Object o = super.get(index);
        return o instanceof String ? (String) o : null;
    }

    /**
     * Returns the first element of the sequence.
     * @exception NoSuchElementException If the sequence is empty.
     */
    public final String first() {
        return (String) super.get(0);
    }

    /**
     * Returns the last element of the sequence.
     * @exception NoSuchElementException If the sequence is empty.
     */
    public final Object last() {
        return (String) super.get(size() - 1);
    }

    /**
     * @return a string representation of this vector in the form :- <br>
     * el1 el2 el3 el4 ..... el(size-1) </p>
     */
    public final String toAString() {
        String s = "";
        for (int i = 0; i < size(); ++i)
            s = s + (String) get(i) + " ";
        s = s + "\n";
        return s;
    }

    /**
     * @return a string representation of this vector in the form :- <br>
     * el1 <br> el2 <br> el3 <br> el4 <br> ..... el(size-1)  </p>
     */
    public final String toNewLineString() {
        String s = "";
        for (int i = 0; i < size(); ++i)
            s = s + (String) get(i) + "\n";
        return s;
    }


    /**
     * copies this string vector into the object array
     */
    public void copyInto(Object[] objects) {
        toArray(objects);
    }


    public Enumeration elements() {
        return new StringTokenizer(toAString());
    }

    /**
     * Sorts the string vector into ascending alpabetical order.
     */
    public void sort() {
        Collections.sort(this);
    }

    /**
     * Adds the elements of the given StringVector to the end of the
     * present one, by reference.
     *
     * @param s The StringVector to be appended
     */
    public void append(StringVector s) {
        ensureCapacity(size() + s.size());
        for (int i = 0; i < s.size(); i++) add(s.at(i));
    }

// For backward compatibility ONLY, try to update classes

    public void addElement(Object el) {
        add(el);
    }

    public Object elementAt(int el) {
        return get(el);
    }

    public void removeElementAt(int i) {
        remove(i);
    }

    public void setElementAt(String str, int i) {
        set(i, str);
    }

    public int indexOf(Object el, int s) {
        for (int i = s; s < size(); ++i)
            if (at(i).indexOf((String) el) != -1)
                return i;

        return -1;
    }

    public void removeAllElements() {
        if (size() == 0)
            return;
        removeRange(0, size() - 1);
    }
}
