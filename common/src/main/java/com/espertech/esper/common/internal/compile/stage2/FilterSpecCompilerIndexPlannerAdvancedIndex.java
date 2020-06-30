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
import com.espertech.esper.common.client.type.EPTypeClass;
import com.espertech.esper.common.internal.collection.Pair;
import com.espertech.esper.common.internal.epl.expression.core.*;
import com.espertech.esper.common.internal.epl.index.advanced.index.quadtree.AdvancedIndexConfigContextPartitionQuadTree;
import com.espertech.esper.common.internal.epl.index.advanced.index.quadtree.SettingsApplicationDotMethodPointInsideRectange;
import com.espertech.esper.common.internal.epl.index.advanced.index.quadtree.SettingsApplicationDotMethodRectangeIntersectsRectangle;
import com.espertech.esper.common.internal.event.core.EventPropertyGetterSPI;
import com.espertech.esper.common.internal.filterspec.*;
import com.espertech.esper.common.internal.type.XYPoint;
import com.espertech.esper.common.internal.type.XYWHRectangle;

import java.io.StringWriter;
import java.util.LinkedHashMap;
import java.util.Locale;

import static com.espertech.esper.common.internal.compile.stage2.FilterSpecCompilerIndexPlannerHelper.getIdentNodeDoubleEval;

public class FilterSpecCompilerIndexPlannerAdvancedIndex {
    protected static FilterSpecParamForge handleAdvancedIndexDescProvider(FilterSpecCompilerAdvIndexDescProvider provider, LinkedHashMap<String, Pair<EventType, String>> arrayEventTypes, String statementName) throws ExprValidationException {
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

        EPTypeClass returnType;
        switch (filterDesc.getIndexType()) {
            case SettingsApplicationDotMethodPointInsideRectange.INDEXTYPE_NAME:
                returnType = XYPoint.EPTYPE;
                break;
            case SettingsApplicationDotMethodRectangeIntersectsRectangle.INDEXTYPE_NAME:
                returnType = XYWHRectangle.EPTYPE;
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
}
