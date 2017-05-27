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
package com.espertech.esper.supportregression.epl;

import java.util.HashMap;
import java.util.Map;

public class SupportJoinMethods {
    public static Map[] fetchVal(String prefix, Integer number) {
        if ((number == null) || (number == 0)) {
            return new Map[0];
        }

        Map[] result = new Map[number];
        for (int i = 0; i < number; i++) {
            result[i] = new HashMap<String, String>();
            result[i].put("val", prefix + Integer.toString(i + 1));
            result[i].put("index", i + 1);
        }

        return result;
    }

    public static Map[] fetchValMultiRow(String prefix, Integer number, Integer numRowsPerIndex) {
        if ((number == null) || (number == 0)) {
            return new Map[0];
        }

        int rows = number;
        if (numRowsPerIndex > 1) {
            rows *= numRowsPerIndex;
        }

        Map[] result = new Map[rows];
        int count = 0;
        for (int i = 0; i < number; i++) {
            for (int j = 0; j < numRowsPerIndex; j++) {
                result[count] = new HashMap<String, String>();
                result[count].put("val", prefix + Integer.toString(i + 1) + "_" + j);
                result[count].put("index", i + 1);
                count++;
            }
        }

        return result;
    }

    public static Map fetchValMultiRowMetadata() {
        return fetchValMetadata();
    }

    public static Map fetchValMetadata() {
        Map<String, Class> values = new HashMap<String, Class>();
        values.put("val", String.class);
        values.put("index", Integer.class);
        return values;
    }
}
