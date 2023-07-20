/*
  Copyright (c) Lightstreamer Srl

  Licensed under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License.
  You may obtain a copy of the License at

      http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.
*/

package com.lightstreamer.adapters.MarketDepthDemo.orders_simulator;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;

import org.apache.logging.log4j.Logger;

import com.lightstreamer.adapters.MarketDepthDemo.Constants;
import com.lightstreamer.adapters.MarketDepthDemo.MarketMaker;
import com.lightstreamer.adapters.MarketDepthDemo.PriceOutOfBoundException;
import com.lightstreamer.adapters.MarketDepthDemo.QtyOutOfBoundException;
import com.lightstreamer.adapters.MarketDepthDemo.RejectProposalException;
    
public class Stock {

    private Logger logger;
    
    private String symbol;

    private String shortDescription;
    
    private double referencePrice;
    
    private String tradingPhase;

    private double lastPrice = 0;
    
    private long lastQty = 0;
    
    private Date last = null;
    
    private long rnd_gen = 10;
    
    private Map<Double, OrderBuy> BuyOrders;
    
    private Map<Double, OrderSell> SellOrders;
    
    private ArrayList<Execution> ExecList;
    
    private Random generator;
    
    private final OrderGenerator myGenerator;
    
    private Object ls_handle = null;

    private Object ls_buy_handle = null;
    
    private Object ls_sell_handle = null;
    
    private MarketMaker listener = null;
    
    public String getSymbol() {
        return symbol;
    }
    
    public double getReferencePrice() {
        return referencePrice;
    }

    public long getRnd_gen() {
        return rnd_gen;
    }
    
    public Object getLs_handle() {
        return ls_handle;
    }

    public void setLs_handle(Object ls_handle) {
        this.ls_handle = ls_handle;
    }

    public Object getLs_buy_handle() {
        return ls_buy_handle;
    }

    public void setLs_buy_handle(Object ls_buy_handle) {
        this.ls_buy_handle = ls_buy_handle;
    }

    public Object getLs_sell_handle() {
        return ls_sell_handle;
    }

    public void setLs_sell_handle(Object ls_sell_handle) {
        this.ls_sell_handle = ls_sell_handle;
    }
   
    public Stock(String sym, String desc, Logger logger, MarketMaker mm, long rnd_gen) {
        this.symbol = sym;
        this.shortDescription = desc;
        this.logger = logger;
        this.listener = mm;
        this.rnd_gen = rnd_gen;
        this.tradingPhase = "Opening Auction";
        
        generator = new Random(System.currentTimeMillis()*rnd_gen);
                
        this.referencePrice = Math.floor(generator.nextDouble() * 15000)/100;
        
        this.logger.info("Stock " + this.shortDescription + " reference price: " + this.referencePrice);
        
        this.myGenerator = new OrderGenerator(this);
        
        this.BuyOrders = new HashMap<Double, OrderBuy>();
        this.SellOrders = new HashMap<Double, OrderSell>();
        this.ExecList = new ArrayList<Execution>();
        
        int i = 0;
        int max = generator.nextInt(10) + 1;
        while ( i++ < max ) {
            OrderBuy ob = myGenerator.initBuyOrder();
            Double k = new Double(ob.getPrice());
            if (!BuyOrders.containsKey(k)) {
                BuyOrders.put(k,ob);
            } else {
                ((OrderBuy)(BuyOrders.get(k))).add(ob);
            }
            
            this.logger.debug("New initial buy order - " + ob.getPrice() + " x " + ob.getQuantity());
        }
        
        i = 0;
        max = generator.nextInt(10) + 1;
        while ( i++ < max ) {
            OrderSell os = myGenerator.initSellOrder();
            Double k = new Double(os.getPrice());
            if (!SellOrders.containsKey(k)) {
                SellOrders.put(k,os);
            } else {
                ((OrderSell)(SellOrders.get(k))).add(os);
            }
            
            this.logger.debug("New initial sell order - " + os.getPrice() + " x " + os.getQuantity());
        }
    }
    
