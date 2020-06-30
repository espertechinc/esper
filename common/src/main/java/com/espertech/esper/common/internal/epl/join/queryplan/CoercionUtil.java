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
package com.espertech.esper.common.internal.epl.join.queryplan;

import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.client.type.EPType;
import com.espertech.esper.common.client.type.EPTypeClass;
import com.espertech.esper.common.client.type.EPTypeNull;
import com.espertech.esper.common.internal.epl.expression.core.ExprNode;
import com.espertech.esper.common.internal.epl.join.querygraph.*;
import com.espertech.esper.common.internal.epl.lookupplan.SubordPropHashKeyForge;
import com.espertech.esper.common.internal.epl.lookupplan.SubordPropRangeKeyForge;
import com.espertech.esper.common.internal.util.JavaClassHelper;

import java.util.List;
import java.util.Map;

public class CoercionUtil {

    private static final EPTypeClass[] NULL_ARRAY = new EPTypeClass[0];

    public static CoercionDesc getCoercionTypesRange(EventType[] typesPerStream, int indexedStream, String[] indexedProp, List<QueryGraphValueEntryRangeForge> rangeEntries) {
        if (rangeEntries.isEmpty()) {
            return new CoercionDesc(false, NULL_ARRAY);
        }

        EPTypeClass[] coercionTypes = new EPTypeClass[rangeEntries.size()];
        boolean mustCoerce = false;
        for (int i = 0; i < rangeEntries.size(); i++) {
            QueryGraphValueEntryRangeForge entry = rangeEntries.get(i);

            String indexed = indexedProp[i];
            EPType valuePropType = JavaClassHelper.getBoxedType(typesPerStream[indexedStream].getPropertyEPType(indexed));
            EPTypeClass coercionType;

            if (entry.getType().isRange()) {
                QueryGraphValueEntryRangeInForge rangeIn = (QueryGraphValueEntryRangeInForge) entry;
                coercionType = getCoercionTypeRangeIn(valuePropType, rangeIn.getExprStart(), rangeIn.getExprEnd());
            } else {
                QueryGraphValueEntryRangeRelOpForge relOp = (QueryGraphValueEntryRangeRelOpForge) entry;
                coercionType = getCoercionType(valuePropType, relOp.getExpression().getForge().getEvaluationType());
            }

            if (coercionType == null) {
                coercionTypes[i] = valuePropType == EPTypeNull.INSTANCE ? null : (EPTypeClass) valuePropType;
            } else {
                mustCoerce = true;
                coercionTypes[i] = coercionType;
            }
        }

        return new CoercionDesc(mustCoerce, coercionTypes);
    }

    /**
     * Returns null if no coercion is required, or an array of classes for use in coercing the
     * lookup keys and index keys into a common type.
     *
     * @param typesPerStream is the event types for each stream
     * @param lookupStream   is the stream looked up from
     * @param indexedStream  is the indexed stream
     * @param keyProps       is the properties to use to look up
     * @param indexProps     is the properties to index on
     * @return coercion types, or null if none required
     */
    public static CoercionDesc getCoercionTypesHash(EventType[] typesPerStream,
                                                    int lookupStream,
                                                    int indexedStream,
                                                    List<QueryGraphValueEntryHashKeyedForge> keyProps,
                                                    String[] indexProps) {
        if (indexProps.length == 0 && keyProps.size() == 0) {
            return new CoercionDesc(false, NULL_ARRAY);
        }
        if (indexProps.length != keyProps.size()) {
            throw new IllegalStateException("Mismatch in the number of key and index properties");
        }

        EPTypeClass[] coercionTypes = new EPTypeClass[indexProps.length];
        boolean mustCoerce = false;
        for (int i = 0; i < keyProps.size(); i++) {
            EPType keyPropType;
            if (keyProps.get(i) instanceof QueryGraphValueEntryHashKeyedForgeExpr) {
                QueryGraphValueEntryHashKeyedForgeExpr hashExpr = (QueryGraphValueEntryHashKeyedForgeExpr) keyProps.get(i);
                keyPropType = hashExpr.getKeyExpr().getForge().getEvaluationType();
            } else {
                QueryGraphValueEntryHashKeyedForgeProp hashKeyProp = (QueryGraphValueEntryHashKeyedForgeProp) keyProps.get(i);
                keyPropType = JavaClassHelper.getBoxedType(typesPerStream[lookupStream].getPropertyEPType(hashKeyProp.getKeyProperty()));
            }

            EPTypeClass indexedPropType = (EPTypeClass) JavaClassHelper.getBoxedType(typesPerStream[indexedStream].getPropertyEPType(indexProps[i]));
            EPTypeClass coercionType = indexedPropType;
            if (keyPropType != indexedPropType) {
                coercionType = JavaClassHelper.getCompareToCoercionType(keyPropType, indexedPropType);
                mustCoerce = true;
            }
            coercionTypes[i] = coercionType;
        }
        return new CoercionDesc(mustCoerce, coercionTypes);
    }

