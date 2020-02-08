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
package com.espertech.esper.common.internal.epl.classprovided.compiletime;

import com.espertech.esper.common.internal.epl.classprovided.core.ClassProvided;
import com.espertech.esper.common.internal.epl.util.CompileTimeResolver;

import java.util.Map;

public interface ClassProvidedCompileTimeResolver extends CompileTimeResolver {
    ClassProvided resolve(String name);

    boolean isEmpty();

    void addTo(Map<String, byte[]> additionalClasses);

    void removeFrom(Map<String, byte[]> moduleBytes);
}