    public void startSimulation() {
        
        if ( !this.tradingPhase.equals("End Of Day")) {
            myGenerator.startSimulation(false);
        } else {
            myGenerator.startSimulation(true);
        }
        
    }
    
    public void stopSimulator() {
        
        if ( !this.tradingPhase.equals("End Of Day")) {
            myGenerator.stopSimulation(false);
        } else {
            myGenerator.stopSimulation(true);
        }
        
    }
    
    public HashMap<String, String> synUpdate() {
        HashMap<String, String> update = new HashMap<String, String>();

        // Data summary
        update.put("symbol",  this.symbol);
        update.put("short_description", this.shortDescription);
        update.put("reference_price", ""+this.referencePrice);
        update.put("trading_phase", this.tradingPhase);
        
        // Last execution
        update.put("last_price", ""+this.lastPrice);
        update.put("last_qty", ""+this.lastQty);
        if ( this.last == null ) {
            update.put("last", null);
        } else {
            update.put("last", ""+this.last.getTime());
        }
        
        // Market Depth
        update.put("buy_depth",""+this.BuyOrders.size());
        update.put("sell_depth",""+this.SellOrders.size());
               
        return update;
    }
    
    public Iterator<OrderBuy> getBuyOrders() {
        return BuyOrders.values().iterator();
    }

    public Iterator<OrderSell> getSellOrders() {
        return SellOrders.values().iterator();
    }
    
    public void newDemoProposal(OrderBase newOrder) {
        try {
            synchronized (myGenerator) {
                mathcingEngine(newOrder);   
            }
        } catch (Exception e) {
            this.logger.error("Unexpected error in the proposal handling (" + e.getMessage() + ").");
        }
    }
    
    public void newTradingProposal(long qty, double price, boolean buy) throws PriceOutOfBoundException, RejectProposalException, QtyOutOfBoundException {
        synchronized (myGenerator) {
            OrderBase newOrder;
            
            if ( this.tradingPhase.equals("End Of Day") ) {
                throw new RejectProposalException("Market closed");
            }
            
            checkPriceOrder(price);
            checkQtyOrder(qty);
            if ( buy ) {
                newOrder = new OrderBuy(price, qty);
            } else {
                newOrder = new OrderSell(price, qty);
            }
            
            mathcingEngine(newOrder);
        }
    }
    

    public void restartTradingDay() {
        this.tradingPhase = "Opening Auction";
        
        logger.info("New trading day for " + symbol);
        
        this.last = null;
        this.lastPrice = 0.0;
        this.lastQty = 0;
        
        listener.newExecution(symbol);
    }
    
    private void checkPriceOrder(double orderPrice) throws PriceOutOfBoundException {
        
        if ( orderPrice > (this.referencePrice*1.50) ) {
            throw new PriceOutOfBoundException(this.referencePrice, true);
        } else if ( orderPrice < (this.referencePrice*0.50) ) {
            throw new PriceOutOfBoundException(this.referencePrice, true);
        } else {
            return ;
        }
        
    }
    
    private void checkQtyOrder(long qty) throws QtyOutOfBoundException {
        
        if (qty > Constants.MAX_QTY) {
            throw new QtyOutOfBoundException();
        }
        
        return ;
    }
        
