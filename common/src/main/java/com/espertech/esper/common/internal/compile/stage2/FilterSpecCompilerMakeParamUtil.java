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
package com.espertech.esper.common.internal.compile.stage2;

import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.internal.collection.Pair;
import com.espertech.esper.common.internal.epl.expression.core.*;
import com.espertech.esper.common.internal.epl.expression.funcs.ExprPlugInSingleRowNode;
import com.espertech.esper.common.internal.epl.expression.ops.*;
import com.espertech.esper.common.internal.epl.index.advanced.index.quadtree.AdvancedIndexConfigContextPartitionQuadTree;
import com.espertech.esper.common.internal.epl.index.advanced.index.quadtree.SettingsApplicationDotMethodPointInsideRectange;
import com.espertech.esper.common.internal.epl.index.advanced.index.quadtree.SettingsApplicationDotMethodRectangeIntersectsRectangle;
import com.espertech.esper.common.internal.event.core.EventPropertyGetterSPI;
import com.espertech.esper.common.internal.event.map.MapEventType;
import com.espertech.esper.common.internal.event.property.IndexedProperty;
import com.espertech.esper.common.internal.event.property.NestedProperty;
import com.espertech.esper.common.internal.event.property.Property;
import com.espertech.esper.common.internal.event.property.PropertyParser;
import com.espertech.esper.common.internal.filterspec.*;
import com.espertech.esper.common.internal.type.RelationalOpEnum;
import com.espertech.esper.common.internal.type.XYPoint;
import com.espertech.esper.common.internal.type.XYWHRectangle;
import com.espertech.esper.common.internal.util.JavaClassHelper;
import com.espertech.esper.common.internal.util.SimpleNumberCoercer;
import com.espertech.esper.common.internal.util.SimpleNumberCoercerFactory;

import java.io.StringWriter;
import java.lang.reflect.Array;
import java.util.*;

/**
 * Helper to compile (validate and optimize) filter expressions as used in pattern and filter-based streams.
 */
public final class FilterSpecCompilerMakeParamUtil {
    /**
     * For a given expression determine if this is optimizable and create the filter parameter
     * representing the expression, or null if not optimizable.
     *
     * @param constituent     is the expression to look at
     * @param arrayEventTypes event types that provide array values
     * @param statementName   statement name
     * @return filter parameter representing the expression, or null
     * @throws ExprValidationException if the expression is invalid
     */
    protected static FilterSpecParamForge makeFilterParam(ExprNode constituent, LinkedHashMap<String, Pair<EventType, String>> arrayEventTypes, String statementName)
            throws ExprValidationException {

        // Is this expression node a simple compare, i.e. a=5 or b<4; these can be indexed
        if ((constituent instanceof ExprEqualsNode) || (constituent instanceof ExprRelationalOpNode)) {
            FilterSpecParamForge param = handleEqualsAndRelOp(constituent, arrayEventTypes, statementName);
            if (param != null) {
                return param;
            }
        }

        constituent = rewriteOrToInIfApplicable(constituent);

        // Is this expression node a simple compare, i.e. a=5 or b<4; these can be indexed
        if (constituent instanceof ExprInNode) {
            FilterSpecParamForge param = handleInSetNode((ExprInNode) constituent, arrayEventTypes);
            if (param != null) {
                return param;
            }
        }

        if (constituent instanceof ExprBetweenNode) {
            FilterSpecParamForge param = handleRangeNode((ExprBetweenNode) constituent, arrayEventTypes, statementName);
            if (param != null) {
                return param;
            }
        }

        if (constituent instanceof ExprPlugInSingleRowNode) {
            FilterSpecParamForge param = handlePlugInSingleRow((ExprPlugInSingleRowNode) constituent);
            if (param != null) {
                return param;
            }
        }

        if (constituent instanceof FilterSpecCompilerAdvIndexDescProvider) {
            FilterSpecParamForge param = handleAdvancedIndexDescProvider((FilterSpecCompilerAdvIndexDescProvider) constituent, arrayEventTypes, statementName);
            if (param != null) {
                return param;
            }
        }

        return null;
    }

