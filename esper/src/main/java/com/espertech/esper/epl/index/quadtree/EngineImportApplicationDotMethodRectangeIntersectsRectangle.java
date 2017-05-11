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
package com.espertech.esper.epl.index.quadtree;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.epl.expression.core.*;
import com.espertech.esper.epl.util.EPLExpressionParamType;
import com.espertech.esper.epl.util.EPLValidationUtil;
import com.espertech.esper.spatial.quadtree.core.BoundingBox;

public class EngineImportApplicationDotMethodRectangeIntersectsRectangle extends EngineImportApplicationDotMethodBase {
    protected final static String LOOKUP_OPERATION_NAME = "rectangle.intersects(rectangle)";
    public final static String INDEXTYPE_NAME = "mxcifquadtree";

    public EngineImportApplicationDotMethodRectangeIntersectsRectangle(String lhsName, ExprNode[] lhs, String dotMethodName, String rhsName, ExprNode[] rhs, ExprNode[] indexNamedParameter) {
        super(lhsName, lhs, dotMethodName, rhsName, rhs, indexNamedParameter);
    }

    protected ExprEvaluator validateAll(String lhsName, ExprNode[] lhs, String rhsName, ExprNode[] rhs, ExprValidationContext validationContext) throws ExprValidationException {
        EPLValidationUtil.validateParameterNumber(lhsName, LHS_VALIDATION_NAME, false, 4, lhs.length);
        EPLValidationUtil.validateParametersTypePredefined(lhs, lhsName, LHS_VALIDATION_NAME, EPLExpressionParamType.NUMERIC);

        EPLValidationUtil.validateParameterNumber(rhsName, RHS_VALIDATION_NAME, true, 4, rhs.length);
        EPLValidationUtil.validateParametersTypePredefined(rhs, rhsName, RHS_VALIDATION_NAME, EPLExpressionParamType.NUMERIC);

        ExprEvaluator meXEval = lhs[0].getExprEvaluator();
        ExprEvaluator meYEval = lhs[1].getExprEvaluator();
        ExprEvaluator meWidthEval = lhs[2].getExprEvaluator();
        ExprEvaluator meHeightEval = lhs[3].getExprEvaluator();

        ExprEvaluator otherXEval = rhs[0].getExprEvaluator();
        ExprEvaluator otherYEval = rhs[1].getExprEvaluator();
        ExprEvaluator otherWidthEval = rhs[2].getExprEvaluator();
        ExprEvaluator otherHeightEval = rhs[3].getExprEvaluator();
        return new RectangleIntersectsRectangleEvaluator(meXEval, meYEval, meWidthEval, meHeightEval, otherXEval, otherYEval, otherWidthEval, otherHeightEval);
    }

    protected String operationName() {
        return LOOKUP_OPERATION_NAME;
    }

    protected String indexTypeName() {
        return INDEXTYPE_NAME;
    }

    public final static class RectangleIntersectsRectangleEvaluator implements ExprEvaluator {

        private final ExprEvaluator meXEval;
        private final ExprEvaluator meYEval;
        private final ExprEvaluator meWidthEval;
        private final ExprEvaluator meHeightEval;
        private final ExprEvaluator otherXEval;
        private final ExprEvaluator otherYEval;
        private final ExprEvaluator otherWidthEval;
        private final ExprEvaluator otherHeightEval;

        public RectangleIntersectsRectangleEvaluator(ExprEvaluator meXEval, ExprEvaluator meYEval, ExprEvaluator meWidthEval, ExprEvaluator meHeightEval, ExprEvaluator otherXEval, ExprEvaluator otherYEval, ExprEvaluator otherWidthEval, ExprEvaluator otherHeightEval) {
            this.meXEval = meXEval;
            this.meYEval = meYEval;
            this.meWidthEval = meWidthEval;
            this.meHeightEval = meHeightEval;
            this.otherXEval = otherXEval;
            this.otherYEval = otherYEval;
            this.otherWidthEval = otherWidthEval;
            this.otherHeightEval = otherHeightEval;
        }

        public Object evaluate(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
            Number meX = (Number) meXEval.evaluate(eventsPerStream, isNewData, context);
            if (meX == null) {
                return null;
            }
            Number meY = (Number) meYEval.evaluate(eventsPerStream, isNewData, context);
            if (meY == null) {
                return null;
            }
            Number meWidth = (Number) meWidthEval.evaluate(eventsPerStream, isNewData, context);
            if (meWidth == null) {
                return null;
            }
            Number meHeight = (Number) meHeightEval.evaluate(eventsPerStream, isNewData, context);
            if (meHeight == null) {
                return null;
            }
            Number otherX = (Number) otherXEval.evaluate(eventsPerStream, isNewData, context);
            if (otherX == null) {
                return null;
            }
            Number otherY = (Number) otherYEval.evaluate(eventsPerStream, isNewData, context);
            if (otherY == null) {
                return null;
            }
            Number otherWidth = (Number) otherWidthEval.evaluate(eventsPerStream, isNewData, context);
            if (otherWidth == null) {
                return null;
            }
            Number otherHeight = (Number) otherHeightEval.evaluate(eventsPerStream, isNewData, context);
            if (otherHeight == null) {
                return null;
            }

            double x = meX.doubleValue();
            double y = meY.doubleValue();
            double width = meWidth.doubleValue();
            double height = meHeight.doubleValue();
            return BoundingBox.intersectsBoxIncludingEnd(x, y, x + width, y + height, otherX.doubleValue(), otherY.doubleValue(), otherWidth.doubleValue(), otherHeight.doubleValue());
        }

        public Class getType() {
            return Boolean.class;
        }
    }
}
