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

import com.espertech.esper.common.internal.epl.enummethod.eval.aggregate.ExprDotForgeAggregate;
import com.espertech.esper.common.internal.epl.enummethod.eval.plain.exceptintersectunion.ExprDotForgeSetExceptIntersectUnion;
import com.espertech.esper.common.internal.epl.enummethod.eval.plain.noop.ExprDotForgeNoOp;
import com.espertech.esper.common.internal.epl.enummethod.eval.plain.reverse.ExprDotForgeReverse;
import com.espertech.esper.common.internal.epl.enummethod.eval.plain.sequenceequal.ExprDotForgeSequenceEqual;
import com.espertech.esper.common.internal.epl.enummethod.eval.plain.take.ExprDotForgeTakeAndTakeLast;
import com.espertech.esper.common.internal.epl.enummethod.eval.singlelambdaopt3form.arrayOf.ExprDotForgeArrayOf;
import com.espertech.esper.common.internal.epl.enummethod.eval.singlelambdaopt3form.average.ExprDotForgeAverage;
import com.espertech.esper.common.internal.epl.enummethod.eval.singlelambdaopt3form.distinctof.ExprDotForgeDistinctOf;
import com.espertech.esper.common.internal.epl.enummethod.eval.singlelambdaopt3form.firstoflastof.ExprDotForgeFirstLastOf;
import com.espertech.esper.common.internal.epl.enummethod.eval.singlelambdaopt3form.allofanyof.ExprDotForgeAllOfAnyOf;
import com.espertech.esper.common.internal.epl.enummethod.eval.singlelambdaopt3form.countof.ExprDotForgeCountOf;
import com.espertech.esper.common.internal.epl.enummethod.eval.singlelambdaopt3form.groupby.ExprDotForgeGroupByOneParam;
import com.espertech.esper.common.internal.epl.enummethod.eval.singlelambdaopt3form.minmax.ExprDotForgeMinMax;
import com.espertech.esper.common.internal.epl.enummethod.eval.singlelambdaopt3form.minmaxby.ExprDotForgeMinByMaxBy;
import com.espertech.esper.common.internal.epl.enummethod.eval.singlelambdaopt3form.mostleastfreq.ExprDotForgeMostLeastFrequent;
import com.espertech.esper.common.internal.epl.enummethod.eval.singlelambdaopt3form.orderby.ExprDotForgeOrderByAscDesc;
import com.espertech.esper.common.internal.epl.enummethod.eval.singlelambdaopt3form.selectfrom.ExprDotForgeSelectFrom;
import com.espertech.esper.common.internal.epl.enummethod.eval.singlelambdaopt3form.sumof.ExprDotForgeSumOf;
import com.espertech.esper.common.internal.epl.enummethod.eval.singlelambdaopt3form.takewhile.ExprDotForgeTakeWhileAndLast;
import com.espertech.esper.common.internal.epl.enummethod.eval.singlelambdaopt3form.where.ExprDotForgeWhere;
import com.espertech.esper.common.internal.epl.enummethod.eval.twolambda.groupby.ExprDotForgeGroupByTwoParam;
import com.espertech.esper.common.internal.epl.enummethod.eval.twolambda.tomap.ExprDotForgeToMap;
import com.espertech.esper.common.internal.epl.methodbase.DotMethodFP;
import com.espertech.esper.common.internal.util.JavaClassHelper;

import java.util.Locale;

public enum EnumMethodBuiltin {

    AGGREGATE("aggregate", ExprDotForgeAggregate.class, EnumMethodEnumParams.AGGREGATE_FP),

    ALLOF("allOf", ExprDotForgeAllOfAnyOf.class, EnumMethodEnumParams.ALLOF_ANYOF),
    ANYOF("anyOf", ExprDotForgeAllOfAnyOf.class, EnumMethodEnumParams.ALLOF_ANYOF),

    TOMAP("toMap", ExprDotForgeToMap.class, EnumMethodEnumParams.TOMAP),
    GROUPBY("groupBy", numParameters -> numParameters == 1 ? new ExprDotForgeGroupByOneParam() : new ExprDotForgeGroupByTwoParam(), EnumMethodEnumParams.GROUP),

    COUNTOF("countOf", ExprDotForgeCountOf.class, EnumMethodEnumParams.COUNTOF_FIRST_LAST),
    MIN("min", ExprDotForgeMinMax.class, EnumMethodEnumParams.ORDERBY_DISTINCT_ARRAYOF_MOSTLEAST_MINMAX),
    MAX("max", ExprDotForgeMinMax.class, EnumMethodEnumParams.ORDERBY_DISTINCT_ARRAYOF_MOSTLEAST_MINMAX),
    AVERAGE("average", ExprDotForgeAverage.class, EnumMethodEnumParams.AVERAGE_SUMOF),
    SUMOF("sumOf", ExprDotForgeSumOf.class, EnumMethodEnumParams.AVERAGE_SUMOF),

