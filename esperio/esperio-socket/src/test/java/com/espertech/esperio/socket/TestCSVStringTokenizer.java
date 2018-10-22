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
package com.espertech.esperio.socket;

import com.espertech.esperio.socket.core.UnescapeUtil;
import com.espertech.esperio.socket.core.WStringTokenizer;
import junit.framework.TestCase;

public class TestCSVStringTokenizer extends TestCase {

    public void testComma() {
        String line = "p0=hello,p1=fox\\u002Chouse";
        WStringTokenizer t = new WStringTokenizer(line, ",");
        assertEquals("p0=hello", t.nextToken());
        assertEquals("p1=fox,house", UnescapeUtil.unescapeJavaString(t.nextToken()));
    }
}
