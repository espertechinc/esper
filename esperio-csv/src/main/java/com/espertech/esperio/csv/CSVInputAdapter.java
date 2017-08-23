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
package com.espertech.esperio.csv;

import com.espertech.esper.adapter.AdapterState;
import com.espertech.esper.adapter.InputAdapter;
import com.espertech.esper.client.EPException;
import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EventType;
import com.espertech.esper.client.PropertyAccessException;
import com.espertech.esper.core.service.EPServiceProviderSPI;
import com.espertech.esper.epl.core.engineimport.EngineImportService;
import com.espertech.esper.event.EventAdapterService;
import com.espertech.esper.event.map.MapEventType;
import com.espertech.esper.util.ExecutionPathDebugLog;
import com.espertech.esper.util.JavaClassHelper;
import net.sf.cglib.core.ReflectUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.beans.PropertyDescriptor;
import java.io.EOFException;
import java.util.*;

/**
 * An event Adapter that uses a CSV file for a source.
 */
public class CSVInputAdapter extends AbstractCoordinatedAdapter implements InputAdapter {
    private static final Logger log = LoggerFactory.getLogger(CSVInputAdapter.class);

    private Integer eventsPerSec;
    private CSVReader reader;
    private AbstractTypeCoercer coercer = new BasicTypeCoercer();
    private String[] propertyOrder;
    private CSVInputAdapterSpec adapterSpec;
    private Map<String, Object> propertyTypes;
    private String eventTypeName;
    private long lastTimestamp = 0;
    private long totalDelay;
    boolean atEOF = false;
    private String[] firstRow;
    private Class beanClass;
    private int rowCount = 0;

    /**
     * Ctor.
     *
     * @param epService - provides the engine runtime and services
     * @param spec      - the parameters for this adapter
     */
    public CSVInputAdapter(EPServiceProvider epService, CSVInputAdapterSpec spec) {
        super(epService, spec.isUsingEngineThread(), spec.isUsingExternalTimer(), spec.isUsingTimeSpanEvents());

        adapterSpec = spec;
        eventTypeName = adapterSpec.geteventTypeName();
        eventsPerSec = spec.getEventsPerSec();

        if (epService != null) {
            finishInitialization(epService, spec);
        }
    }

    /**
     * Ctor.
     *
     * @param epService          - provides the engine runtime and services
     * @param adapterInputSource - the source of the CSV file
     * @param eventTypeName      - the type name of the Map event to create from the CSV data
     */
    public CSVInputAdapter(EPServiceProvider epService, AdapterInputSource adapterInputSource, String eventTypeName) {
        this(epService, new CSVInputAdapterSpec(adapterInputSource, eventTypeName));
    }

    /**
     * Ctor for adapters that will be passed to an AdapterCoordinator.
     *
     * @param adapterSpec contains parameters that specify the behavior of the input adapter
     */
    public CSVInputAdapter(CSVInputAdapterSpec adapterSpec) {
        this(null, adapterSpec);
    }

    /**
     * Ctor for adapters that will be passed to an AdapterCoordinator.
     *
     * @param adapterInputSource - the parameters for this adapter
     * @param eventTypeName      - the event type name that the input adapter generates events for
     */
    public CSVInputAdapter(AdapterInputSource adapterInputSource, String eventTypeName) {
        this(null, adapterInputSource, eventTypeName);
    }


    /* (non-Javadoc)
     * @see com.espertech.esperio.ReadableAdapter#read()
     */
    public SendableEvent read() throws EPException {
        if (stateManager.getState() == AdapterState.DESTROYED || atEOF) {
            return null;
        }

        try {
            if (eventsToSend.isEmpty()) {
                if (beanClass != null) {
                    return new SendableBeanEvent(newMapEvent(), beanClass, eventTypeName, totalDelay, scheduleSlot);
                } else {
                    return new SendableMapEvent(newMapEvent(), eventTypeName, totalDelay, scheduleSlot);
                }
            } else {
                SendableEvent theEvent = eventsToSend.first();
                eventsToSend.remove(theEvent);
                return theEvent;
            }
        } catch (EOFException e) {
            if ((ExecutionPathDebugLog.isDebugEnabled) && (log.isDebugEnabled())) {
                log.debug(".read reached end of CSV file");
            }
            atEOF = true;
            if (stateManager.getState() == AdapterState.STARTED) {
                stop();
            } else {
                destroy();
            }
            return null;
        }
    }


