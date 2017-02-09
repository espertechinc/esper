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
package com.espertech.esper.epl.variable;

/**
 * A holder for versioned values that holds a current version-value and a prior version-value pair.
 */
public class CurrentValue<T> {
    private VersionedValue<T> currentVersion;
    private VersionedValue<T> priorVersion;

    /**
     * Ctor.
     *
     * @param currentVersion current version and value
     * @param priorVersion   prior version and value
     */
    public CurrentValue(VersionedValue<T> currentVersion, VersionedValue<T> priorVersion) {
        this.currentVersion = currentVersion;
        this.priorVersion = priorVersion;
    }

    /**
     * Returns the current version.
     *
     * @return current version
     */
    public VersionedValue<T> getCurrentVersion() {
        return currentVersion;
    }

    /**
     * Returns the prior version.
     *
     * @return prior version
     */
    public VersionedValue<T> getPriorVersion() {
        return priorVersion;
    }
}
