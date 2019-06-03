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
package com.espertech.esperio.socket.core;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.internal.event.core.*;
import com.espertech.esper.common.internal.util.SimpleTypeParser;
import com.espertech.esper.common.internal.util.SimpleTypeParserFactory;
import com.espertech.esper.runtime.internal.kernel.service.EPRuntimeSPI;
import com.espertech.esperio.socket.config.DataType;
import com.espertech.esperio.socket.config.SocketConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.Socket;
import java.util.*;

public class WorkerThread extends Thread {

    private final static Logger log = LoggerFactory.getLogger(WorkerThread.class);

    private final EPRuntimeSPI runtime;
    private final EsperSocketServiceRunnable runnable;
    private final String serviceName;
    private final Socket socket;
    private final Map<String, WriterCacheEntry> streamCache = new HashMap<String, WriterCacheEntry>();
    private final SocketConfig socketConfig;

    private ObjectInputStream ois;
    private BufferedReader br;
    private boolean isShutdown;

    public WorkerThread(String serviceName, EPRuntimeSPI runtime, EsperSocketServiceRunnable runnable, Socket socket, SocketConfig socketConfig) throws IOException {
        this.serviceName = serviceName;
        this.runtime = runtime;
        this.runnable = runnable;
        this.socket = socket;
        this.socketConfig = socketConfig;

        if (socketConfig.getDataType() == DataType.PROPERTY_ORDERED_CSV) {
            if (socketConfig.getStream() == null || socketConfig.getStream().length() == 0) {
                throw new IllegalArgumentException("Invalid null or empty value provided for required 'stream' parameter");
            }
            if (socketConfig.getPropertyOrder() == null || socketConfig.getPropertyOrder().length() == 0) {
                throw new IllegalArgumentException("Invalid null or empty value provided for required 'propertyOrder' parameter");
            }
        }

        if ((socketConfig.getDataType() == null) || (socketConfig.getDataType() == DataType.OBJECT)) {
            ois = new ObjectInputStream(socket.getInputStream());
        } else {
            br = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        }
    }

    public void setShutdown(boolean shutdown) {
        isShutdown = shutdown;
    }

    public void run() {

        try {
            while (!Thread.interrupted() && socket.isConnected()) {

                if (ois != null) {
                    Object object = ois.readObject();
                    handleObject(object);
                } else {
                    String str = br.readLine();

                    if (str != null) {
                        handleString(str);
                    } else {
                        break;
                    }
                }
            }
        } catch (EOFException ex) {
            log.debug("EOF received from connection");
        } catch (IOException ex) {
            if (!isShutdown) {
                log.error("I/O error: " + ex.getMessage(), ex);
            }
        } catch (ClassNotFoundException ex) {
            log.error("Class not found: " + ex.getMessage());
        } finally {
            try {
                socket.close();
                runnable.remove(this);
            } catch (IOException ignore) {
            }
        }
    }

    private void handleObject(Object input) {
        try {
            if (input instanceof Map) {
                Map map = (Map) input;
                String type = (String) map.get("stream");
                if (type == null) {
                    log.warn("Expected value for event type not found in map event provided to adapter");
                    return;
                }
                runtime.getEventService().sendEventMap(map, type);
            } else {
                runtime.getEventService().sendEventBean(input, input.getClass().getSimpleName());
            }
        } catch (Throwable t) {
            log.error("Unexpected exception encountered sending event " + input + " service '" + serviceName + "' :" + t.getMessage(), t);
        }
    }

    private void handleString(String input) {
        if (input == null) {
            return;
        }
        if (socketConfig.getDataType() != DataType.JSON) {
            handleCSV(input);
        } else {
            handleJSON(input);
        }
    }

