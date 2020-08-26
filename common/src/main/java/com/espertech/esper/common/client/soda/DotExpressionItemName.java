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
package com.espertech.esper.common.client.soda;

import java.io.Serializable;
import java.io.StringWriter;

/**
 * Dot-expresson item representing an identifier without parameters.
 */
public class DotExpressionItemName extends DotExpressionItem implements Serializable {

    private static final long serialVersionUID = 7090706540657160089L;
    private String name;

    /**
     * Ctor.
     */
    public DotExpressionItemName() {
    }

    /**
     * Ctor.
     *
     * @param name       the property name
     */
    public DotExpressionItemName(String name) {
        this.name = name;
    }

    /**
     * Returns method name or nested property name.
     *
     * @return method name or nested property name
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the method name or nested property name.
     *
     * @param name method name or nested property name to set
     */
    public void setName(String name) {
        this.name = name;
    }

    public void renderItem(StringWriter writer) {
        writer.append(name);
    }
}
