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

import com.espertech.esper.type.OuterJoinType;

import java.io.Serializable;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * The from-clause names the streams to select upon.
 * <p>
 * The most common projected stream is a filter-based stream which is created by {@link FilterStream}.
 * <p>
 * Multiple streams can be joined by adding each stream individually.
 * <p>
 * Outer joins are also handled by this class. To create an outer join consisting of 2 streams,
 * add one {@link OuterJoinQualifier} that defines the outer join relationship between the 2 streams. The outer joins between
 * N streams, add N-1 {@link OuterJoinQualifier} qualifiers.
 */
public class FromClause implements Serializable {
    private static final long serialVersionUID = 0L;

    private List<Stream> streams;
    private List<OuterJoinQualifier> outerJoinQualifiers;

    /**
     * Ctor.
     */
    public FromClause() {
        streams = new ArrayList<Stream>();
        outerJoinQualifiers = new ArrayList<OuterJoinQualifier>();
    }

    /**
     * Creates an empty from-clause to which one adds streams via the add methods.
     *
     * @return empty from clause
     */
    public static FromClause create() {
        return new FromClause();
    }

    /**
     * Creates a from-clause that lists 2 projected streams joined via outer join.
     *
     * @param stream             first stream in outer join
     * @param outerJoinQualifier qualifies the outer join
     * @param streamSecond       second stream in outer join
     * @return from clause
     */
    public static FromClause create(Stream stream, OuterJoinQualifier outerJoinQualifier, Stream streamSecond) {
        return new FromClause(stream, outerJoinQualifier, streamSecond);
    }

    /**
     * Creates a from clause that selects from a single stream.
     * <p>
     * Use {@link FilterStream} to create filter-based streams to add.
     *
     * @param streams is one or more streams to add to the from clause.
     * @return from clause
     */
    public static FromClause create(Stream... streams) {
        return new FromClause(streams);
    }

    /**
     * Ctor for an outer join between two streams.
     *
     * @param streamOne          first stream in outer join
     * @param outerJoinQualifier type of outer join and fields joined on
     * @param streamTwo          second stream in outer join
     */
    public FromClause(Stream streamOne, OuterJoinQualifier outerJoinQualifier, Stream streamTwo) {
        this(streamOne);
        add(streamTwo);
        outerJoinQualifiers.add(outerJoinQualifier);
    }

    /**
     * Ctor.
     *
     * @param streamsList is zero or more streams in the from-clause.
     */
    public FromClause(Stream... streamsList) {
        streams = new ArrayList<Stream>();
        outerJoinQualifiers = new ArrayList<OuterJoinQualifier>();
        streams.addAll(Arrays.asList(streamsList));
    }

    /**
     * Adds a stream.
     * <p>
     * Use {@link FilterStream} to add filter-based streams.
     *
     * @param stream to add
     * @return from clause
     */
    public FromClause add(Stream stream) {
        streams.add(stream);
        return this;
    }

    /**
     * Adds an outer join descriptor that defines how the streams are related via outer joins.
     * <p>
     * For joining N streams, add N-1 outer join qualifiers.
     *
     * @param outerJoinQualifier is the type of outer join and the fields in the outer join
     * @return from clause
     */
    public FromClause add(OuterJoinQualifier outerJoinQualifier) {
        outerJoinQualifiers.add(outerJoinQualifier);
        return this;
    }

    /**
     * Returns the list of streams in the from-clause.
     *
     * @return list of streams
     */
    public List<Stream> getStreams() {
        return streams;
    }

    /**
     * Renders the from-clause in textual representation.
     *
     * @param writer    to output to
     * @param formatter for newline-whitespace formatting
     */
    public void toEPL(StringWriter writer, EPStatementFormatter formatter) {
        toEPLOptions(writer, formatter, true);
    }

    /**
     * Renders the from-clause in textual representation.
     *
     * @param writer      to output to
     * @param includeFrom flag whether to add the "from" literal
     * @param formatter   for newline-whitespace formatting
     */
    public void toEPLOptions(StringWriter writer, EPStatementFormatter formatter, boolean includeFrom) {
        String delimiter = "";
        if (includeFrom) {
            formatter.beginFrom(writer);
            writer.write("from");
        }

        if ((outerJoinQualifiers == null) || (outerJoinQualifiers.size() == 0)) {
            boolean first = true;
            for (Stream stream : streams) {
                writer.write(delimiter);
                formatter.beginFromStream(writer, first);
                first = false;
                stream.toEPL(writer, formatter);
                delimiter = ",";
            }
        } else {
            if (outerJoinQualifiers.size() != (streams.size() - 1)) {
                throw new IllegalArgumentException("Number of outer join qualifiers must be one less then the number of streams.");
            }
            boolean first = true;
            for (int i = 0; i < streams.size(); i++) {
                Stream stream = streams.get(i);
                formatter.beginFromStream(writer, first);
                first = false;
                stream.toEPL(writer, formatter);

                if (i > 0) {
                    OuterJoinQualifier qualCond = outerJoinQualifiers.get(i - 1);
                    if (qualCond.getLeft() != null) {
                        writer.write(" on ");
                        qualCond.getLeft().toEPL(writer, ExpressionPrecedenceEnum.MINIMUM);
                        writer.write(" = ");
                        qualCond.getRight().toEPL(writer, ExpressionPrecedenceEnum.MINIMUM);

                        if (qualCond.getAdditionalProperties().size() > 0) {
                            for (PropertyValueExpressionPair pair : qualCond.getAdditionalProperties()) {
                                writer.write(" and ");
                                pair.getLeft().toEPL(writer, ExpressionPrecedenceEnum.MINIMUM);
                                writer.write(" = ");
                                pair.getRight().toEPL(writer, ExpressionPrecedenceEnum.MINIMUM);
                            }
                        }
                    }
                }

                if (i < streams.size() - 1) {
                    OuterJoinQualifier qualType = outerJoinQualifiers.get(i);
                    writer.write(" ");
                    if (qualType.getType() != OuterJoinType.INNER) {
                        writer.write(qualType.getType().getText());
                        writer.write(" outer");
                    } else {
                        writer.write(qualType.getType().getText());
                    }
                    writer.write(" join");
                }
            }
        }
    }

    /**
     * Returns the outer join descriptors, if this is an outer join, or an empty list if
     * none of the streams are outer joined.
     *
     * @return list of outer join qualifiers
     */
    public List<OuterJoinQualifier> getOuterJoinQualifiers() {
        return outerJoinQualifiers;
    }

    /**
     * Set the streams.
     *
     * @param streams to set
     */
    public void setStreams(List<Stream> streams) {
        this.streams = streams;
    }

    /**
     * Set outer joins.
     *
     * @param outerJoinQualifiers to set
     */
    public void setOuterJoinQualifiers(List<OuterJoinQualifier> outerJoinQualifiers) {
        this.outerJoinQualifiers = outerJoinQualifiers;
    }
}
