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
package com.espertech.esper.common.internal.epl.fafquery.querymethod;

import com.espertech.esper.common.internal.compile.stage1.spec.NamedWindowConsumerStreamSpec;
import com.espertech.esper.common.internal.compile.stage1.spec.StreamSpecCompiled;
import com.espertech.esper.common.internal.compile.stage1.spec.TableQueryStreamSpec;
import com.espertech.esper.common.internal.compile.stage2.StatementSpecCompiled;
import com.espertech.esper.common.internal.epl.expression.core.ExprValidationException;

/**
 * Starts and provides the stop method for EPL statements.
 */
public class FAFQueryMethodHelper {
    protected static void validateFAFQuery(StatementSpecCompiled statementSpec) throws ExprValidationException {
        for (int i = 0; i < statementSpec.getStreamSpecs().length; i++) {
            StreamSpecCompiled streamSpec = statementSpec.getStreamSpecs()[i];
            if (!(streamSpec instanceof NamedWindowConsumerStreamSpec || streamSpec instanceof TableQueryStreamSpec)) {
                throw new ExprValidationException("On-demand queries require tables or named windows and do not allow event streams or patterns");
            }
            if (streamSpec.getViewSpecs().length != 0) {
                throw new ExprValidationException("Views are not a supported feature of on-demand queries");
            }
        }
        if (statementSpec.getRaw().getOutputLimitSpec() != null) {
            throw new ExprValidationException("Output rate limiting is not a supported feature of on-demand queries");
        }
    }
}
