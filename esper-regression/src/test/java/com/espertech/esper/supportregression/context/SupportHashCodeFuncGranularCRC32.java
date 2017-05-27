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
package com.espertech.esper.supportregression.context;

import com.espertech.esper.regression.context.ExecContextHashSegmented;

import java.util.zip.CRC32;

public class SupportHashCodeFuncGranularCRC32 implements ExecContextHashSegmented.HashCodeFunc {
    private int granularity;

    public SupportHashCodeFuncGranularCRC32(int granularity) {
        this.granularity = granularity;
    }

    public int codeFor(String key) {
        long codeMod = computeCRC32(key) % granularity;
        return (int) codeMod;
    }

    public static long computeCRC32(String key) {
        CRC32 crc = new CRC32();
        crc.update(key.getBytes());
        return crc.getValue();
    }
}
