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
package com.espertech.esper.common.client.configuration.runtime;

import com.espertech.esper.common.client.util.FilterServiceProfile;

import java.io.Serializable;

/**
 * Holds runtime execution-related settings.
 */
public class ConfigurationRuntimeExecution implements Serializable {
    private static final long serialVersionUID = -7222514049255015505L;

    private boolean prioritized;
    private boolean fairlock;
    private boolean disableLocking;
    private FilterServiceProfile filterServiceProfile = FilterServiceProfile.READMOSTLY;
    private int declaredExprValueCacheSize = 1;

    /**
     * Ctor - sets up defaults.
     */
    protected ConfigurationRuntimeExecution() {
        prioritized = false;
    }

    /**
     * Returns false (the default) if the runtime does not consider statement priority and preemptive instructions,
     * or true to enable priority-based statement execution order.
     *
     * @return false by default to indicate unprioritized statement execution
     */
    public boolean isPrioritized() {
        return prioritized;
    }

    /**
     * Set to false (the default) if the runtime does not consider statement priority and preemptive instructions,
     * or true for enable priority-based statement execution order.
     *
     * @param prioritized false by default to indicate unprioritized statement execution
     */
    public void setPrioritized(boolean prioritized) {
        this.prioritized = prioritized;
    }

    /**
     * Returns true for fair locking, false for unfair locks.
     *
     * @return fairness flag
     */
    public boolean isFairlock() {
        return fairlock;
    }

    /**
     * Set to true for fair locking, false for unfair locks.
     *
     * @param fairlock fairness flag
     */
    public void setFairlock(boolean fairlock) {
        this.fairlock = fairlock;
    }

    /**
     * Returns indicator whether statement-level locks are disabled.
     * The default is false meaning statement-level locks are taken by default and depending on EPL optimizations.
     * If set to true statement-level locks are never taken.
     *
     * @return indicator for statement-level locks
     */
    public boolean isDisableLocking() {
        return disableLocking;
    }

    /**
     * Set to true to indicate that statement-level locks are disabled.
     * The default is false meaning statement-level locks are taken by default and depending on EPL optimizations.
     * If set to true statement-level locks are never taken.
     *
     * @param disableLocking false to take statement-level locks as required, or true to disable statement-level locking
     */
    public void setDisableLocking(boolean disableLocking) {
        this.disableLocking = disableLocking;
    }

    /**
     * Returns the filter service profile for tuning filtering operations.
     *
     * @return filter service profile
     */
    public FilterServiceProfile getFilterServiceProfile() {
        return filterServiceProfile;
    }

    /**
     * Set the filter service profile for tuning filtering operations.
     *
     * @param filterServiceProfile filter service profile
     */
    public void setFilterServiceProfile(FilterServiceProfile filterServiceProfile) {
        this.filterServiceProfile = filterServiceProfile;
    }

    /**
     * Returns the cache size for declared expression values
     *
     * @return value
     */
    public int getDeclaredExprValueCacheSize() {
        return declaredExprValueCacheSize;
    }

    /**
     * Sets the cache size for declared expression values
     *
     * @param declaredExprValueCacheSize value
     */
    public void setDeclaredExprValueCacheSize(int declaredExprValueCacheSize) {
        this.declaredExprValueCacheSize = declaredExprValueCacheSize;
    }
}
