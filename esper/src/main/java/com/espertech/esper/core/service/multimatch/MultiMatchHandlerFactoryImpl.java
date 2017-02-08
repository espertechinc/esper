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

public class MultiMatchHandlerFactoryImpl implements MultiMatchHandlerFactory {
    public MultiMatchHandler getDefaultHandler() {
        return MultiMatchHandlerSubqueryPreevalNoDedup.INSTANCE;
    }

    public MultiMatchHandler makeNoDedupNoSubq() {
        return MultiMatchHandlerNoSubqueryNoDedup.INSTANCE;
    }

    public MultiMatchHandler makeNoDedupSubselectPreval() {
        return MultiMatchHandlerSubqueryPreevalNoDedup.INSTANCE;
    }

    public MultiMatchHandler makeNoDedupSubselectPosteval() {
        return MultiMatchHandlerSubqueryPostevalNoDedup.INSTANCE;
    }

    public MultiMatchHandler makeDedupNoSubq() {
        return MultiMatchHandlerNoSubqueryWDedup.INSTANCE;
    }

    public MultiMatchHandler makeDedupSubq(boolean isSubselectPreeval) {
        return new MultiMatchHandlerSubqueryWDedup(isSubselectPreeval);
    }
}
