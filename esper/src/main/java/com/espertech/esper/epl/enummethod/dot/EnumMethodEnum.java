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
package com.espertech.esper.epl.enummethod.dot;

import com.espertech.esper.epl.enummethod.eval.*;
import com.espertech.esper.epl.methodbase.DotMethodFP;

import java.util.Locale;

public enum EnumMethodEnum {

    AGGREGATE("aggregate", ExprDotEvalAggregate.class, EnumMethodEnumParams.AGGREGATE_FP),

    ALLOF("allOf", ExprDotEvalAllOfAnyOf.class, EnumMethodEnumParams.ALLOF_ANYOF),
    ANYOF("anyOf", ExprDotEvalAllOfAnyOf.class, EnumMethodEnumParams.ALLOF_ANYOF),

    TOMAP("toMap", ExprDotEvalToMap.class, EnumMethodEnumParams.MAP),
    GROUPBY("groupBy", ExprDotEvalGroupBy.class, EnumMethodEnumParams.GROUP),

    COUNTOF("countOf", ExprDotEvalCountOf.class, EnumMethodEnumParams.COUNTOF_FIRST_LAST),
    MIN("min", ExprDotEvalMinMax.class, EnumMethodEnumParams.MIN_MAX),
    MAX("max", ExprDotEvalMinMax.class, EnumMethodEnumParams.MIN_MAX),
    AVERAGE("average", ExprDotEvalAverage.class, EnumMethodEnumParams.AVERAGE_SUMOF),
    SUMOF("sumOf", ExprDotEvalSumOf.class, EnumMethodEnumParams.AVERAGE_SUMOF),

    MOSTFREQUENT("mostFrequent", ExprDotEvalMostLeastFrequent.class, EnumMethodEnumParams.MOST_LEAST_FREQ),
    LEASTFREQUENT("leastFrequent", ExprDotEvalMostLeastFrequent.class, EnumMethodEnumParams.MOST_LEAST_FREQ),

    SELECTFROM("selectFrom", ExprDotEvalSelectFrom.class, EnumMethodEnumParams.SELECTFROM_MINBY_MAXBY),

    FIRST("firstOf", ExprDotEvalFirstLastOf.class, EnumMethodEnumParams.COUNTOF_FIRST_LAST),
    LAST("lastOf", ExprDotEvalFirstLastOf.class, EnumMethodEnumParams.COUNTOF_FIRST_LAST),
    MINBY("minBy", ExprDotEvalMinByMaxBy.class, EnumMethodEnumParams.SELECTFROM_MINBY_MAXBY),
    MAXBY("maxBy", ExprDotEvalMinByMaxBy.class, EnumMethodEnumParams.SELECTFROM_MINBY_MAXBY),

    TAKE("take", ExprDotEvalTakeAndTakeLast.class, EnumMethodEnumParams.TAKE),
    TAKELAST("takeLast", ExprDotEvalTakeAndTakeLast.class, EnumMethodEnumParams.TAKELAST),
    TAKEWHILE("takeWhile", ExprDotEvalTakeWhileAndLast.class, EnumMethodEnumParams.WHERE_FP),
    TAKEWHILELAST("takeWhileLast", ExprDotEvalTakeWhileAndLast.class, EnumMethodEnumParams.WHERE_FP),
    ORDERBY("orderBy", ExprDotEvalOrderByAscDesc.class, EnumMethodEnumParams.ORDERBY_DISTINCT),
    ORDERBYDESC("orderByDesc", ExprDotEvalOrderByAscDesc.class, EnumMethodEnumParams.ORDERBY_DISTINCT),
    DISTINCT("distinctOf", ExprDotEvalDistinct.class, EnumMethodEnumParams.ORDERBY_DISTINCT),
    WHERE("where", ExprDotEvalWhere.class, EnumMethodEnumParams.WHERE_FP),
    UNION("union", ExprDotEvalSetExceptUnionIntersect.class, EnumMethodEnumParams.SET_LOGIC_FP),
    EXCEPT("except", ExprDotEvalSetExceptUnionIntersect.class, EnumMethodEnumParams.SET_LOGIC_FP),
    INTERSECT("intersect", ExprDotEvalSetExceptUnionIntersect.class, EnumMethodEnumParams.SET_LOGIC_FP),
    REVERSE("reverse", ExprDotEvalReverse.class, EnumMethodEnumParams.NOOP_REVERSE),
    NOOP("esperInternalNoop", ExprDotEvalNoOp.class, EnumMethodEnumParams.NOOP_REVERSE),

    SEQUENCE_EQUAL("sequenceequal", ExprDotEvalSequenceEqual.class, EnumMethodEnumParams.SEQ_EQUALS_FP);

    private final String nameCamel;
    private final Class implementation;
    private final DotMethodFP[] footprints;

    private EnumMethodEnum(String nameCamel, Class implementation, DotMethodFP[] footprints) {
        this.nameCamel = nameCamel;
        this.implementation = implementation;
        this.footprints = footprints;
    }

    public String getNameCamel() {
        return nameCamel;
    }

    public DotMethodFP[] getFootprints() {
        return footprints;
    }

    public static boolean isEnumerationMethod(String name) {
        for (EnumMethodEnum e : EnumMethodEnum.values()) {
            if (e.getNameCamel().toLowerCase(Locale.ENGLISH).equals(name.toLowerCase(Locale.ENGLISH))) {
                return true;
            }
        }
        return false;
    }

    public static EnumMethodEnum fromName(String name) {
        for (EnumMethodEnum e : EnumMethodEnum.values()) {
            if (e.getNameCamel().toLowerCase(Locale.ENGLISH).equals(name.toLowerCase(Locale.ENGLISH))) {
                return e;
            }
        }
        return null;
    }

    public Class getImplementation() {
        return implementation;
    }
}