    private static FilterSpecParamForge handleAdvancedIndexDescProvider(FilterSpecCompilerAdvIndexDescProvider provider, LinkedHashMap<String, Pair<EventType, String>> arrayEventTypes, String statementName) throws ExprValidationException {
        FilterSpecCompilerAdvIndexDesc filterDesc = provider.getFilterSpecDesc();
        if (filterDesc == null) {
            return null;
        }

        ExprNode[] keyExpressions = filterDesc.getKeyExpressions();
        EventPropertyGetterSPI xGetter = resolveFilterIndexRequiredGetter(filterDesc.getIndexName(), keyExpressions[0]);
        EventPropertyGetterSPI yGetter = resolveFilterIndexRequiredGetter(filterDesc.getIndexName(), keyExpressions[1]);
        EventPropertyGetterSPI widthGetter = resolveFilterIndexRequiredGetter(filterDesc.getIndexName(), keyExpressions[2]);
        EventPropertyGetterSPI heightGetter = resolveFilterIndexRequiredGetter(filterDesc.getIndexName(), keyExpressions[3]);
        AdvancedIndexConfigContextPartitionQuadTree config = (AdvancedIndexConfigContextPartitionQuadTree) filterDesc.getIndexSpec();

        StringWriter builder = new StringWriter();
        ExprNodeUtilityPrint.toExpressionString(keyExpressions[0], builder);
        builder.append(",");
        ExprNodeUtilityPrint.toExpressionString(keyExpressions[1], builder);
        builder.append(",");
        ExprNodeUtilityPrint.toExpressionString(keyExpressions[2], builder);
        builder.append(",");
        ExprNodeUtilityPrint.toExpressionString(keyExpressions[3], builder);
        builder.append("/");
        builder.append(filterDesc.getIndexName().toLowerCase(Locale.ENGLISH));
        builder.append("/");
        builder.append(filterDesc.getIndexType().toLowerCase(Locale.ENGLISH));
        builder.append("/");
        config.toConfiguration(builder);
        String expression = builder.toString();

        Class returnType;
        switch (filterDesc.getIndexType()) {
            case SettingsApplicationDotMethodPointInsideRectange.INDEXTYPE_NAME:
                returnType = XYPoint.class;
                break;
            case SettingsApplicationDotMethodRectangeIntersectsRectangle.INDEXTYPE_NAME:
                returnType = XYWHRectangle.class;
                break;
            default:
                throw new IllegalStateException("Unrecognized index type " + filterDesc.getIndexType());
        }

        FilterSpecLookupableAdvancedIndexForge lookupable = new FilterSpecLookupableAdvancedIndexForge(expression, null, returnType, config, xGetter, yGetter, widthGetter, heightGetter, filterDesc.getIndexType());

        ExprNode[] indexExpressions = filterDesc.getIndexExpressions();
        FilterSpecParamFilterForEvalDoubleForge xEval = resolveFilterIndexDoubleEval(filterDesc.getIndexName(), indexExpressions[0], arrayEventTypes, statementName);
        FilterSpecParamFilterForEvalDoubleForge yEval = resolveFilterIndexDoubleEval(filterDesc.getIndexName(), indexExpressions[1], arrayEventTypes, statementName);
        switch (filterDesc.getIndexType()) {
            case SettingsApplicationDotMethodPointInsideRectange.INDEXTYPE_NAME:
                return new FilterSpecParamAdvancedIndexQuadTreePointRegionForge(lookupable, FilterOperator.ADVANCED_INDEX, xEval, yEval);
            case SettingsApplicationDotMethodRectangeIntersectsRectangle.INDEXTYPE_NAME:
                FilterSpecParamFilterForEvalDoubleForge widthEval = resolveFilterIndexDoubleEval(filterDesc.getIndexName(), indexExpressions[2], arrayEventTypes, statementName);
                FilterSpecParamFilterForEvalDoubleForge heightEval = resolveFilterIndexDoubleEval(filterDesc.getIndexName(), indexExpressions[3], arrayEventTypes, statementName);
                return new FilterSpecParamAdvancedIndexQuadTreeMXCIFForge(lookupable, FilterOperator.ADVANCED_INDEX, xEval, yEval, widthEval, heightEval);
            default:
                throw new IllegalStateException("Unrecognized index type " + filterDesc.getIndexType());
        }
    }

    private static FilterSpecParamFilterForEvalDoubleForge resolveFilterIndexDoubleEval(String indexName, ExprNode indexExpression, LinkedHashMap<String, Pair<EventType, String>> arrayEventTypes, String statementName) throws ExprValidationException {
        FilterSpecParamFilterForEvalDoubleForge resolved = null;
        if (indexExpression instanceof ExprIdentNode) {
            resolved = getIdentNodeDoubleEval((ExprIdentNode) indexExpression, arrayEventTypes, statementName);
        } else if (indexExpression instanceof ExprContextPropertyNode) {
            ExprContextPropertyNode node = (ExprContextPropertyNode) indexExpression;
            resolved = new FilterForEvalContextPropDoubleForge(node.getGetter(), node.getPropertyName());
        } else if (indexExpression.getForge().getForgeConstantType().isCompileTimeConstant()) {
            double d = ((Number) indexExpression.getForge().getExprEvaluator().evaluate(null, true, null)).doubleValue();
            resolved = new FilterForEvalConstantDoubleForge(d);
        } else if (indexExpression.getForge().getForgeConstantType().isConstant()) {
            resolved = new FilterForEvalConstRuntimeExprForge(indexExpression);
        }
        if (resolved != null) {
            return resolved;
        }
        throw new ExprValidationException("Invalid filter-indexable expression '" + ExprNodeUtilityPrint.toExpressionStringMinPrecedenceSafe(indexExpression) + "' in respect to index '" + indexName + "': expected either a constant, context-builtin or property from a previous pattern match");
    }

