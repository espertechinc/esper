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
package com.espertech.esper.common.internal.serde.serdeset.additional;

import com.espertech.esper.common.client.serde.DataInputOutputSerde;
import com.espertech.esper.common.client.serde.EventBeanCollatedWriter;
import com.espertech.esper.common.internal.util.CollectionUtil;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class DIOMapPropertySerde implements DataInputOutputSerde<Map> {
    private final String[] keys;
    private final DataInputOutputSerde[] serdes;

    public DIOMapPropertySerde(String[] keys, DataInputOutputSerde[] serdes) {
        this.keys = keys;
        this.serdes = serdes;
    }

    public void write(Map object, DataOutput output, byte[] unitKey, EventBeanCollatedWriter writer) throws IOException {
        for (int i = 0; i < keys.length; i++) {
            Object value = keys[i];
            serdes[i].write(value, output, unitKey, writer);
        }
    }

    public Map read(DataInput input, byte[] unitKey) throws IOException {
        Map<String, Object> map = new HashMap<>(CollectionUtil.capacityHashMap(keys.length));
        for (int i = 0; i < keys.length; i++) {
            Object value = serdes[i].read(input, unitKey);
            map.put(keys[i], value);
        }
        return map;
    }
}
