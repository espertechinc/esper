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
package com.espertech.esper.common.internal.event.json.parser.forge;

import com.espertech.esper.common.internal.event.json.write.JsonWriteForge;
import com.espertech.esper.common.internal.event.json.parser.delegates.endvalue.JsonEndValueForge;

public class JsonForgeDesc {
    private final JsonDelegateForge optionalStartObjectForge;
    private final JsonDelegateForge optionalStartArrayForge;
    private final JsonEndValueForge endValueForge;
    private final JsonWriteForge writeForge;

    public JsonForgeDesc(String fieldName, JsonDelegateForge optionalStartObjectForge, JsonDelegateForge optionalStartArrayForge, JsonEndValueForge endValueForge, JsonWriteForge writeForge) {
        this.optionalStartObjectForge = optionalStartObjectForge;
        this.optionalStartArrayForge = optionalStartArrayForge;
        this.endValueForge = endValueForge;
        this.writeForge = writeForge;
        if (endValueForge == null || writeForge == null) {
            throw new IllegalArgumentException("Unexpected null forge for end-value or write forge for field '" + fieldName + "'");
        }
    }

    public JsonDelegateForge getOptionalStartObjectForge() {
        return optionalStartObjectForge;
    }

    public JsonEndValueForge getEndValueForge() {
        return endValueForge;
    }

    public JsonDelegateForge getOptionalStartArrayForge() {
        return optionalStartArrayForge;
    }

    public JsonWriteForge getWriteForge() {
        return writeForge;
    }
}
