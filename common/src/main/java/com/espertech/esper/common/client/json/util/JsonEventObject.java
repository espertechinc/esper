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
package com.espertech.esper.common.client.json.util;

import com.espertech.esper.common.client.json.minimaljson.WriterConfig;

import java.io.IOException;
import java.io.Writer;
import java.util.Map;

/**
 * All JSON underlying event objects implement this interface.
 * <p>
 *     In general, byte code does not use the Map methods and instead
 *     uses the implementation class fields directly.
 * </p>
 * <p>
 *     This is a read-only implementation of the Map interface.
 * </p>
 * <p>
 *     All predefined properties as well as all dynamic properties become available through the Map interface.
 * </p>
 */
public interface JsonEventObject extends Map<String, Object> {
    /**
     * Write JSON to the provided writer and using the provided configuration.
     * @param writer writer
     * @param config JSON writer settings
     * @throws IOException when an IO exception occurs
     */
    void writeTo(Writer writer, WriterConfig config) throws IOException;

    /**
     * Returns the JSON string given a writer configuration
     * @param config JSON writer settings
     * @return JSON
     */
    String toString(WriterConfig config);

    /**
     * Returns the JSON string using {@link WriterConfig#MINIMAL}.
     * @return JSON
     */
    String toString();
}
