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
package com.espertech.esper.supportregression.dataflow;

import com.espertech.esper.dataflow.annotations.DataFlowContext;
import com.espertech.esper.dataflow.annotations.DataFlowOperator;
import com.espertech.esper.dataflow.annotations.OutputType;
import com.espertech.esper.dataflow.annotations.OutputTypes;
import com.espertech.esper.dataflow.interfaces.EPDataFlowEmitter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.StringTokenizer;

@DataFlowOperator
@OutputTypes({
        @OutputType(name = "line", type = int.class),
        @OutputType(name = "wordCount", type = int.class),
        @OutputType(name = "charCount", type = int.class)
})
public class MyTokenizerCounter {
    private static final Logger log = LoggerFactory.getLogger(MyTokenizerCounter.class);

    @DataFlowContext
    private EPDataFlowEmitter graphContext;

    public void onInput(String line) {
        StringTokenizer tokenizer = new StringTokenizer(line, " \t");
        int wordCount = tokenizer.countTokens();
        int charCount = 0;
        while (tokenizer.hasMoreTokens()) {
            String token = tokenizer.nextToken();
            charCount += token.length();
        }
        log.debug("Submitting stat words[" + wordCount + "] chars[" + charCount + "] for line '" + line + "'");
        graphContext.submit(new Object[]{1, wordCount, charCount});
    }
}


