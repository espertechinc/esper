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
package com.espertech.esper.common.internal.epl.approx.countminsketch;

import com.espertech.esper.common.client.type.EPTypeClass;
import com.espertech.esper.common.client.util.CountMinSketchAgentForge;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

public class CountMinSketchSpecForge {
    public final static EPTypeClass EPTYPE = new EPTypeClass(CountMinSketchSpecForge.class);

    private CountMinSketchSpecHashes hashesSpec;
    private Integer topkSpec;
    private CountMinSketchAgentForge agent;

    public CountMinSketchSpecForge(CountMinSketchSpecHashes hashesSpec, Integer topkSpec, CountMinSketchAgentForge agent) {
        this.hashesSpec = hashesSpec;
        this.topkSpec = topkSpec;
        this.agent = agent;
    }

    public CountMinSketchSpecHashes getHashesSpec() {
        return hashesSpec;
    }

    public Integer getTopkSpec() {
        return topkSpec;
    }

    public void setTopkSpec(Integer topkSpec) {
        this.topkSpec = topkSpec;
    }

    public CountMinSketchAgentForge getAgent() {
        return agent;
    }

    public void setAgent(CountMinSketchAgentForge agent) {
        this.agent = agent;
    }

    public CodegenExpression codegenMake(CodegenMethodScope parent, CodegenClassScope classScope) {
        CodegenMethod method = parent.makeChild(CountMinSketchSpec.EPTYPE, this.getClass(), classScope);
        method.getBlock()
                .declareVarNewInstance(CountMinSketchSpec.EPTYPE, "spec")
                .exprDotMethod(ref("spec"), "setHashesSpec", hashesSpec.codegenMake(method, classScope))
                .exprDotMethod(ref("spec"), "setTopkSpec", constant(topkSpec))
                .exprDotMethod(ref("spec"), "setAgent", agent.codegenMake(method, classScope))
                .methodReturn(ref("spec"));
        return localMethod(method);
    }
}

