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
package com.espertech.esper.type;

import com.espertech.esper.collection.Pair;

import java.util.List;

/**
 * Pattern matching utility.
 */
public class StringPatternSetUtil {
    /**
     * Executes a seriers of include/exclude patterns against a match string,
     * returning the last pattern match result as boolean in/out.
     *
     * @param defaultValue the default value if there are no patterns or no matches change the value
     * @param patterns     to match against, true in the pair for include, false for exclude
     * @param literal      to match
     * @return true for included, false for excluded
     */
    public static Boolean evaluate(boolean defaultValue, List<Pair<StringPatternSet, Boolean>> patterns, String literal) {
        boolean result = defaultValue;

        for (Pair<StringPatternSet, Boolean> item : patterns) {
            if (result) {
                if (!item.getSecond()) {
                    boolean testResult = item.getFirst().match(literal);
                    if (testResult) {
                        result = false;
                    }
                }
            } else {
                if (item.getSecond()) {
                    boolean testResult = item.getFirst().match(literal);
                    if (testResult) {
                        result = true;
                    }
                }
            }
        }

        return result;
    }
}
