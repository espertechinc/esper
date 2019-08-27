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
package com.espertech.esper.common.client.hook.enummethod;

import com.espertech.esper.common.internal.epl.methodbase.DotMethodFP;

/**
 * Provides footprint information for enumeration method extension.
 */
public class EnumMethodDescriptor {
    private final DotMethodFP[] footprints;

    /**
     * Ctor.
     * @param footprints footprint array, one array item for each distinct footprint
     */
    public EnumMethodDescriptor(DotMethodFP[] footprints) {
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
