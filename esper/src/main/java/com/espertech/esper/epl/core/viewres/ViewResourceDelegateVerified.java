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
package com.espertech.esper.epl.core.viewres;

/**
 * Coordinates between view factories and requested resource (by expressions) the
 * availability of view resources to expressions.
 */
public class ViewResourceDelegateVerified {
    private final boolean hasPrior;
    private final boolean hasPrevious;
    private final ViewResourceDelegateVerifiedStream[] perStream;

    public ViewResourceDelegateVerified(boolean hasPrior, boolean hasPrevious, ViewResourceDelegateVerifiedStream[] perStream) {
        this.hasPrior = hasPrior;
        this.hasPrevious = hasPrevious;
        this.perStream = perStream;
    }

    public ViewResourceDelegateVerifiedStream[] getPerStream() {
        return perStream;
    }

    public boolean isHasPrior() {
        return hasPrior;
    }

    public boolean isHasPrevious() {
        return hasPrevious;
    }
}
