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

package com.espertech.esper.regression.resultset;

import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;
import junit.framework.TestCase;
import com.espertech.esper.client.*;
import com.espertech.esper.client.soda.*;
import com.espertech.esper.client.time.CurrentTimeEvent;
import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.EventType;
import com.espertech.esper.supportregression.bean.SupportBeanString;
import com.espertech.esper.supportregression.bean.SupportMarketDataBean;
import com.espertech.esper.supportregression.bean.SupportBean;
import com.espertech.esper.supportregression.client.SupportConfigFactory;
import com.espertech.esper.util.SerializableObjectCopier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.text.Collator;

public class TestOrderBySimple extends TestCase {

	private static final Logger log = LoggerFactory.getLogger(TestOrderBySimple.class);
	private EPServiceProvider epService;
    private List<Double> prices;
    private List<String> symbols;
    private SupportUpdateListener testListener;
	private List<Long> volumes;

    public void setUp()
    {
        Configuration config = SupportConfigFactory.getConfiguration();
        epService = EPServiceProviderManager.getDefaultProvider(config);
        epService.initialize();
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.startTest(epService, this.getClass(), getName());}
        symbols = new LinkedList<String>();
        prices = new LinkedList<Double>();
        volumes = new LinkedList<Long>();
    }

    protected void tearDown() throws Exception {
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.endTest();}
        testListener = null;
        prices = null;
        symbols = null;
        volumes = null;
    }

    public void testOrderByMultiDelivery() {
        // test for QWY-933597 or ESPER-409
        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(0));
        epService.getEPAdministrator().getConfiguration().addEventType("SupportBean", SupportBean.class);

        // try pattern
        SupportUpdateListener listener = new SupportUpdateListener();
        String stmtText = "select a.theString from pattern [every a=SupportBean(theString like 'A%') -> b=SupportBean(theString like 'B%')] order by a.theString desc";
        EPStatement stmtOne = epService.getEPAdministrator().createEPL(stmtText);
        stmtOne.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean("A1", 1));
        epService.getEPRuntime().sendEvent(new SupportBean("A2", 2));
        epService.getEPRuntime().sendEvent(new SupportBean("B", 3));

        EventBean[] received = listener.getNewDataListFlattened();
        assertEquals(2, received.length);
        EPAssertionUtil.assertPropsPerRow(received, "a.theString".split(","), new Object[][]{{"A2"}, {"A1"}});

        // try pattern with output limit
        SupportUpdateListener listenerThree = new SupportUpdateListener();
        String stmtTextThree = "select a.theString from pattern [every a=SupportBean(theString like 'A%') -> b=SupportBean(theString like 'B%')] " +
                "output every 2 events order by a.theString desc";
        EPStatement stmtThree = epService.getEPAdministrator().createEPL(stmtTextThree);
        stmtThree.addListener(listenerThree);

        epService.getEPRuntime().sendEvent(new SupportBean("A1", 1));
        epService.getEPRuntime().sendEvent(new SupportBean("A2", 2));
        epService.getEPRuntime().sendEvent(new SupportBean("A3", 3));
        epService.getEPRuntime().sendEvent(new SupportBean("B", 3));

        EventBean[] receivedThree = listenerThree.getNewDataListFlattened();
        assertEquals(2, receivedThree.length);
        EPAssertionUtil.assertPropsPerRow(receivedThree, "a.theString".split(","), new Object[][]{{"A2"}, {"A1"}});

        // try grouped time window
        String stmtTextTwo = "select rstream theString from SupportBean#groupwin(theString)#time(10) order by theString desc";
        EPStatement stmtTwo = epService.getEPAdministrator().createEPL(stmtTextTwo);
        SupportUpdateListener listenerTwo = new SupportUpdateListener();
        stmtTwo.addListener(listenerTwo);

        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(1000));
        epService.getEPRuntime().sendEvent(new SupportBean("A1", 1));
        epService.getEPRuntime().sendEvent(new SupportBean("A2", 1));

        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(11000));
        EventBean[] receivedTwo = listenerTwo.getNewDataListFlattened();
        assertEquals(2, receivedTwo.length);
        EPAssertionUtil.assertPropsPerRow(receivedTwo, "theString".split(","), new Object[][]{{"A2"}, {"A1"}});
    }

    public void testCollatorSortLocale()
    {
        String frenchForSin = "p\u00E9ch\u00E9";
        String frenchForFruit = "p\u00EAche";

        String[] sortedFrench = (frenchForFruit + "," + frenchForSin).split(",");

        assertEquals(1, frenchForFruit.compareTo(frenchForSin));
        assertEquals(-1, frenchForSin.compareTo(frenchForFruit));
        Locale.setDefault(Locale.FRENCH);
        assertEquals(1, frenchForFruit.compareTo(frenchForSin));
        assertEquals(-1, Collator.getInstance().compare(frenchForFruit, frenchForSin));
        assertEquals(-1, frenchForSin.compareTo(frenchForFruit));
        assertEquals(1, Collator.getInstance().compare(frenchForSin, frenchForFruit));
        assertFalse(frenchForSin.equals(frenchForFruit));

        /*
        Collections.sort(items);
        System.out.println("Sorted default" + items);

        Collections.sort(items, new Comparator<String>() {
            Collator collator = Collator.getInstance(Locale.FRANCE);
            public int compare(String o1, String o2)
            {
                return collator.compare(o1, o2);
            }
        });
        System.out.println("Sorted FR" + items);
        */

        Configuration config = SupportConfigFactory.getConfiguration();
        config.getEngineDefaults().getLanguage().setSortUsingCollator(true);
        epService = EPServiceProviderManager.getDefaultProvider(config);
        epService.initialize();
        epService.getEPAdministrator().getConfiguration().addEventType("SupportBean", SupportBean.class.getName());

        // test order by
        String stmtText = "select theString from SupportBean#keepall order by theString asc";
        EPStatement stmtOne = epService.getEPAdministrator().createEPL(stmtText);
        epService.getEPRuntime().sendEvent(new SupportBean(frenchForSin, 1));
        epService.getEPRuntime().sendEvent(new SupportBean(frenchForFruit, 1));
        EPAssertionUtil.assertPropsPerRow(stmtOne.iterator(), "theString".split(","), new Object[][]{{sortedFrench[0]}, {sortedFrench[1]}});

        // test sort view
        SupportUpdateListener listener = new SupportUpdateListener();
        stmtText = "select irstream theString from SupportBean#sort(2, theString asc)";
        EPStatement stmtTwo = epService.getEPAdministrator().createEPL(stmtText);
        stmtTwo.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean(frenchForSin, 1));
        epService.getEPRuntime().sendEvent(new SupportBean(frenchForFruit, 1));
        epService.getEPRuntime().sendEvent(new SupportBean("abc", 1));

        assertEquals(frenchForSin, listener.getLastOldData()[0].get("theString"));
        Locale.setDefault(Locale.US);
    }

    public void testIterator()
	{
    	String statementString = "select symbol, theString, price from " +
    	            SupportMarketDataBean.class.getName() + "#length(10) as one, " +
    	            SupportBeanString.class.getName() + "#length(100) as two " +
                    "where one.symbol = two.theString " +
                    "order by price";
        EPStatement statement = epService.getEPAdministrator().createEPL(statementString);
        sendJoinEvents();
        sendEvent("CAT", 50);
        sendEvent("IBM", 49);
        sendEvent("CAT", 15);
        sendEvent("IBM", 100);
        EPAssertionUtil.assertPropsPerRow(statement.iterator(), new String[]{"symbol", "theString", "price"},
                new Object[][]{
                        {"CAT", "CAT", 15d},
                        {"IBM", "IBM", 49d},
                        {"CAT", "CAT", 50d},
                        {"IBM", "IBM", 100d},
                });

        sendEvent("KGB", 75);
        EPAssertionUtil.assertPropsPerRow(statement.iterator(), new String[]{"symbol", "theString", "price"},
                new Object[][]{
                        {"CAT", "CAT", 15d},
                        {"IBM", "IBM", 49d},
                        {"CAT", "CAT", 50d},
                        {"KGB", "KGB", 75d},
                        {"IBM", "IBM", 100d},
                });
    }

    public void testAcrossJoin()
	{
    	String statementString = "select symbol, theString from " +
    	SupportMarketDataBean.class.getName() + "#length(10) as one, " +
    	SupportBeanString.class.getName() + "#length(100) as two " +
    	"where one.symbol = two.theString " +
    	"output every 6 events "  +
    	"order by price";
    	createAndSend(statementString);
    	sendJoinEvents();
    	orderValuesByPriceJoin();
    	assertValues(symbols, "symbol");
    	assertValues(symbols, "theString");
       	assertOnlyProperties(Arrays.asList(new String[] {"symbol", "theString"}));
        clearValues();

    	statementString = "select symbol from " +
    	SupportMarketDataBean.class.getName() + "#length(10) as one, " +
    	SupportBeanString.class.getName() + "#length(100) as two " +
    	"where one.symbol = two.theString " +
    	"output every 6 events "  +
    	"order by theString, price";
    	createAndSend(statementString);
    	sendJoinEvents();
    	orderValuesBySymbolPrice();
    	assertValues(symbols, "symbol");
       	assertOnlyProperties(Arrays.asList(new String[] {"symbol"}));
    	clearValues();
	}

    public void testDescending_OM() throws Exception
	{
        String stmtText = "select symbol from " +
                SupportMarketDataBean.class.getName() + "#length(5) " +
                "output every 6 events "  +
                "order by price desc";

        EPStatementObjectModel model = new EPStatementObjectModel();
        model.setSelectClause(SelectClause.create("symbol"));
        model.setFromClause(FromClause.create(FilterStream.create(SupportMarketDataBean.class.getName()).addView("length", Expressions.constant(5))));
        model.setOutputLimitClause(OutputLimitClause.create(6));
        model.setOrderByClause(OrderByClause.create().add("price", true));
        model = (EPStatementObjectModel) SerializableObjectCopier.copy(model);
        assertEquals(stmtText, model.toEPL());

        testListener = new SupportUpdateListener();
        EPStatement statement = epService.getEPAdministrator().create(model);
        statement.addListener(testListener);
        sendEvent("IBM", 2);
        sendEvent("KGB", 1);
        sendEvent("CMU", 3);
        sendEvent("IBM", 6);
        sendEvent("CAT", 6);
        sendEvent("CAT", 5);

		orderValuesByPriceDesc();
		assertValues(symbols, "symbol");
		clearValues();
    }

    public void testDescending()
	{
		String statementString = "select symbol from " +
		SupportMarketDataBean.class.getName() + "#length(5) " +
		"output every 6 events "  +
		"order by price desc";
		createAndSend(statementString);
		orderValuesByPriceDesc();
		assertValues(symbols, "symbol");
		clearValues();

		statementString = "select symbol from " +
		SupportMarketDataBean.class.getName() + "#length(5) " +
		"output every 6 events "  +
		"order by price desc, symbol asc";
		createAndSend(statementString);
		orderValuesByPrice();
		Collections.reverse(symbols);
		assertValues(symbols, "symbol");
		clearValues();

		statementString = "select symbol from " +
		SupportMarketDataBean.class.getName() + "#length(5) " +
		"output every 6 events "  +
		"order by price asc";
		createAndSend(statementString);
		orderValuesByPrice();
		assertValues(symbols, "symbol");
		clearValues();

		statementString = "select symbol, volume from " +
		SupportMarketDataBean.class.getName() + "#length(5) " +
		"output every 6 events "  +
		"order by symbol desc";
		createAndSend(statementString);
		orderValuesBySymbol();
		Collections.reverse(symbols);
		assertValues(symbols, "symbol");
		assertValues(volumes, "volume");
		clearValues();

		statementString = "select symbol, price from " +
		SupportMarketDataBean.class.getName() + "#length(5) " +
		"output every 6 events "  +
		"order by symbol desc, price desc";
		createAndSend(statementString);
		orderValuesBySymbolPrice();
		Collections.reverse(symbols);
		Collections.reverse(prices);
		assertValues(symbols, "symbol");
		assertValues(prices, "price");
		clearValues();

		statementString = "select symbol, price from " +
		SupportMarketDataBean.class.getName() + "#length(5) " +
		"output every 6 events "  +
		"order by symbol, price";
		createAndSend(statementString);
		orderValuesBySymbolPrice();
		assertValues(symbols, "symbol");
		assertValues(prices, "price");
		clearValues();
	}

    public void testExpressions()
	{
		String statementString = "select symbol from " +
	 	SupportMarketDataBean.class.getName() + "#length(10) " +
	 	"output every 6 events "  +
	 	"order by (price * 6) + 5";
	 	createAndSend(statementString);
	 	orderValuesByPrice();
	 	assertValues(symbols, "symbol");
		assertOnlyProperties(Arrays.asList(new String[] {"symbol"}));
	 	clearValues();

	 	epService.initialize();

		statementString = "select symbol, price from " +
	 	SupportMarketDataBean.class.getName() + "#length(10) " +
	 	"output every 6 events "  +
	 	"order by (price * 6) + 5, price";
	 	createAndSend(statementString);
	 	orderValuesByPrice();
	 	assertValues(symbols, "symbol");
	   	assertOnlyProperties(Arrays.asList(new String[] {"symbol", "price"}));
	 	clearValues();

	 	epService.initialize();

		statementString = "select symbol, 1+volume*23 from " +
	 	SupportMarketDataBean.class.getName() + "#length(10) " +
	 	"output every 6 events "  +
	 	"order by (price * 6) + 5, price, volume";
	 	createAndSend(statementString);
	 	orderValuesByPrice();
	 	assertValues(symbols, "symbol");
	   	assertOnlyProperties(Arrays.asList(new String[] {"symbol", "1+volume*23"}));
	 	clearValues();

	 	epService.initialize();

		statementString = "select symbol from " +
	 	SupportMarketDataBean.class.getName() + "#length(10) " +
	 	"output every 6 events "  +
	 	"order by volume*price, symbol";
	 	createAndSend(statementString);
	 	orderValuesBySymbol();
	 	assertValues(symbols, "symbol");
	   	assertOnlyProperties(Arrays.asList(new String[] {"symbol"}));
	 	clearValues();
	}

    public void testAliasesSimple()
    {
        String statementString = "select symbol as mySymbol from " +
        SupportMarketDataBean.class.getName() + "#length(5) " +
        "output every 6 events "  +
        "order by mySymbol";
        createAndSend(statementString);
        orderValuesBySymbol();
        assertValues(symbols, "mySymbol");
           assertOnlyProperties(Arrays.asList(new String[] {"mySymbol"}));
        clearValues();

        statementString = "select symbol as mySymbol, price as myPrice from " +
        SupportMarketDataBean.class.getName() + "#length(5) " +
        "output every 6 events "  +
        "order by myPrice";
        createAndSend(statementString);
        orderValuesByPrice();
        assertValues(symbols, "mySymbol");
        assertValues(prices, "myPrice");
           assertOnlyProperties(Arrays.asList(new String[] {"mySymbol", "myPrice"}));
        clearValues();

        statementString = "select symbol, price as myPrice from " +
         SupportMarketDataBean.class.getName() + "#length(10) " +
         "output every 6 events "  +
         "order by (myPrice * 6) + 5, price";
         createAndSend(statementString);
         orderValuesByPrice();
         assertValues(symbols, "symbol");
           assertOnlyProperties(Arrays.asList(new String[] {"symbol", "myPrice"}));
         clearValues();

        statementString = "select symbol, 1+volume*23 as myVol from " +
         SupportMarketDataBean.class.getName() + "#length(10) " +
         "output every 6 events "  +
         "order by (price * 6) + 5, price, myVol";
         createAndSend(statementString);
         orderValuesByPrice();
         assertValues(symbols, "symbol");
           assertOnlyProperties(Arrays.asList(new String[] {"symbol", "myVol"}));
         clearValues();
    }

    public void testExpressionsJoin()
    {
    	String statementString = "select symbol from " +
    	SupportMarketDataBean.class.getName() + "#length(10) as one, " +
    	SupportBeanString.class.getName() + "#length(100) as two " +
    	"where one.symbol = two.theString " +
     	"output every 6 events "  +
     	"order by (price * 6) + 5";
     	createAndSend(statementString);
     	sendJoinEvents();
     	orderValuesByPriceJoin();
     	assertValues(symbols, "symbol");
    	assertOnlyProperties(Arrays.asList(new String[] {"symbol"}));
     	clearValues();

     	epService.initialize();

    	statementString = "select symbol, price from " +
    	SupportMarketDataBean.class.getName() + "#length(10) as one, " +
    	SupportBeanString.class.getName() + "#length(100) as two " +
    	"where one.symbol = two.theString " +
     	"output every 6 events "  +
     	"order by (price * 6) + 5, price";
     	createAndSend(statementString);
     	sendJoinEvents();
     	orderValuesByPriceJoin();
     	assertValues(prices, "price");
       	assertOnlyProperties(Arrays.asList(new String[] {"symbol", "price"}));
     	clearValues();

     	epService.initialize();

    	statementString = "select symbol, 1+volume*23 from " +
    	SupportMarketDataBean.class.getName() + "#length(10) as one, " +
    	SupportBeanString.class.getName() + "#length(100) as two " +
    	"where one.symbol = two.theString " +
     	"output every 6 events "  +
     	"order by (price * 6) + 5, price, volume";
     	createAndSend(statementString);
     	sendJoinEvents();
     	orderValuesByPriceJoin();
     	assertValues(symbols, "symbol");
       	assertOnlyProperties(Arrays.asList(new String[] {"symbol", "1+volume*23"}));
     	clearValues();

     	epService.initialize();

    	statementString = "select symbol from " +
    	SupportMarketDataBean.class.getName() + "#length(10) as one, " +
    	SupportBeanString.class.getName() + "#length(100) as two " +
    	"where one.symbol = two.theString " +
     	"output every 6 events "  +
     	"order by volume*price, symbol";
     	createAndSend(statementString);
     	sendJoinEvents();
     	orderValuesBySymbol();
     	assertValues(symbols, "symbol");
       	assertOnlyProperties(Arrays.asList(new String[] {"symbol"}));
     	clearValues();
    }

    public void testInvalid()
	{
		String statementString = "select symbol from " +
		SupportMarketDataBean.class.getName() + "#length(5) " +
		"output every 6 events "  +
		"order by sum(price)";
		try
		{
			createAndSend(statementString);
			fail();
		}
		catch(EPStatementException ex)
		{
			// expected
		}

		statementString = "select sum(price) from " +
		SupportMarketDataBean.class.getName() + "#length(5) " +
		"output every 6 events "  +
		"order by sum(price + 6)";
		try
		{
			createAndSend(statementString);
			fail();
		}
		catch(EPStatementException ex)
		{
			// expected
		}

		statementString = "select sum(price + 6) from " +
		SupportMarketDataBean.class.getName() + "#length(5) " +
		"output every 6 events "  +
		"order by sum(price)";
		try
		{
			createAndSend(statementString);
			fail();
		}
		catch(EPStatementException ex)
		{
			// expected
		}
	}

    public void testInvalidJoin()
    {
    	String statementString = "select symbol from " +
    	SupportMarketDataBean.class.getName() + "#length(10) as one, " +
    	SupportBeanString.class.getName() + "#length(100) as two " +
    	"where one.symbol = two.theString " +
    	"output every 6 events "  +
    	"order by sum(price)";
    	try
    	{
    		createAndSend(statementString);
    		fail();
    	}
    	catch(EPStatementException ex)
    	{
    		// expected
    	}

    	statementString = "select sum(price) from " +
    	SupportMarketDataBean.class.getName() + "#length(10) as one, " +
    	SupportBeanString.class.getName() + "#length(100) as two " +
    	"where one.symbol = two.theString " +
    	"output every 6 events "  +
    	"order by sum(price + 6)";
    	try
    	{
    		createAndSend(statementString);
    		fail();
    	}
    	catch(EPStatementException ex)
    	{
    		// expected
    	}

    	statementString = "select sum(price + 6) from " +
    	SupportMarketDataBean.class.getName() + "#length(10) as one, " +
    	SupportBeanString.class.getName() + "#length(100) as two " +
    	"where one.symbol = two.theString " +
    	"output every 6 events "  +
    	"order by sum(price)";
    	try
    	{
    		createAndSend(statementString);
    		fail();
    	}
    	catch(EPStatementException ex)
    	{
    		// expected
    	}
    }

    public void testMultipleKeys()
	{
		String statementString = "select symbol from " +
		SupportMarketDataBean.class.getName() + "#length(10) " +
		"output every 6 events "  +
		"order by symbol, price";
		createAndSend(statementString);
		orderValuesBySymbolPrice();
		assertValues(symbols, "symbol");
	   	assertOnlyProperties(Arrays.asList(new String[] {"symbol"}));
		clearValues();

		statementString = "select symbol from " +
	 	SupportMarketDataBean.class.getName() + "#length(10) " +
	 	"output every 6 events "  +
	 	"order by price, symbol, volume";
	 	createAndSend(statementString);
	 	orderValuesByPriceSymbol();
	 	assertValues(symbols, "symbol");
	   	assertOnlyProperties(Arrays.asList(new String[] {"symbol"}));
	 	clearValues();

		statementString = "select symbol, volume*2 from " +
	 	SupportMarketDataBean.class.getName() + "#length(10) " +
	 	"output every 6 events "  +
	 	"order by price, volume";
	 	createAndSend(statementString);
	 	orderValuesByPrice();
	 	assertValues(symbols, "symbol");
	   	assertOnlyProperties(Arrays.asList(new String[] {"symbol", "volume*2"}));
	 	clearValues();
	}

	public void testAliases()
	{
		String statementString = "select symbol as mySymbol from " +
		SupportMarketDataBean.class.getName() + "#length(5) " +
		"output every 6 events "  +
		"order by mySymbol";
		createAndSend(statementString);
		orderValuesBySymbol();
		assertValues(symbols, "mySymbol");
	   	assertOnlyProperties(Arrays.asList(new String[] {"mySymbol"}));
		clearValues();

		statementString = "select symbol as mySymbol, price as myPrice from " +
		SupportMarketDataBean.class.getName() + "#length(5) " +
		"output every 6 events "  +
		"order by myPrice";
		createAndSend(statementString);
		orderValuesByPrice();
		assertValues(symbols, "mySymbol");
		assertValues(prices, "myPrice");
	   	assertOnlyProperties(Arrays.asList(new String[] {"mySymbol", "myPrice"}));
		clearValues();

		statementString = "select symbol, price as myPrice from " +
	 	SupportMarketDataBean.class.getName() + "#length(10) " +
	 	"output every 6 events "  +
	 	"order by (myPrice * 6) + 5, price";
	 	createAndSend(statementString);
	 	orderValuesByPrice();
	 	assertValues(symbols, "symbol");
	   	assertOnlyProperties(Arrays.asList(new String[] {"symbol", "myPrice"}));
	 	clearValues();

		statementString = "select symbol, 1+volume*23 as myVol from " +
	 	SupportMarketDataBean.class.getName() + "#length(10) " +
	 	"output every 6 events "  +
	 	"order by (price * 6) + 5, price, myVol";
	 	createAndSend(statementString);
	 	orderValuesByPrice();
	 	assertValues(symbols, "symbol");
	   	assertOnlyProperties(Arrays.asList(new String[] {"symbol", "myVol"}));
	 	clearValues();

		statementString = "select symbol as mySymbol from " +
		SupportMarketDataBean.class.getName() + "#length(5) " +
		"order by price, mySymbol";
		createAndSend(statementString);
		symbols.add("CAT");
		assertValues(symbols, "mySymbol");
		clearValues();
		sendEvent("FOX", 10);
		symbols.add("FOX");
		assertValues(symbols, "mySymbol");
		clearValues();
	}

    public void testMultipleKeysJoin()
    {
    	String statementString = "select symbol from " +
    	SupportMarketDataBean.class.getName() + "#length(10) as one, " +
    	SupportBeanString.class.getName() + "#length(100) as two " +
    	"where one.symbol = two.theString " +
    	"output every 6 events "  +
    	"order by symbol, price";
    	createAndSend(statementString);
    	sendJoinEvents();
    	orderValuesBySymbolPrice();
    	assertValues(symbols, "symbol");
       	assertOnlyProperties(Arrays.asList(new String[] {"symbol"}));
    	clearValues();

    	statementString = "select symbol from " +
    	SupportMarketDataBean.class.getName() + "#length(10) as one, " +
    	SupportBeanString.class.getName() + "#length(100) as two " +
    	"where one.symbol = two.theString " +
     	"output every 6 events "  +
     	"order by price, symbol, volume";
     	createAndSend(statementString);
    	sendJoinEvents();
     	orderValuesByPriceSymbol();
     	assertValues(symbols, "symbol");
       	assertOnlyProperties(Arrays.asList(new String[] {"symbol"}));
     	clearValues();

    	statementString = "select symbol, volume*2 from " +
    	SupportMarketDataBean.class.getName() + "#length(10) as one, " +
    	SupportBeanString.class.getName() + "#length(100) as two " +
    	"where one.symbol = two.theString " +
     	"output every 6 events "  +
     	"order by price, volume";
     	createAndSend(statementString);
    	sendJoinEvents();
     	orderValuesByPriceJoin();
     	assertValues(symbols, "symbol");
       	assertOnlyProperties(Arrays.asList(new String[] {"symbol", "volume*2"}));
     	clearValues();
    }

    public void testSimple()
	{
		String statementString = "select symbol from " +
		SupportMarketDataBean.class.getName() + "#length(5) " +
		"output every 6 events "  +
		"order by price";
		createAndSend(statementString);
		orderValuesByPrice();
		assertValues(symbols, "symbol");
	   	assertOnlyProperties(Arrays.asList(new String[] {"symbol"}));
		clearValues();

		statementString = "select symbol, price from " +
		SupportMarketDataBean.class.getName() + "#length(5) " +
		"output every 6 events "  +
		"order by price";
		createAndSend(statementString);
		orderValuesByPrice();
		assertValues(symbols, "symbol");
		assertValues(prices, "price");
	   	assertOnlyProperties(Arrays.asList(new String[] {"symbol", "price"}));
		clearValues();

		statementString = "select symbol, volume from " +
		SupportMarketDataBean.class.getName() + "#length(5) " +
		"output every 6 events "  +
		"order by price";
		createAndSend(statementString);
		orderValuesByPrice();
		assertValues(symbols, "symbol");
		assertValues(volumes, "volume");
	   	assertOnlyProperties(Arrays.asList(new String[] {"symbol", "volume"}));
		clearValues();

		statementString = "select symbol, volume*2 from " +
		SupportMarketDataBean.class.getName() + "#length(5) " +
		"output every 6 events "  +
		"order by price";
		createAndSend(statementString);
		orderValuesByPrice();
		assertValues(symbols, "symbol");
		assertValues(volumes, "volume*2");
	   	assertOnlyProperties(Arrays.asList(new String[] {"symbol", "volume*2"}));
		clearValues();

		statementString = "select symbol, volume from " +
		SupportMarketDataBean.class.getName() + "#length(5) " +
		"output every 6 events "  +
		"order by symbol";
		createAndSend(statementString);
		orderValuesBySymbol();
		assertValues(symbols, "symbol");
		assertValues(volumes, "volume");
	   	assertOnlyProperties(Arrays.asList(new String[] {"symbol", "volume"}));
		clearValues();

		statementString = "select price from " +
		SupportMarketDataBean.class.getName() + "#length(5) " +
		"output every 6 events "  +
		"order by symbol";
		createAndSend(statementString);
		orderValuesBySymbol();
		assertValues(prices, "price");
	   	assertOnlyProperties(Arrays.asList(new String[] {"price"}));
		clearValues();
	}

    public void testSimpleJoin()
    {
    	String statementString = "select symbol from " +
    	SupportMarketDataBean.class.getName() + "#length(10) as one, " +
    	SupportBeanString.class.getName() + "#length(100) as two " +
    	"where one.symbol = two.theString " +
    	"output every 6 events "  +
    	"order by price";
    	createAndSend(statementString);
    	sendJoinEvents();
    	orderValuesByPriceJoin();
    	assertValues(symbols, "symbol");
       	assertOnlyProperties(Arrays.asList(new String[] {"symbol"}));
    	clearValues();

    	statementString = "select symbol, price from " +
    	SupportMarketDataBean.class.getName() + "#length(10) as one, " +
    	SupportBeanString.class.getName() + "#length(100) as two " +
    	"where one.symbol = two.theString " +
    	"output every 6 events "  +
    	"order by price";
    	createAndSend(statementString);
    	sendJoinEvents();
    	orderValuesByPriceJoin();
    	assertValues(symbols, "symbol");
    	assertValues(prices, "price");
       	assertOnlyProperties(Arrays.asList(new String[] {"symbol", "price"}));
    	clearValues();

    	statementString = "select symbol, volume from " +
    	SupportMarketDataBean.class.getName() + "#length(10) as one, " +
    	SupportBeanString.class.getName() + "#length(100) as two " +
    	"where one.symbol = two.theString " +
    	"output every 6 events "  +
    	"order by price";
    	createAndSend(statementString);
    	sendJoinEvents();
    	orderValuesByPriceJoin();
    	assertValues(symbols, "symbol");
    	assertValues(volumes, "volume");
       	assertOnlyProperties(Arrays.asList(new String[] {"symbol", "volume"}));
    	clearValues();

    	statementString = "select symbol, volume*2 from " +
    	SupportMarketDataBean.class.getName() + "#length(10) as one, " +
    	SupportBeanString.class.getName() + "#length(100) as two " +
    	"where one.symbol = two.theString " +
    	"output every 6 events "  +
    	"order by price";
    	createAndSend(statementString);
    	sendJoinEvents();
    	orderValuesByPriceJoin();
    	assertValues(symbols, "symbol");
    	assertValues(volumes, "volume*2");
       	assertOnlyProperties(Arrays.asList(new String[] {"symbol", "volume*2"}));
    	clearValues();

    	statementString = "select symbol, volume from " +
    	SupportMarketDataBean.class.getName() + "#length(10) as one, " +
    	SupportBeanString.class.getName() + "#length(100) as two " +
    	"where one.symbol = two.theString " +
    	"output every 6 events "  +
    	"order by symbol";
    	createAndSend(statementString);
    	sendJoinEvents();
    	orderValuesBySymbol();
    	assertValues(symbols, "symbol");
    	assertValues(volumes, "volume");
       	assertOnlyProperties(Arrays.asList(new String[] {"symbol", "volume"}));
    	clearValues();

    	statementString = "select price from " +
    	SupportMarketDataBean.class.getName() + "#length(10) as one, " +
    	SupportBeanString.class.getName() + "#length(100) as two " +
    	"where one.symbol = two.theString " +
    	"output every 6 events "  +
    	"order by symbol, price";
    	createAndSend(statementString);
    	sendJoinEvents();
    	orderValuesBySymbolJoin();
    	assertValues(prices, "price");
       	assertOnlyProperties(Arrays.asList(new String[] {"price"}));
    	clearValues();
    }

    public void testWildcard()
	{
		String statementString = "select * from " +
		SupportMarketDataBean.class.getName() + "#length(5) " +
		"output every 6 events "  +
		"order by price";
		createAndSend(statementString);
		orderValuesByPrice();
		assertValues(symbols, "symbol");
		assertValues(prices, "price");
		assertValues(volumes, "volume");
	   	assertOnlyProperties(Arrays.asList(new String[] {"symbol", "id", "volume", "price", "feed"}));
		clearValues();

		statementString = "select * from " +
		SupportMarketDataBean.class.getName() + "#length(5) " +
		"output every 6 events "  +
		"order by symbol";
		createAndSend(statementString);
		orderValuesBySymbol();
		assertValues(symbols, "symbol");
		assertValues(prices, "price");
		assertValues(volumes, "volume");
	   	assertOnlyProperties(Arrays.asList(new String[] {"symbol", "volume", "price", "feed", "id"}));
		clearValues();
	}


    public void testWildcardJoin()
    {
    	String statementString = "select * from " +
    	SupportMarketDataBean.class.getName() + "#length(10) as one, " +
    	SupportBeanString.class.getName() + "#length(100) as two " +
    	"where one.symbol = two.theString " +
    	"output every 6 events " +
    	"order by price";
    	createAndSend(statementString);
    	sendJoinEvents();
    	orderValuesByPriceJoin();
    	assertSymbolsJoinWildCard();
    	clearValues();

    	epService.initialize();

    	statementString = "select * from " +
    	SupportMarketDataBean.class.getName() + "#length(10) as one, " +
    	SupportBeanString.class.getName() + "#length(100) as two " +
    	"where one.symbol = two.theString " +
    	"output every 6 events "  +
    	"order by symbol, price";
    	createAndSend(statementString);
    	sendJoinEvents();
    	orderValuesBySymbolJoin();
    	assertSymbolsJoinWildCard();
    	clearValues();
    }

    public void testNoOutputClauseView()
    {
		String statementString = "select symbol from " +
		SupportMarketDataBean.class.getName() + "#length(5) " +
		"order by price";
		createAndSend(statementString);
		symbols.add("CAT");
		assertValues(symbols, "symbol");
		clearValues();
		sendEvent("FOX", 10);
		symbols.add("FOX");
		assertValues(symbols, "symbol");
		clearValues();

		epService.initialize();

		// Set start time
		sendTimeEvent(0);

		statementString = "select symbol from " +
		SupportMarketDataBean.class.getName() + "#time_batch(1 sec) " +
		"order by price";
		createAndSend(statementString);
		orderValuesByPrice();
		sendTimeEvent(1000);
		assertValues(symbols, "symbol");
	   	assertOnlyProperties(Arrays.asList(new String[] {"symbol"}));
		clearValues();
    }

    public void testNoOutputClauseJoin()
    {
    	String statementString = "select symbol from " +
    	SupportMarketDataBean.class.getName() + "#length(10) as one, " +
    	SupportBeanString.class.getName() + "#length(100) as two " +
    	"where one.symbol = two.theString " +
    	"order by price";
    	createAndSend(statementString);
    	sendJoinEvents();
		symbols.add("KGB");
		assertValues(symbols, "symbol");
		clearValues();
		sendEvent("DOG", 10);
		symbols.add("DOG");
		assertValues(symbols, "symbol");
		clearValues();

		epService.initialize();

		// Set start time
		sendTimeEvent(0);

    	statementString = "select symbol from " +
    	SupportMarketDataBean.class.getName() + "#time_batch(1) as one, " +
    	SupportBeanString.class.getName() + "#length(100) as two " +
    	"where one.symbol = two.theString " +
    	"order by price, symbol";
    	createAndSend(statementString);
    	sendJoinEvents();
    	orderValuesByPriceJoin();
		sendTimeEvent(1000);
    	assertValues(symbols, "symbol");
       	assertOnlyProperties(Arrays.asList(new String[] {"symbol"}));
    	clearValues();
    }

	private void assertOnlyProperties(List<String> requiredProperties)
    {
    	EventBean[] events = testListener.getLastNewData();
    	if(events == null || events.length == 0)
    	{
    		return;
    	}
    	EventType type = events[0].getEventType();
    	List<String> actualProperties = new ArrayList<String>(Arrays.asList(type.getPropertyNames()));
    	log.debug(".assertOnlyProperties actualProperties=="+actualProperties);
    	assertTrue(actualProperties.containsAll(requiredProperties));
    	actualProperties.removeAll(requiredProperties);
    	assertTrue(actualProperties.isEmpty());
    }

	private void assertSymbolsJoinWildCard()
    {
    	EventBean[] events = testListener.getLastNewData();
    	log.debug(".assertValues event type = " + events[0].getEventType());
    	log.debug(".assertValues values: " + symbols);
    	log.debug(".assertValues events.length==" + events.length);
    	for(int i = 0; i < events.length; i++)
    	{
    		SupportMarketDataBean theEvent = (SupportMarketDataBean)events[i].get("one");
    		assertEquals(symbols.get(i), theEvent.getSymbol());
    	}
    }

    private void assertValues(List values, String valueName)
    {
    	EventBean[] events = testListener.getLastNewData();
    	assertEquals(values.size(), events.length);
    	log.debug(".assertValues values: " + values);
    	for(int i = 0; i < events.length; i++)
    	{
    		log.debug(".assertValues events["+i+"]=="+events[i].get(valueName));
    		assertEquals(values.get(i), events[i].get(valueName));
    	}
    }

	private void clearValues()
    {
    	prices.clear();
    	volumes.clear();
    	symbols.clear();
    }

	private void createAndSend(String statementString) {
		testListener = new SupportUpdateListener();
		EPStatement statement = epService.getEPAdministrator().createEPL(statementString);
    	statement.addListener(testListener);
    	sendEvent("IBM", 2);
    	sendEvent("KGB", 1);
    	sendEvent("CMU", 3);
    	sendEvent("IBM", 6);
    	sendEvent("CAT", 6);
    	sendEvent("CAT", 5);
	}


	private void orderValuesByPrice()
    {
    	symbols.add(0, "KGB");
    	symbols.add(1, "IBM");
    	symbols.add(2, "CMU");
    	symbols.add(3, "CAT");
    	symbols.add(4, "IBM");
    	symbols.add(5, "CAT");
    	prices.add(0, 1d);
    	prices.add(1, 2d);
    	prices.add(2, 3d);
    	prices.add(3, 5d);
    	prices.add(4, 6d);
    	prices.add(5, 6d);
    	volumes.add(0, 0l);
    	volumes.add(1, 0l);
    	volumes.add(2, 0l);
    	volumes.add(3, 0l);
    	volumes.add(4, 0l);
    	volumes.add(5, 0l);
    }

    private void orderValuesByPriceDesc()
    {
    	symbols.add(0, "IBM");
    	symbols.add(1, "CAT");
    	symbols.add(2, "CAT");
    	symbols.add(3, "CMU");
    	symbols.add(4, "IBM");
    	symbols.add(5, "KGB");
    	prices.add(0, 6d);
    	prices.add(1, 6d);
    	prices.add(2, 5d);
    	prices.add(3, 3d);
    	prices.add(4, 2d);
    	prices.add(5, 1d);
    	volumes.add(0, 0l);
    	volumes.add(1, 0l);
    	volumes.add(2, 0l);
    	volumes.add(3, 0l);
    	volumes.add(4, 0l);
    	volumes.add(5, 0l);
    }

	private void orderValuesByPriceJoin()
    {
    	symbols.add(0, "KGB");
    	symbols.add(1, "IBM");
    	symbols.add(2, "CMU");
    	symbols.add(3, "CAT");
    	symbols.add(4, "CAT");
    	symbols.add(5, "IBM");
    	prices.add(0, 1d);
    	prices.add(1, 2d);
    	prices.add(2, 3d);
    	prices.add(3, 5d);
    	prices.add(4, 6d);
    	prices.add(5, 6d);
    	volumes.add(0, 0l);
    	volumes.add(1, 0l);
    	volumes.add(2, 0l);
    	volumes.add(3, 0l);
    	volumes.add(4, 0l);
    	volumes.add(5, 0l);
    }

    private void orderValuesByPriceSymbol()
    {
    	symbols.add(0, "KGB");
    	symbols.add(1, "IBM");
    	symbols.add(2, "CMU");
    	symbols.add(3, "CAT");
    	symbols.add(4, "CAT");
    	symbols.add(5, "IBM");
    	prices.add(0, 1d);
    	prices.add(1, 2d);
    	prices.add(2, 3d);
    	prices.add(3, 5d);
    	prices.add(4, 6d);
    	prices.add(5, 6d);
    	volumes.add(0, 0l);
    	volumes.add(1, 0l);
    	volumes.add(2, 0l);
    	volumes.add(3, 0l);
    	volumes.add(4, 0l);
    	volumes.add(5, 0l);
    }

	private void orderValuesBySymbol()
    {
    	symbols.add(0, "CAT");
    	symbols.add(1, "CAT");
    	symbols.add(2, "CMU");
    	symbols.add(3, "IBM");
    	symbols.add(4, "IBM");
    	symbols.add(5, "KGB");
    	prices.add(0, 6d);
    	prices.add(1, 5d);
    	prices.add(2, 3d);
    	prices.add(3, 2d);
    	prices.add(4, 6d);
    	prices.add(5, 1d);
    	volumes.add(0, 0l);
    	volumes.add(1, 0l);
    	volumes.add(2, 0l);
    	volumes.add(3, 0l);
    	volumes.add(4, 0l);
    	volumes.add(5, 0l);
    }

    private void orderValuesBySymbolJoin()
    {
    	symbols.add(0, "CAT");
    	symbols.add(1, "CAT");
    	symbols.add(2, "CMU");
    	symbols.add(3, "IBM");
    	symbols.add(4, "IBM");
    	symbols.add(5, "KGB");
    	prices.add(0, 5d);
    	prices.add(1, 6d);
    	prices.add(2, 3d);
    	prices.add(3, 2d);
    	prices.add(4, 6d);
    	prices.add(5, 1d);
    	volumes.add(0, 0l);
    	volumes.add(1, 0l);
    	volumes.add(2, 0l);
    	volumes.add(3, 0l);
    	volumes.add(4, 0l);
    	volumes.add(5, 0l);
    }

	private void orderValuesBySymbolPrice()
    {
    	symbols.add(0, "CAT");
    	symbols.add(1, "CAT");
    	symbols.add(2, "CMU");
    	symbols.add(3, "IBM");
    	symbols.add(4, "IBM");
    	symbols.add(5, "KGB");
    	prices.add(0, 5d);
    	prices.add(1, 6d);
    	prices.add(2, 3d);
    	prices.add(3, 2d);
    	prices.add(4, 6d);
    	prices.add(5, 1d);
    	volumes.add(0, 0l);
    	volumes.add(1, 0l);
    	volumes.add(2, 0l);
    	volumes.add(3, 0l);
    	volumes.add(4, 0l);
    	volumes.add(5, 0l);
    }

	private void sendEvent(String symbol, double price)
	{
	    SupportMarketDataBean bean = new SupportMarketDataBean(symbol, price, 0L, null);
	    epService.getEPRuntime().sendEvent(bean);
	}

	private void sendTimeEvent(int millis)
	{
        CurrentTimeEvent theEvent = new CurrentTimeEvent(millis);
        epService.getEPRuntime().sendEvent(theEvent);
	}

	private void sendJoinEvents()
	{
		epService.getEPRuntime().sendEvent(new SupportBeanString("CAT"));
		epService.getEPRuntime().sendEvent(new SupportBeanString("IBM"));
		epService.getEPRuntime().sendEvent(new SupportBeanString("CMU"));
		epService.getEPRuntime().sendEvent(new SupportBeanString("KGB"));
		epService.getEPRuntime().sendEvent(new SupportBeanString("DOG"));
	}

}
