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
package com.espertech.esper.common.internal.epl.historical.database.core;

import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionRef;
import com.espertech.esper.common.internal.compile.multikey.MultiKeyPlan;
import com.espertech.esper.common.internal.compile.multikey.MultiKeyPlanner;
import com.espertech.esper.common.internal.compile.stage3.StatementBaseInfo;
import com.espertech.esper.common.internal.compile.stage3.StatementCompileTimeServices;
import com.espertech.esper.common.internal.compile.stage3.StmtClassForgeableFactory;
import com.espertech.esper.common.internal.context.aifactory.core.SAIFFInitializeSymbol;
import com.espertech.esper.common.internal.epl.expression.core.*;
import com.espertech.esper.common.internal.epl.expression.visitor.ExprNodeIdentifierCollectVisitor;
import com.espertech.esper.common.internal.epl.historical.common.HistoricalEventViewableForgeBase;
import com.espertech.esper.common.internal.epl.streamtype.StreamTypeService;
import com.espertech.esper.common.internal.util.CollectionUtil;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

public class HistoricalEventViewableDatabaseForge extends HistoricalEventViewableForgeBase {
    private final String databaseName;
    private final String[] inputParameters;
    private final String preparedStatementText;
    private final Map<String, DBOutputTypeDesc> outputTypes;

    public HistoricalEventViewableDatabaseForge(int streamNum, EventType eventType, String databaseName, String[] inputParameters, String preparedStatementText, Map<String, DBOutputTypeDesc> outputTypes) {
        super(streamNum, eventType);
        this.databaseName = databaseName;
        this.inputParameters = inputParameters;
        this.preparedStatementText = preparedStatementText;
        this.outputTypes = outputTypes;
    }

    public List<StmtClassForgeableFactory> validate(StreamTypeService typeService, StatementBaseInfo base, StatementCompileTimeServices services)
        throws ExprValidationException {

        int count = 0;
        ExprValidationContext validationContext = new ExprValidationContextBuilder(typeService, base.getStatementRawInfo(), services)
            .withAllowBindingConsumption(true).build();
        ExprNode[] inputParamNodes = new ExprNode[inputParameters.length];
        for (String inputParam : inputParameters) {
            ExprNode raw = findSQLExpressionNode(streamNum, count, base.getStatementSpec().getRaw().getSqlParameters());
            if (raw == null) {
                throw new ExprValidationException("Internal error find expression for historical stream parameter " + count + " stream " + streamNum);
            }
            ExprNode evaluator = ExprNodeUtilityValidate.getValidatedSubtree(ExprNodeOrigin.DATABASEPOLL, raw, validationContext);
            inputParamNodes[count++] = evaluator;

            ExprNodeIdentifierCollectVisitor visitor = new ExprNodeIdentifierCollectVisitor();
            visitor.visit(evaluator);
            for (ExprIdentNode identNode : visitor.getExprProperties()) {
                if (identNode.getStreamId() == streamNum) {
                    throw new ExprValidationException("Invalid expression '" + inputParam + "' resolves to the historical data itself");
                }
                subordinateStreams.add(identNode.getStreamId());
            }
        }
        this.inputParamEvaluators = ExprNodeUtilityQuery.getForges(inputParamNodes);

        // plan multikey
        MultiKeyPlan multiKeyPlan = MultiKeyPlanner.planMultiKey(inputParamEvaluators, false, base.getStatementRawInfo(), services.getSerdeResolver());
        this.multiKeyClassRef = multiKeyPlan.getClassRef();

        return multiKeyPlan.getMultiKeyForgeables();
    }

    public Class typeOfImplementation() {
        return HistoricalEventViewableDatabaseFactory.class;
    }

    public void codegenSetter(CodegenExpressionRef ref, CodegenMethod method, SAIFFInitializeSymbol symbols, CodegenClassScope classScope) {
        method.getBlock()
            .exprDotMethod(ref, "setDatabaseName", constant(databaseName))
            .exprDotMethod(ref, "setInputParameters", constant(inputParameters))
            .exprDotMethod(ref, "setPreparedStatementText", constant(preparedStatementText))
            .exprDotMethod(ref, "setOutputTypes", makeOutputTypes(method, symbols, classScope));
    }

    private CodegenExpression makeOutputTypes(CodegenMethodScope parent, SAIFFInitializeSymbol symbols, CodegenClassScope classScope) {
        CodegenMethod method = parent.makeChild(Map.class, this.getClass(), classScope);
        method.getBlock().declareVar(Map.class, "types", newInstance(HashMap.class, constant(CollectionUtil.capacityHashMap(outputTypes.size()))));
        for (Map.Entry<String, DBOutputTypeDesc> entry : outputTypes.entrySet()) {
            method.getBlock().exprDotMethod(ref("types"), "put", constant(entry.getKey()), entry.getValue().make());
        }
        method.getBlock().methodReturn(ref("types"));
        return localMethod(method);
    }

    private static ExprNode findSQLExpressionNode(int myStreamNumber, int count, Map<Integer, List<ExprNode>> sqlParameters) {
        if ((sqlParameters == null) || (sqlParameters.isEmpty())) {
            return null;
        }
        List<ExprNode> parameters = sqlParameters.get(myStreamNumber);
        if ((parameters == null) || (parameters.isEmpty()) || (parameters.size() < (count + 1))) {
            return null;
        }
        return parameters.get(count);
    }
}
