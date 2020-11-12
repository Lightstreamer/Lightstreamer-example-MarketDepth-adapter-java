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


package com.lightstreamer.adapters.MarketDepthDemo;

import java.io.File;
import java.util.Iterator;
import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.lightstreamer.interfaces.data.DataProviderException;
import com.lightstreamer.interfaces.data.ItemEventListener;
import com.lightstreamer.interfaces.data.SmartDataProvider;
import com.lightstreamer.interfaces.data.SubscriptionException;
import com.lightstreamer.adapters.MarketDepthDemo.orders_simulator.*;

public class MarketDepthDataAdapter implements SmartDataProvider, MarketMaker {
    
    private Logger logger;
    
    private ItemEventListener listener;
    
    private Map<String, Stock> availableStocks;
    
    private static final ExecutorService executor = Executors.newSingleThreadExecutor();

    @Override
    public void init(Map params, File configDir) throws DataProviderException {
        
        logger = LogManager.getLogger(Constants.LOGGER_CAT);
        
        availableStocks = new HashMap<String, Stock>();
        
        availableStocks.put("SF_TECH", new Stock("SF_TECH","SuperFast Tech.",logger, this, Constants.RND_GENERATOR_1));
        
        availableStocks.put("AXY_COMP", new Stock("AXY_COMP","AXY COMPANY Ltd",logger, this, Constants.RND_GENERATOR_2));
        
        availableStocks.put("BTTQ_I", new Stock("BTTQ_I", "BTTQ Industries Sa",logger, this, Constants.RND_GENERATOR_3));
        
        availableStocks.put("OS_C", new Stock("OS_C", "Old&Slow Company ...",logger, this, Constants.RND_GENERATOR_4));
        
        
        MarketDepthMetadataAdapter.setDataDadapter(this);
        
        logger.info("MarketDepth Demo Data Adapter start.");
    }
    
    @Override
    public void setListener(ItemEventListener listener) {
     // Save the update listener
     this.listener = listener;
     }
    
    @Override
    public boolean isSnapshotAvailable(String arg0) throws SubscriptionException {
        // We have always the snapshot available from our feed
        return true;
    }
    
    @Override
    public void subscribe(String item, Object handle, boolean arg2)
            throws SubscriptionException {
        
        logger.debug("Received subscribe request for item: " + item + ".");
        
        if (availableStocks.containsKey(item)) {
            Stock myStock = availableStocks.get(item);
            this.listener.smartUpdate(handle, myStock.synUpdate() , true);
            this.listener.smartEndOfSnapshot(handle);
            myStock.setLs_handle(handle);
        } else if (item.endsWith(Constants.BUY_SUFFIX)) {
            String k = item.substring(0, item.indexOf(Constants.BUY_SUFFIX));
            if (availableStocks.containsKey(k)) {
                Stock myStock = availableStocks.get(k);
                sendBuyOrderSnapshot(myStock.getBuyOrders(),handle);
                myStock.setLs_buy_handle(handle);
                
                myStock.startSimulation();
            }
        } else if (item.endsWith(Constants.SELL_SUFFIX)) {
            String k = item.substring(0, item.indexOf(Constants.SELL_SUFFIX));
            if (availableStocks.containsKey(k)) {
                Stock myStock = availableStocks.get(k);
                sendSellOrderSnapshot(myStock.getSellOrders(),handle);
                myStock.setLs_sell_handle(handle);
            }
        } else {
            logger.warn("" + item + " item unrecognized.");
        }
        
        return ;
    }

    @Override
    public void unsubscribe(String item)
            throws SubscriptionException {
        
        logger.debug("Received unsubscribe request for item: " + item + ".");
        
        if (availableStocks.containsKey(item)) {
            (availableStocks.get(item)).setLs_handle(null);
            (availableStocks.get(item)).stopSimulator();
        }
        
        return ;
    }
    
    @Override
    public void subscribe(String portfolioId, boolean arg1) {
        //  Never called on a SmartDataProvider
        assert(false);
    }
    
    private void sendBuyOrderSnapshot(Iterator<OrderBuy> list, Object handle) {
        final Object h = handle;
        if ( listener == null ) {
            return ;
        }
        
        while ( list.hasNext() ) {
            OrderBuy ob = list.next();
            final HashMap<String, String> update = new HashMap<String, String>();
        
            update.put("command", "ADD");
            update.put("key", ""+ob.getPrice());
            update.put("qty", ""+ob.getQuantity());
            
            logger.debug("ADD command for buy order " + ob.getPrice() + " x " + ob.getQuantity());
            
            // If we have a listener create a new Runnable to be used as a task to pass the new update to the listener
            Runnable updateTask = new Runnable() {
                @Override
                public void run() {                   
                    listener.smartUpdate(h, update, true);
                }
            };

            // We add the task on the executor to pass to the listener the actual status
            executor.execute(updateTask);
        }
        
    }

