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
package com.espertech.esper.common.internal.view.expression;

import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.client.meta.EventTypeApplicationType;
import com.espertech.esper.common.client.meta.EventTypeIdPair;
import com.espertech.esper.common.client.meta.EventTypeMetadata;
import com.espertech.esper.common.client.meta.EventTypeTypeClass;
import com.espertech.esper.common.client.util.EventTypeBusModifier;
import com.espertech.esper.common.client.util.NameAccessModifier;
import com.espertech.esper.common.internal.bytecodemodel.base.*;
import com.espertech.esper.common.internal.bytecodemodel.core.CodegenClassMethods;
import com.espertech.esper.common.internal.bytecodemodel.core.CodegenCtor;
import com.espertech.esper.common.internal.bytecodemodel.core.CodegenInnerClass;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionRef;
import com.espertech.esper.common.internal.bytecodemodel.name.CodegenFieldNameViewAgg;
import com.espertech.esper.common.internal.bytecodemodel.util.CodegenStackGenerator;
import com.espertech.esper.common.internal.compile.stage3.StmtClassForgeableRSPFactoryProvider;
import com.espertech.esper.common.internal.context.aifactory.core.SAIFFInitializeSymbol;
import com.espertech.esper.common.internal.context.module.EPStatementInitServices;
import com.espertech.esper.common.internal.epl.agg.core.*;
import com.espertech.esper.common.internal.epl.expression.agg.base.ExprAggregateNode;
import com.espertech.esper.common.internal.epl.expression.agg.base.ExprAggregateNodeUtil;
import com.espertech.esper.common.internal.epl.expression.codegen.CodegenLegoMethodExpression;
import com.espertech.esper.common.internal.epl.expression.codegen.ExprForgeCodegenNames;
import com.espertech.esper.common.internal.epl.expression.core.ExprNode;
import com.espertech.esper.common.internal.epl.expression.core.ExprValidationException;
import com.espertech.esper.common.internal.epl.expression.visitor.ExprNodeSummaryVisitor;
import com.espertech.esper.common.internal.epl.expression.visitor.ExprNodeVariableVisitor;
import com.espertech.esper.common.internal.epl.streamtype.StreamTypeService;
import com.espertech.esper.common.internal.epl.streamtype.StreamTypeServiceImpl;
import com.espertech.esper.common.internal.epl.variable.compiletime.VariableMetaData;
import com.espertech.esper.common.internal.epl.variable.core.VariableDeployTimeResolver;
import com.espertech.esper.common.internal.event.core.BaseNestableEventUtil;
import com.espertech.esper.common.internal.event.core.EventTypeUtility;
import com.espertech.esper.common.internal.schedule.ScheduleHandleCallbackProvider;
import com.espertech.esper.common.internal.util.JavaClassHelper;
import com.espertech.esper.common.internal.view.core.*;
import com.espertech.esper.common.internal.view.util.ViewForgeSupport;

import java.util.*;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;
import static com.espertech.esper.common.internal.epl.expression.codegen.ExprForgeCodegenNames.*;

/**
 * Base factory for expression-based window and batch view.
 */
public abstract class ExpressionViewForgeBase extends ViewFactoryForgeBase implements DataWindowViewForge, DataWindowViewForgeWithPrevious, ScheduleHandleCallbackProvider {
    protected ExprNode expiryExpression;
    protected Map<String, VariableMetaData> variableNames;
    protected EventType builtinType;
    protected int scheduleCallbackId = -1;
    protected AggregationServiceForgeDesc aggregationServiceForgeDesc;
    protected int streamNumber;

    protected abstract void makeSetters(CodegenExpressionRef factory, CodegenBlock block);

    public void attach(EventType parentEventType, int streamNumber, ViewForgeEnv viewForgeEnv) throws ViewParameterException {
        this.eventType = parentEventType;
        this.streamNumber = streamNumber;

        // define built-in fields
        LinkedHashMap<String, Object> builtinTypeDef = ExpressionViewOAFieldEnum.asMapOfTypes(eventType);
        String outputEventTypeName = viewForgeEnv.getStatementCompileTimeServices().getEventTypeNameGeneratorStatement().getViewExpr(streamNumber);
        EventTypeMetadata metadata = new EventTypeMetadata(outputEventTypeName, viewForgeEnv.getModuleName(), EventTypeTypeClass.VIEWDERIVED, EventTypeApplicationType.OBJECTARR, NameAccessModifier.TRANSIENT, EventTypeBusModifier.NONBUS, false, EventTypeIdPair.unassigned());
        Map<String, Object> propertyTypes = EventTypeUtility.getPropertyTypesNonPrimitive(builtinTypeDef);
        builtinType = BaseNestableEventUtil.makeOATypeCompileTime(metadata, propertyTypes, null, null, null, null, viewForgeEnv.getBeanEventTypeFactoryProtected(), viewForgeEnv.getEventTypeCompileTimeResolver());
        viewForgeEnv.getEventTypeModuleCompileTimeRegistry().newType(builtinType);

        StreamTypeService streamTypeService = new StreamTypeServiceImpl(new EventType[]{eventType, builtinType}, new String[2], new boolean[2], false, false);

        // validate expression
        expiryExpression = ViewForgeSupport.validateExpr(getViewName(), expiryExpression, streamTypeService, viewForgeEnv, 0, streamNumber);

        ExprNodeSummaryVisitor summaryVisitor = new ExprNodeSummaryVisitor();
        expiryExpression.accept(summaryVisitor);
        if (summaryVisitor.isHasSubselect() || summaryVisitor.isHasStreamSelect() || summaryVisitor.isHasPreviousPrior()) {
            throw new ViewParameterException("Invalid expiry expression: Sub-select, previous or prior functions are not supported in this context");
        }

        Class returnType = expiryExpression.getForge().getEvaluationType();
        if (JavaClassHelper.getBoxedType(returnType) != Boolean.class) {
            throw new ViewParameterException("Invalid return value for expiry expression, expected a boolean return value but received " + JavaClassHelper.getParameterAsString(returnType));
        }

        // determine variables used, if any
        ExprNodeVariableVisitor visitor = new ExprNodeVariableVisitor(viewForgeEnv.getStatementCompileTimeServices().getVariableCompileTimeResolver());
        expiryExpression.accept(visitor);
        variableNames = visitor.getVariableNames();

        // determine aggregation nodes, if any
        List<ExprAggregateNode> aggregateNodes = new ArrayList<ExprAggregateNode>();
        ExprAggregateNodeUtil.getAggregatesBottomUp(expiryExpression, aggregateNodes);
        if (!aggregateNodes.isEmpty()) {
            try {
                aggregationServiceForgeDesc = AggregationServiceFactoryFactory.getService(Collections.emptyList(), Collections.emptyMap(),
                        Collections.emptyList(), null, null, aggregateNodes, Collections.emptyList(), Collections.emptyList(), false,
                        viewForgeEnv.getAnnotations(), viewForgeEnv.getVariableCompileTimeResolver(), false, null, null,
                        streamTypeService.getEventTypes(), null, viewForgeEnv.getContextName(), null, null, false, false, false,
                        viewForgeEnv.getClasspathImportServiceCompileTime(), viewForgeEnv.getStatementRawInfo(), viewForgeEnv.getSerdeResolver());
            } catch (ExprValidationException ex) {
                throw new ViewParameterException(ex.getMessage(), ex);
            }
        }
    }

