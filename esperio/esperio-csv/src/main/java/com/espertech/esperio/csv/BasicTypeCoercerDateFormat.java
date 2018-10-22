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
package com.espertech.esperio.csv;

import java.text.DateFormat;
import java.util.HashMap;
import java.util.Map;

/**
 * Date format coercion.
 */
public class BasicTypeCoercerDateFormat extends BasicTypeCoercer {

    private Map<String, DateFormat> dateFormats = new HashMap<String, DateFormat>();

    @Override
    public Object coerce(String property, String source) throws Exception {
        DateFormat df = dateFormats.get(property);
        if (df != null) {
            return df.parse(source).getTime();
        }
        return super.coerce(property, source);
    }

    /**
     * Add a date format.
     *
     * @param propertyName property to add
     * @param df           the date format
     */
    public void addDateFormat(String propertyName, DateFormat df) {
        this.dateFormats.put(propertyName, df);
    }
}
