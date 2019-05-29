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

import java.util.Collections;
import java.util.Map;

import static com.espertech.esper.common.internal.event.json.serde.SupportDIOJson.assertSerde;

public class TestDIOJsonObjectSerde extends TestCase {

    public void testMap() {
        assertSerde(DIOJsonObjectSerde.INSTANCE, null);

        assertEntry(null);
        assertEntry(1);
        assertEntry(1d);
        assertEntry("abc");
        assertEntry(true);
        assertEntry(false);
        assertEntry(Collections.singletonMap("k", 1));
        assertEntry(new Object[]{"a", true, null, Collections.singletonMap("k", 1)});
        assertEntry(Collections.singletonMap("k", Collections.singletonMap("k2", 2)));
    }

    private void assertEntry(Object value) {
        Map<String, Object> map = Collections.singletonMap("k", value);
        assertSerde(DIOJsonObjectSerde.INSTANCE, map);
    }
}