    public void assign(CodegenMethod method, CodegenExpressionRef factory, SAIFFInitializeSymbol symbols, CodegenClassScope classScope) {
        if (scheduleCallbackId == -1) {
            throw new IllegalStateException("Schedule callback id not provided");
        }

        CodegenInnerClass evalClass = makeExpiryEval(classScope);
        classScope.addInnerClass(evalClass);

        method.getBlock()
                .declareVar(evalClass.getClassName(), "eval", CodegenExpressionBuilder.newInstance(evalClass.getClassName()))
                .exprDotMethod(factory, "setBuiltinMapType", EventTypeUtility.resolveTypeCodegen(builtinType, EPStatementInitServices.REF))
                .exprDotMethod(factory, "setScheduleCallbackId", constant(scheduleCallbackId))
                .exprDotMethod(factory, "setAggregationServiceFactory", makeAggregationService(classScope, method, symbols))
                .exprDotMethod(factory, "setAggregationResultFutureAssignable", ref("eval"))
                .exprDotMethod(factory, "setExpiryEval", ref("eval"));
        if (variableNames != null && !variableNames.isEmpty()) {
            method.getBlock().exprDotMethod(factory, "setVariables", VariableDeployTimeResolver.makeResolveVariables(variableNames.values(), symbols.getAddInitSvc(method)));
        }
        makeSetters(factory, method.getBlock());
    }

    public void setScheduleCallbackId(int id) {
        this.scheduleCallbackId = id;
    }

    private CodegenExpression makeAggregationService(CodegenClassScope classScope, CodegenMethodScope parent, SAIFFInitializeSymbol symbols) {
        if (aggregationServiceForgeDesc == null) {
            return constantNull();
        }

        AggregationClassNames aggregationClassNames = new AggregationClassNames(CodegenPackageScopeNames.classPostfixAggregationForView(streamNumber));
        AggregationServiceFactoryMakeResult aggResult = AggregationServiceFactoryCompiler.makeInnerClassesAndInit(false, aggregationServiceForgeDesc.getAggregationServiceFactoryForge(), parent, classScope, classScope.getOutermostClassName(), aggregationClassNames);
        classScope.addInnerClasses(aggResult.getInnerClasses());
        return localMethod(aggResult.getInitMethod(), symbols.getAddInitSvc(parent));
    }

    private CodegenInnerClass makeExpiryEval(CodegenClassScope classScope) {
        String classNameExpressionEval = "exprview_eval_" + streamNumber;

        CodegenMethod evalMethod = CodegenMethod.makeParentNode(Object.class, this.getClass(), CodegenSymbolProviderEmpty.INSTANCE, classScope).addParam(ExprForgeCodegenNames.PARAMS);
        CodegenMethod evalMethodCall = CodegenLegoMethodExpression.codegenExpression(expiryExpression.getForge(), evalMethod, classScope);
        evalMethod.getBlock().methodReturn(localMethod(evalMethodCall, REF_EPS, REF_ISNEWDATA, REF_EXPREVALCONTEXT));

        CodegenMethod assignMethod = CodegenMethod.makeParentNode(void.class, this.getClass(), CodegenSymbolProviderEmpty.INSTANCE, classScope).addParam(AggregationResultFuture.class, "future");
        CodegenExpression field = classScope.getPackageScope().addOrGetFieldWellKnown(new CodegenFieldNameViewAgg(streamNumber), AggregationResultFuture.class);
        assignMethod.getBlock().assignRef(field, ref("future"));

        CodegenClassMethods innerMethods = new CodegenClassMethods();
        CodegenStackGenerator.recursiveBuildStack(evalMethod, "evaluate", innerMethods);
        CodegenStackGenerator.recursiveBuildStack(assignMethod, "assign", innerMethods);

        CodegenCtor ctor = new CodegenCtor(StmtClassForgeableRSPFactoryProvider.class, classScope, Collections.emptyList());

        return new CodegenInnerClass(classNameExpressionEval, AggregationResultFutureAssignableWEval.class, ctor, Collections.emptyList(), innerMethods);
    }
}
