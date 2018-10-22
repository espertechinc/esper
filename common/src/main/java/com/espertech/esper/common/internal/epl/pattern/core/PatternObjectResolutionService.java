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
package com.espertech.esper.common.internal.epl.pattern.core;

import com.espertech.esper.common.internal.compile.stage1.spec.PatternGuardSpec;
import com.espertech.esper.common.internal.compile.stage1.spec.PatternObserverSpec;
import com.espertech.esper.common.internal.epl.pattern.guard.GuardForge;
import com.espertech.esper.common.internal.epl.pattern.observer.ObserverForge;

/**
 * Factory service for resolving pattern objects such as guards and observers.
 */
public interface PatternObjectResolutionService {
    /**
     * Creates an observer factory considering configured plugged-in resources.
     *
     * @param spec is the namespace, name and parameters for the observer
     * @return observer factory
     * @throws PatternObjectException if the observer cannot be resolved
     */
    ObserverForge create(PatternObserverSpec spec) throws PatternObjectException;

    /**
     * Creates a guard factory considering configured plugged-in resources.
     *
     * @param spec is the namespace, name and parameters for the guard
     * @return guard factory
     * @throws PatternObjectException if the guard cannot be resolved
     */
    GuardForge create(PatternGuardSpec spec) throws PatternObjectException;
}