    private void mathcingEngine(OrderBase newOrder) {
        if ( newOrder instanceof OrderBuy ) {
            TreeSet<Double> sellLevels = new TreeSet<Double>(SellOrders.keySet());          
            long remainQty = newOrder.getQuantity();
            
            while (remainQty > 0) {
                if (sellLevels.isEmpty()) {
                    Double key = new Double(newOrder.getPrice());
                
                    if ( BuyOrders.containsKey(key)) {
                        if ( ((OrderBuy)(BuyOrders.get(key))).getQuantity() < Constants.FULL_BUCKET ) {
                            ((OrderBuy)(BuyOrders.get(key))).add(newOrder);
                            listener.orderLevelChange(symbol, (OrderBuy)(BuyOrders.get(key)));
                        }
                    } else {
                        BuyOrders.put(key, (OrderBuy)newOrder);
                        listener.newOrderLevel(symbol, newOrder);
                    }
                    
                    remainQty = 0;
                } else {
                    long execQty;
                    double sellLevel = sellLevels.first().doubleValue();
                    
                    logger.debug("High Sell level: " + sellLevels.first());
                    
                    if ( sellLevel <= newOrder.getPrice() ) {
                        execQty = executionS(sellLevels.first(), newOrder);
                        
                        if ( newExecution(execQty, sellLevel) ) {
                        
                            remainQty -= execQty;
                            newOrder.setQuantity(remainQty);
                        
                            logger.debug("Remain qty: " + remainQty);
                            sellLevels = new TreeSet<Double>(SellOrders.keySet());
                        } else {
                            remainQty = 0;
                        }
                    } else {
                        Double key = new Double(newOrder.getPrice());
                                                
                        if ( BuyOrders.containsKey(key)) {
                            if ( ((OrderBuy)(BuyOrders.get(key))).getQuantity() < Constants.FULL_BUCKET ) {
                                ((OrderBuy)(BuyOrders.get(key))).add(newOrder);
                                listener.orderLevelChange(symbol, (OrderBuy)(BuyOrders.get(key)));   
                            }
                        } else {
                            BuyOrders.put(key, (OrderBuy)newOrder);
                            listener.newOrderLevel(symbol, newOrder);
                        }
                        remainQty = 0;
                    }
                }
            }
        } else if ( newOrder instanceof OrderSell ) {
            TreeSet<Double> buyLevels = new TreeSet<Double>(BuyOrders.keySet());
            long remainQty = newOrder.getQuantity();
            
            while (remainQty > 0) {
            
                if (buyLevels.isEmpty()) {
                    Double key = new Double(newOrder.getPrice());
                    
                    if ( SellOrders.containsKey(key)) {
                        if ( ((OrderSell)(SellOrders.get(key))).getQuantity() < Constants.FULL_BUCKET ) {
                            ((OrderSell)(SellOrders.get(key))).add(newOrder);
                            listener.orderLevelChange(symbol, (OrderSell)(SellOrders.get(key)));
                        }
                    } else {
                        SellOrders.put(key, (OrderSell)newOrder);
                        listener.newOrderLevel(symbol, newOrder);
                    }
                    
                    remainQty = 0;
                } else {                
                    long execQty;
                    double buyLevel = buyLevels.last().doubleValue();
                    
                    if ( buyLevel >= newOrder.getPrice() ) {
                        execQty = executionB(buyLevels.last(), newOrder);
                        
                        if ( newExecution(execQty, buyLevel) ) { 
                            remainQty -= execQty;
                            newOrder.setQuantity(remainQty);
                            buyLevels = new TreeSet<Double>(BuyOrders.keySet());
                        } else {
                            remainQty = 0;
                        }
                    } else {
                        Double key = new Double(newOrder.getPrice());
                                                
                        if ( SellOrders.containsKey(key)) {
                            if ( ((OrderSell)(SellOrders.get(key))).getQuantity() < Constants.FULL_BUCKET ) {
                                ((OrderSell)(SellOrders.get(key))).add(newOrder);
                                listener.orderLevelChange(symbol, (OrderSell)(SellOrders.get(key)));
                            }
                        } else {
                            SellOrders.put(key, (OrderSell)newOrder);
                            listener.newOrderLevel(symbol, newOrder);
                        }
                        remainQty = 0;
                    }
                }
            }
            
        } else {
            return ;
        }
        
        return ;
    }
    
    private long executionS(Double sellLevel, OrderBase newOrder) {
        long qty = newOrder.getQuantity();
        
        if ( SellOrders.containsKey(sellLevel) ) {
            OrderSell os = SellOrders.get(sellLevel);
            long sellLevelQty = os.getQuantity();
            
            logger.debug("High Sell level Qty: " + sellLevelQty);
            
            if ( sellLevelQty <= qty ) {
                
                SellOrders.remove(sellLevel);
                listener.deleteOrderLevel(symbol, os);
                
                return sellLevelQty;
            } else {
                SellOrders.get(sellLevel).remove(newOrder);
                listener.orderLevelChange(symbol, os);
            }
        } 
        
        return qty;
    }