    /* (non-Javadoc)
     * @see com.espertech.esperio.csv.AbstractCoordinatedAdapter#setEPService(com.espertech.esper.client.EPServiceProvider)
     */
    @Override
    public void setEPService(EPServiceProvider epService) {
        super.setEPService(epService);
        finishInitialization(epService, adapterSpec);
    }

    /**
     * Sets the coercing provider.
     *
     * @param coercer to use for coercing
     */
    public void setCoercer(AbstractTypeCoercer coercer) {
        this.coercer = coercer;
    }

    /**
     * Close the CSVReader.
     */
    protected void close() {
        reader.close();
    }

    /**
     * Remove the first member of eventsToSend. If there is
     * another record in the CSV file, insert the event created
     * from it into eventsToSend.
     */
    protected void replaceFirstEventToSend() {
        eventsToSend.remove(eventsToSend.first());
        SendableEvent theEvent = read();
        if (theEvent != null) {
            eventsToSend.add(theEvent);
        }
    }

    /**
     * Reset all the changeable state of this ReadableAdapter, as if it were just created.
     */
    protected void reset() {
        lastTimestamp = 0;
        totalDelay = 0;
        atEOF = false;
        if (reader.isResettable()) {
            reader.reset();
        }
    }

    private void finishInitialization(EPServiceProvider epService, CSVInputAdapterSpec spec) {
        assertValidParameters(epService, spec);

        EPServiceProviderSPI spi = (EPServiceProviderSPI) epService;

        scheduleSlot = spi.getSchedulingMgmtService().allocateBucket().allocateSlot();

        reader = new CSVReader(spec.getAdapterInputSource());
        reader.setLooping(spec.isLooping());

        String[] firstRow = getFirstRow();

        Map<String, Object> givenPropertyTypes = constructPropertyTypes(spec.geteventTypeName(), spec.getPropertyTypes(), spi.getEventAdapterService());

        propertyOrder = spec.getPropertyOrder() != null ?
                spec.getPropertyOrder() :
                CSVPropertyOrderHelper.resolvePropertyOrder(firstRow, givenPropertyTypes);

        reader.setIsUsingTitleRow(isUsingTitleRow(firstRow, propertyOrder));
        if (!isUsingTitleRow(firstRow, propertyOrder)) {
            this.firstRow = firstRow;
        }

        propertyTypes = resolvePropertyTypes(givenPropertyTypes, spi.getEngineImportService());
        if (givenPropertyTypes == null) {
            spi.getEventAdapterService().addNestableMapType(eventTypeName, new HashMap<String, Object>(propertyTypes), null, true, true, true, false, false);
        }

        coercer.setPropertyTypes(propertyTypes);
    }

    private Map<String, Object> newMapEvent() throws EOFException {
        ++rowCount;
        String[] row = firstRow != null ? firstRow : reader.getNextRecord();
        firstRow = null;
        Map<String, Object> map = createMapFromRow(row);
        updateTotalDelay(map, reader.getAndClearIsReset());
        return map;
    }

    private Map<String, Object> createMapFromRow(String[] row) {
        Map<String, Object> map = new HashMap<String, Object>();

        int count = 0;

        try {
            for (String property : propertyOrder) {
                // Skip properties that are in the title row but not
                // part of the map to send
                if ((propertyTypes != null) &&
                        (!propertyTypes.containsKey(property)) &&
                        (!property.equals(adapterSpec.getTimestampColumn()))) {
                    count++;
                    continue;
                }
                Object value = coercer.coerce(property, row[count++]);
                map.put(property, value);
            }
        } catch (Exception e) {
            throw new EPException(e);
        }
        return map;
    }

