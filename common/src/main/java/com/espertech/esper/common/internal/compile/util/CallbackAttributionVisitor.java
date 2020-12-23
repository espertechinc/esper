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
package com.espertech.esper.common.internal.compile.util;

public interface CallbackAttributionVisitor<T> {
    T accept(CallbackAttributionSubquery attribution);
    T accept(CallbackAttributionStreamPattern attribution);
    T accept(CallbackAttributionContextController attribution);
    T accept(CallbackAttributionContextCondition attribution);
    T accept(CallbackAttributionContextConditionPattern attribution);
    T accept(CallbackAttributionNamedWindow attribution);
    T accept(CallbackAttributionStream attribution);
    T accept(CallbackAttributionDataflow attribution);
    T accept(CallbackAttributionMatchRecognize attribution);
    T accept(CallbackAttributionOutputRate attribution);
    T accept(CallbackAttributionStreamGrouped attribution);
    T accept(CallbackAttributionSubqueryGrouped attribution);
}
