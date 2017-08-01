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
package com.espertech.esperio.file;

import com.espertech.esper.client.EPException;
import com.espertech.esper.client.EventType;
import com.espertech.esper.client.PropertyAccessException;
import com.espertech.esper.client.dataflow.EPDataFlowSignalFinalMarker;
import com.espertech.esper.client.dataflow.EPDataFlowSignalWindowMarker;
import com.espertech.esper.client.util.DateTime;
import com.espertech.esper.codegen.model.expression.CodegenExpression;
import com.espertech.esper.core.service.StatementContext;
import com.espertech.esper.dataflow.annotations.DataFlowContext;
import com.espertech.esper.dataflow.annotations.DataFlowOpProvideSignal;
import com.espertech.esper.dataflow.annotations.DataFlowOperator;
import com.espertech.esper.dataflow.interfaces.*;
import com.espertech.esper.event.EventBeanManufactureException;
import com.espertech.esper.event.EventBeanManufacturer;
import com.espertech.esper.event.EventTypeUtility;
import com.espertech.esper.event.WriteablePropertyDescriptor;
import com.espertech.esper.util.CollectionUtil;
import com.espertech.esper.util.SimpleTypeParser;
import com.espertech.esper.util.SimpleTypeParserFactory;
import com.espertech.esperio.csv.AdapterInputSource;
import com.espertech.esperio.csv.CSVReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.EOFException;
import java.util.*;

import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.constant;
import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.staticMethod;

@DataFlowOperator
@DataFlowOpProvideSignal
public class FileSourceCSV implements DataFlowSourceOperator {

    private static final Logger log = LoggerFactory.getLogger(FileSourceCSV.class);

    private final AdapterInputSource adapterInputSource;
    private final boolean hasHeaderLine;
    private final boolean hasTitleLine;
    private final Integer numLoops;
    private final String[] propertyNames;
    private final String dateFormat;

    private StatementContext statementContext;
    private EventType outputEventType;
    private ParseMakePropertiesDesc parseMake;

    private boolean firstRow = true;
    private int loopCount;

    @DataFlowContext
    protected EPDataFlowEmitter graphContext;

    private CSVReader reader;

    public FileSourceCSV(AdapterInputSource adapterInputSource, boolean hasHeaderLine, boolean hasTitleLine, Integer numLoops, String[] propertyNames, String dateFormat) {
        this.adapterInputSource = adapterInputSource;
        this.hasHeaderLine = hasHeaderLine;
        this.hasTitleLine = hasTitleLine;
        this.numLoops = numLoops;
        this.propertyNames = propertyNames;
        this.dateFormat = dateFormat;
    }

    public DataFlowOpInitializeResult initialize(DataFlowOpInitializateContext context) throws Exception {
        statementContext = context.getStatementContext();
        outputEventType = context.getOutputPorts().get(0).getOptionalDeclaredType() != null ? context.getOutputPorts().get(0).getOptionalDeclaredType().getEventType() : null;
        if (outputEventType == null) {
            throw new IllegalArgumentException("No event type provided for output, please provide an event type name");
        }

        // use event type's full list of properties
        if (!hasTitleLine) {
            if (propertyNames != null) {
                parseMake = setupProperties(false, propertyNames, outputEventType, context.getStatementContext(), dateFormat);
            } else {
                parseMake = setupProperties(false, outputEventType.getPropertyNames(), outputEventType, context.getStatementContext(), dateFormat);
            }
        }

        return null;
    }

    public void open(DataFlowOpOpenContext openContext) {
        reader = new CSVReader(adapterInputSource);
    }

    public void next() {
        try {
            String[] nextRecord = reader.getNextRecord();

            if (firstRow) {
                // determine the parsers from the title line
                if (hasTitleLine && parseMake == null) {
                    parseMake = setupProperties(true, nextRecord, outputEventType, statementContext, dateFormat);
                }

                if (hasTitleLine || hasHeaderLine) {
                    nextRecord = reader.getNextRecord();
                }
            }

            int[] propertyIndexes = parseMake.getIndexes();
            Object[] tuple = new Object[propertyIndexes.length];
            for (int i = 0; i < propertyIndexes.length; i++) {
                tuple[i] = parseMake.getParsers()[i].parse(nextRecord[propertyIndexes[i]]);
            }
            Object underlying = parseMake.getEventBeanManufacturer().makeUnderlying(tuple);

            if (underlying instanceof Object[]) {
                graphContext.submit((Object[]) underlying);
            } else {
                graphContext.submit(underlying);
            }
            firstRow = false;
        } catch (EOFException e) {
            if (numLoops != null) {
                loopCount++;
                if (loopCount >= numLoops) {
                    graphContext.submitSignal(new EPDataFlowSignalFinalMarker() {
                    });
                } else {
                    // reset
                    graphContext.submitSignal(new EPDataFlowSignalWindowMarker() {
                    });
                    firstRow = true;
                    if (reader.isResettable()) {
                        reader.reset();
                    } else {
                        reader = new CSVReader(adapterInputSource);
                    }
                }
            } else {
                graphContext.submitSignal(new EPDataFlowSignalFinalMarker() {
                });
            }
        }
    }

