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
package com.espertech.esper.common.internal.util;

import java.nio.charset.Charset;
import java.util.zip.CRC32;

public class CRC32Util {
    public static long computeCRC32(String name) {
        CRC32 crc32 = new CRC32();
        crc32.update(name.getBytes(Charset.forName("UTF-8")));
        return crc32.getValue();
    }
}
