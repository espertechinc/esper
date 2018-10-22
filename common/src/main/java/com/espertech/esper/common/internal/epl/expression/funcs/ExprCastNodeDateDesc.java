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
package com.espertech.esper.common.internal.epl.expression.funcs;

import com.espertech.esper.common.internal.epl.expression.core.ExprForge;

public class ExprCastNodeDateDesc {
    private final boolean iso8601Format;
    private final ExprForge dynamicDateFormat;
    private final String staticDateFormatString;
    private final boolean deployTimeConstant;

    public ExprCastNodeDateDesc(boolean iso8601Format, ExprForge dynamicDateFormat, String staticDateFormatString, boolean deployTimeConstant) {
        this.iso8601Format = iso8601Format;
        this.dynamicDateFormat = dynamicDateFormat;
        this.staticDateFormatString = staticDateFormatString;
        this.deployTimeConstant = deployTimeConstant;
    }

    public boolean isIso8601Format() {
        return iso8601Format;
    }

    public ExprForge getDynamicDateFormat() {
        return dynamicDateFormat;
    }

    public String getStaticDateFormatString() {
        return staticDateFormatString;
    }

    public boolean isDeployTimeConstant() {
        return deployTimeConstant;
    }
}
