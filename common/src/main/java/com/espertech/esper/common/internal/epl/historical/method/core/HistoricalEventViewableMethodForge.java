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
package com.espertech.esper.common.internal.epl.historical.method.core;

import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionRef;
import com.espertech.esper.common.internal.collection.Pair;
import com.espertech.esper.common.internal.compile.multikey.MultiKeyPlanner;
import com.espertech.esper.common.internal.compile.multikey.MultiKeyPlan;
import com.espertech.esper.common.internal.compile.stage1.spec.MethodStreamSpec;
import com.espertech.esper.common.internal.compile.stage3.StatementBaseInfo;
import com.espertech.esper.common.internal.compile.stage3.StatementCompileTimeServices;
import com.espertech.esper.common.internal.compile.stage3.StmtClassForgeableFactory;
import com.espertech.esper.common.internal.context.aifactory.core.SAIFFInitializeSymbol;
import com.espertech.esper.common.internal.epl.expression.core.*;
import com.espertech.esper.common.internal.epl.expression.visitor.ExprNodeIdentifierAndStreamRefVisitor;
import com.espertech.esper.common.internal.epl.historical.common.HistoricalEventViewableForgeBase;
import com.espertech.esper.common.internal.epl.historical.method.poll.MethodConversionStrategyForge;
import com.espertech.esper.common.internal.epl.historical.method.poll.MethodTargetStrategyForge;
import com.espertech.esper.common.internal.epl.streamtype.StreamTypeService;
import com.espertech.esper.common.internal.util.JavaClassHelper;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.constant;

public class HistoricalEventViewableMethodForge extends HistoricalEventViewableForgeBase {

    private final MethodStreamSpec methodStreamSpec;
    private final MethodPollingViewableMeta metadata;

    private MethodTargetStrategyForge target;
    private MethodConversionStrategyForge conversion;

    public HistoricalEventViewableMethodForge(int streamNum, EventType eventType, MethodStreamSpec methodStreamSpec, MethodPollingViewableMeta metadata) {
        super(streamNum, eventType);
        this.methodStreamSpec = methodStreamSpec;
        this.metadata = metadata;
    }

    public List<StmtClassForgeableFactory> validate(StreamTypeService typeService, StatementBaseInfo base, StatementCompileTimeServices services)
            throws ExprValidationException {
        // validate and visit
        ExprValidationContext validationContext = new ExprValidationContextBuilder(typeService, base.getStatementRawInfo(), services).withAllowBindingConsumption(true).build();

        ExprNodeIdentifierAndStreamRefVisitor visitor = new ExprNodeIdentifierAndStreamRefVisitor(true);
        final List<ExprNode> validatedInputParameters = new ArrayList<ExprNode>();
        for (ExprNode exprNode : methodStreamSpec.getExpressions()) {
            ExprNode validated = ExprNodeUtilityValidate.getValidatedSubtree(ExprNodeOrigin.METHODINVJOIN, exprNode, validationContext);
            validatedInputParameters.add(validated);
            validated.accept(visitor);
        }

        // determine required streams
        for (ExprNodePropOrStreamDesc ref : visitor.getRefs()) {
            subordinateStreams.add(ref.getStreamNum());
        }

        // class-based evaluation
        Method targetMethod = null;
        if (metadata.getMethodProviderClass() != null) {
            // resolve actual method to use
            ExprNodeUtilResolveExceptionHandler handler = new ExprNodeUtilResolveExceptionHandler() {
                public ExprValidationException handle(Exception e) {
                    if (methodStreamSpec.getExpressions().size() == 0) {
                        return new ExprValidationException("Method footprint does not match the number or type of expression parameters, expecting no parameters in method: " + e.getMessage());
                    }
                    Class[] resultTypes = ExprNodeUtilityQuery.getExprResultTypes(validatedInputParameters);
                    return new ExprValidationException("Method footprint does not match the number or type of expression parameters, expecting a method where parameters are typed '" +
                            JavaClassHelper.getParameterAsString(resultTypes) + "': " + e.getMessage());
                }
            };
            ExprNodeUtilMethodDesc desc = ExprNodeUtilityResolve.resolveMethodAllowWildcardAndStream(
                    metadata.getMethodProviderClass().getName(), metadata.isStaticMethod() ? null : metadata.getMethodProviderClass(),
                    methodStreamSpec.getMethodName(), validatedInputParameters, false, null, handler, methodStreamSpec.getMethodName(),
                    base.getStatementRawInfo(), services);
            this.inputParamEvaluators = desc.getChildForges();
            targetMethod = desc.getReflectionMethod();
        } else {
            // script-based evaluation
            this.inputParamEvaluators = ExprNodeUtilityQuery.getForges(ExprNodeUtilityQuery.toArray(validatedInputParameters));
        }

        // plan multikey
        MultiKeyPlan multiKeyPlan = MultiKeyPlanner.planMultiKey(inputParamEvaluators, false, base.getStatementRawInfo(), services.getSerdeResolver());
        this.multiKeyClassRef = multiKeyPlan.getClassRef();

        Pair<MethodTargetStrategyForge, MethodConversionStrategyForge> strategies = PollExecStrategyPlanner.plan(metadata, targetMethod, eventType);
        this.target = strategies.getFirst();
        this.conversion = strategies.getSecond();

        return multiKeyPlan.getMultiKeyForgeables();
    }

    public Class typeOfImplementation() {
        return HistoricalEventViewableMethodFactory.class;
    }

    public void codegenSetter(CodegenExpressionRef ref, CodegenMethod method, SAIFFInitializeSymbol symbols, CodegenClassScope classScope) {
        String configName = metadata.getMethodProviderClass() != null ? metadata.getMethodProviderClass().getName() : methodStreamSpec.getMethodName();
        method.getBlock()
                .exprDotMethod(ref, "setConfigurationName", constant(configName))
                .exprDotMethod(ref, "setTargetStrategy", target.make(method, symbols, classScope))
                .exprDotMethod(ref, "setConversionStrategy", conversion.make(method, symbols, classScope));
    }
}
