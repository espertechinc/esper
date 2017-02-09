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
 * A wrapper for a thread-local to hold the current version for variables visible for a thread, as well
 * as uncommitted values of variables for a thread.
 */
public class VariableVersionThreadLocal {
    private ThreadLocal<VariableVersionThreadEntry> perThreadVersion;

    /**
     * Ctor.
     */
    public VariableVersionThreadLocal() {
        perThreadVersion = new ThreadLocal<VariableVersionThreadEntry>() {
            protected synchronized VariableVersionThreadEntry initialValue() {
                return new VariableVersionThreadEntry(0, null);
            }
        };
    }

    /**
     * Returns the version and uncommitted values for the current thread.
     *
     * @return entry for current thread
     */
    public VariableVersionThreadEntry getCurrentThread() {
        VariableVersionThreadEntry entry = perThreadVersion.get();
        if (entry == null) {
            entry = new VariableVersionThreadEntry(0, null);
            perThreadVersion.set(entry);
        }
        return entry;
    }
}
