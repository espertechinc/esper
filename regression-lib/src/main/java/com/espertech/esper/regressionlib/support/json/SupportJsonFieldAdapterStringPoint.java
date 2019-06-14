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

import java.awt.*;
import java.io.IOException;

public class SupportJsonFieldAdapterStringPoint implements JsonFieldAdapterString<Point> {
    public Point parse(String value) {
        if (value == null) {
            return null;
        }
        String[] split = value.split(",");
        return new Point(Integer.parseInt(split[0]), Integer.parseInt(split[1]));
    }

    public void write(Point value, JsonWriter writer) throws IOException {
        if (value == null) {
            writer.writeLiteral("null");
            return;
        }
        writer.writeString(value.x + "," + value.y);
    }
}
