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
package com.espertech.esper.common.internal.event.json.compiletime;

import com.espertech.esper.common.internal.event.json.core.JsonEventType;
import com.espertech.esper.common.internal.event.json.parser.forge.JsonForgeDesc;

import java.util.Map;

public class StmtClassForgeableJsonDesc {
    private final Map<String, Object> propertiesThisType;
    private final Map<String, JsonUnderlyingField> fieldDescriptorsInclSupertype;
    private final boolean dynamic;
    private final int numFieldsSupertype;
    private final JsonEventType optionalSupertype;
    private final Map<String, JsonForgeDesc> forges;

    public StmtClassForgeableJsonDesc(Map<String, Object> propertiesThisType, Map<String, JsonUnderlyingField> fieldDescriptorsInclSupertype, boolean dynamic, int numFieldsSupertype, JsonEventType optionalSupertype, Map<String, JsonForgeDesc> forges) {
        this.propertiesThisType = propertiesThisType;
        this.fieldDescriptorsInclSupertype = fieldDescriptorsInclSupertype;
        this.dynamic = dynamic;
        this.numFieldsSupertype = numFieldsSupertype;
        this.optionalSupertype = optionalSupertype;
        this.forges = forges;
    }

    public Map<String, Object> getPropertiesThisType() {
        return propertiesThisType;
    }

    public Map<String, JsonUnderlyingField> getFieldDescriptorsInclSupertype() {
        return fieldDescriptorsInclSupertype;
    }

    public boolean isDynamic() {
        return dynamic;
    }

    public int getNumFieldsSupertype() {
        return numFieldsSupertype;
    }

    public JsonEventType getOptionalSupertype() {
        return optionalSupertype;
    }

    public Map<String, JsonForgeDesc> getForges() {
        return forges;
    }
}