    private static EventPropertyGetterSPI resolveFilterIndexRequiredGetter(String indexName, ExprNode keyExpression) throws ExprValidationException {
        if (!(keyExpression instanceof ExprIdentNode)) {
            throw new ExprValidationException("Invalid filter-index lookup expression '" + ExprNodeUtilityPrint.toExpressionStringMinPrecedenceSafe(keyExpression) + "' in respect to index '" + indexName + "': expected an event property name");
        }
        return ((ExprIdentNode) keyExpression).getExprEvaluatorIdent().getGetter();
    }

    private static FilterSpecParamForge handlePlugInSingleRow(ExprPlugInSingleRowNode constituent) {
        if (JavaClassHelper.getBoxedType(constituent.getForge().getEvaluationType()) != Boolean.class) {
            return null;
        }
        if (!constituent.getFilterLookupEligible()) {
            return null;
        }
        ExprFilterSpecLookupableForge lookupable = constituent.getFilterLookupable();
        return new FilterSpecParamConstantForge(lookupable, FilterOperator.EQUAL, true);
    }

    private static FilterSpecParamForge handleEqualsAndRelOp(ExprNode constituent, LinkedHashMap<String, Pair<EventType, String>> arrayEventTypes, String statementName)
            throws ExprValidationException {
        FilterOperator op;
        if (constituent instanceof ExprEqualsNode) {
            ExprEqualsNode equalsNode = (ExprEqualsNode) constituent;
            if (!equalsNode.isIs()) {
                op = FilterOperator.EQUAL;
                if (equalsNode.isNotEquals()) {
                    op = FilterOperator.NOT_EQUAL;
                }
            } else {
                op = FilterOperator.IS;
                if (equalsNode.isNotEquals()) {
                    op = FilterOperator.IS_NOT;
                }
            }
        } else {
            ExprRelationalOpNode relNode = (ExprRelationalOpNode) constituent;
            if (relNode.getRelationalOpEnum() == RelationalOpEnum.GT) {
                op = FilterOperator.GREATER;
            } else if (relNode.getRelationalOpEnum() == RelationalOpEnum.LT) {
                op = FilterOperator.LESS;
            } else if (relNode.getRelationalOpEnum() == RelationalOpEnum.LE) {
                op = FilterOperator.LESS_OR_EQUAL;
            } else if (relNode.getRelationalOpEnum() == RelationalOpEnum.GE) {
                op = FilterOperator.GREATER_OR_EQUAL;
            } else {
                throw new IllegalStateException("Opertor '" + relNode.getRelationalOpEnum() + "' not mapped");
            }
        }

        ExprNode left = constituent.getChildNodes()[0];
        ExprNode right = constituent.getChildNodes()[1];

        // check identifier and constant combination
        if ((right.getForge().getForgeConstantType().isCompileTimeConstant()) && (left instanceof ExprFilterOptimizableNode)) {
            ExprFilterOptimizableNode filterOptimizableNode = (ExprFilterOptimizableNode) left;
            if (filterOptimizableNode.getFilterLookupEligible()) {
                ExprFilterSpecLookupableForge lookupable = filterOptimizableNode.getFilterLookupable();
                Object constant = right.getForge().getExprEvaluator().evaluate(null, true, null);
                constant = handleConstantsCoercion(lookupable, constant);
                return new FilterSpecParamConstantForge(lookupable, op, constant);
            }
        }
        if ((left.getForge().getForgeConstantType().isCompileTimeConstant()) && (right instanceof ExprFilterOptimizableNode)) {
            ExprFilterOptimizableNode filterOptimizableNode = (ExprFilterOptimizableNode) right;
            if (filterOptimizableNode.getFilterLookupEligible()) {
                ExprFilterSpecLookupableForge lookupable = filterOptimizableNode.getFilterLookupable();
                Object constant = left.getForge().getExprEvaluator().evaluate(null, true, null);
                constant = handleConstantsCoercion(lookupable, constant);
                FilterOperator opReversed = op.isComparisonOperator() ? op.reversedRelationalOp() : op;
                return new FilterSpecParamConstantForge(lookupable, opReversed, constant);
            }
        }
        // check identifier and expression containing other streams
        if ((left instanceof ExprIdentNode) && (right instanceof ExprIdentNode)) {
            ExprIdentNode identNodeLeft = (ExprIdentNode) left;
            ExprIdentNode identNodeRight = (ExprIdentNode) right;

            if ((identNodeLeft.getStreamId() == 0) && (identNodeLeft.getFilterLookupEligible()) && (identNodeRight.getStreamId() != 0)) {
                return handleProperty(op, identNodeLeft, identNodeRight, arrayEventTypes, statementName);
            }
            if ((identNodeRight.getStreamId() == 0) && (identNodeRight.getFilterLookupEligible()) && (identNodeLeft.getStreamId() != 0)) {
                op = getReversedOperator(constituent, op); // reverse operators, as the expression is "stream1.prop xyz stream0.prop"
                return handleProperty(op, identNodeRight, identNodeLeft, arrayEventTypes, statementName);
            }
        }

        if ((left instanceof ExprFilterOptimizableNode) && (right instanceof ExprContextPropertyNode)) {
            ExprFilterOptimizableNode filterOptimizableNode = (ExprFilterOptimizableNode) left;
            ExprContextPropertyNode ctxNode = (ExprContextPropertyNode) right;
            ExprFilterSpecLookupableForge lookupable = filterOptimizableNode.getFilterLookupable();
            if (filterOptimizableNode.getFilterLookupEligible()) {
                SimpleNumberCoercer numberCoercer = getNumberCoercer(lookupable.getReturnType(), ctxNode.getType(), lookupable.getExpression());
                return new FilterSpecParamContextPropForge(lookupable, op, ctxNode.getGetter(), numberCoercer);
            }
        }
        if ((left instanceof ExprContextPropertyNode) && (right instanceof ExprFilterOptimizableNode)) {
            ExprFilterOptimizableNode filterOptimizableNode = (ExprFilterOptimizableNode) right;
            ExprContextPropertyNode ctxNode = (ExprContextPropertyNode) left;
            ExprFilterSpecLookupableForge lookupable = filterOptimizableNode.getFilterLookupable();
            if (filterOptimizableNode.getFilterLookupEligible()) {
                op = getReversedOperator(constituent, op); // reverse operators, as the expression is "stream1.prop xyz stream0.prop"
                SimpleNumberCoercer numberCoercer = getNumberCoercer(lookupable.getReturnType(), ctxNode.getType(), lookupable.getExpression());
                return new FilterSpecParamContextPropForge(lookupable, op, ctxNode.getGetter(), numberCoercer);
            }
        }

        if ((left instanceof ExprFilterOptimizableNode) && (right.getForge().getForgeConstantType().isDeployTimeTimeConstant() && right instanceof ExprNodeDeployTimeConst)) {
            ExprFilterOptimizableNode filterOptimizableNode = (ExprFilterOptimizableNode) left;
            ExprNodeDeployTimeConst deployTimeConst = (ExprNodeDeployTimeConst) right;
            ExprFilterSpecLookupableForge lookupable = filterOptimizableNode.getFilterLookupable();
            if (filterOptimizableNode.getFilterLookupEligible()) {
                Class returnType = right.getForge().getEvaluationType();
                SimpleNumberCoercer numberCoercer = getNumberCoercer(lookupable.getReturnType(), returnType, lookupable.getExpression());
                return new FilterSpecParamDeployTimeConstParamForge(lookupable, op, deployTimeConst, returnType, numberCoercer);
            }
        }
        if ((left.getForge().getForgeConstantType().isDeployTimeTimeConstant() && left instanceof ExprNodeDeployTimeConst) && (right instanceof ExprFilterOptimizableNode)) {
            ExprFilterOptimizableNode filterOptimizableNode = (ExprFilterOptimizableNode) right;
            ExprNodeDeployTimeConst deployTimeConst = (ExprNodeDeployTimeConst) left;
            ExprFilterSpecLookupableForge lookupable = filterOptimizableNode.getFilterLookupable();
            if (filterOptimizableNode.getFilterLookupEligible()) {
                Class returnType = left.getForge().getEvaluationType();
                op = getReversedOperator(constituent, op); // reverse operators, as the expression is "stream1.prop xyz stream0.prop"
                SimpleNumberCoercer numberCoercer = getNumberCoercer(lookupable.getReturnType(), returnType, lookupable.getExpression());
                return new FilterSpecParamDeployTimeConstParamForge(lookupable, op, deployTimeConst, returnType, numberCoercer);
            }
        }

        return null;
    }

