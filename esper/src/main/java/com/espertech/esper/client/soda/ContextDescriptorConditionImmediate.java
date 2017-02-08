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

import java.io.StringWriter;

/**
 * Context condition that starts/initiates immediately.
 */
public class ContextDescriptorConditionImmediate implements ContextDescriptorCondition {

    private static final long serialVersionUID = 5606387968813709118L;

    /**
     * Ctor.
     */
    public ContextDescriptorConditionImmediate() {
    }

    public void toEPL(StringWriter writer, EPStatementFormatter formatter) {
        writer.write("@now");
    }
}
