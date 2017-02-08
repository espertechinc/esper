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
package com.espertech.esper.epl.expression.funcs;

import com.espertech.esper.epl.expression.core.ExprEvaluator;

import java.time.format.DateTimeFormatter;

public class ExprCastNodeDateDesc {
    private final String staticDateFormat;
    private final ExprEvaluator dynamicDateFormat;
    private final boolean iso8601Format;
    private final DateTimeFormatter dateTimeFormatter;

    public ExprCastNodeDateDesc(String staticDateFormat, ExprEvaluator dynamicDateFormat, boolean iso8601Format, DateTimeFormatter dateTimeFormatter) {
        this.staticDateFormat = staticDateFormat;
        this.dynamicDateFormat = dynamicDateFormat;
        this.iso8601Format = iso8601Format;
        this.dateTimeFormatter = dateTimeFormatter;
    }

    public String getStaticDateFormat() {
        return staticDateFormat;
    }

    public ExprEvaluator getDynamicDateFormat() {
        return dynamicDateFormat;
    }

    public boolean iso8601Format() {
        return iso8601Format;
    }

    public DateTimeFormatter getDateTimeFormatter() {
        return dateTimeFormatter;
    }
}