    private void sendSellOrderSnapshot(Iterator<OrderSell> list, Object handle) {
        final Object h = handle;
        if ( listener == null ) {
            return ;
        }
        
        while ( list.hasNext() ) {
            OrderSell os = list.next();
            final HashMap<String, String> update = new HashMap<String, String>();
        
            update.put("command", "ADD");
            update.put("key", ""+os.getPrice());
            update.put("qty", ""+os.getQuantity());
            
            logger.debug("ADD command for sell order " + os.getPrice() + " x " + os.getQuantity());
            
            //If we have a listener create a new Runnable to be used as a task to pass the new update to the listener
            Runnable updateTask = new Runnable() {
                @Override
                public void run() {                   
                    listener.smartUpdate(h, update, true);
                }
            };

            //We add the task on the executor to pass to the listener the actual status
            executor.execute(updateTask);
        }

    }
    
    public void newTradingProposal(String symbol, double price, long qty, boolean buy) throws PriceOutOfBoundException, RejectProposalException, QtyOutOfBoundException {
        if (availableStocks.containsKey(symbol)) {
            Stock myStock = availableStocks.get(symbol);
            
            myStock.newTradingProposal(qty, price, buy);
        }
        
        return ;
    }
    
    @Override
    public void newOrderLevel(String symbol, OrderBase ob) {
        if (availableStocks.containsKey(symbol)) {
            Stock myStock = availableStocks.get(symbol);
            final HashMap<String, String> update = new HashMap<String, String>();
            
            update.put("command", "ADD");
            update.put("key", ""+ob.getPrice());
            update.put("qty", ""+ob.getQuantity());
            
            if (ob instanceof OrderBuy ) {
                logger.debug("ADD command for buy order " + ob.getPrice() + " x " + ob.getQuantity());
                
                listener.smartUpdate(myStock.getLs_buy_handle(), update, false);
            } else {
                logger.debug("ADD command for sell order " + ob.getPrice() + " x " + ob.getQuantity());
                
                listener.smartUpdate(myStock.getLs_sell_handle(), update, false);
            }
            
            listener.smartUpdate(myStock.getLs_handle(), myStock.synUpdate(), false);
        }
    }
    
    @Override
    public void orderLevelChange(String symbol, OrderBase ob) {
        if (availableStocks.containsKey(symbol)) {
            Stock myStock = availableStocks.get(symbol);
            final HashMap<String, String> update = new HashMap<String, String>();
            
            update.put("command", "UPDATE");
            update.put("key", ""+ob.getPrice());
            update.put("qty", ""+ob.getQuantity());
            
            if (ob instanceof OrderBuy ) {
                logger.debug("UPDATE command for buy order " + ob.getPrice() + " x " + ob.getQuantity());
                
                listener.smartUpdate(myStock.getLs_buy_handle(), update, false);
            } else {
                                
                logger.debug("UPDATE command for sell order " + ob.getPrice() + " x " + ob.getQuantity());
                
                listener.smartUpdate(myStock.getLs_sell_handle(), update, false);
            }
        }
    }
    
    @Override
    public void deleteOrderLevel(String symbol, OrderBase ob) {
        if (availableStocks.containsKey(symbol)) {
            Stock myStock = availableStocks.get(symbol);
            final HashMap<String, String> update = new HashMap<String, String>();
        
            update.put("command", "DELETE");
            update.put("key", ""+ ob.getPrice());
        
            if (ob instanceof OrderSell ) {
                logger.debug("DELETE command for sell order " + ob.getPrice() + " x " + ob.getQuantity());
                
                listener.smartUpdate(myStock.getLs_sell_handle(), update, false);
            } else {
                logger.debug("DELETE command for buy order " + ob.getPrice() + " x " + ob.getQuantity());
                
                listener.smartUpdate(myStock.getLs_buy_handle(), update, false);
            }
            
            listener.smartUpdate(myStock.getLs_handle(), myStock.synUpdate(), false);
        }
    }
    
    @Override
    public void newExecution(String symbol) {
        Stock myStock = availableStocks.get(symbol);
        
        logger.debug("Syn update for " + symbol + ".");
        
        listener.smartUpdate(myStock.getLs_handle(), myStock.synUpdate(), false);
        // For Time&Sales push of the new deal.
    }
    
    @Override
    public void endOfDay(String symbol) {
        Stock myStock = availableStocks.get(symbol);
        
        if (myStock != null ) {
            listener.smartUpdate(myStock.getLs_handle(), myStock.synUpdate(), false);
        
            if ( myStock.getLs_buy_handle() != null ) {
                logger.info("Buy side " + myStock.getSymbol() + ": clearSnapshot.");
                
                listener.smartClearSnapshot(myStock.getLs_buy_handle());
            }
            if ( myStock.getLs_sell_handle() != null ) {
                logger.info("Sell side " + myStock.getSymbol() + ": clearSnapshot.");
                
                listener.smartClearSnapshot(myStock.getLs_sell_handle());
            }
        }
    }

}
