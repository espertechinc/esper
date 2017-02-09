/*
 ***************************************************************************************
 *  Copyright (C) 2006 EsperTech, Inc. All rights reserved.                            *
 *  http://www.espertech.com/esper                                                     *
 *  http://www.espertech.com                                                           *
 *  ---------------------------------------------------------------------------------- *
 *  The software in this package is published under the terms of the GPL license       *
 *  a copy of which has been included with this distribution in the license.txt file.  *
 ***************************************************************************************
 */
package com.espertech.esper.util;

import java.io.Serializable;
import java.util.Locale;

/**
 * Utility for performing a SQL Like comparsion.
 */
public class LikeUtil implements Serializable {
    private final static int UNDERSCORE_CHAR = 1;
    private final static int PERCENT_CHAR = 2;

    private char[] cLike;
    private int[] wildCardType;
    private int iLen;
    private boolean isIgnoreCase;
    private int iFirstWildCard;
    private boolean isNull;
    private Character escapeChar;
    private static final long serialVersionUID = 3226305829536318662L;

    /**
     * Ctor.
     *
     * @param pattern    is the SQL-like pattern to
     * @param escape     is the escape character
     * @param ignorecase is true to ignore the case, or false if not
     */
    public LikeUtil(String pattern, Character escape, boolean ignorecase) {
        escapeChar = escape;
        isIgnoreCase = ignorecase;
        normalize(pattern);
    }

    /**
     * Execute the string.
     *
     * @param compareString is the string to compare
     * @return true if pattern matches, or false if not
     */
    public boolean compare(String compareString) {

        if (isIgnoreCase) {
            compareString = compareString.toUpperCase(Locale.ENGLISH);
        }

        return compareAt(compareString, 0, 0, compareString.length()) ? Boolean.TRUE
                : Boolean.FALSE;
    }

    /**
     * Resets the search pattern.
     *
     * @param pattern is the new pattern to match against
     */
    public void resetPattern(String pattern) {
        normalize(pattern);
    }

    private boolean compareAt(String s, int i, int j, int jLen) {

        for (; i < iLen; i++) {
            switch (wildCardType[i]) {

                case 0:                  // general character
                    if ((j >= jLen) || (cLike[i] != s.charAt(j++))) {
                        return false;
                    }
                    break;

                case LikeUtil.UNDERSCORE_CHAR:    // underscore: do not test this character
                    if (j++ >= jLen) {
                        return false;
                    }
                    break;

                case LikeUtil.PERCENT_CHAR:       // percent: none or any character(s)
                    if (++i >= iLen) {
                        return true;
                    }

                    while (j < jLen) {
                        if ((cLike[i] == s.charAt(j))
                                && compareAt(s, i, j, jLen)) {
                            return true;
                        }

                        j++;
                    }

                    return false;
            }
        }

        if (j != jLen) {
            return false;
        }

        return true;
    }

    private void normalize(String pattern) {

        isNull = pattern == null;

        if (!isNull && isIgnoreCase) {
            pattern = pattern.toUpperCase(Locale.ENGLISH);
        }

        iLen = 0;
        iFirstWildCard = -1;

        int l = pattern == null ? 0
                : pattern.length();

        cLike = new char[l];
        wildCardType = new int[l];

        boolean bEscaping = false,
                bPercent = false;

        for (int i = 0; i < l; i++) {
            char c = pattern.charAt(i);

            if (!bEscaping) {
                if (escapeChar != null && escapeChar.charValue() == c) {
                    bEscaping = true;

                    continue;
                } else if (c == '_') {
                    wildCardType[iLen] = LikeUtil.UNDERSCORE_CHAR;

                    if (iFirstWildCard == -1) {
                        iFirstWildCard = iLen;
                    }
                } else if (c == '%') {
                    if (bPercent) {
                        continue;
                    }

                    bPercent = true;
                    wildCardType[iLen] = LikeUtil.PERCENT_CHAR;

                    if (iFirstWildCard == -1) {
                        iFirstWildCard = iLen;
                    }
                } else {
                    bPercent = false;
                }
            } else {
                bPercent = false;
                bEscaping = false;
            }

            cLike[iLen++] = c;
        }

        for (int i = 0; i < iLen - 1; i++) {
            if ((wildCardType[i] == LikeUtil.PERCENT_CHAR)
                    && (wildCardType[i + 1] == LikeUtil.UNDERSCORE_CHAR)) {
                wildCardType[i] = LikeUtil.UNDERSCORE_CHAR;
                wildCardType[i + 1] = LikeUtil.PERCENT_CHAR;
            }
        }
    }

    boolean hasWildcards() {
        return iFirstWildCard != -1;
    }

    boolean isEquivalentToFalsePredicate() {
        return isNull;
    }

    boolean isEquivalentToEqualsPredicate() {
        return iFirstWildCard == -1;
    }

    boolean isEquivalentToNotNullPredicate() {

        if (isNull || !hasWildcards()) {
            return false;
        }

        for (int i = 0; i < wildCardType.length; i++) {
            if (wildCardType[i] != LikeUtil.PERCENT_CHAR) {
                return false;
            }
        }

        return true;
    }

    boolean isEquivalentToBetweenPredicate() {

        return iFirstWildCard > 0
                && iFirstWildCard == wildCardType.length - 1
                && cLike[iFirstWildCard] == '%';
    }

    boolean isEquivalentToBetweenPredicateAugmentedWithLike() {
        return iFirstWildCard > 0 && cLike[iFirstWildCard] == '%';
    }
}
