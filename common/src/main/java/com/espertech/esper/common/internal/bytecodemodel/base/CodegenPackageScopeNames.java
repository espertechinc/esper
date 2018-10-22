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
package com.espertech.esper.common.internal.bytecodemodel.base;

public class CodegenPackageScopeNames {
    public final static String AGG_MR = "aggTop_mr_";

    public static String aggTop() {
        return "aggTop";
    }

    public static String aggView(int streamNumber) {
        return "aggview_" + streamNumber;
    }

    public static String previous(int streamNumber) {
        return "prev_" + streamNumber;
    }

    public static String previousMatchRecognize() {
        return "prevmr";
    }

    public static String prior(int streamNumber) {
        return "prior_" + streamNumber;
    }

    public static String priorSubquery(int subqueryNum) {
        return "prior_subq_" + subqueryNum;
    }

    public static String anyField(int number) {
        return "f" + number;
    }

    public static String anySubstitutionParam(int number) {
        return "p" + number;
    }

    public static String subqueryResultFuture(int subselectNumber) {
        return "subq_" + subselectNumber;
    }

    public static String previousSubquery(int subqueryNum) {
        return "prev_subq_" + subqueryNum;
    }

    public static String aggregationSubquery(int subqueryNum) {
        return "aggTop_subq_" + subqueryNum;
    }

    public static String aggregationMatchRecognize(int streamNum) {
        return AGG_MR + streamNum;
    }

    public static String classPostfixAggregationForView(int streamNumber) {
        return "view_" + streamNumber;
    }

    public static String classPostfixAggregationForSubquery(int subqueryNumber) {
        return "subq_" + subqueryNumber;
    }

    public static String tableAccessResultFuture(int tableAccessNumber) {
        return "ta_" + tableAccessNumber;
    }
}