    private long executionB(Double buyLevel, OrderBase newOrder) {
        long qty = newOrder.getQuantity();
        
        if ( BuyOrders.containsKey(buyLevel) ) {
            OrderBuy ob = BuyOrders.get(buyLevel);
            long buyLevelQty = ob.getQuantity();
            
            logger.debug("High Buy level Qty: " + buyLevelQty);
            
            if ( buyLevelQty <= qty ) {
                
                BuyOrders.remove(buyLevel);
                listener.deleteOrderLevel(symbol, ob);
                
                return buyLevelQty;
            } else {
                BuyOrders.get(buyLevel).remove(newOrder);
                listener.orderLevelChange(symbol, ob);
            }
        } 
        
        return qty;
    }
    
    private void deleteDueToRefPriceChng() {
        double buyLevel = 0.0;
        double sellLevel = 0.0;
        Set<Double> buyKeys = BuyOrders.keySet();
        Set<Double> sellKeys = SellOrders.keySet();
        Iterator<Double> iterator =  buyKeys.iterator();
        
        while (iterator.hasNext()) {
            buyLevel = (Double)(iterator.next()).doubleValue();
            logger.debug("deleteDueToRefPrice: " + buyLevel + " ... ");
            if ( buyLevel < (this.referencePrice*0.75) ) {
                OrderBase ob =  BuyOrders.get(buyLevel);
                iterator.remove();
                listener.deleteOrderLevel(symbol, ob);
                
                logger.debug(" ... deleted.");
            } else if ( buyLevel > (this.referencePrice*1.25) ) {
                OrderBase ob =  BuyOrders.get(buyLevel);
                iterator.remove();
                listener.deleteOrderLevel(symbol, ob);
                
                logger.debug(" ... deleted.");
            }
        }
        
        iterator =  sellKeys.iterator();
        
        while (iterator.hasNext()) {
            sellLevel = (Double)(iterator.next()).doubleValue();
            if ( sellLevel < (this.referencePrice*0.75) ) {
                OrderBase ob =  SellOrders.get(sellLevel);
                iterator.remove();
                listener.deleteOrderLevel(symbol,ob);
            } else if ( sellLevel > (this.referencePrice*1.25) ) {
                OrderBase ob =  SellOrders.get(sellLevel);
                iterator.remove();
                listener.deleteOrderLevel(symbol, ob);
            }
        }
        
        return ;
    }
    
    private boolean newExecution(long execQty, double price) {
        
        if ( ExecList.size() > Constants.TRADING_DAY_LASTING) {
            // The duration of the trading day depends on the number of deals!?!?!?!?
            // Ok, it's just a demo.
            
            this.tradingPhase = "End Of Day";
            listener.newExecution(symbol);
            
            this.myGenerator.stopSimulation(false);
            ExecList.clear();
            
            // update reference price with the last price
            this.referencePrice = price;
            this.myGenerator.setBasePrice(price);
            
            BuyOrders.clear();
            SellOrders.clear();
            
            listener.endOfDay(symbol);
            
            logger.info("For " + symbol + " starts overnight phase.");
            
            this.myGenerator.overnight();
            
            return false;
        } else {
            if ( ExecList.isEmpty() ) {        
                this.tradingPhase = "Trading";
            }
            Execution exec = new Execution(execQty, price, new Date(System.currentTimeMillis()));
            
            logger.debug("New execution for " + symbol + ": " + execQty + " x" + price);
            
            this.last = exec.getTs();
            this.lastPrice = exec.getPrice();
            this.lastQty = exec.getQty();
            ExecList.add(exec);
            
            
            this.myGenerator.setBasePrice(price);
            
            if ( ((this.referencePrice*1.15) < price ) || ((this.referencePrice*0.85) > price ) ) {
                // Update reference price.
                this.referencePrice = price;
                
                // Delete all trading proposals out of range.
                deleteDueToRefPriceChng();
            }
        
            listener.newExecution(symbol);
            
            return true;
        }
        
    }
}
