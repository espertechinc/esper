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
package com.espertech.esper.client.annotation;

/**
 * Use this annotation to install a statement-specific hook or callback at time of statement creation.
 * <p>
 * See {@link HookType} to the types of hooks that may be installed.
 */
public @interface Hook {
    /**
     * Returns the simple class name (using imports) or fully-qualified class name of the hook.
     *
     * @return class name
     */
    String hook();

    /**
     * Returns hook type.
     *
     * @return hook type
     */
    HookType type();
}
