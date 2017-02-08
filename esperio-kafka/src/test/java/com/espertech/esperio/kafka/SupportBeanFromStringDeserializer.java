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

import org.apache.kafka.common.serialization.Deserializer;

import java.util.Map;

public class SupportBeanFromStringDeserializer implements Deserializer {
    public void configure(Map map, boolean b) {
    }

    public Object deserialize(String s, byte[] bytes) {
        String value = new String(bytes);
        return new SupportBean(value, 10);
    }

    public void close() {

    }
}
