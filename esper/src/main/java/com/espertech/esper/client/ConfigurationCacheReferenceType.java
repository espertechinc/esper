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
package com.espertech.esper.client;

/**
 * Enum indicating what kind of references are used to store the cache map's keys and values.
 */
public enum ConfigurationCacheReferenceType {
    /**
     * Constant indicating that hard references should be used.
     * <p>
     * Does not allow garbage collection to remove cache entries.
     */
    HARD,

    /**
     * Constant indicating that soft references should be used.
     * <p>
     * Allows garbage collection to remove cache entries only after all weak references have been collected.
     */
    SOFT,

    /**
     * Constant indicating that weak references should be used.
     * <p>
     * Allows garbage collection to remove cache entries.
     */
    WEAK;

    /**
     * The default policy is set to WEAK to reduce the chance that out-of-memory errors occur
     * as caches fill, and stay backwards compatible with prior Esper releases.
     *
     * @return default reference type
     */
    public static ConfigurationCacheReferenceType getDefault() {
        return WEAK;
    }
}
