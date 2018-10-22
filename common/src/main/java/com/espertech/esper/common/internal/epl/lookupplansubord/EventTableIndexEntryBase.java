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
package com.espertech.esper.common.internal.epl.lookupplansubord;

public abstract class EventTableIndexEntryBase {
    private final String optionalIndexName;
    private final String optionalIndexModuleName;

    public EventTableIndexEntryBase(String optionalIndexName, String optionalIndexModuleName) {
        this.optionalIndexName = optionalIndexName;
        this.optionalIndexModuleName = optionalIndexModuleName;
    }

    public String getOptionalIndexName() {
        return optionalIndexName;
    }

    public String getOptionalIndexModuleName() {
        return optionalIndexModuleName;
    }
}
