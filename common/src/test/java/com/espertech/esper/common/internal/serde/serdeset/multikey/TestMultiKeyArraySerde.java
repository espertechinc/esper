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
package com.espertech.esper.common.internal.serde.serdeset.multikey;

import com.espertech.esper.common.internal.collection.*;
import junit.framework.TestCase;

import java.io.IOException;

import static com.espertech.esper.common.internal.serde.serdeset.builtin.TestBuiltinSerde.assertSerde;

public class TestMultiKeyArraySerde extends TestCase {
    public void testSerde() throws IOException {
        assertSerde(DIOMultiKeyArrayBooleanSerde.INSTANCE, new MultiKeyArrayBoolean(new boolean[]{true, false}));
        assertSerde(DIOMultiKeyArrayByteSerde.INSTANCE, new MultiKeyArrayByte(new byte[]{1, 2}));
        assertSerde(DIOMultiKeyArrayCharSerde.INSTANCE, new MultiKeyArrayChar(new char[]{'a', 'b'}));
        assertSerde(DIOMultiKeyArrayDoubleSerde.INSTANCE, new MultiKeyArrayDouble(new double[]{1d, 2d}));
        assertSerde(DIOMultiKeyArrayFloatSerde.INSTANCE, new MultiKeyArrayFloat(new float[]{1f, 2f}));
        assertSerde(DIOMultiKeyArrayIntSerde.INSTANCE, new MultiKeyArrayInt(new int[]{1, 2}));
        assertSerde(DIOMultiKeyArrayLongSerde.INSTANCE, new MultiKeyArrayLong(new long[]{1, 2}));
        assertSerde(DIOMultiKeyArrayObjectSerde.INSTANCE, new MultiKeyArrayObject(new Object[]{"A", "B"}));
        assertSerde(DIOMultiKeyArrayShortSerde.INSTANCE, new MultiKeyArrayShort(new short[]{1, 2}));
    }
}