    public static CoercionDesc getCoercionTypesRange(EventType viewableEventType, Map<String, SubordPropRangeKeyForge> rangeProps, EventType[] typesPerStream) {
        if (rangeProps.isEmpty()) {
            return new CoercionDesc(false, NULL_ARRAY);
        }

        EPTypeClass[] coercionTypes = new EPTypeClass[rangeProps.size()];
        boolean mustCoerce = false;
        int count = 0;
        for (Map.Entry<String, SubordPropRangeKeyForge> entry : rangeProps.entrySet()) {
            SubordPropRangeKeyForge subQRange = entry.getValue();
            QueryGraphValueEntryRangeForge rangeDesc = entry.getValue().getRangeInfo();

            EPType valuePropType = JavaClassHelper.getBoxedType(viewableEventType.getPropertyEPType(entry.getKey()));
            EPTypeClass coercionType;

            if (rangeDesc.getType().isRange()) {
                QueryGraphValueEntryRangeInForge rangeIn = (QueryGraphValueEntryRangeInForge) rangeDesc;
                coercionType = getCoercionTypeRangeIn(valuePropType, rangeIn.getExprStart(), rangeIn.getExprEnd());
            } else {
                QueryGraphValueEntryRangeRelOpForge relOp = (QueryGraphValueEntryRangeRelOpForge) rangeDesc;
                coercionType = getCoercionType(valuePropType, relOp.getExpression().getForge().getEvaluationType());
            }

            if (coercionType == null) {
                coercionTypes[count++] = valuePropType == EPTypeNull.INSTANCE ? null : (EPTypeClass) valuePropType;
            } else {
                mustCoerce = true;
                coercionTypes[count++] = coercionType;
            }
        }
        return new CoercionDesc(mustCoerce, coercionTypes);
    }

    private static EPTypeClass getCoercionType(EPType valuePropType, EPType keyPropTypeExpr) {
        EPTypeClass coercionType = null;
        EPType keyPropType = JavaClassHelper.getBoxedType(keyPropTypeExpr);
        if (!valuePropType.equals(keyPropType)) {
            coercionType = JavaClassHelper.getCompareToCoercionType(valuePropType, keyPropType);
        }
        return coercionType;
    }

    public static CoercionDesc getCoercionTypesHash(EventType viewableEventType, String[] indexProps, List<SubordPropHashKeyForge> hashKeys) {
        if (indexProps.length == 0 && hashKeys.size() == 0) {
            return new CoercionDesc(false, NULL_ARRAY);
        }
        if (indexProps.length != hashKeys.size()) {
            throw new IllegalStateException("Mismatch in the number of key and index properties");
        }

        EPTypeClass[] coercionTypes = new EPTypeClass[indexProps.length];
        boolean mustCoerce = false;
        for (int i = 0; i < hashKeys.size(); i++) {
            EPType keyPropType = JavaClassHelper.getBoxedType(hashKeys.get(i).getHashKey().getKeyExpr().getForge().getEvaluationType());
            EPTypeClass indexedPropType = (EPTypeClass) JavaClassHelper.getBoxedType(viewableEventType.getPropertyEPType(indexProps[i]));
            EPTypeClass coercionType = indexedPropType;
            if (keyPropType != indexedPropType) {
                coercionType = JavaClassHelper.getCompareToCoercionType(keyPropType, indexedPropType);
                mustCoerce = true;
            }
            coercionTypes[i] = coercionType;
        }
        return new CoercionDesc(mustCoerce, coercionTypes);
    }

    public static EPTypeClass getCoercionTypeRangeIn(EPType valuePropType, ExprNode exprStart, ExprNode exprEnd) {
        EPTypeClass coercionType = null;
        EPType startPropType = JavaClassHelper.getBoxedType(exprStart.getForge().getEvaluationType());
        EPType endPropType = JavaClassHelper.getBoxedType(exprEnd.getForge().getEvaluationType());

        if (!(valuePropType.equals(startPropType))) {
            coercionType = JavaClassHelper.getCompareToCoercionType(valuePropType, startPropType);
        }
        if (!(valuePropType.equals(endPropType))) {
            coercionType = JavaClassHelper.getCompareToCoercionType(coercionType, endPropType);
        }
        if (coercionType == null) {
            return null;
        }
        return coercionType;
    }

    public static EPTypeClass[] getCoercionTypes(EPType[] propTypes) {
        EPTypeClass[] classes = new EPTypeClass[propTypes.length];
        for (int i = 0; i < propTypes.length; i++) {
            EPType type = propTypes[i];
            classes[i] = type == null || type == EPTypeNull.INSTANCE ? null : (EPTypeClass) type;
        }
        return classes;
    }
}
