/*
 * *************************************************************************************
 *  Copyright (C) 2006-2015 EsperTech, Inc. All rights reserved.                       *
 *  http://www.espertech.com/esper                                                     *
 *  http://www.espertech.com                                                           *
 *  ---------------------------------------------------------------------------------- *
 *  The software in this package is published under the terms of the GPL license       *
 *  a copy of which has been included with this distribution in the license.txt file.  *
 * *************************************************************************************
 */

package com.espertech.esper.regression.epl;

import com.espertech.esper.client.Configuration;
import com.espertech.esper.client.ConfigurationException;
import com.espertech.esper.client.EPPreparedStatement;
import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPServiceProviderManager;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.EPStatementException;
import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.EventType;
import com.espertech.esper.client.UpdateListener;
import com.espertech.esper.client.VariableConstantValueException;
import com.espertech.esper.client.VariableNotFoundException;
import com.espertech.esper.client.VariableValueException;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.client.soda.EPStatementObjectModel;
import com.espertech.esper.client.soda.Expressions;
import com.espertech.esper.client.soda.FilterStream;
import com.espertech.esper.client.soda.FromClause;
import com.espertech.esper.client.soda.OnClause;
import com.espertech.esper.client.soda.SelectClause;
import com.espertech.esper.core.service.EPRuntimeSPI;
import com.espertech.esper.core.service.EPServiceProviderSPI;
import com.espertech.esper.core.service.EPStatementSPI;
import com.espertech.esper.filter.FilterOperator;
import com.espertech.esper.filter.FilterServiceSPI;
import com.espertech.esper.filter.FilterSet;
import com.espertech.esper.filter.FilterValueSet;
import com.espertech.esper.filter.FilterValueSetParam;
import com.espertech.esper.support.bean.SupportBean;
import com.espertech.esper.support.bean.SupportBean_A;
import com.espertech.esper.support.bean.SupportBean_S0;
import com.espertech.esper.support.bean.SupportBean_S1;
import com.espertech.esper.support.bean.SupportEnum;
import com.espertech.esper.support.client.SupportConfigFactory;
import com.espertech.esper.support.event.EventTypeAssertionEnum;
import com.espertech.esper.support.event.EventTypeAssertionUtil;
import junit.framework.TestCase;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.Serializable;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class TestConcatInThreads extends TestCase
{
    private EPServiceProvider epService;
    private SupportUpdateListener listener;

    public void setUp()
    {
        epService = EPServiceProviderManager.getDefaultProvider(SupportConfigFactory.getConfiguration());
        epService.initialize();
        listener = new SupportUpdateListener();
    }

    protected void tearDown() throws Exception {
        listener = null;
    }

    private static final String INPUT = "Input";
    private static final String STAGE = "Stage";
    private static final String TRIGGER = "Trigger";

    private static final String NAME = "name";
    private static final String ID = "id";
    private static final String NAME_VALUE = "JoeBloggs";

    private static Map<String, Object> map(Object... keyAndValues) {
        final Map<String, Object> result = new HashMap<String, Object>(keyAndValues.length / 2);
        for(int i = 0; i < keyAndValues.length; i = i + 2) {
            result.put((String) keyAndValues[i], keyAndValues[i+1]);
        }
        return result;
    }

    private static final class MyListener implements UpdateListener {

        private List<String> badNameNames = new ArrayList<String>();

        @Override
        public void update(final EventBean[] newEvents, final EventBean[] oldEvents) {
            //Check name on new events - this should always contain 'NAME,NAME'
            for(int i = 0; i < newEvents.length; i++) {
                final String nameName = (String) newEvents[i].get(NAME);
                if(!(NAME_VALUE + "," + NAME_VALUE).equals(nameName)) {
                    synchronized (badNameNames) {
                        badNameNames.add(nameName);
                    }
                }
            }
        }
    }

    private static final class InputPush implements Callable<Object> {

        private final EPServiceProvider epService;

        public InputPush(final EPServiceProvider epService) {
            this.epService = epService;
        }

        @Override
        public Object call() throws Exception {
            epService.getEPRuntime().sendEvent(map(NAME,  NAME_VALUE), INPUT);
            return null;
        }
    }

    private static final class TriggerOutput implements Callable<Object> {

        private final EPServiceProvider epService;
        private final int id;

        public TriggerOutput(final EPServiceProvider epService, final int id) {
            this.epService = epService;
            this.id = id;
        }

        @Override
        public Object call() throws Exception {
            epService.getEPRuntime().sendEvent(map(ID, id), TRIGGER);
            return null;
        }
    }

    private static final String first10String(final List<String> strings) {
        final StringBuilder sb = new StringBuilder(1000);
        sb.append("First <=10 bad names:\n");
        for(int i = 0; i < Math.min(10, strings.size()); i++) {
            sb.append("  " + strings.get(i) + "\n");
        }
        return sb.toString();
    }

    public void testMultiThreads() throws Exception {
        epService.getEPAdministrator().getConfiguration().addEventType(INPUT, map(NAME, String.class));
        epService.getEPAdministrator().getConfiguration().addEventType(STAGE, map(ID, int.class, NAME, String.class));
        epService.getEPAdministrator().getConfiguration().addEventType(TRIGGER, map(ID, int.class));
        epService.getEPAdministrator().createEPL("create context PartitionOnId coalesce\n" +
                        "hash_code(id) from Stage,\n" +
                        "hash_code(id) from Trigger\n" +
                        "granularity 2");
        epService.getEPAdministrator().createEPL("context PartitionOnId\n" +
                        "create window StageWin.std:unique(id) as select id, name from " + STAGE);
        epService.getEPAdministrator().createEPL("insert into Stage select 1 as id, name from " + INPUT);
        epService.getEPAdministrator().createEPL("insert into Stage select 2 as id, name from " + INPUT);
        epService.getEPAdministrator().createEPL("context PartitionOnId\n" +
                        "create window PartitionedStage.std:unique(id) as " + STAGE);
        epService.getEPAdministrator().createEPL("context PartitionOnId\n" +
                        "insert into PartitionedStage select id, name from " + STAGE);
        final EPStatement statement = epService.getEPAdministrator().createEPL("context PartitionOnId\n" +
                        "select ps.id, ps.name || ',' || ps.name as name\n" +
                        "from " + TRIGGER + " trig unidirectional\n" +
                        "join PartitionedStage ps on ps.id = trig.id\n" +
                        "output every 1 events");
        final MyListener listener = new MyListener();
        statement.addListener(listener);

        final ExecutorService service = Executors.newFixedThreadPool(4);
        try {
            final InputPush push = new InputPush(epService);
            final List<TriggerOutput> triggers = new ArrayList<TriggerOutput>(2);
            triggers.add(new TriggerOutput(epService, 1));
            triggers.add(new TriggerOutput(epService, 2));

            //repeatedly push an event and then release it
            for(int i = 0; i < 1000; i++) {
                service.submit(push).get(); //get to wait for it

                final List<Future<Object>> ts = service.invokeAll(triggers);
                for(final Future<Object> t : ts) {
                    t.get(); //wait for completion
                }
            }
        } finally {
            service.shutdownNow();
        }

        //Final assertion - we should have no badNameNames
        assertEquals(first10String(listener.badNameNames), 0, listener.badNameNames.size());
    }

}
