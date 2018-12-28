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
package com.espertech.esper.common.internal.epl.expression.agg.accessagg;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.client.util.CountMinSketchAgentForge;
import com.espertech.esper.common.client.util.CountMinSketchAgentStringUTF16Forge;
import com.espertech.esper.common.client.util.StatementType;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.compile.stage2.StatementRawInfo;
import com.espertech.esper.common.internal.compile.stage3.StatementCompileTimeServices;
import com.espertech.esper.common.internal.epl.agg.access.countminsketch.AggregationForgeFactoryAccessCountMinSketchAdd;
import com.espertech.esper.common.internal.epl.agg.access.countminsketch.AggregationForgeFactoryAccessCountMinSketchState;
import com.espertech.esper.common.internal.epl.agg.access.countminsketch.AggregationStateCountMinSketchForge;
import com.espertech.esper.common.internal.epl.agg.core.AggregationForgeFactory;
import com.espertech.esper.common.internal.epl.approx.countminsketch.CountMinSketchAggType;
import com.espertech.esper.common.internal.epl.approx.countminsketch.CountMinSketchSpecForge;
import com.espertech.esper.common.internal.epl.approx.countminsketch.CountMinSketchSpecHashes;
import com.espertech.esper.common.internal.epl.expression.agg.base.ExprAggregateNode;
import com.espertech.esper.common.internal.epl.expression.agg.base.ExprAggregateNodeBase;
import com.espertech.esper.common.internal.epl.expression.codegen.ExprForgeCodegenSymbol;
import com.espertech.esper.common.internal.epl.expression.core.*;
import com.espertech.esper.common.internal.util.JavaClassHelper;

import java.util.Collection;
import java.util.Map;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.constantNull;

/**
 * Represents the Count-min sketch aggregate function.
 */
public class ExprAggMultiFunctionCountMinSketchNode extends ExprAggregateNodeBase implements ExprAggMultiFunctionNode, ExprEnumerationEval {

    private static final double DEFAULT_EPS_OF_TOTAL_COUNT = 0.0001;
    private static final double DEFAULT_CONFIDENCE = 0.99;
    private static final int DEFAULT_SEED = 1234567;
    private static final CountMinSketchAgentStringUTF16Forge DEFAULT_AGENT = new CountMinSketchAgentStringUTF16Forge();

    public static final String MSG_NAME = "Count-min-sketch";
    private static final String NAME_EPS_OF_TOTAL_COUNT = "epsOfTotalCount";
    private static final String NAME_CONFIDENCE = "confidence";
    private static final String NAME_SEED = "seed";
    private static final String NAME_TOPK = "topk";
    private static final String NAME_AGENT = "agent";

    private final CountMinSketchAggType aggType;
    private AggregationForgeFactory forgeFactory;

    public ExprAggMultiFunctionCountMinSketchNode(boolean distinct, CountMinSketchAggType aggType) {
        super(distinct);
        this.aggType = aggType;
    }

    public AggregationForgeFactory validateAggregationChild(ExprValidationContext validationContext) throws ExprValidationException {
        if (isDistinct()) {
            throw new ExprValidationException(getMessagePrefix() + "is not supported with distinct");
        }

        // for declaration, validate the specification and return the state factory
        if (aggType == CountMinSketchAggType.STATE) {
            if (validationContext.getStatementRawInfo().getStatementType() != StatementType.CREATE_TABLE) {
                throw new ExprValidationException(getMessagePrefix() + "can only be used in create-table statements");
            }
            CountMinSketchSpecForge specification = validateSpecification(validationContext);
            AggregationStateCountMinSketchForge stateFactory = new AggregationStateCountMinSketchForge(this, specification);
            forgeFactory = new AggregationForgeFactoryAccessCountMinSketchState(this, stateFactory);
            return forgeFactory;
        }

        if (aggType != CountMinSketchAggType.ADD) {
            // other methods are only used with table-access expressions
            throw new ExprValidationException(getMessagePrefix() + "requires the use of a table-access expression");
        }

        if (validationContext.getStatementRawInfo().getIntoTableName() == null) {
            throw new ExprValidationException(getMessagePrefix() + "can only be used with into-table");
        }
        if (positionalParams.length == 0 || positionalParams.length > 1) {
            throw new ExprValidationException(getMessagePrefix() + "requires a single parameter expression");
        }
        ExprNodeUtilityValidate.getValidatedSubtree(ExprNodeOrigin.AGGPARAM, this.getChildNodes(), validationContext);

        // obtain evaluator
        ExprForge addOrFrequencyEvaluator = null;
        Class addOrFrequencyEvaluatorReturnType = null;
        if (aggType == CountMinSketchAggType.ADD) {
            addOrFrequencyEvaluator = getChildNodes()[0].getForge();
            addOrFrequencyEvaluatorReturnType = addOrFrequencyEvaluator.getEvaluationType();
        }

        forgeFactory = new AggregationForgeFactoryAccessCountMinSketchAdd(this, addOrFrequencyEvaluator, addOrFrequencyEvaluatorReturnType);
        return forgeFactory;
    }

    public ExprEnumerationEval getExprEvaluatorEnumeration() {
        return this;
    }

