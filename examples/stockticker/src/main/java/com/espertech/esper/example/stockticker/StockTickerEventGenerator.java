package com.espertech.esper.example.stockticker;

import com.espertech.esper.example.stockticker.eventbean.PriceLimit;
import com.espertech.esper.example.stockticker.eventbean.StockTick;

import java.util.Random;
import java.util.LinkedList;

public class StockTickerEventGenerator
{
    private Random random = new Random(System.currentTimeMillis());

    public LinkedList<Object> makeEventStream(int numberOfTicks,
                                              int ratioOutOfLimit,
                                              int numberOfStocks,
                                              double priceLimitPctLowerLimit,
                                              double priceLimitPctUpperLimit,
                                              double priceLowerLimit,
                                              double priceUpperLimit,
                                              boolean isLastTickOutOfLimit)
    {
        LinkedList<Object> stream = new LinkedList<Object>();

        PriceLimit limitBeans[] = makeLimits("example_user", numberOfStocks, priceLimitPctLowerLimit, priceLimitPctUpperLimit);

        for (int i = 0; i < limitBeans.length; i++)
        {
            stream.add(limitBeans[i]);
        }

        // The first stock ticker sets up an initial price
        StockTick initialPrices[] = makeInitialPriceStockTicks(limitBeans, priceLowerLimit, priceUpperLimit);

        for (int i = 0; i < initialPrices.length; i++)
        {
            stream.add(initialPrices[i]);
        }

        for (int i = 0; i < numberOfTicks; i++)
        {
            int index = i % limitBeans.length;
            StockTick tick = makeStockTick(limitBeans[index], initialPrices[index]);

            // Generate an out-of-limit price
            if ((i % ratioOutOfLimit) == 0)
            {
                tick = new StockTick(tick.getStockSymbol(), -1);
            }

            // Last tick is out-of-limit as well
            if ((i == (numberOfTicks - 1)) && (isLastTickOutOfLimit))
            {
                tick = new StockTick(tick.getStockSymbol(), 9999);
            }

            stream.add(tick);
        }

        return stream;
    }

    public StockTick makeStockTick(PriceLimit limitBean, StockTick initialPrice)
    {
        String stockSymbol = limitBean.getStockSymbol();
        double range = initialPrice.getPrice() * limitBean.getLimitPct() / 100;
        double price = (initialPrice.getPrice() - range + (range * 2 * random.nextDouble()));

        double priceReducedPrecision = to1tenthPrecision(price);

        if (priceReducedPrecision < (initialPrice.getPrice() - range))
        {
            priceReducedPrecision = initialPrice.getPrice();
        }

        if (priceReducedPrecision > (initialPrice.getPrice() + range))
        {
            priceReducedPrecision = initialPrice.getPrice();
        }

        return new StockTick(stockSymbol, priceReducedPrecision);
    }

    public PriceLimit[] makeLimits(String userName,
                                          int numBeans,
                                          double limit_pct_lower_boundary,
                                          double limit_pct_upper_boundary)
    {
        PriceLimit[] limitBeans = new PriceLimit[numBeans];

        for (int i = 0; i < numBeans; i++)
        {
            String stockSymbol = "SYM_" + i;

            double diff = limit_pct_upper_boundary - limit_pct_lower_boundary;
            double limitPct = limit_pct_lower_boundary + (random.nextDouble() * diff);

            limitBeans[i] = new PriceLimit(userName, stockSymbol, to1tenthPrecision(limitPct));
        }

        return limitBeans;
    }

    public StockTick[] makeInitialPriceStockTicks(PriceLimit limitBeans[],
                                              double price_lower_boundary,
                                              double price_upper_boundary)
    {
        StockTick[] stockTickBeans = new StockTick[limitBeans.length];

        for (int i = 0; i < stockTickBeans.length; i++)
        {
            String stockSymbol = limitBeans[i].getStockSymbol();

            // Determine a random price
            double diff = price_upper_boundary - price_lower_boundary;
            double price = price_lower_boundary + random.nextDouble() * diff;

            stockTickBeans[i] = new StockTick(stockSymbol, to1tenthPrecision(price));
        }

        return stockTickBeans;
    }

    private double to1tenthPrecision(double aDouble)
    {
        int intValue = (int) (aDouble * 10);
        return intValue / 10.0;
    }
}
