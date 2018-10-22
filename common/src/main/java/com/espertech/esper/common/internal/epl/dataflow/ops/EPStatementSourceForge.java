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
package com.espertech.esper.common.internal.epl.dataflow.ops;

import com.espertech.esper.common.client.dataflow.annotations.DataFlowOpParameter;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.context.aifactory.core.SAIFFInitializeBuilder;
import com.espertech.esper.common.internal.context.aifactory.core.SAIFFInitializeSymbol;
import com.espertech.esper.common.internal.epl.dataflow.interfaces.DataFlowOpForgeInitializeContext;
import com.espertech.esper.common.internal.epl.dataflow.interfaces.DataFlowOpForgeInitializeResult;
import com.espertech.esper.common.internal.epl.dataflow.interfaces.DataFlowOpOutputPort;
import com.espertech.esper.common.internal.epl.dataflow.interfaces.DataFlowOperatorForge;
import com.espertech.esper.common.internal.epl.expression.core.ExprNode;
import com.espertech.esper.common.internal.epl.expression.core.ExprValidationException;

import java.util.Map;

import static com.espertech.esper.common.internal.epl.dataflow.core.EPDataFlowServiceImpl.OP_PACKAGE_NAME;

public class EPStatementSourceForge implements DataFlowOperatorForge {

    @DataFlowOpParameter
    private ExprNode statementDeploymentId;

    @DataFlowOpParameter
    private ExprNode statementName;

    @DataFlowOpParameter
    private Map<String, Object> statementFilter; // interface EPDataFlowEPStatementFilter

    @DataFlowOpParameter
    private Map<String, Object> collector; // interface EPDataFlowIRStreamCollector

    private boolean submitEventBean;

    public DataFlowOpForgeInitializeResult initializeForge(DataFlowOpForgeInitializeContext context) throws ExprValidationException {

        if (context.getOutputPorts().size() != 1) {
            throw new IllegalArgumentException("EPStatementSource operator requires one output stream but produces " + context.getOutputPorts().size() + " streams");
        }

        if (statementName != null && statementFilter != null) {
            throw new ExprValidationException("Both 'statementName' or 'statementFilter' parameters were provided, only either one is expected");
        }
        if ((statementDeploymentId == null && statementName != null) |
                (statementDeploymentId != null && statementName == null)) {
            throw new ExprValidationException("Both 'statementDeploymentId' and 'statementName' are required when either of these are specified");
        }

        DataFlowOpOutputPort portZero = context.getOutputPorts().get(0);
        if (portZero != null && portZero.getOptionalDeclaredType() != null && portZero.getOptionalDeclaredType().isWildcard()) {
            submitEventBean = true;
        }

        return null;
    }

    public CodegenExpression make(CodegenMethodScope parent, SAIFFInitializeSymbol symbols, CodegenClassScope classScope) {
        return new SAIFFInitializeBuilder(OP_PACKAGE_NAME + ".epstatementsource.EPStatementSourceFactory", this.getClass(), "stmtSrc", parent, symbols, classScope)
                .exprnode("statementDeploymentId", statementDeploymentId)
                .exprnode("statementName", statementName)
                .map("statementFilter", statementFilter)
                .map("collector", collector)
                .constant("submitEventBean", submitEventBean)
                .build();
    }
}
