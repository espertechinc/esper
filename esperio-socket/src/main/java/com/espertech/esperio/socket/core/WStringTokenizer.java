package com.espertech.esperio.socket.core;

import java.util.StringTokenizer;
import java.util.NoSuchElementException;

public class WStringTokenizer extends StringTokenizer {
    private String tbt;
    private String d;
    private int startpos = 0;

    public WStringTokenizer(String str, String delim) {
        super(str, delim);
        tbt = new String(str);
        d = new String(delim);
    }

    public int countTokens() {
        int tokens = 0;
        int temp = startpos;
        while (true) {
            try {
                nextToken();
                tokens++;
            }
            catch (NoSuchElementException e) {
                break;
            }
        }
        startpos = temp;
        return tokens;
    }

    public boolean hasMoreElements() {
        return hasMoreTokens();
    }

    public boolean hasMoreTokens() {
        if (countTokens() > 0) return true;
        else return false;
    }

    public Object nextElement() {
        return (Object) d;
    }

    public String nextToken() throws NoSuchElementException {
        int result = 0;
        String s;

        if (startpos > tbt.length()) throw (new NoSuchElementException());
        result = tbt.indexOf(d, startpos);
        if (result < 0) result = tbt.length();
        s = new String(tbt.substring(startpos, result));
        startpos = result + d.length();
        return s;
    }

    public String nextToken(String delim) throws NoSuchElementException {
        d = delim;
        return nextToken();
    }
}