    private static FilterOperator getReversedOperator(ExprNode constituent, FilterOperator op) {
        if (!(constituent instanceof ExprRelationalOpNode)) {
            return op;
        }

        ExprRelationalOpNode relNode = (ExprRelationalOpNode) constituent;
        RelationalOpEnum relationalOpEnum = relNode.getRelationalOpEnum();

        if (relationalOpEnum == RelationalOpEnum.GT) {
            return FilterOperator.LESS;
        } else if (relationalOpEnum == RelationalOpEnum.LT) {
            return FilterOperator.GREATER;
        } else if (relationalOpEnum == RelationalOpEnum.LE) {
            return FilterOperator.GREATER_OR_EQUAL;
        } else if (relationalOpEnum == RelationalOpEnum.GE) {
            return FilterOperator.LESS_OR_EQUAL;
        }
        return op;
    }

    private static FilterSpecParamForge handleProperty(FilterOperator op, ExprIdentNode identNodeLeft, ExprIdentNode identNodeRight, LinkedHashMap<String, Pair<EventType, String>> arrayEventTypes, String statementName)
            throws ExprValidationException {
        String propertyName = identNodeLeft.getResolvedPropertyName();

        Class leftType = identNodeLeft.getForge().getEvaluationType();
        Class rightType = identNodeRight.getForge().getEvaluationType();

        SimpleNumberCoercer numberCoercer = getNumberCoercer(leftType, rightType, propertyName);
        boolean isMustCoerce = numberCoercer != null;
        Class numericCoercionType = JavaClassHelper.getBoxedType(leftType);

        String streamName = identNodeRight.getResolvedStreamName();
        if (arrayEventTypes != null && !arrayEventTypes.isEmpty() && arrayEventTypes.containsKey(streamName)) {
            EventType innerEventType = getArrayInnerEventType(arrayEventTypes, streamName);
            Pair<Integer, String> indexAndProp = getStreamIndex(identNodeRight.getResolvedPropertyName());
            return new FilterSpecParamEventPropIndexedForge(identNodeLeft.getFilterLookupable(), op, identNodeRight.getResolvedStreamName(), indexAndProp.getFirst(),
                    indexAndProp.getSecond(), innerEventType, isMustCoerce, numberCoercer, numericCoercionType, statementName);
        }
        return new FilterSpecParamEventPropForge(identNodeLeft.getFilterLookupable(), op, identNodeRight.getResolvedStreamName(), identNodeRight.getResolvedPropertyName(), identNodeRight.getExprEvaluatorIdent(),
                isMustCoerce, numberCoercer, numericCoercionType, statementName);
    }

