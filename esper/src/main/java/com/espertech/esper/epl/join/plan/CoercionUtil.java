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
package com.espertech.esper.epl.join.plan;

import com.espertech.esper.client.EventType;
import com.espertech.esper.epl.expression.core.ExprNode;
import com.espertech.esper.epl.lookup.SubordPropHashKey;
import com.espertech.esper.epl.lookup.SubordPropRangeKey;
import com.espertech.esper.util.JavaClassHelper;

import java.util.List;
import java.util.Map;

public class CoercionUtil {

    private static final Class[] NULL_ARRAY = new Class[0];

    public static CoercionDesc getCoercionTypesRange(EventType[] typesPerStream, int indexedStream, String[] indexedProp, List<QueryGraphValueEntryRange> rangeEntries) {
        if (rangeEntries.isEmpty()) {
            return new CoercionDesc(false, NULL_ARRAY);
        }

        Class[] coercionTypes = new Class[rangeEntries.size()];
        boolean mustCoerce = false;
        for (int i = 0; i < rangeEntries.size(); i++) {
            QueryGraphValueEntryRange entry = rangeEntries.get(i);

            String indexed = indexedProp[i];
            Class valuePropType = JavaClassHelper.getBoxedType(typesPerStream[indexedStream].getPropertyType(indexed));
            Class coercionType;

            if (entry.getType().isRange()) {
                QueryGraphValueEntryRangeIn rangeIn = (QueryGraphValueEntryRangeIn) entry;
                coercionType = getCoercionTypeRangeIn(valuePropType, rangeIn.getExprStart(), rangeIn.getExprEnd());
            } else {
                QueryGraphValueEntryRangeRelOp relOp = (QueryGraphValueEntryRangeRelOp) entry;
                coercionType = getCoercionType(valuePropType, relOp.getExpression().getForge().getEvaluationType());
            }

            if (coercionType == null) {
                coercionTypes[i] = valuePropType;
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
                                                    List<QueryGraphValueEntryHashKeyed> keyProps,
                                                    String[] indexProps) {
        if (indexProps.length == 0 && keyProps.size() == 0) {
            return new CoercionDesc(false, NULL_ARRAY);
        }
        if (indexProps.length != keyProps.size()) {
            throw new IllegalStateException("Mismatch in the number of key and index properties");
        }

        Class[] coercionTypes = new Class[indexProps.length];
        boolean mustCoerce = false;
        for (int i = 0; i < keyProps.size(); i++) {
            Class keyPropType;
            if (keyProps.get(i) instanceof QueryGraphValueEntryHashKeyedExpr) {
                QueryGraphValueEntryHashKeyedExpr hashExpr = (QueryGraphValueEntryHashKeyedExpr) keyProps.get(i);
                keyPropType = hashExpr.getKeyExpr().getForge().getEvaluationType();
            } else {
                QueryGraphValueEntryHashKeyedProp hashKeyProp = (QueryGraphValueEntryHashKeyedProp) keyProps.get(i);
                keyPropType = JavaClassHelper.getBoxedType(typesPerStream[lookupStream].getPropertyType(hashKeyProp.getKeyProperty()));
            }

            Class indexedPropType = JavaClassHelper.getBoxedType(typesPerStream[indexedStream].getPropertyType(indexProps[i]));
            Class coercionType = indexedPropType;
            if (keyPropType != indexedPropType) {
                coercionType = JavaClassHelper.getCompareToCoercionType(keyPropType, indexedPropType);
                mustCoerce = true;
            }
            coercionTypes[i] = coercionType;
        }
        return new CoercionDesc(mustCoerce, coercionTypes);
    }

    public static Class getCoercionTypeRange(EventType indexedType, String indexedProp, SubordPropRangeKey rangeKey) {
        QueryGraphValueEntryRange desc = rangeKey.getRangeInfo();
        if (desc.getType().isRange()) {
            QueryGraphValueEntryRangeIn rangeIn = (QueryGraphValueEntryRangeIn) desc;
            return getCoercionTypeRangeIn(indexedType.getPropertyType(indexedProp), rangeIn.getExprStart(), rangeIn.getExprEnd());
        } else {
            QueryGraphValueEntryRangeRelOp relOp = (QueryGraphValueEntryRangeRelOp) desc;
            return getCoercionType(indexedType.getPropertyType(indexedProp), relOp.getExpression().getForge().getEvaluationType());
        }
    }

    public static CoercionDesc getCoercionTypesRange(EventType viewableEventType, Map<String, SubordPropRangeKey> rangeProps, EventType[] typesPerStream) {
        if (rangeProps.isEmpty()) {
            return new CoercionDesc(false, NULL_ARRAY);
        }

        Class[] coercionTypes = new Class[rangeProps.size()];
        boolean mustCoerce = false;
        int count = 0;
        for (Map.Entry<String, SubordPropRangeKey> entry : rangeProps.entrySet()) {
            SubordPropRangeKey subQRange = entry.getValue();
            QueryGraphValueEntryRange rangeDesc = entry.getValue().getRangeInfo();

            Class valuePropType = JavaClassHelper.getBoxedType(viewableEventType.getPropertyType(entry.getKey()));
            Class coercionType;

            if (rangeDesc.getType().isRange()) {
                QueryGraphValueEntryRangeIn rangeIn = (QueryGraphValueEntryRangeIn) rangeDesc;
                coercionType = getCoercionTypeRangeIn(valuePropType, rangeIn.getExprStart(), rangeIn.getExprEnd());
            } else {
                QueryGraphValueEntryRangeRelOp relOp = (QueryGraphValueEntryRangeRelOp) rangeDesc;
                coercionType = getCoercionType(valuePropType, relOp.getExpression().getForge().getEvaluationType());
            }

            if (coercionType == null) {
                coercionTypes[count++] = valuePropType;
            } else {
                mustCoerce = true;
                coercionTypes[count++] = coercionType;
            }
        }
        return new CoercionDesc(mustCoerce, coercionTypes);
    }

    private static Class getCoercionType(Class valuePropType, Class keyPropTypeExpr) {
        Class coercionType = null;
        Class keyPropType = JavaClassHelper.getBoxedType(keyPropTypeExpr);
        if (valuePropType != keyPropType) {
            coercionType = JavaClassHelper.getCompareToCoercionType(valuePropType, keyPropType);
        }
        return coercionType;
    }

    public static CoercionDesc getCoercionTypesHash(EventType viewableEventType, String[] indexProps, List<SubordPropHashKey> hashKeys) {
        if (indexProps.length == 0 && hashKeys.size() == 0) {
            return new CoercionDesc(false, NULL_ARRAY);
        }
        if (indexProps.length != hashKeys.size()) {
            throw new IllegalStateException("Mismatch in the number of key and index properties");
        }

        Class[] coercionTypes = new Class[indexProps.length];
        boolean mustCoerce = false;
        for (int i = 0; i < hashKeys.size(); i++) {
            Class keyPropType = JavaClassHelper.getBoxedType(hashKeys.get(i).getHashKey().getKeyExpr().getForge().getEvaluationType());
            Class indexedPropType = JavaClassHelper.getBoxedType(viewableEventType.getPropertyType(indexProps[i]));
            Class coercionType = indexedPropType;
            if (keyPropType != indexedPropType) {
                coercionType = JavaClassHelper.getCompareToCoercionType(keyPropType, indexedPropType);
                mustCoerce = true;
            }
            coercionTypes[i] = coercionType;
        }
        return new CoercionDesc(mustCoerce, coercionTypes);
    }

    public static Class getCoercionTypeRangeIn(Class valuePropType, ExprNode exprStart, ExprNode exprEnd) {
        Class coercionType = null;
        Class startPropType = JavaClassHelper.getBoxedType(exprStart.getForge().getEvaluationType());
        Class endPropType = JavaClassHelper.getBoxedType(exprEnd.getForge().getEvaluationType());

        if (valuePropType != startPropType) {
            coercionType = JavaClassHelper.getCompareToCoercionType(valuePropType, startPropType);
        }
        if (valuePropType != endPropType) {
            coercionType = JavaClassHelper.getCompareToCoercionType(coercionType, endPropType);
        }
        return coercionType;
    }
}
