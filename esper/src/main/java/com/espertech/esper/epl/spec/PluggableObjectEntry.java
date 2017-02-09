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
package com.espertech.esper.epl.spec;

import java.io.Serializable;

public class PluggableObjectEntry {
    private final PluggableObjectType type;
    private final Serializable customConfigs;

    public PluggableObjectEntry(PluggableObjectType type, Serializable customConfigs) {
        this.type = type;
        this.customConfigs = customConfigs;
    }

    public PluggableObjectType getType() {
        return type;
    }

    public Serializable getCustomConfigs() {
        return customConfigs;
    }
}