    public String getAggregationFunctionName() {
        return aggType.getFuncName();
    }

    public final boolean equalsNodeAggregateMethodOnly(ExprAggregateNode node) {
        return false;
    }

    public CountMinSketchAggType getAggType() {
        return aggType;
    }

    public EventType getEventTypeCollection(StatementRawInfo statementRawInfo, StatementCompileTimeServices compileTimeServices) throws ExprValidationException {
        return null;
    }

    public Collection<EventBean> evaluateGetROCollectionEvents(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
        return null;
    }

    public Class getComponentTypeCollection() throws ExprValidationException {
        return null;
    }

    public Collection evaluateGetROCollectionScalar(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
        return null;
    }

    public CodegenExpression evaluateGetROCollectionScalarCodegen(CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        return null;
    }

    public CodegenExpression evaluateGetROCollectionEventsCodegen(CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        return constantNull();
    }

    public EventType getEventTypeSingle(StatementRawInfo statementRawInfo, StatementCompileTimeServices compileTimeServices) throws ExprValidationException {
        return null;
    }

    public EventBean evaluateGetEventBean(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
        return null;
    }

    public CodegenExpression evaluateGetEventBeanCodegen(CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        return constantNull();
    }


    @Override
    protected boolean isExprTextWildcardWhenNoParams() {
        return false;
    }

    private CountMinSketchSpecForge validateSpecification(final ExprValidationContext exprValidationContext) throws ExprValidationException {
        // default specification
        CountMinSketchSpecHashes hashes = new CountMinSketchSpecHashes(DEFAULT_EPS_OF_TOTAL_COUNT, DEFAULT_CONFIDENCE, DEFAULT_SEED);
        final CountMinSketchSpecForge spec = new CountMinSketchSpecForge(hashes, null, DEFAULT_AGENT);

        // no parameters
        if (this.getChildNodes().length == 0) {
            return spec;
        }

        // check expected parameter type: a json object
        if (this.getChildNodes().length > 1 || !(this.getChildNodes()[0] instanceof ExprConstantNode)) {
            throw getDeclaredWrongParameterExpr();
        }
        ExprConstantNode constantNode = (ExprConstantNode) this.getChildNodes()[0];
        Object value = constantNode.getConstantValue();
        if (!(value instanceof Map)) {
            throw getDeclaredWrongParameterExpr();
        }

        // define what to populate
        PopulateFieldWValueDescriptor[] descriptors = new PopulateFieldWValueDescriptor[]{
            new PopulateFieldWValueDescriptor(NAME_EPS_OF_TOTAL_COUNT, Double.class, spec.getHashesSpec().getClass(), new PopulateFieldValueSetter() {
                public void set(Object value) {
                    if (value != null) {
                        spec.getHashesSpec().setEpsOfTotalCount((Double) value);
                    }
                }
            }, true),
            new PopulateFieldWValueDescriptor(NAME_CONFIDENCE, Double.class, spec.getHashesSpec().getClass(), new PopulateFieldValueSetter() {
                public void set(Object value) {
                    if (value != null) {
                        spec.getHashesSpec().setConfidence((Double) value);
                    }
                }
            }, true),
            new PopulateFieldWValueDescriptor(NAME_SEED, Integer.class, spec.getHashesSpec().getClass(), new PopulateFieldValueSetter() {
                public void set(Object value) {
                    if (value != null) {
                        spec.getHashesSpec().setSeed((Integer) value);
                    }
                }
            }, true),
            new PopulateFieldWValueDescriptor(NAME_TOPK, Integer.class, spec.getClass(), new PopulateFieldValueSetter() {
                public void set(Object value) {
                    if (value != null) {
                        spec.setTopkSpec((Integer) value);
                    }
                }
            }, true),
            new PopulateFieldWValueDescriptor(NAME_AGENT, String.class, spec.getClass(), new PopulateFieldValueSetter() {
                public void set(Object value) throws ExprValidationException {
                    if (value != null) {
                        CountMinSketchAgentForge transform;
                        try {
                            Class transformClass = exprValidationContext.getClasspathImportService().resolveClass((String) value, false);
                            transform = (CountMinSketchAgentForge) JavaClassHelper.instantiate(CountMinSketchAgentForge.class, transformClass);
                        } catch (Exception e) {
                            throw new ExprValidationException("Failed to instantiate agent provider: " + e.getMessage(), e);
                        }
                        spec.setAgent(transform);
                    }
                }
            }, true),
        };

        // populate from json, validates incorrect names, coerces types, instantiates transform
        PopulateUtil.populateSpecCheckParameters(descriptors, (Map<String, Object>) value, spec, ExprNodeOrigin.AGGPARAM, exprValidationContext);

        return spec;
    }

    public ExprValidationException getDeclaredWrongParameterExpr() throws ExprValidationException {
        return new ExprValidationException(getMessagePrefix() + " expects either no parameter or a single json parameter object");
    }

    protected boolean isFilterExpressionAsLastParameter() {
        return false;
    }

    public AggregationForgeFactory getAggregationForgeFactory() {
        return forgeFactory;
    }

    private String getMessagePrefix() {
        return MSG_NAME + " aggregation function '" + aggType.getFuncName() + "' ";
    }
}