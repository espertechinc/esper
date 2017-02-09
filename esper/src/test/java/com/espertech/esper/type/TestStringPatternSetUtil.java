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
import junit.framework.TestCase;

import java.util.LinkedList;
import java.util.List;

public class TestStringPatternSetUtil extends TestCase {
    private List<Pair<StringPatternSet, Boolean>> patterns = new LinkedList<Pair<StringPatternSet, Boolean>>();

    public void testEmpty() {
        assertTrue(StringPatternSetUtil.evaluate(true, patterns, "abc"));
        assertFalse(StringPatternSetUtil.evaluate(false, patterns, "abc"));
    }

    public void testCombinationLike() {
        patterns.add(new Pair<StringPatternSet, Boolean>(new StringPatternSetLike("%123%"), true));
        patterns.add(new Pair<StringPatternSet, Boolean>(new StringPatternSetLike("%abc%"), false));
        patterns.add(new Pair<StringPatternSet, Boolean>(new StringPatternSetLike("%def%"), true));
        patterns.add(new Pair<StringPatternSet, Boolean>(new StringPatternSetLike("%xyz%"), false));

        runAssertion();
    }

    public void testCombinationRegex() {
        patterns.add(new Pair<StringPatternSet, Boolean>(new StringPatternSetRegex("(.)*123(.)*"), true));
        patterns.add(new Pair<StringPatternSet, Boolean>(new StringPatternSetRegex("(.)*abc(.)*"), false));
        patterns.add(new Pair<StringPatternSet, Boolean>(new StringPatternSetRegex("(.)*def(.)*"), true));
        patterns.add(new Pair<StringPatternSet, Boolean>(new StringPatternSetRegex("(.)*xyz(.)*"), false));

        runAssertion();
    }

    private void runAssertion() {
        assertTrue(StringPatternSetUtil.evaluate(false, patterns, "123"));
        assertFalse(StringPatternSetUtil.evaluate(false, patterns, "123abc"));
        assertTrue(StringPatternSetUtil.evaluate(false, patterns, "123abcdef"));
        assertFalse(StringPatternSetUtil.evaluate(false, patterns, "123abcdefxyz"));
        assertFalse(StringPatternSetUtil.evaluate(false, patterns, "456"));
        assertTrue(StringPatternSetUtil.evaluate(true, patterns, "456"));
    }
}