    MOSTFREQUENT("mostFrequent", ExprDotForgeMostLeastFrequent.class, EnumMethodEnumParams.ORDERBY_DISTINCT_ARRAYOF_MOSTLEAST_MINMAX),
    LEASTFREQUENT("leastFrequent", ExprDotForgeMostLeastFrequent.class, EnumMethodEnumParams.ORDERBY_DISTINCT_ARRAYOF_MOSTLEAST_MINMAX),

    SELECTFROM("selectFrom", ExprDotForgeSelectFrom.class, EnumMethodEnumParams.SELECTFROM_MINMAXBY),

    FIRST("firstOf", ExprDotForgeFirstLastOf.class, EnumMethodEnumParams.COUNTOF_FIRST_LAST),
    LAST("lastOf", ExprDotForgeFirstLastOf.class, EnumMethodEnumParams.COUNTOF_FIRST_LAST),
    MINBY("minBy", ExprDotForgeMinByMaxBy.class, EnumMethodEnumParams.SELECTFROM_MINMAXBY),
    MAXBY("maxBy", ExprDotForgeMinByMaxBy.class, EnumMethodEnumParams.SELECTFROM_MINMAXBY),

    TAKE("take", ExprDotForgeTakeAndTakeLast.class, EnumMethodEnumParams.TAKE),
    TAKELAST("takeLast", ExprDotForgeTakeAndTakeLast.class, EnumMethodEnumParams.TAKELAST),
    TAKEWHILE("takeWhile", ExprDotForgeTakeWhileAndLast.class, EnumMethodEnumParams.WHERE_FP),
    TAKEWHILELAST("takeWhileLast", ExprDotForgeTakeWhileAndLast.class, EnumMethodEnumParams.WHERE_FP),
    ORDERBY("orderBy", ExprDotForgeOrderByAscDesc.class, EnumMethodEnumParams.ORDERBY_DISTINCT_ARRAYOF_MOSTLEAST_MINMAX),
    ORDERBYDESC("orderByDesc", ExprDotForgeOrderByAscDesc.class, EnumMethodEnumParams.ORDERBY_DISTINCT_ARRAYOF_MOSTLEAST_MINMAX),
    DISTINCT("distinctOf", ExprDotForgeDistinctOf.class, EnumMethodEnumParams.ORDERBY_DISTINCT_ARRAYOF_MOSTLEAST_MINMAX),
    WHERE("where", ExprDotForgeWhere.class, EnumMethodEnumParams.WHERE_FP),
    UNION("union", ExprDotForgeSetExceptIntersectUnion.class, EnumMethodEnumParams.SET_LOGIC_FP),
    EXCEPT("except", ExprDotForgeSetExceptIntersectUnion.class, EnumMethodEnumParams.SET_LOGIC_FP),
    INTERSECT("intersect", ExprDotForgeSetExceptIntersectUnion.class, EnumMethodEnumParams.SET_LOGIC_FP),
    REVERSE("reverse", ExprDotForgeReverse.class, EnumMethodEnumParams.NOOP_REVERSE),
    NOOP("esperInternalNoop", ExprDotForgeNoOp.class, EnumMethodEnumParams.NOOP_REVERSE),

    SEQUENCE_EQUAL("sequenceequal", ExprDotForgeSequenceEqual.class, EnumMethodEnumParams.SEQ_EQUALS_FP),

    ARRAYOF("arrayOf", ExprDotForgeArrayOf.class, EnumMethodEnumParams.ORDERBY_DISTINCT_ARRAYOF_MOSTLEAST_MINMAX);

    private final String nameCamel;
    private final EnumMethodEnum enumMethodEnum;
    private final ExprDotForgeEnumMethodFactory factory;
    private final DotMethodFP[] footprints;

    EnumMethodBuiltin(String nameCamel, Class implementationClass, DotMethodFP[] footprints) {
        this(nameCamel, numParams -> (ExprDotForgeEnumMethod) JavaClassHelper.instantiate(ExprDotForgeEnumMethod.class, implementationClass), footprints);
    }

    EnumMethodBuiltin(String nameCamel, ExprDotForgeEnumMethodFactory factory, DotMethodFP[] footprints) {
        this.nameCamel = nameCamel;
        this.enumMethodEnum = EnumMethodEnum.valueOf(nameCamel.toUpperCase(Locale.ENGLISH));
        this.factory = factory;
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
