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
package com.espertech.esper.epl.join.plan;

import java.util.ArrayList;
import java.util.List;

public class QueryPlanIndexUniqueHelper {
    public static ReducedHashKeys reduceToUniqueIfPossible(String[] hashPropsProvided, Class[] hashCoercionTypes, List<QueryGraphValueEntryHashKeyed> hashFunctions, String[][] hashPropsRequiredPerIndex) {
        if (hashPropsRequiredPerIndex == null || hashPropsRequiredPerIndex.length == 0) {
            return null;
        }
        for (String[] hashPropsRequired : hashPropsRequiredPerIndex) {
            int[] indexes = checkSufficientGetAssignment(hashPropsRequired, hashPropsProvided);
            if (indexes != null) {
                String[] props = new String[indexes.length];
                Class[] types = new Class[indexes.length];
                List<QueryGraphValueEntryHashKeyed> functions = new ArrayList<QueryGraphValueEntryHashKeyed>();
                for (int i = 0; i < indexes.length; i++) {
                    props[i] = hashPropsProvided[indexes[i]];
                    types[i] = hashCoercionTypes == null ? null : hashCoercionTypes[indexes[i]];
                    functions.add(hashFunctions.get(indexes[i]));
                }
                return new ReducedHashKeys(props, types, functions);
            }
        }
        return null;
    }

    public static int[] checkSufficientGetAssignment(String[] hashPropsRequired, String[] hashPropsProvided) {
        if (hashPropsProvided == null || hashPropsRequired == null || hashPropsProvided.length < hashPropsRequired.length) {
            return null;
        }
        // first pass: determine if possible
        for (String required : hashPropsRequired) {
            boolean found = false;
            for (String provided : hashPropsProvided) {
                if (provided.equals(required)) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                return null;
            }
        }

        // second pass: determine assignments
        int[] indexes = new int[hashPropsRequired.length];
        for (int i = 0; i < indexes.length; i++) {
            int foundIndex = -1;
            String required = hashPropsRequired[i];
            for (int j = 0; j < hashPropsProvided.length; j++) {
                if (hashPropsProvided[j].equals(required)) {
                    foundIndex = j;
                    break;
                }
            }
            indexes[i] = foundIndex;
        }
        return indexes;
    }

    public static class ReducedHashKeys {
        private final String[] propertyNames;
        private final Class[] coercionTypes;
        private final List<QueryGraphValueEntryHashKeyed> hashKeyFunctions;

        private ReducedHashKeys(String[] propertyNames, Class[] coercionTypes, List<QueryGraphValueEntryHashKeyed> hashKeyFunctions) {
            this.propertyNames = propertyNames;
            this.coercionTypes = coercionTypes;
            this.hashKeyFunctions = hashKeyFunctions;
        }

        public String[] getPropertyNames() {
            return propertyNames;
        }

        public Class[] getCoercionTypes() {
            return coercionTypes;
        }

        public List<QueryGraphValueEntryHashKeyed> getHashKeyFunctions() {
            return hashKeyFunctions;
        }
    }
}
