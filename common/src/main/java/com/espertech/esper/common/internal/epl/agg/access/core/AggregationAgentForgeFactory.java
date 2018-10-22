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
package com.espertech.esper.common.internal.epl.agg.access.core;

import com.espertech.esper.common.internal.epl.expression.core.ExprForge;
import com.espertech.esper.common.internal.epl.expression.core.ExprNode;
import com.espertech.esper.common.internal.settings.ClasspathImportService;

public class AggregationAgentForgeFactory {
    public static AggregationAgentForge make(int streamNum, ExprNode optionalFilter, ClasspathImportService classpathImportService, boolean isFireAndForget, String statementName) {
        ExprForge evaluator = optionalFilter == null ? null : optionalFilter.getForge();
        if (streamNum == 0) {
            if (optionalFilter == null) {
                return AggregationAgentDefault.INSTANCE;
            } else {
                return new AggregationAgentDefaultWFilterForge(evaluator);
            }
        } else {
            if (optionalFilter == null) {
                return new AggregationAgentRewriteStreamForge(streamNum);
            } else {
                return new AggregationAgentRewriteStreamWFilterForge(streamNum, evaluator);
            }
        }
    }
}