    public void close(DataFlowOpCloseContext openContext) {
        if (reader != null) {
            reader.close();
            reader = null;
        }
        if (adapterInputSource != null) {
            adapterInputSource.close();
        }
    }

    private static ParseMakePropertiesDesc setupProperties(boolean requireOneMatch, String[] propertyNamesOffered, EventType outputEventType, StatementContext statementContext, final String dateFormat) {
        Set<WriteablePropertyDescriptor> writeables = statementContext.getEventAdapterService().getWriteableProperties(outputEventType, false);

        List<Integer> indexesList = new ArrayList<Integer>();
        List<SimpleTypeParser> parserList = new ArrayList<SimpleTypeParser>();
        List<WriteablePropertyDescriptor> writablesList = new ArrayList<WriteablePropertyDescriptor>();

        for (int i = 0; i < propertyNamesOffered.length; i++) {
            String propertyName = propertyNamesOffered[i];
            Class propertyType;
            try {
                propertyType = outputEventType.getPropertyType(propertyName);
            } catch (PropertyAccessException ex) {
                throw new EPException("Invalid property name '" + propertyName + "': " + ex.getMessage(), ex);
            }
            if (propertyType == null) {
                continue;
            }
            SimpleTypeParser parser;
            if (propertyType == Date.class || propertyType == Calendar.class) {
                final String df = dateFormat != null ? dateFormat : DateTime.DEFAULT_XMLLIKE_DATE_FORMAT;
                if (propertyType == Date.class) {
                    parser = new SimpleTypeParser() {
                        public Object parse(String text) {
                            return DateTime.toDate(text, df);
                        }

                        public CodegenExpression codegen(CodegenExpression input) {
                            return staticMethod(DateTime.class, "toDate", input, constant(df));
                        }
                    };
                } else {
                    parser = new SimpleTypeParser() {
                        public Object parse(String text) {
                            return DateTime.toCalendar(text, df);
                        }

                        public CodegenExpression codegen(CodegenExpression input) {
                            return staticMethod(DateTime.class, "toCalendar", input, constant(df));
                        }
                    };
                }
            } else {
                parser = SimpleTypeParserFactory.getParser(propertyType);
            }

            WriteablePropertyDescriptor writable = EventTypeUtility.findWritable(propertyName, writeables);
            if (writable == null) {
                continue;
            }
            indexesList.add(i);
            parserList.add(parser);
            writablesList.add(writable);
        }

        if (indexesList.isEmpty() && requireOneMatch) {
            throw new EPException("Failed to match any of the properties " + Arrays.toString(propertyNamesOffered) + " to the event type properties of event type '" + outputEventType.getName() + "'");
        }

        SimpleTypeParser[] parsers = parserList.toArray(new SimpleTypeParser[parserList.size()]);
        WriteablePropertyDescriptor[] writables = writablesList.toArray(new WriteablePropertyDescriptor[parserList.size()]);
        int[] indexes = CollectionUtil.intArray(indexesList);
        EventBeanManufacturer manufacturer;
        try {
            manufacturer = statementContext.getEventAdapterService().getManufacturer(outputEventType, writables, statementContext.getEngineImportService(), false);
        } catch (EventBeanManufactureException e) {
            throw new EPException("Event type '" + outputEventType.getName() + "' cannot be written to: " + e.getMessage(), e);
        }
        return new ParseMakePropertiesDesc(indexes, parsers, manufacturer);
    }

    private static class ParseMakePropertiesDesc {
        private final int[] indexes;
        private final SimpleTypeParser[] parsers;
        private final EventBeanManufacturer eventBeanManufacturer;

        private ParseMakePropertiesDesc(int[] indexes, SimpleTypeParser[] parsers, EventBeanManufacturer eventBeanManufacturer) {
            this.indexes = indexes;
            this.parsers = parsers;
            this.eventBeanManufacturer = eventBeanManufacturer;
        }

        public int[] getIndexes() {
            return indexes;
        }

        public SimpleTypeParser[] getParsers() {
            return parsers;
        }

        public EventBeanManufacturer getEventBeanManufacturer() {
            return eventBeanManufacturer;
        }
    }
}
