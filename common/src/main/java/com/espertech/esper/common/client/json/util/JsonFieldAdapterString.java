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

import com.espertech.esper.common.client.json.minimaljson.JsonWriter;

import java.io.IOException;

/**
 * JSON field adapter for strings.
 */
public interface JsonFieldAdapterString<T> extends JsonFieldAdapter {
    /**
     * Parse
     * @param value value to parse
     * @return result
     */
    T parse(String value);

    /**
     * Write
     * @param value to write
     * @param writer output
     * @throws IOException in case of failure
     */
    void write(T value, JsonWriter writer) throws IOException;
}
