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
package com.espertech.esper.compiler.client.option;

/**
 * Implement this interface to receive Janino-specific class detail for inlined-classes.
 * <p>
 * The compiler invokes the callback for each inlined-class that it compiles.
 * </p>
 */
public interface InlinedClassInspectionOption {
    /**
     * Provides Janino-specific class detail for inlined-classes
     *
     * @param env compiler-specific information for inlined classes
     */
    void visit(InlinedClassInspectionContext env);
}
