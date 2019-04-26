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
package com.espertech.esper.common.internal.context.aifactory.ontrigger.ontrigger;

import com.espertech.esper.common.internal.compile.stage3.StmtClassForgeableFactory;
import com.espertech.esper.common.internal.epl.expression.core.ExprNode;
import com.espertech.esper.common.internal.epl.expression.subquery.ExprSubselectNode;
import com.espertech.esper.common.internal.epl.expression.table.ExprTableAccessNode;
import com.espertech.esper.common.internal.epl.resultset.core.ResultSetProcessorDesc;
import com.espertech.esper.common.internal.epl.subselect.SubSelectFactoryForge;
import com.espertech.esper.common.internal.epl.table.strategy.ExprTableEvalStrategyFactoryForge;

import java.util.List;
import java.util.Map;

public class OnTriggerPlanValidationResult {
    private final Map<ExprSubselectNode, SubSelectFactoryForge> subselectForges;
    private final Map<ExprTableAccessNode, ExprTableEvalStrategyFactoryForge> tableAccessForges;
    private final ResultSetProcessorDesc resultSetProcessorPrototype;
    private final ExprNode validatedJoin;
    private final String zeroStreamAliasName;
    private final List<StmtClassForgeableFactory> additionalForgeables;

    public OnTriggerPlanValidationResult(Map<ExprSubselectNode, SubSelectFactoryForge> subselectForges, Map<ExprTableAccessNode, ExprTableEvalStrategyFactoryForge> tableAccessForges, ResultSetProcessorDesc resultSetProcessorPrototype, ExprNode validatedJoin, String zeroStreamAliasName, List<StmtClassForgeableFactory> additionalForgeables) {
        this.subselectForges = subselectForges;
        this.tableAccessForges = tableAccessForges;
        this.resultSetProcessorPrototype = resultSetProcessorPrototype;
        this.validatedJoin = validatedJoin;
        this.zeroStreamAliasName = zeroStreamAliasName;
        this.additionalForgeables = additionalForgeables;
    }

    public Map<ExprSubselectNode, SubSelectFactoryForge> getSubselectForges() {
        return subselectForges;
    }

    public Map<ExprTableAccessNode, ExprTableEvalStrategyFactoryForge> getTableAccessForges() {
        return tableAccessForges;
    }

    public ResultSetProcessorDesc getResultSetProcessorPrototype() {
        return resultSetProcessorPrototype;
    }

    public ExprNode getValidatedJoin() {
        return validatedJoin;
    }

    public String getZeroStreamAliasName() {
        return zeroStreamAliasName;
    }

    public List<StmtClassForgeableFactory> getAdditionalForgeables() {
        return additionalForgeables;
    }
}
