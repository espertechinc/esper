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
package com.espertech.esper.regressionlib.support.extend.aggfunc;

import com.espertech.esper.common.client.hook.aggfunc.AggregationFunction;

import java.io.StringWriter;
import java.util.LinkedHashMap;
import java.util.Map;

public class SupportSimpleWordCSVFunction implements AggregationFunction {
    private Map<String, Integer> countPerWord = new LinkedHashMap<String, Integer>();

    public void enter(Object value) {
        String word = (String) value;
        Integer count = countPerWord.get(word);
        if (count == null) {
            countPerWord.put(word, 1);
        } else {
            countPerWord.put(word, count + 1);
        }
    }

    public void leave(Object value) {
        String word = (String) value;
        Integer count = countPerWord.get(word);
        if (count == null) {
            countPerWord.put(word, 1);
        } else if (count == 1) {
            countPerWord.remove(word);
        } else {
            countPerWord.put(word, count - 1);
        }
    }

    public Object getValue() {
        StringWriter writer = new StringWriter();
        String delimiter = "";
        for (Map.Entry<String, Integer> entry : countPerWord.entrySet()) {
            writer.append(delimiter);
            delimiter = ",";
            writer.append(entry.getKey());
        }
        return writer.toString();
    }

    public void clear() {
        countPerWord.clear();
    }
}
