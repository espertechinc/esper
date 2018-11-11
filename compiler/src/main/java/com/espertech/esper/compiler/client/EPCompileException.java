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
package com.espertech.esper.compiler.client;

import com.espertech.esper.common.client.util.ExceptionWithLineItems;

import java.util.Collections;
import java.util.List;

/**
 * Indicates an exception compiling a module or fire-and-forget query
 * <p>
 * May carry information on individual items.
 * </p>
 */
public class EPCompileException extends Exception implements ExceptionWithLineItems {

    private final List<EPCompileExceptionItem> items;

    /**
     * Ctor.
     *
     * @param message message
     */
    public EPCompileException(String message) {
        super(message);
        items = Collections.emptyList();
    }

    /**
     * Ctor
     *
     * @param message message
     * @param cause   cause
     */
    public EPCompileException(String message, Throwable cause) {
        super(message, cause);
        items = Collections.emptyList();
    }

    /**
     * Ctor
     *
     * @param message message
     * @param cause   cause
     * @param items   additional information on items
     */
    public EPCompileException(String message, Throwable cause, List<EPCompileExceptionItem> items) {
        super(message, cause);
        this.items = items;
    }

    /**
     * Returns compilation items.
     *
     * @return items
     */

    public List<EPCompileExceptionItem> getItems() {
        return items;
    }
}
