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
package com.espertech.esper.common.internal.epl.enummethod.dot;

import com.espertech.esper.common.internal.epl.enummethod.eval.*;
import com.espertech.esper.common.internal.epl.methodbase.DotMethodFP;
import com.espertech.esper.common.internal.util.JavaClassHelper;

import java.util.Locale;

public enum EnumMethodBuiltin {

    AGGREGATE("aggregate", ExprDotForgeAggregate.class, EnumMethodEnumParams.AGGREGATE_FP),

    ALLOF("allOf", ExprDotForgeAllOfAnyOf.class, EnumMethodEnumParams.ALLOF_ANYOF),
    ANYOF("anyOf", ExprDotForgeAllOfAnyOf.class, EnumMethodEnumParams.ALLOF_ANYOF),

    TOMAP("toMap", ExprDotForgeToMap.class, EnumMethodEnumParams.MAP),
    GROUPBY("groupBy", ExprDotForgeGroupBy.class, EnumMethodEnumParams.GROUP),

    COUNTOF("countOf", ExprDotForgeCountOf.class, EnumMethodEnumParams.COUNTOF_FIRST_LAST),
    MIN("min", ExprDotForgeMinMax.class, EnumMethodEnumParams.MIN_MAX),
    MAX("max", ExprDotForgeMinMax.class, EnumMethodEnumParams.MIN_MAX),
    AVERAGE("average", ExprDotForgeAverage.class, EnumMethodEnumParams.AVERAGE_SUMOF),
    SUMOF("sumOf", ExprDotForgeSumOf.class, EnumMethodEnumParams.AVERAGE_SUMOF),

    MOSTFREQUENT("mostFrequent", ExprDotForgeMostLeastFrequent.class, EnumMethodEnumParams.MOST_LEAST_FREQ),
    LEASTFREQUENT("leastFrequent", ExprDotForgeMostLeastFrequent.class, EnumMethodEnumParams.MOST_LEAST_FREQ),

    SELECTFROM("selectFrom", ExprDotForgeSelectFrom.class, EnumMethodEnumParams.SELECTFROM),

    FIRST("firstOf", ExprDotForgeFirstLastOf.class, EnumMethodEnumParams.COUNTOF_FIRST_LAST),
    LAST("lastOf", ExprDotForgeFirstLastOf.class, EnumMethodEnumParams.COUNTOF_FIRST_LAST),
    MINBY("minBy", ExprDotForgeMinByMaxBy.class, EnumMethodEnumParams.MINBY_MAXBY),
    MAXBY("maxBy", ExprDotForgeMinByMaxBy.class, EnumMethodEnumParams.MINBY_MAXBY),

    TAKE("take", ExprDotForgeTakeAndTakeLast.class, EnumMethodEnumParams.TAKE),
    TAKELAST("takeLast", ExprDotForgeTakeAndTakeLast.class, EnumMethodEnumParams.TAKELAST),
    TAKEWHILE("takeWhile", ExprDotForgeTakeWhileAndLast.class, EnumMethodEnumParams.WHERE_FP),
    TAKEWHILELAST("takeWhileLast", ExprDotForgeTakeWhileAndLast.class, EnumMethodEnumParams.WHERE_FP),
    ORDERBY("orderBy", ExprDotForgeOrderByAscDesc.class, EnumMethodEnumParams.ORDERBY_DISTINCT),
    ORDERBYDESC("orderByDesc", ExprDotForgeOrderByAscDesc.class, EnumMethodEnumParams.ORDERBY_DISTINCT),
    DISTINCT("distinctOf", ExprDotForgeDistinct.class, EnumMethodEnumParams.ORDERBY_DISTINCT),
    WHERE("where", ExprDotForgeWhere.class, EnumMethodEnumParams.WHERE_FP),
    UNION("union", ExprDotForgeSetExceptUnionIntersect.class, EnumMethodEnumParams.SET_LOGIC_FP),
    EXCEPT("except", ExprDotForgeSetExceptUnionIntersect.class, EnumMethodEnumParams.SET_LOGIC_FP),
    INTERSECT("intersect", ExprDotForgeSetExceptUnionIntersect.class, EnumMethodEnumParams.SET_LOGIC_FP),
    REVERSE("reverse", ExprDotForgeReverse.class, EnumMethodEnumParams.NOOP_REVERSE),
    NOOP("esperInternalNoop", ExprDotForgeNoOp.class, EnumMethodEnumParams.NOOP_REVERSE),

    SEQUENCE_EQUAL("sequenceequal", ExprDotForgeSequenceEqual.class, EnumMethodEnumParams.SEQ_EQUALS_FP);

    private final String nameCamel;
    private final EnumMethodEnum enumMethodEnum;
    private final ExprDotForgeEnumMethodFactory factory;
    private final DotMethodFP[] footprints;

    EnumMethodBuiltin(String nameCamel, Class implementationClass, DotMethodFP[] footprints) {
        this.nameCamel = nameCamel;
        this.enumMethodEnum = EnumMethodEnum.valueOf(nameCamel.toUpperCase(Locale.ENGLISH));
        this.factory = () -> (ExprDotForgeEnumMethod) JavaClassHelper.instantiate(ExprDotForgeEnumMethod.class, implementationClass);
        this.footprints = footprints;
    }

    public String getNameCamel() {
        return nameCamel;
    }

    public DotMethodFP[] getFootprints() {
        return footprints;
    }

    public ExprDotForgeEnumMethodFactory getFactory() {
        return factory;
    }

    public EnumMethodDesc getDescriptor() {
        return new EnumMethodDesc(nameCamel, enumMethodEnum, factory, footprints);
    }
}
