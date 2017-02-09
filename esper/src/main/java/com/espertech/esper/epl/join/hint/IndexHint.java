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
package com.espertech.esper.epl.join.hint;

import com.espertech.esper.client.EPException;
import com.espertech.esper.client.annotation.HintEnum;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class IndexHint {

    private final List<SelectorInstructionPair> pairs;

    public IndexHint(List<SelectorInstructionPair> pairs) {
        this.pairs = pairs;
    }

    public static IndexHint getIndexHint(Annotation[] annotations) {
        List<String> hints = HintEnum.INDEX.getHintAssignedValues(annotations);
        if (hints == null) {
            return null;
        }
        List<SelectorInstructionPair> pairs = new ArrayList<SelectorInstructionPair>();
        for (String hint : hints) {
            String[] hintAtoms = HintEnum.splitCommaUnlessInParen(hint);
            List<IndexHintSelector> selectors = new ArrayList<IndexHintSelector>();
            List<IndexHintInstruction> instructions = new ArrayList<IndexHintInstruction>();
            for (int i = 0; i < hintAtoms.length; i++) {
                String hintAtom = hintAtoms[i];
                if (hintAtom.toLowerCase(Locale.ENGLISH).trim().equals("bust")) {
                    instructions.add(new IndexHintInstructionBust());
                } else if (hintAtom.toLowerCase(Locale.ENGLISH).trim().equals("explicit")) {
                    instructions.add(new IndexHintInstructionExplicit());
                } else if (checkValueInParen("subquery", hintAtom.toLowerCase(Locale.ENGLISH))) {
                    int subqueryNum = extractValueParen(hintAtom);
                    selectors.add(new IndexHintSelectorSubquery(subqueryNum));
                } else {
                    instructions.add(new IndexHintInstructionIndexName(hintAtom.trim()));
                }
            }
            pairs.add(new SelectorInstructionPair(selectors, instructions));
        }
        return new IndexHint(pairs);
    }

    public List<IndexHintInstruction> getInstructionsSubquery(int subqueryNumber) {
        for (SelectorInstructionPair pair : pairs) {
            if (pair.getSelector().isEmpty()) { // empty selector mean hint applies to all
                return pair.getInstructions();
            }
            for (IndexHintSelector selector : pair.getSelector()) {
                if (selector.matchesSubquery(subqueryNumber)) {
                    return pair.getInstructions();
                }
            }
        }
        return Collections.emptyList();
    }

    public List<IndexHintInstruction> getInstructionsFireAndForget() {
        for (SelectorInstructionPair pair : pairs) {
            if (pair.getSelector().isEmpty()) { // empty selector mean hint applies to all
                return pair.getInstructions();
            }
        }
        return Collections.emptyList();
    }

    protected static boolean checkValueInParen(String type, String value) {
        int indexOpen = value.indexOf('(');
        if (indexOpen != -1) {
            String noparen = value.substring(0, indexOpen).trim().toLowerCase(Locale.ENGLISH);
            if (type.equals(noparen)) {
                return true;
            }
        }
        return false;
    }

    protected static boolean checkValueAssignment(String type, String value) {
        int indexEquals = value.indexOf('=');
        if (indexEquals != -1) {
            String noequals = value.substring(0, indexEquals).trim().toLowerCase(Locale.ENGLISH);
            if (type.equals(noequals)) {
                return true;
            }
        }
        return false;
    }

    protected static int extractValueParen(String text) {
        int indexOpen = text.indexOf('(');
        int indexClosed = text.lastIndexOf(')');
        if (indexOpen != -1) {
            String value = text.substring(indexOpen + 1, indexClosed).trim();
            try {
                return Integer.parseInt(value);
            } catch (Exception ex) {
                throw new EPException("Failed to parse '" + value + "' as an index hint integer value");
            }
        }
        throw new IllegalStateException("Not a parentheses value");
    }

    protected static Object extractValueEqualsStringOrInt(String text) {
        String value = extractValueEquals(text);
        try {
            return Integer.parseInt(value);
        } catch (Exception ex) {
            return value;
        }
    }

    protected static String extractValueEquals(String text) {
        int indexEquals = text.indexOf('=');
        if (indexEquals != -1) {
            return text.substring(indexEquals + 1).trim();
        }
        throw new IllegalStateException("Not a parentheses value");
    }
}
