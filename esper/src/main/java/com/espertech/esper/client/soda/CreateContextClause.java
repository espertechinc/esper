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
package com.espertech.esper.client.soda;

import java.io.Serializable;
import java.io.StringWriter;

/**
 * Create a context.
 */
public class CreateContextClause implements Serializable {
    private static final long serialVersionUID = 0L;

    private String contextName;
    private ContextDescriptor descriptor;

    /**
     * Ctor.
     */
    public CreateContextClause() {
    }

    /**
     * Ctor.
     *
     * @param contextName context name
     * @param descriptor  context dimension descriptor
     */
    public CreateContextClause(String contextName, ContextDescriptor descriptor) {
        this.contextName = contextName;
        this.descriptor = descriptor;
    }

    /**
     * Returns the context name
     *
     * @return context name
     */
    public String getContextName() {
        return contextName;
    }

    /**
     * Sets the context name
     *
     * @param contextName context name
     */
    public void setContextName(String contextName) {
        this.contextName = contextName;
    }

    /**
     * Returns the context dimension informatin
     *
     * @return context descriptor
     */
    public ContextDescriptor getDescriptor() {
        return descriptor;
    }

    /**
     * Sets the context dimension informatin
     *
     * @param descriptor context descriptor
     */
    public void setDescriptor(ContextDescriptor descriptor) {
        this.descriptor = descriptor;
    }

    /**
     * Render as EPL.
     *
     * @param writer    to output to
     * @param formatter formatter
     */
    public void toEPL(StringWriter writer, EPStatementFormatter formatter) {
        writer.append("create context ");
        writer.append(contextName);
        writer.append(" as ");
        descriptor.toEPL(writer, formatter);
    }
}
