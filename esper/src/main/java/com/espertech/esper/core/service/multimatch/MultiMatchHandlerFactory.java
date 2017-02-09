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
package com.espertech.esper.core.service.multimatch;

public interface MultiMatchHandlerFactory {
    MultiMatchHandler getDefaultHandler();

    MultiMatchHandler makeNoDedupNoSubq();

    MultiMatchHandler makeNoDedupSubselectPreval();

    MultiMatchHandler makeNoDedupSubselectPosteval();

    MultiMatchHandler makeDedupNoSubq();

    MultiMatchHandler makeDedupSubq(boolean isSubselectPreeval);
}