    private static EventType getArrayInnerEventType(LinkedHashMap<String, Pair<EventType, String>> arrayEventTypes, String streamName) {
        Pair<EventType, String> arrayEventType = arrayEventTypes.get(streamName);
        Object prop = ((MapEventType) arrayEventType.getFirst()).getTypes().get(streamName);
        return ((EventType[]) prop)[0];
    }

    private static SimpleNumberCoercer getNumberCoercer(Class leftType, Class rightType, String expression) throws ExprValidationException {
        Class numericCoercionType = JavaClassHelper.getBoxedType(leftType);
        if (rightType != leftType) {
            if (JavaClassHelper.isNumeric(rightType)) {
                if (!JavaClassHelper.canCoerce(rightType, leftType)) {
                    throwConversionError(rightType, leftType, expression);
                }
                return SimpleNumberCoercerFactory.getCoercer(rightType, numericCoercionType);
            }
        }
        return null;
    }

    private static Pair<Integer, String> getStreamIndex(String resolvedPropertyName) {
        Property property = PropertyParser.parseAndWalkLaxToSimple(resolvedPropertyName);
        if (!(property instanceof NestedProperty)) {
            throw new IllegalStateException("Expected a nested property providing an index for array match '" + resolvedPropertyName + "'");
        }
        NestedProperty nested = (NestedProperty) property;
        if (nested.getProperties().size() < 2) {
            throw new IllegalStateException("Expected a nested property name for array match '" + resolvedPropertyName + "', none found");
        }
        if (!(nested.getProperties().get(0) instanceof IndexedProperty)) {
            throw new IllegalStateException("Expected an indexed property for array match '" + resolvedPropertyName + "', please provide an index");
        }
        int index = ((IndexedProperty) nested.getProperties().get(0)).getIndex();
        nested.getProperties().remove(0);
        StringWriter writer = new StringWriter();
        nested.toPropertyEPL(writer);
        return new Pair<>(index, writer.toString());
    }

    private static void throwConversionError(Class fromType, Class toType, String propertyName)
            throws ExprValidationException {
        String text = "Implicit conversion from datatype '" +
                fromType.getSimpleName() +
                "' to '" +
                toType.getSimpleName() +
                "' for property '" +
                propertyName +
                "' is not allowed (strict filter type coercion)";
        throw new ExprValidationException(text);
    }

