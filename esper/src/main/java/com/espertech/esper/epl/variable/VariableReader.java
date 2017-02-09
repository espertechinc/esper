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
 * Reads and writes variable values.
 * <p>
 * Works closely with {@link VariableService} in determining the version to read.
 */
public class VariableReader {
    private final VariableMetaData variableMetaData;
    private final VariableVersionThreadLocal versionThreadLocal;
    private volatile VersionedValueList<Object> versionsHigh;
    private volatile VersionedValueList<Object> versionsLow;

    public VariableReader(VariableMetaData variableMetaData, VariableVersionThreadLocal versionThreadLocal, VersionedValueList<Object> versionsLow) {
        this.variableMetaData = variableMetaData;
        this.versionThreadLocal = versionThreadLocal;
        this.versionsLow = versionsLow;
        this.versionsHigh = null;
    }

    /**
     * For roll-over (overflow) in version numbers, sets a new collection of versioned-values for the variable
     * to use when requests over the version rollover boundary are made.
     *
     * @param versionsHigh the list of versions for roll-over
     */
    public void setVersionsHigh(VersionedValueList<Object> versionsHigh) {
        this.versionsHigh = versionsHigh;
    }

    /**
     * Sets a new list of versioned-values to inquire against, for use when version numbers roll-over.
     *
     * @param versionsLow the list of versions for read
     */
    public void setVersionsLow(VersionedValueList<Object> versionsLow) {
        this.versionsLow = versionsLow;
    }

    /**
     * Returns the value of a variable.
     * <p>
     * Considers the version set via thread-local for the thread's atomic read of variable values.
     *
     * @return value of variable at the version applicable for the thead
     */
    public Object getValue() {
        VariableVersionThreadEntry entry = versionThreadLocal.getCurrentThread();
        if (entry.getUncommitted() != null) {
            // Check existance as null values are allowed
            if (entry.getUncommitted().containsKey(variableMetaData.getVariableNumber())) {
                return entry.getUncommitted().get(variableMetaData.getVariableNumber()).getSecond();
            }
        }

        int myVersion = entry.getVersion();
        VersionedValueList<Object> versions = versionsLow;
        if (myVersion >= VariableServiceImpl.ROLLOVER_READER_BOUNDARY) {
            if (versionsHigh != null) {
                versions = versionsHigh;
            }
        }
        return versions.getVersion(myVersion);
    }

    public VariableMetaData getVariableMetaData() {
        return variableMetaData;
    }

    public VersionedValueList<Object> getVersionsLow() {
        return versionsLow;
    }
}
