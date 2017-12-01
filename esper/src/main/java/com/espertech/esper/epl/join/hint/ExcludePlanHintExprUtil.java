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
package com.espertech.esper.epl.join.hint;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.core.context.mgr.ContextManagementServiceImpl;
import com.espertech.esper.core.service.EPAdministratorHelper;
import com.espertech.esper.core.service.StatementContext;
import com.espertech.esper.core.support.SupportEventAdapterService;
import com.espertech.esper.epl.declexpr.ExprDeclaredServiceImpl;
import com.espertech.esper.epl.expression.core.*;
import com.espertech.esper.epl.spec.SelectClauseStreamSelectorEnum;
import com.espertech.esper.epl.spec.StatementSpecRaw;
import com.espertech.esper.epl.table.mgmt.TableServiceImpl;
import com.espertech.esper.epl.util.EPLValidationUtil;
import com.espertech.esper.event.EventTypeMetadata;
import com.espertech.esper.event.arr.ObjectArrayEventBean;
import com.espertech.esper.event.arr.ObjectArrayEventType;
import com.espertech.esper.pattern.PatternNodeFactoryImpl;

import java.util.LinkedHashMap;

public class ExcludePlanHintExprUtil {

    protected final static ObjectArrayEventType OAEXPRESSIONTYPE;

    static {
        LinkedHashMap<String, Object> properties = new LinkedHashMap<String, Object>();
        properties.put("from_streamnum", Integer.class);
        properties.put("to_streamnum", Integer.class);
        properties.put("from_streamname", String.class);
        properties.put("to_streamname", String.class);
        properties.put("opname", String.class);
        properties.put("exprs", String[].class);
        OAEXPRESSIONTYPE = new ObjectArrayEventType(EventTypeMetadata.createAnonymous(ExcludePlanHintExprUtil.class.getSimpleName(), EventTypeMetadata.ApplicationType.OBJECTARR),
                ExcludePlanHintExprUtil.class.getSimpleName(), 0, SupportEventAdapterService.getService(), properties, null, null, null);
    }

    public static EventBean toEvent(int fromStreamnum,
                                    int toStreamnum,
                                    String fromStreamname,
                                    String toStreamname,
                                    String opname,
                                    ExprNode[] expressions) {
        String[] texts = new String[expressions.length];
        for (int i = 0; i < expressions.length; i++) {
            texts[i] = ExprNodeUtilityCore.toExpressionStringMinPrecedenceSafe(expressions[i]);
        }
        Object[] event = new Object[]{fromStreamnum, toStreamnum, fromStreamname, toStreamname, opname, texts};
        return new ObjectArrayEventBean(event, OAEXPRESSIONTYPE);
    }

    public static ExprForge toExpression(String hint, StatementContext statementContext) throws ExprValidationException {
        String toCompile = "select * from java.lang.Object#time(" + hint + ")";
        StatementSpecRaw raw = EPAdministratorHelper.compileEPL(toCompile, hint, false, null,
                SelectClauseStreamSelectorEnum.ISTREAM_ONLY, statementContext.getEngineImportService(),
                statementContext.getVariableService(),
                statementContext.getEngineURI(), statementContext.getConfigSnapshot(),
                new PatternNodeFactoryImpl(), new ContextManagementServiceImpl(statementContext.getEngineURI()),
                new ExprDeclaredServiceImpl(), new TableServiceImpl());
        ExprNode expr = raw.getStreamSpecs().get(0).getViewSpecs()[0].getObjectParameters().get(0);
        ExprNode validated = EPLValidationUtil.validateSimpleGetSubtree(ExprNodeOrigin.HINT, expr, statementContext, OAEXPRESSIONTYPE, false);
        return validated.getForge();
    }
}
