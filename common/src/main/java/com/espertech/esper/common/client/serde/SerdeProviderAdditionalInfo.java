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
package com.espertech.esper.common.client.serde;

import com.espertech.esper.common.client.util.StatementType;
import com.espertech.esper.common.internal.compile.stage2.StatementRawInfo;

import java.lang.annotation.Annotation;

/**
 * For use with high-availability and scale-out only, this class provides additional information passed to serde provider, for use with {@link SerdeProvider}
 */
public abstract class SerdeProviderAdditionalInfo {
    private final StatementRawInfo raw;

    /**
     * Ctor.
     * @param raw statement information
     */
    public SerdeProviderAdditionalInfo(StatementRawInfo raw) {
        this.raw = raw;
    }

    /**
     * Returns the statement name
     * @return name
     */
    public String getStatementName() {
        return raw.getStatementName();
    }

    /**
     * Returns the statement annotations
     * @return annotations
     */
    public Annotation[] getAnnotations() {
        return raw.getAnnotations();
    }

    /**
     * Returns the statement type
     * @return statement type
     */
    public StatementType getStatementType() {
        return raw.getStatementType();
    }

    /**
     * Returns the context name or null if no context associated
     * @return context name
     */
    public String getContextName() {
        return raw.getContextName();
    }

    /**
     * Returns the module name
     * @return module name
     */
    public String getModuleName() {
        return raw.getModuleName();
    }
}
