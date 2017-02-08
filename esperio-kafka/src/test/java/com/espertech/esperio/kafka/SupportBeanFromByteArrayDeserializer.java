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
package com.espertech.esperio.kafka;

import com.espertech.esper.util.SerializerUtil;
import org.apache.kafka.common.serialization.Deserializer;

import java.util.Map;

public class SupportBeanFromByteArrayDeserializer implements Deserializer {
    public void configure(Map map, boolean b) {
    }

    public Object deserialize(String s, byte[] bytes) {
        return SerializerUtil.byteArrToObject(bytes);
    }

    public void close() {

    }
}