    private Map<String, Object> constructPropertyTypes(String eventTypeName, Map<String, Object> propertyTypesGiven, EventAdapterService eventAdapterService) {
        Map<String, Object> propertyTypes = new HashMap<String, Object>();
        EventType eventType = eventAdapterService.getExistsTypeByName(eventTypeName);
        if (eventType == null) {
            if (propertyTypesGiven != null) {
                eventAdapterService.addNestableMapType(eventTypeName, new HashMap<String, Object>(propertyTypesGiven), null, true, true, true, false, false);
            }
            return propertyTypesGiven;
        }
        if (!eventType.getUnderlyingType().equals(Map.class)) {
            beanClass = eventType.getUnderlyingType();
        }
        if (propertyTypesGiven != null && eventType.getPropertyNames().length != propertyTypesGiven.size()) {
            // allow this scenario for beans as we may want to bring in a subset of properties
            if (beanClass != null) {
                return propertyTypesGiven;
            } else {
                throw new EPException("Event type " + eventTypeName + " has already been declared with a different number of parameters");
            }
        }
        for (String property : eventType.getPropertyNames()) {
            Class type;
            try {
                type = eventType.getPropertyType(property);
            } catch (PropertyAccessException e) {
                // thrown if trying to access an invalid property on an EventBean
                throw new EPException(e);
            }
            if (propertyTypesGiven != null && propertyTypesGiven.get(property) == null) {
                throw new EPException("Event type " + eventTypeName + "has already been declared with different parameters");
            }
            if (propertyTypesGiven != null && !propertyTypesGiven.get(property).equals(type)) {
                throw new EPException("Event type " + eventTypeName + "has already been declared with a different type for property " + property);
            }
            // we can't set read-only properties for bean
            if (!eventType.getUnderlyingType().equals(Map.class)) {
                PropertyDescriptor[] pds = ReflectUtils.getBeanProperties(beanClass);
                PropertyDescriptor pd = null;
                for (PropertyDescriptor p : pds) {
                    if (p.getName().equals(property))
                        pd = p;
                }
                if (pd == null) {
                    continue;
                }
                if (pd.getWriteMethod() == null) {
                    if (propertyTypesGiven == null) {
                        continue;
                    } else {
                        throw new EPException("Event type " + eventTypeName + "property " + property + " is read only");
                    }
                }
            }
            propertyTypes.put(property, type);
        }

        // flatten nested types
        Map<String, Object> flattenPropertyTypes = new HashMap<String, Object>();
        for (String p : propertyTypes.keySet()) {
            Object type = propertyTypes.get(p);
            if (type instanceof Class && ((Class) type).getName().equals("java.util.Map") && eventType instanceof MapEventType) {
                MapEventType mapEventType = (MapEventType) eventType;
                Map<String, Object> nested = (Map) mapEventType.getTypes().get(p);
                for (String nestedProperty : nested.keySet()) {
                    flattenPropertyTypes.put(p + "." + nestedProperty, nested.get(nestedProperty));
                }
            } else if (type instanceof Class) {
                Class c = (Class) type;
                if (!c.isPrimitive() && !c.getName().startsWith("java")) {
                    PropertyDescriptor[] pds = ReflectUtils.getBeanProperties(c);
                    for (PropertyDescriptor pd : pds) {
                        if (pd.getWriteMethod() != null)
                            flattenPropertyTypes.put(p + "." + pd.getName(), pd.getPropertyType());
                    }
                } else {
                    flattenPropertyTypes.put(p, type);
                }
            } else {
                flattenPropertyTypes.put(p, type);
            }
        }
        return flattenPropertyTypes;
    }

