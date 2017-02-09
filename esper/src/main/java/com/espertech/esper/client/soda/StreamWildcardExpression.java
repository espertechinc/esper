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
package com.espertech.esper.client.soda;

import java.io.StringWriter;

/**
 * Represents "stream.*" in for example "mystream.*"
 */
public class StreamWildcardExpression extends ExpressionBase {
    private static final long serialVersionUID = -1413481344199867067L;

    private String streamName;

    /**
     * Ctor.
     *
     * @param streamName stream name
     */
    public StreamWildcardExpression(String streamName) {
        this.streamName = streamName;
    }

    /**
     * Ctor.
     */
    public StreamWildcardExpression() {
    }

    /**
     * Returns the stream name.
     *
     * @return stream name
     */
    public String getStreamName() {
        return streamName;
    }

    /**
     * Sets the stream name.
     *
     * @param streamName stream name
     */
    public void setStreamName(String streamName) {
        this.streamName = streamName;
    }

    public void toPrecedenceFreeEPL(StringWriter writer) {
        writer.append(streamName);
        writer.append(".*");
    }

    public ExpressionPrecedenceEnum getPrecedence() {
        return ExpressionPrecedenceEnum.UNARY;
    }
}