    // expressions automatically coerce to the most upwards type
    // filters require the same type
    private static Object handleConstantsCoercion(ExprFilterSpecLookupableForge lookupable, Object constant)
            throws ExprValidationException {
        Class identNodeType = lookupable.getReturnType();
        if (!JavaClassHelper.isNumeric(identNodeType)) {
            return constant;    // no coercion required, other type checking performed by expression this comes from
        }

        if (constant == null) {
            // null constant type
            return null;
        }

        if (!JavaClassHelper.canCoerce(constant.getClass(), identNodeType)) {
            throwConversionError(constant.getClass(), identNodeType, lookupable.getExpression());
        }

        Class identNodeTypeBoxed = JavaClassHelper.getBoxedType(identNodeType);
        return JavaClassHelper.coerceBoxed((Number) constant, identNodeTypeBoxed);
    }

    public static ExprNode rewriteOrToInIfApplicable(ExprNode constituent) {
        if (!(constituent instanceof ExprOrNode) || constituent.getChildNodes().length < 2) {
            return constituent;
        }

        // check eligibility
        ExprNode[] childNodes = constituent.getChildNodes();
        for (ExprNode child : childNodes) {
            if (!(child instanceof ExprEqualsNode)) {
                return constituent;
            }
            ExprEqualsNode equalsNode = (ExprEqualsNode) child;
            if (equalsNode.isIs() || equalsNode.isNotEquals()) {
                return constituent;
            }
        }

        // find common-expression node
        ExprNode commonExpressionNode;
        ExprNode lhs = childNodes[0].getChildNodes()[0];
        ExprNode rhs = childNodes[0].getChildNodes()[1];
        if (ExprNodeUtilityCompare.deepEquals(lhs, rhs, false)) {
            return constituent;
        }
        if (isExprExistsInAllEqualsChildNodes(childNodes, lhs)) {
            commonExpressionNode = lhs;
        } else if (isExprExistsInAllEqualsChildNodes(childNodes, rhs)) {
            commonExpressionNode = rhs;
        } else {
            return constituent;
        }

        // build node
        ExprInNodeImpl in = new ExprInNodeImpl(false);
        in.addChildNode(commonExpressionNode);
        for (int i = 0; i < constituent.getChildNodes().length; i++) {
            ExprNode child = constituent.getChildNodes()[i];
            int nodeindex = ExprNodeUtilityCompare.deepEquals(commonExpressionNode, childNodes[i].getChildNodes()[0], false) ? 1 : 0;
            in.addChildNode(child.getChildNodes()[nodeindex]);
        }

        // validate
        try {
            in.validateWithoutContext();
        } catch (ExprValidationException ex) {
            return constituent;
        }

        return in;
    }

    private static boolean isExprExistsInAllEqualsChildNodes(ExprNode[] childNodes, ExprNode search) {
        for (ExprNode child : childNodes) {
            ExprNode lhs = child.getChildNodes()[0];
            ExprNode rhs = child.getChildNodes()[1];
            if (!ExprNodeUtilityCompare.deepEquals(lhs, search, false) && !ExprNodeUtilityCompare.deepEquals(rhs, search, false)) {
                return false;
            }
            if (ExprNodeUtilityCompare.deepEquals(lhs, rhs, false)) {
                return false;
            }
        }
        return true;
    }

    private static FilterSpecParamForge handleRangeNode(ExprBetweenNode betweenNode, LinkedHashMap<String, Pair<EventType, String>> arrayEventTypes, String statementName) {
        ExprNode left = betweenNode.getChildNodes()[0];
        if (left instanceof ExprFilterOptimizableNode) {
            ExprFilterOptimizableNode filterOptimizableNode = (ExprFilterOptimizableNode) left;
            ExprFilterSpecLookupableForge lookupable = filterOptimizableNode.getFilterLookupable();
            FilterOperator op = FilterOperator.parseRangeOperator(betweenNode.isLowEndpointIncluded(), betweenNode.isHighEndpointIncluded(),
                    betweenNode.isNotBetween());

            FilterSpecParamFilterForEvalForge low = handleRangeNodeEndpoint(betweenNode.getChildNodes()[1], arrayEventTypes, statementName);
            FilterSpecParamFilterForEvalForge high = handleRangeNodeEndpoint(betweenNode.getChildNodes()[2], arrayEventTypes, statementName);

            if ((low != null) && (high != null)) {
                return new FilterSpecParamRangeForge(lookupable, op, low, high);
            }
        }
        return null;
    }

