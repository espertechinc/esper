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

import com.espertech.esper.common.client.EPException;
import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.client.PropertyAccessException;
import com.espertech.esper.common.client.dataflow.annotations.DataFlowContext;
import com.espertech.esper.common.client.dataflow.util.EPDataFlowSignalFinalMarker;
import com.espertech.esper.common.client.dataflow.util.EPDataFlowSignalWindowMarker;
import com.espertech.esper.common.client.util.DateTime;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.context.util.StatementContext;
import com.espertech.esper.common.internal.epl.dataflow.interfaces.*;
import com.espertech.esper.common.internal.event.core.EventBeanManufactureException;
import com.espertech.esper.common.internal.event.core.EventBeanManufacturer;
import com.espertech.esper.common.internal.event.core.EventTypeUtility;
import com.espertech.esper.common.internal.event.core.WriteablePropertyDescriptor;
import com.espertech.esper.common.internal.util.CollectionUtil;
import com.espertech.esper.common.internal.util.SimpleTypeParser;
import com.espertech.esper.common.internal.util.SimpleTypeParserFactory;
import com.espertech.esperio.csv.AdapterInputSource;
import com.espertech.esperio.csv.CSVReader;

import java.io.EOFException;
import java.util.*;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.constant;
import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.staticMethod;

public class FileSourceCSV implements DataFlowSourceOperator {

    private final FileSourceFactory factory;
    private final AdapterInputSource adapterInputSource;
    private final boolean hasHeaderLine;
    private final boolean hasTitleLine;
    private final Integer numLoops;
    private final String dateFormat;

    private StatementContext statementContext;
    private ParseMakePropertiesDesc parseMake;

    private boolean firstRow = true;
    private int loopCount;

    @DataFlowContext
    protected EPDataFlowEmitter graphContext;

    private CSVReader reader;

    public FileSourceCSV(FileSourceFactory factory, DataFlowOpInitializeContext context, AdapterInputSource adapterInputSource, boolean hasHeaderLine, boolean hasTitleLine, Integer numLoops, String[] propertyNames, String dateFormat) {
        this.factory = factory;
        this.adapterInputSource = adapterInputSource;
        this.hasHeaderLine = hasHeaderLine;
        this.hasTitleLine = hasTitleLine;
        this.numLoops = numLoops;
        this.dateFormat = dateFormat;

        statementContext = context.getAgentInstanceContext().getStatementContext();

        // use event type's full list of properties
        if (!hasTitleLine) {
            if (propertyNames != null) {
                parseMake = setupProperties(false, propertyNames, factory.getOutputEventType(), statementContext, dateFormat);
            } else {
                parseMake = setupProperties(false, factory.getOutputEventType().getPropertyNames(), factory.getOutputEventType(), statementContext, dateFormat);
            }
        }
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
                    parseMake = setupProperties(true, nextRecord, factory.getOutputEventType(), statementContext, dateFormat);
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
        Set<WriteablePropertyDescriptor> writeables = EventTypeUtility.getWriteableProperties(outputEventType, false, false);

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
            manufacturer = EventTypeUtility.getManufacturer(outputEventType, writables, statementContext.getClasspathImportServiceRuntime(), false, statementContext.getEventTypeAvroHandler()).getManufacturer(statementContext.getEventBeanTypedEventFactory());
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