    private void handleCSV(String input) {
        try {
            Map<String, String> parameters = new HashMap<String, String>();
            WStringTokenizer tokenizer = new WStringTokenizer(input, ",");
            String eventTypeName;

            if (socketConfig.getDataType() != DataType.PROPERTY_ORDERED_CSV) {
                while (tokenizer.hasMoreTokens()) {
                    String item = tokenizer.nextToken();
                    int index = item.indexOf("=");
                    if (index != -1) {
                        String value = item.substring(index + 1, item.length());
                        String unescaped = socketConfig.isUnescape() ? UnescapeUtil.unescapeJavaString(value) : value;
                        parameters.put(item.substring(0, index), unescaped);
                    }
                }
                eventTypeName = parameters.get("stream");
            } else {
                // handle property-ordered-csv
                int idx = -1;
                String[] propertyOrder = socketConfig.getPropertyOrder().split(",");
                while (tokenizer.hasMoreTokens()) {
                    idx++;
                    String value = tokenizer.nextToken();
                    String unescaped = socketConfig.isUnescape() ? UnescapeUtil.unescapeJavaString(value) : value;
                    if (idx < propertyOrder.length) {
                        parameters.put(propertyOrder[idx].trim(), unescaped);
                    }
                }
                eventTypeName = socketConfig.getStream();
            }

            WriterCacheEntry cacheEntry = streamCache.get(eventTypeName);
            if (cacheEntry == null) {
                cacheEntry = makeCacheEntry(eventTypeName);
                streamCache.put(eventTypeName, cacheEntry);
            }

            if (cacheEntry == null) {
                return;
            }

            Object[] values = new Object[cacheEntry.getParsers().length];
            for (int i = 0; i < cacheEntry.getParsers().length; i++) {
                String value = parameters.get(cacheEntry.getWritableProperties()[i].getPropertyName());
                if (value == null) {
                    continue;
                }
                values[i] = cacheEntry.getParsers()[i].parse(value);
            }

            EventBean theEvent = cacheEntry.getEventBeanManufacturer().make(values);
            runtime.getEventServiceSPI().processWrappedEvent(theEvent);
        } catch (Throwable t) {
            log.error("Unexpected exception encountered sending event " + input + " service '" + serviceName + "' :" + t.getMessage(), t);
        }
    }

    private void handleJSON(String input) {
        try {
            final String prefix = "stream=";
            if (!input.startsWith(prefix)) {
                log.error("Message does not start with '" + prefix + "=', message: " + input);
                return;
            }

            final String jsonDelim = ",json=";
            int indexStart = input.indexOf(jsonDelim);
            if (indexStart == -1) {
                log.error("Failed to find '" + jsonDelim + "' in message: " + input);
                return;
            }

            String eventTypeName = input.substring(prefix.length(), indexStart);
            String json = input.substring(indexStart + jsonDelim.length());
            runtime.getEventServiceSPI().sendEventJson(json, eventTypeName);
        } catch (Throwable t) {
            log.error("Unexpected exception encountered sending event " + input + " service '" + serviceName + "' :" + t.getMessage(), t);
        }
    }

    private WriterCacheEntry makeCacheEntry(String eventTypeName) {

        EventType eventType = runtime.getServicesContext().getEventTypeRepositoryBus().getNameToTypeMap().get(eventTypeName);
        if (eventType == null) {
            log.info("Event type by name '" + eventTypeName + "' not found.");
            return null;
        }

        if (!(eventType instanceof EventTypeSPI)) {
            log.info("Event type by name '" + eventTypeName + "' is not writable.");
            return null;
        }

        EventTypeSPI eventTypeSPI = (EventTypeSPI) eventType;

        Set<WriteablePropertyDescriptor> writablesSet = EventTypeUtility.getWriteableProperties(eventTypeSPI, false, false);
        List<WriteablePropertyDescriptor> writablePropertiesList = new ArrayList<WriteablePropertyDescriptor>();
        List<SimpleTypeParser> parserList = new ArrayList<SimpleTypeParser>();

        for (WriteablePropertyDescriptor writableDesc : writablesSet) {
            SimpleTypeParser parser = SimpleTypeParserFactory.getParser(writableDesc.getType());
            if (parser == null) {
                log.debug("No parser found for type '" + writableDesc.getType() + "'");
                continue;
            }

            writablePropertiesList.add(writableDesc);
            parserList.add(parser);
        }

        WriteablePropertyDescriptor[] writableProperties = writablePropertiesList.toArray(new WriteablePropertyDescriptor[writablePropertiesList.size()]);
        SimpleTypeParser[] parsers = parserList.toArray(new SimpleTypeParser[parserList.size()]);

        EventBeanManufacturer eventBeanManufacturer;
        try {
            eventBeanManufacturer = EventTypeUtility.getManufacturer(eventType, writableProperties, runtime.getServicesContext().getClasspathImportServiceRuntime(), false, runtime.getServicesContext().getEventTypeAvroHandler()).getManufacturer(runtime.getServicesContext().getEventBeanTypedEventFactory());
        } catch (EventBeanManufactureException e) {
            log.info("Unable to create manufacturer for event type: " + e.getMessage(), e);
            return null;
        }

        return new WriterCacheEntry(eventBeanManufacturer, writableProperties, parsers);
    }
}
