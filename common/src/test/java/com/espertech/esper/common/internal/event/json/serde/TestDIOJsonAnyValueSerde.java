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

import static com.espertech.esper.common.internal.event.json.serde.SupportDIOJson.assertSerde;

public class TestDIOJsonAnyValueSerde extends TestCase {

    public void testArray() {
        assertValue(null);
        assertValue(1);
        assertValue(1d);
        assertValue("abc");
        assertValue(true);
        assertValue(false);
        assertValue(Collections.singletonMap("k", 1));
        assertValue(new Object[]{"a", true, null, Collections.singletonMap("k", 1)});
        assertValue(Collections.singletonMap("k", Collections.singletonMap("k2", 2)));
    }

    private void assertValue(Object any) {
        assertSerde(DIOJsonAnyValueSerde.INSTANCE, any);
    }
}
