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
package com.espertech.esper.regressionlib.support.json;

import com.espertech.esper.common.client.json.minimaljson.JsonWriter;
import com.espertech.esper.common.client.json.util.JsonFieldAdapterString;
import com.espertech.esper.common.client.util.DateTime;

import java.io.IOException;
import java.util.Date;

public class SupportJsonFieldAdapterStringDate implements JsonFieldAdapterString<Date> {
    public Date parse(String value) {
        return value == null ? null : DateTime.parseDefaultDate(value);
    }

    public void write(Date value, JsonWriter writer) throws IOException {
        if (value == null) {
            writer.writeLiteral("null");
            return;
        }
        writer.writeString(DateTime.print(value));
    }
}
