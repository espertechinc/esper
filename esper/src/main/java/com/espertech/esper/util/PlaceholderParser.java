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

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * Parser for strings with substitution parameters of the form ${parameter}.
 */
public class PlaceholderParser {
    /**
     * Parses a string to find placeholders of format ${placeholder}.
     * <p>
     * Example: "My ${thing} is ${color}"
     * <p>
     * The example above parses into 4 fragements: a text fragment of value "My ",
     * a parameter fragment "thing", a text fragement " is " and a parameter
     * fragment "color".
     *
     * @param parseString is the string to parse
     * @return list of fragements that can be either text fragments or placeholder fragments
     * @throws PlaceholderParseException if the string cannot be parsed to indicate syntax errors
     */
    public static List<Fragment> parsePlaceholder(String parseString) throws PlaceholderParseException {
        List<Fragment> result = new ArrayList<Fragment>();
        int currOutputIndex = 0;
        int currSearchIndex = 0;

        while (true) {
            if (currSearchIndex == parseString.length()) {
                break;
            }

            int startIndex = parseString.indexOf("${", currSearchIndex);
            if (startIndex == -1) {
                // no more parameters, add any remainder of string
                if (currOutputIndex < parseString.length()) {
                    String endString = parseString.substring(currOutputIndex, parseString.length());
                    TextFragment textFragment = new TextFragment(endString);
                    result.add(textFragment);
                }
                break;
            }
            // add text so far
            if (startIndex > 0) {
                String textSoFar = parseString.substring(currOutputIndex, startIndex);
                if (textSoFar.length() != 0) {
                    result.add(new TextFragment(textSoFar));
                }
            }
            // check if the parameter is escaped
            if ((startIndex > 0) && (parseString.charAt(startIndex - 1) == '$')) {
                currOutputIndex = startIndex + 1;
                currSearchIndex = startIndex + 1;
                continue;
            }

            int endIndex = parseString.indexOf('}', startIndex);
            if (endIndex == -1) {
                throw new PlaceholderParseException("Syntax error in property or variable: '" + parseString.substring(startIndex, parseString.length()) + "'");
            }

            // add placeholder
            String between = parseString.substring(startIndex + 2, endIndex);
            ParameterFragment parameterFragment = new ParameterFragment(between);
            result.add(parameterFragment);
            currOutputIndex = endIndex + 1;
            currSearchIndex = endIndex;
        }

        // Combine adjacent text fragements
        LinkedList<Fragment> fragments = new LinkedList<Fragment>();
        fragments.add(result.get(0));
        for (int i = 1; i < result.size(); i++) {
            Fragment fragment = result.get(i);
            if (!(result.get(i) instanceof TextFragment)) {
                fragments.add(fragment);
                continue;
            }
            if (!(fragments.getLast() instanceof TextFragment)) {
                fragments.add(fragment);
                continue;
            }
            TextFragment textFragment = (TextFragment) fragments.getLast();
            fragments.removeLast();
            fragments.add(new TextFragment(textFragment.getValue() + fragment.getValue()));
        }

        return fragments;
    }

    /**
     * Fragment is a parse result, a parse results in an ordered list of fragments.
     */
    public static abstract class Fragment {
        private String value;

        /**
         * Ctor.
         *
         * @param value is the fragment text
         */
        protected Fragment(String value) {
            this.value = value;
        }

        /**
         * Returns the string text of the fragment.
         *
         * @return fragment string
         */
        public String getValue() {
            return value;
        }

        /**
         * Returns true to indicate this is a parameter and not a text fragment.
         *
         * @return true if parameter fragement, false if text fragment.
         */
        public abstract boolean isParameter();

        public int hashCode() {
            return value != null ? value.hashCode() : 0;
        }

        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Fragment fragment = (Fragment) o;

            return value != null ? value.equals(fragment.value) : fragment.value == null;
        }
    }

    /**
     * Represents a piece of text in a parse string with placeholder values.
     */
    public static class TextFragment extends Fragment {
        /**
         * Ctor.
         *
         * @param value is the text
         */
        public TextFragment(String value) {
            super(value);
        }

        public int hashCode() {
            return super.hashCode();
        }

        public boolean equals(Object obj) {
            if (!(obj instanceof TextFragment)) {
                return false;
            }
            TextFragment other = (TextFragment) obj;
            return other.getValue().equals(this.getValue());
        }

        public String toString() {
            return "text=" + getValue();
        }

        public boolean isParameter() {
            return false;
        }
    }

    /**
     * Represents a parameter in a parsed string of texts and parameters.
     */
    public static class ParameterFragment extends Fragment {
        /**
         * Ctor.
         *
         * @param value is the parameter name
         */
        public ParameterFragment(String value) {
            super(value);
        }

        public int hashCode() {
            return super.hashCode();
        }

        public boolean equals(Object obj) {
            if (!(obj instanceof ParameterFragment)) {
                return false;
            }
            ParameterFragment other = (ParameterFragment) obj;
            return other.getValue().equals(this.getValue());
        }

        public boolean isParameter() {
            return true;
        }

        public String toString() {
            return "param=" + getValue();
        }
    }
}