    private static FilterSpecParamFilterForEvalForge handleRangeNodeEndpoint(ExprNode endpoint, LinkedHashMap<String, Pair<EventType, String>> arrayEventTypes, String statementName) {
        // constant
        if (endpoint.getForge().getForgeConstantType().isCompileTimeConstant()) {
            Object value = endpoint.getForge().getExprEvaluator().evaluate(null, true, null);
            if (value == null) {
                return null;
            }
            if (value instanceof String) {
                return new FilterForEvalConstantStringForge((String) value);
            } else {
                return new FilterForEvalConstantDoubleForge(((Number) value).doubleValue());
            }
        }

        if (endpoint instanceof ExprContextPropertyNode) {
            ExprContextPropertyNode node = (ExprContextPropertyNode) endpoint;
            if (JavaClassHelper.isImplementsCharSequence(node.getType())) {
                return new FilterForEvalContextPropStringForge(node.getGetter(), node.getPropertyName());
            } else {
                return new FilterForEvalContextPropDoubleForge(node.getGetter(), node.getPropertyName());
            }
        }

        if (endpoint.getForge().getForgeConstantType().isDeployTimeTimeConstant() && endpoint instanceof ExprNodeDeployTimeConst) {
            ExprNodeDeployTimeConst node = (ExprNodeDeployTimeConst) endpoint;
            if (JavaClassHelper.isImplementsCharSequence(endpoint.getForge().getEvaluationType())) {
                return new FilterForEvalDeployTimeConstStringForge(node);
            } else {
                return new FilterForEvalDeployTimeConstDoubleForge(node);
            }
        }

        // or property
        if (endpoint instanceof ExprIdentNode) {
            return getIdentNodeDoubleEval((ExprIdentNode) endpoint, arrayEventTypes, statementName);
        }

        return null;
    }

    private static FilterSpecParamFilterForEvalDoubleForge getIdentNodeDoubleEval(ExprIdentNode node, LinkedHashMap<String, Pair<EventType, String>> arrayEventTypes, String statementName) {
        if (node.getStreamId() == 0) {
            return null;
        }

        if (arrayEventTypes != null && !arrayEventTypes.isEmpty() && arrayEventTypes.containsKey(node.getResolvedStreamName())) {
            Pair<Integer, String> indexAndProp = getStreamIndex(node.getResolvedPropertyName());
            EventType eventType = getArrayInnerEventType(arrayEventTypes, node.getResolvedStreamName());
            return new FilterForEvalEventPropIndexedDoubleForge(node.getResolvedStreamName(), indexAndProp.getFirst(), indexAndProp.getSecond(), eventType);
        } else {
            return new FilterForEvalEventPropDoubleForge(node.getResolvedStreamName(), node.getResolvedPropertyName(), node.getExprEvaluatorIdent());
        }
    }

