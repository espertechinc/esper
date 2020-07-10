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
package com.espertech.esper.common.internal.epl.index.unindexed;

import com.espertech.esper.common.client.type.EPTypeClass;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.context.aifactory.core.SAIFFInitializeSymbol;
import com.espertech.esper.common.internal.epl.index.base.EventTableFactoryFactoryForgeBase;
import com.espertech.esper.common.client.util.StateMgmtSetting;

import java.util.Collections;
import java.util.List;

public class UnindexedEventTableFactoryFactoryForge extends EventTableFactoryFactoryForgeBase {
    private final StateMgmtSetting stateMgmtSettings;

    public UnindexedEventTableFactoryFactoryForge(int indexedStreamNum, Integer subqueryNum, boolean isFireAndForget, StateMgmtSetting stateMgmtSettings) {
        super(indexedStreamNum, subqueryNum, isFireAndForget);
        this.stateMgmtSettings = stateMgmtSettings;
    }

    public String toQueryPlan() {
        return this.getClass().getSimpleName() + " streamNum=" + indexedStreamNum;
    }

    public Class getEventTableClass() {
        return UnindexedEventTable.class;
    }

    protected EPTypeClass typeOf() {
        return UnindexedEventTableFactoryFactory.EPTYPE;
    }

    protected List<CodegenExpression> additionalParams(CodegenMethod method, SAIFFInitializeSymbol symbols, CodegenClassScope classScope) {
        return Collections.singletonList(stateMgmtSettings.toExpression());
    }
}
