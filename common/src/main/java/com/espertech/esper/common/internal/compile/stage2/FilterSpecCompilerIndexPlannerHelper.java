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
import com.espertech.esper.common.client.annotation.HintEnum;
import com.espertech.esper.common.internal.collection.Pair;
import com.espertech.esper.common.internal.compile.stage3.StatementCompileTimeServices;
import com.espertech.esper.common.internal.epl.expression.core.*;
import com.espertech.esper.common.internal.epl.expression.visitor.ExprNodeStreamUseCollectVisitor;
import com.espertech.esper.common.internal.epl.pattern.core.MatchedEventConvertorForge;
import com.espertech.esper.common.internal.event.map.MapEventType;
import com.espertech.esper.common.internal.event.property.IndexedProperty;
import com.espertech.esper.common.internal.event.property.NestedProperty;
import com.espertech.esper.common.internal.event.property.Property;
import com.espertech.esper.common.internal.event.property.PropertyParser;
import com.espertech.esper.common.internal.filterspec.FilterForEvalEventPropDoubleForge;
import com.espertech.esper.common.internal.filterspec.FilterForEvalEventPropIndexedDoubleForge;
import com.espertech.esper.common.internal.filterspec.FilterSpecParamFilterForEvalDoubleForge;
import com.espertech.esper.common.internal.serde.compiletime.resolve.DataInputOutputSerdeForge;
import com.espertech.esper.common.internal.util.JavaClassHelper;
import com.espertech.esper.common.internal.util.SimpleNumberCoercer;
import com.espertech.esper.common.internal.util.SimpleNumberCoercerFactory;

import java.io.StringWriter;
import java.util.*;

public class FilterSpecCompilerIndexPlannerHelper {
    protected static SimpleNumberCoercer getNumberCoercer(Class leftType, Class rightType, String expression) throws ExprValidationException {
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

    protected static void throwConversionError(Class fromType, Class toType, String propertyName)
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

    protected static MatchedEventConvertorForge getMatchEventConvertor(ExprNode value, LinkedHashMap<String, Pair<EventType, String>> taggedEventTypes, LinkedHashMap<String, Pair<EventType, String>> arrayEventTypes, LinkedHashSet<String> allTagNamesOrdered) throws ExprValidationException {
        ExprNodeStreamUseCollectVisitor streamUseCollectVisitor = new ExprNodeStreamUseCollectVisitor();
        value.accept(streamUseCollectVisitor);

        Set<Integer> streams = new HashSet<>(streamUseCollectVisitor.getReferenced().size());
        for (ExprStreamRefNode streamRefNode : streamUseCollectVisitor.getReferenced()) {
            if (streamRefNode.getStreamReferencedIfAny() == null) {
                continue;
            }
            streams.add(streamRefNode.getStreamReferencedIfAny());
        }

        return new MatchedEventConvertorForge(taggedEventTypes, arrayEventTypes, allTagNamesOrdered, streams, true);
    }

    protected static Pair<Integer, String> getStreamIndex(String resolvedPropertyName) {
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

    protected static boolean isLimitedValueExpression(ExprNode node) {
        FilterSpecExprNodeVisitorValueLimitedExpr visitor = new FilterSpecExprNodeVisitorValueLimitedExpr();
        node.accept(visitor);
        return visitor.isLimited();
    }

    protected static EventType getArrayInnerEventType(LinkedHashMap<String, Pair<EventType, String>> arrayEventTypes, String streamName) {
        Pair<EventType, String> arrayEventType = arrayEventTypes.get(streamName);
        Object prop = ((MapEventType) arrayEventType.getFirst()).getTypes().get(streamName);
        return ((EventType[]) prop)[0];
    }

    // expressions automatically coerce to the most upwards type
    // filters require the same type
    protected static Object handleConstantsCoercion(ExprFilterSpecLookupableFactoryForge lookupable, Object constant)
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

    protected static FilterSpecParamFilterForEvalDoubleForge getIdentNodeDoubleEval(ExprIdentNode node, LinkedHashMap<String, Pair<EventType, String>> arrayEventTypes, String statementName) {
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

    protected static boolean isLimitedLookupableExpression(ExprNode node) {
        FilterSpecExprNodeVisitorLookupableLimitedExpr visitor = new FilterSpecExprNodeVisitorLookupableLimitedExpr();
        node.accept(visitor);
        return visitor.isLimited() && visitor.isHasStreamZeroReference();
    }

    protected static ExprFilterSpecLookupableFactoryForgePremade makeLimitedLookupableForgeMayNull(ExprNode lookupable, StatementRawInfo raw, StatementCompileTimeServices services) throws ExprValidationException {
        List<String> hints = HintEnum.FILTERINDEX.getHintAssignedValues(raw.getAnnotations());
        if (hints == null) {
            return null;
        }
        for (String hint : hints) {
            String[] hintAtoms = HintEnum.splitCommaUnlessInParen(hint);
            for (int i = 0; i < hintAtoms.length; i++) {
                String hintAtom = hintAtoms[i];
                if (!hintAtom.toLowerCase(Locale.ENGLISH).trim().equals("lkupcomposite")) {
                    throw new ExprValidationException("Unrecognized filterindex hint value '" + hintAtom + "'");
                }
            }
        }
        Class lookupableType = lookupable.getForge().getEvaluationType();
        String expression = ExprNodeUtilityPrint.toExpressionStringMinPrecedenceSafe(lookupable);
        FilterSpecCompilerIndexLimitedLookupableGetterForge getterForge = new FilterSpecCompilerIndexLimitedLookupableGetterForge(lookupable);
        DataInputOutputSerdeForge serde = services.getSerdeResolver().serdeForFilter(lookupableType, raw);
        return new ExprFilterSpecLookupableFactoryForgePremade(expression, getterForge, lookupableType, true, serde);
    }
}
