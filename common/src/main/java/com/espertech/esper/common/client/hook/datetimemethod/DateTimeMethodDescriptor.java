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
package com.espertech.esper.common.client.hook.datetimemethod;

import com.espertech.esper.common.internal.epl.methodbase.DotMethodFP;

/**
 * Provides footprint information for date-time method extension.
 */
public class DateTimeMethodDescriptor {
    private final DotMethodFP[] footprints;

    /**
     * Ctor.
     * @param footprints footprint array, one array item for each distinct footprint
     */
    public DateTimeMethodDescriptor(DotMethodFP[] footprints) {
        this.footprints = footprints;
    }

    /**
     * Returns the footprints
     * @return footprints
     */
    public DotMethodFP[] getFootprints() {
        return footprints;
    }
}
