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

public class EngineImportApplicationDotMethodPointInsideRectange extends EngineImportApplicationDotMethodBase {
    protected final static String LOOKUP_OPERATION_NAME = "point.inside(rectangle)";
    public final static String INDEXTYPE_NAME = "pointregionquadtree";

    public EngineImportApplicationDotMethodPointInsideRectange(String lhsName, ExprNode[] lhs, String dotMethodName, String rhsName, ExprNode[] rhs, ExprNode[] indexNamedParameter) {
        super(lhsName, lhs, dotMethodName, rhsName, rhs, indexNamedParameter);
    }

    protected ExprEvaluator validateAll(String lhsName, ExprNode[] lhs, String rhsName, ExprNode[] rhs, ExprValidationContext validationContext) throws ExprValidationException {
        EPLValidationUtil.validateParameterNumber(lhsName, LHS_VALIDATION_NAME, false, 2, lhs.length);
        EPLValidationUtil.validateParametersTypePredefined(lhs, lhsName, LHS_VALIDATION_NAME, EPLExpressionParamType.NUMERIC);

        EPLValidationUtil.validateParameterNumber(rhsName, RHS_VALIDATION_NAME, true, 4, rhs.length);
        EPLValidationUtil.validateParametersTypePredefined(rhs, rhsName, RHS_VALIDATION_NAME, EPLExpressionParamType.NUMERIC);

        ExprEvaluator pxEval = lhs[0].getExprEvaluator();
        ExprEvaluator pyEval = lhs[1].getExprEvaluator();
        ExprEvaluator xEval = rhs[0].getExprEvaluator();
        ExprEvaluator yEval = rhs[1].getExprEvaluator();
        ExprEvaluator widthEval = rhs[2].getExprEvaluator();
        ExprEvaluator heightEval = rhs[3].getExprEvaluator();
        return new PointIntersectsRectangleEvaluator(pxEval, pyEval, xEval, yEval, widthEval, heightEval);
    }

    protected String operationName() {
        return LOOKUP_OPERATION_NAME;
    }

    protected String indexTypeName() {
        return INDEXTYPE_NAME;
    }

    public final static class PointIntersectsRectangleEvaluator implements ExprEvaluator {
        private final ExprEvaluator pxEval;
        private final ExprEvaluator pyEval;
        private final ExprEvaluator xEval;
        private final ExprEvaluator yEval;
        private final ExprEvaluator widthEval;
        private final ExprEvaluator heightEval;

        PointIntersectsRectangleEvaluator(ExprEvaluator pxEval, ExprEvaluator pyEval, ExprEvaluator xEval, ExprEvaluator yEval, ExprEvaluator widthEval, ExprEvaluator heightEval) {
            this.pxEval = pxEval;
            this.pyEval = pyEval;
            this.xEval = xEval;
            this.yEval = yEval;
            this.widthEval = widthEval;
            this.heightEval = heightEval;
        }

        public Object evaluate(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
            Number px = (Number) pxEval.evaluate(eventsPerStream, isNewData, context);
            if (px == null) {
                return null;
            }
            Number py = (Number) pyEval.evaluate(eventsPerStream, isNewData, context);
            if (py == null) {
                return null;
            }
            Number x = (Number) xEval.evaluate(eventsPerStream, isNewData, context);
            if (x == null) {
                return null;
            }
            Number y = (Number) yEval.evaluate(eventsPerStream, isNewData, context);
            if (y == null) {
                return null;
            }
            Number width = (Number) widthEval.evaluate(eventsPerStream, isNewData, context);
            if (width == null) {
                return null;
            }
            Number height = (Number) heightEval.evaluate(eventsPerStream, isNewData, context);
            if (height == null) {
                return null;
            }
            return BoundingBox.containsPoint(x.doubleValue(), y.doubleValue(), width.doubleValue(), height.doubleValue(), px.doubleValue(), py.doubleValue());
        }

        public Class getType() {
            return Boolean.class;
        }
    }
}