    private static FilterSpecParamForge handleInSetNode(ExprInNode constituent, LinkedHashMap<String, Pair<EventType, String>> arrayEventTypes)
            throws ExprValidationException {
        ExprNode left = constituent.getChildNodes()[0];
        if (!(left instanceof ExprFilterOptimizableNode)) {
            return null;
        }

        ExprFilterOptimizableNode filterOptimizableNode = (ExprFilterOptimizableNode) left;
        ExprFilterSpecLookupableForge lookupable = filterOptimizableNode.getFilterLookupable();
        FilterOperator op = FilterOperator.IN_LIST_OF_VALUES;
        if (constituent.isNotIn()) {
            op = FilterOperator.NOT_IN_LIST_OF_VALUES;
        }

        int expectedNumberOfConstants = constituent.getChildNodes().length - 1;
        List<FilterSpecParamInValueForge> listofValues = new ArrayList<FilterSpecParamInValueForge>();
        Iterator<ExprNode> it = Arrays.asList(constituent.getChildNodes()).iterator();
        it.next();  // ignore the first node as it's the identifier
        while (it.hasNext()) {
            ExprNode subNode = it.next();
            if (subNode.getForge().getForgeConstantType().isCompileTimeConstant()) {
                Object constant = subNode.getForge().getExprEvaluator().evaluate(null, true, null);
                if (constant instanceof Collection) {
                    return null;
                }
                if (constant instanceof Map) {
                    return null;
                }
                if ((constant != null) && (constant.getClass().isArray())) {
                    for (int i = 0; i < Array.getLength(constant); i++) {
                        Object arrayElement = Array.get(constant, i);
                        Object arrayElementCoerced = handleConstantsCoercion(lookupable, arrayElement);
                        listofValues.add(new FilterForEvalConstantAnyTypeForge(arrayElementCoerced));
                        if (i > 0) {
                            expectedNumberOfConstants++;
                        }
                    }
                } else {
                    constant = handleConstantsCoercion(lookupable, constant);
                    listofValues.add(new FilterForEvalConstantAnyTypeForge(constant));
                }
            } else if (subNode instanceof ExprContextPropertyNode) {
                ExprContextPropertyNode contextPropertyNode = (ExprContextPropertyNode) subNode;
                Class returnType = contextPropertyNode.getType();
                SimpleNumberCoercer coercer;
                if (JavaClassHelper.isCollectionMapOrArray(returnType)) {
                    checkArrayCoercion(returnType, lookupable.getReturnType(), lookupable.getExpression());
                    coercer = null;
                } else {
                    coercer = getNumberCoercer(left.getForge().getEvaluationType(), contextPropertyNode.getType(), lookupable.getExpression());
                }
                Class finalReturnType = coercer != null ? coercer.getReturnType() : returnType;
                listofValues.add(new FilterForEvalContextPropForge(contextPropertyNode.getPropertyName(), contextPropertyNode.getGetter(), coercer, finalReturnType));
            } else if (subNode.getForge().getForgeConstantType().isDeployTimeTimeConstant() && subNode instanceof ExprNodeDeployTimeConst) {
                ExprNodeDeployTimeConst deployTimeConst = (ExprNodeDeployTimeConst) subNode;
                Class returnType = subNode.getForge().getEvaluationType();
                SimpleNumberCoercer coercer;
                if (JavaClassHelper.isCollectionMapOrArray(returnType)) {
                    checkArrayCoercion(returnType, lookupable.getReturnType(), lookupable.getExpression());
                    coercer = null;
                } else {
                    coercer = getNumberCoercer(left.getForge().getEvaluationType(), returnType, lookupable.getExpression());
                }
                listofValues.add(new FilterForEvalDeployTimeConstForge(deployTimeConst, coercer, returnType));
            }
            if (subNode instanceof ExprIdentNode) {
                ExprIdentNode identNodeInner = (ExprIdentNode) subNode;
                if (identNodeInner.getStreamId() == 0) {
                    break; // for same event evals use the boolean expression, via count compare failing below
                }

                boolean isMustCoerce = false;
                Class coerceToType = JavaClassHelper.getBoxedType(lookupable.getReturnType());
                Class identReturnType = identNodeInner.getForge().getEvaluationType();

                if (JavaClassHelper.isCollectionMapOrArray(identReturnType)) {
                    checkArrayCoercion(identReturnType, lookupable.getReturnType(), lookupable.getExpression());
                    coerceToType = identReturnType;
                    // no action
                } else if (identReturnType != lookupable.getReturnType()) {
                    if (JavaClassHelper.isNumeric(lookupable.getReturnType())) {
                        if (!JavaClassHelper.canCoerce(identReturnType, lookupable.getReturnType())) {
                            throwConversionError(identReturnType, lookupable.getReturnType(), lookupable.getExpression());
                        }
                        isMustCoerce = true;
                    } else {
                        break;  // assumed not compatible
                    }
                }

                FilterSpecParamInValueForge inValue;
                String streamName = identNodeInner.getResolvedStreamName();
                if (arrayEventTypes != null && !arrayEventTypes.isEmpty() && arrayEventTypes.containsKey(streamName)) {
                    Pair<Integer, String> indexAndProp = getStreamIndex(identNodeInner.getResolvedPropertyName());
                    EventType innerEventType = getArrayInnerEventType(arrayEventTypes, streamName);
                    inValue = new FilterForEvalEventPropIndexedForge(identNodeInner.getResolvedStreamName(), indexAndProp.getFirst(),
                            indexAndProp.getSecond(), innerEventType, isMustCoerce, coerceToType);
                } else {
                    inValue = new FilterForEvalEventPropForge(identNodeInner.getResolvedStreamName(), identNodeInner.getResolvedPropertyName(), identNodeInner.getExprEvaluatorIdent(), isMustCoerce, coerceToType);
                }

                listofValues.add(inValue);
            }
        }

        // Fallback if not all values in the in-node can be resolved to properties or constants
        if (listofValues.size() == expectedNumberOfConstants) {
            return new FilterSpecParamInForge(lookupable, op, listofValues);
        }
        return null;
    }

    private static void checkArrayCoercion(Class returnTypeValue, Class returnTypeLookupable, String propertyName) throws ExprValidationException {
        if (returnTypeValue == null || !returnTypeValue.isArray()) {
            return;
        }
        if (!JavaClassHelper.isArrayTypeCompatible(returnTypeLookupable, returnTypeValue.getComponentType())) {
            throwConversionError(returnTypeValue.getComponentType(), returnTypeLookupable, propertyName);
        }
    }
}