    private void updateTotalDelay(Map<String, Object> map, boolean isFirstRow) {
        if (eventsPerSec != null) {
            int msecPerEvent = 1000 / eventsPerSec;
            totalDelay += msecPerEvent;
        } else if (adapterSpec.getTimestampColumn() != null) {
            Long timestamp = resolveTimestamp(map);
            if (timestamp == null) {
                throw new EPException("Couldn't resolve the timestamp for record " + map);
            } else if (timestamp < 0) {
                throw new EPException("Encountered negative timestamp for CSV record : " + map);
            } else {
                long timestampDifference = 0;
                if (timestamp < lastTimestamp) {
                    if (!isFirstRow) {
                        throw new EPException("Subsequent timestamp " + timestamp + " is smaller than previous timestamp " + lastTimestamp);
                    } else {
                        timestampDifference = timestamp;
                    }
                } else {
                    timestampDifference = timestamp - lastTimestamp;
                }
                lastTimestamp = timestamp;
                totalDelay += timestampDifference;
            }
        }
    }

    private Long resolveTimestamp(Map<String, Object> map) {
        if (adapterSpec.getTimestampColumn() != null) {
            Object value = map.get(adapterSpec.getTimestampColumn());
            return Long.parseLong(value.toString());
        } else {
            return null;
        }
    }

    private Map<String, Object> resolvePropertyTypes(Map<String, Object> propertyTypes, EngineImportService engineImportService) {
        if (propertyTypes != null) {
            return propertyTypes;
        }

        Map<String, Object> result = new HashMap<String, Object>();
        for (int i = 0; i < propertyOrder.length; i++) {
            String name = propertyOrder[i];
            Class type = String.class;
            if (name.contains(" ")) {
                String[] typeAndName = name.split("\\s");
                try {
                    name = typeAndName[1];
                    type = JavaClassHelper.getClassForName(JavaClassHelper.getBoxedClassName(typeAndName[0]), engineImportService.getClassForNameProvider());
                    propertyOrder[i] = name;
                } catch (Throwable e) {
                    log.warn("Unable to use given type for property, will default to String: " + propertyOrder[i], e);
                }
            }
            result.put(name, type);
        }
        return result;
    }

    private boolean isUsingTitleRow(String[] firstRow, String[] propertyOrder) {
        if (firstRow == null) {
            return false;
        }
        Set<String> firstRowSet = new HashSet<String>(Arrays.asList(firstRow));
        Set<String> propertyOrderSet = new HashSet<String>(Arrays.asList(propertyOrder));
        return firstRowSet.equals(propertyOrderSet);
    }

    private String[] getFirstRow() {
        String[] firstRow;
        try {
            firstRow = reader.getNextRecord();
        } catch (EOFException e) {
            atEOF = true;
            firstRow = null;
        }
        return firstRow;
    }

    private void assertValidEventsPerSec(Integer eventsPerSec) {
        if (eventsPerSec != null) {
            if (eventsPerSec < 1 || eventsPerSec > 1000) {
                throw new IllegalArgumentException("Illegal value of eventsPerSec:" + eventsPerSec);
            }
        }
    }

    private void assertValidParameters(EPServiceProvider epService, CSVInputAdapterSpec adapterSpec) {
        if (!(epService instanceof EPServiceProviderSPI)) {
            throw new IllegalArgumentException("Invalid type of EPServiceProvider");
        }

        if (adapterSpec.geteventTypeName() == null) {
            throw new NullPointerException("eventTypeName cannot be null");
        }

        if (adapterSpec.getAdapterInputSource() == null) {
            throw new NullPointerException("adapterInputSource cannot be null");
        }

        assertValidEventsPerSec(adapterSpec.getEventsPerSec());

        if (adapterSpec.isLooping() && !adapterSpec.getAdapterInputSource().isResettable()) {
            throw new EPException("Cannot loop on a non-resettable input source");
        }
    }

    /**
     * Returns row count.
     *
     * @return row count
     */
    public int getRowCount() {
        return rowCount;
    }
}
