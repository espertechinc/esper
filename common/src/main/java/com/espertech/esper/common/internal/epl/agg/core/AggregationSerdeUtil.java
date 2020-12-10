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
package com.espertech.esper.common.internal.epl.agg.core;

import com.espertech.esper.common.client.EPException;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

public class AggregationSerdeUtil {
    public static void writeVersion(short version, DataOutput output) throws IOException {
        output.writeShort(version);
    }

    public static void readVersionChecked(short versionExpected, DataInput input) throws IOException {
        short version = input.readShort();
        if (version != versionExpected) {
            throw new EPException("Serde version mismatch, expected version " + versionExpected + " but received version " + version);
        }
    }
}
