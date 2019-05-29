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
package com.espertech.esper.common.internal.event.json.serde;

import junit.framework.TestCase;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;

import static com.espertech.esper.common.internal.event.json.serde.SupportDIOJson.assertSerde;

public class TestDIOJsonArraySerde extends TestCase {

    public void testArray() {
        assertSerde(DIOJsonArraySerde.INSTANCE, null);

        assertEntry();
        assertEntry(null);
        assertEntry(1, 1d, "abc", null);
        assertEntry(true, false);
        assertEntry(Collections.singletonMap("k", 1), new Object[]{"a", true, null, Collections.singletonMap("k", 1)});
    }

    private void assertEntry(Object ... values) {
        assertSerde(DIOJsonArraySerde.INSTANCE, values);
    }
}
