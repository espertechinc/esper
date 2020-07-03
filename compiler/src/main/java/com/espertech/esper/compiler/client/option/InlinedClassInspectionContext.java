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

import org.codehaus.janino.util.ClassFile;

/**
 * Provides the environment to {@link InlinedClassInspectionOption}.
 */
public class InlinedClassInspectionContext {
    private final ClassFile[] janinoClassFiles;

    /**
     * Ctor.
     * @param janinoClassFiles Janino class files
     */
    public InlinedClassInspectionContext(ClassFile[] janinoClassFiles) {
        this.janinoClassFiles = janinoClassFiles;
    }

    /**
     * Returns the Janino class files
     * @return Janino class files
     */
    public ClassFile[] getJaninoClassFiles() {
        return janinoClassFiles;
    }
}
