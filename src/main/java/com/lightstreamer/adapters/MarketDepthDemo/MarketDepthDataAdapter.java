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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.lightstreamer.adapter.common.InitException;
import com.lightstreamer.adapter.common.Mode;
import com.lightstreamer.adapter.data.DataAdapter;
import com.lightstreamer.adapter.data.ItemListener;
import com.lightstreamer.adapter.data.ItemManager;
import com.lightstreamer.adapter.data.SubscriptionException;
import com.lightstreamer.adapters.MarketDepthDemo.orders_simulator.*;

public class MarketDepthDataAdapter implements DataAdapter, MarketMaker {
    
    private static final String LOGGER_CAT = "LS_demos_Logger.MarketDepthDemo.adapters";

    private static final String BUY_SUFFIX = "_BUYSIDE";
    
    private static final String SELL_SUFFIX = "_SELLSIDE";

    private Logger logger;
    
    private Map<String, Stock> availableStocks;
    
    private ExecutorService executor = Executors.newSingleThreadExecutor();

    @Override
    public void init(Map<String, String> params, File configDir) throws InitException {
        
        logger = LogManager.getLogger(LOGGER_CAT);

        logger.info("MarketDepth Demo Data Adapter start.");
    }

    public void configure(Map<String, String> params) {

        long tradingDayLasting = 750;
        long rndGenerator1 = 70;
        long rndGenerator2 = 700;
        long rndGenerator3 = 7000;
        long rndGenerator4 = 25000;

        if (params.containsKey("TRADING_DAY_LASTING")) {
            tradingDayLasting = Long.parseLong(params.get("TRADING_DAY_LASTING"));
        }
        if (params.containsKey("RND_GENERATOR_SUPERFAST")) {
            rndGenerator1 = Long.parseLong(params.get("RND_GENERATOR_SUPERFAST"));
        }
        if (params.containsKey("RND_GENERATOR_FAST")) {
            rndGenerator2 = Long.parseLong(params.get("RND_GENERATOR_FAST"));
        }
        if (params.containsKey("RND_GENERATOR_REGULAR")) {
            rndGenerator3 = Long.parseLong(params.get("RND_GENERATOR_REGULAR"));
        }
        if (params.containsKey("RND_GENERATOR_SLOW")) {
            rndGenerator4 = Long.parseLong(params.get("RND_GENERATOR_SLOW"));
        }

        logger.debug("Rnd generator time (1): " + rndGenerator1);
        logger.debug("Rnd generator time (2): " + rndGenerator2);
        logger.debug("Rnd generator time (3): " + rndGenerator3);
        logger.debug("Rnd generator time (4): " + rndGenerator4);
        
        availableStocks = new HashMap<String, Stock>();
        
        availableStocks.put("SF_TECH", new Stock("SF_TECH","SuperFast Tech.",logger, this, rndGenerator1, tradingDayLasting));
        
        availableStocks.put("AXY_COMP", new Stock("AXY_COMP","AXY COMPANY Ltd",logger, this, rndGenerator2, tradingDayLasting));
        
        availableStocks.put("BTTQ_I", new Stock("BTTQ_I", "BTTQ Industries Sa",logger, this, rndGenerator3, tradingDayLasting));
        
        availableStocks.put("OS_C", new Stock("OS_C", "Old&Slow Company ...",logger, this, rndGenerator4, tradingDayLasting));
    }

    @Override
    public CompletionStage<ItemManager> getItemManager(String itemName)
            throws SubscriptionException {
        
        logger.debug("Received subscribe request for item: " + itemName + ".");
        
        if (availableStocks.containsKey(itemName)) {
            return CompletableFuture.completedFuture(new MarketDepthItemManager(itemName, Mode.MERGE));
        } else if (itemName.endsWith(BUY_SUFFIX)) {
            String k = itemName.substring(0, itemName.indexOf(BUY_SUFFIX));
            if (availableStocks.containsKey(k)) {
                return CompletableFuture.completedFuture(new MarketDepthItemManager(itemName, Mode.COMMAND));
            }
        } else if (itemName.endsWith(SELL_SUFFIX)) {
            String k = itemName.substring(0, itemName.indexOf(SELL_SUFFIX));
            if (availableStocks.containsKey(k)) {
                return CompletableFuture.completedFuture(new MarketDepthItemManager(itemName, Mode.COMMAND));
            }
        }
        
        logger.warn("" + itemName + " item unrecognized.");
        CompletableFuture<ItemManager> f = new CompletableFuture<>();
        f.completeExceptionally(new SubscriptionException("No such item: " + itemName));
        return f;
    }

    @Override
    public void shutdown() {
        for (Stock stock : availableStocks.values()) {
            stock.stopSimulator();
        }
        executor.shutdown();
    }

    private class MarketDepthItemManager implements ItemManager {
        private final String itemName;
        private final Mode mode;
        private volatile ItemListener listener;

        MarketDepthItemManager(String itemName, Mode mode) {
            this.itemName = itemName;
            this.mode = mode;
        }

        @Override
        public Mode getSupportedMode() {
            return mode;
        }

        @Override
        public boolean isSnapshotAvailable() {
            // We have always the snapshot available from our feed
            return true;
        }

        @Override
        public CompletionStage<Void> subscribe(ItemListener listener) {
            this.listener = listener;
            if (availableStocks.containsKey(itemName)) {
                Stock myStock = availableStocks.get(itemName);
                listener.snapshot(myStock.synUpdate(), null);
                myStock.setLs_handle(listener);
            } else if (itemName.endsWith(BUY_SUFFIX)) {
                String k = itemName.substring(0, itemName.indexOf(BUY_SUFFIX));
                if (availableStocks.containsKey(k)) {
                    Stock myStock = availableStocks.get(k);
                    sendBuyOrderSnapshot(myStock.getBuyOrders(), listener);
                    myStock.setLs_buy_handle(listener);
                    
                    myStock.startSimulation();
                }
            } else if (itemName.endsWith(SELL_SUFFIX)) {
                String k = itemName.substring(0, itemName.indexOf(SELL_SUFFIX));
                if (availableStocks.containsKey(k)) {
                    Stock myStock = availableStocks.get(k);
                    sendSellOrderSnapshot(myStock.getSellOrders(), listener);
                    myStock.setLs_sell_handle(listener);
                }
            }
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public CompletionStage<Void> unsubscribe() {
            this.listener = null;
            logger.debug("Received unsubscribe request for item: " + itemName + ".");
            
            if (availableStocks.containsKey(itemName)) {
                (availableStocks.get(itemName)).setLs_handle(null);
                (availableStocks.get(itemName)).stopSimulator();
            } else if (itemName.endsWith(BUY_SUFFIX)) {
                String k = itemName.substring(0, itemName.indexOf(BUY_SUFFIX));
                if (availableStocks.containsKey(k)) {
                    (availableStocks.get(k)).setLs_buy_handle(null);
                }
            } else if (itemName.endsWith(SELL_SUFFIX)) {
                String k = itemName.substring(0, itemName.indexOf(SELL_SUFFIX));
                if (availableStocks.containsKey(k)) {
                    (availableStocks.get(k)).setLs_sell_handle(null);
                }
            }
            return CompletableFuture.completedFuture(null);
        }

        private void sendBuyOrderSnapshot(Iterator<OrderBuy> list, ItemListener itemListener) {
            
            if ( itemListener == null ) {
                return ;
            }

            final ItemListener l = itemListener;

            while ( list.hasNext() ) {
                OrderBuy ob = list.next();
                final HashMap<String, String> update = new HashMap<String, String>();
            
                update.put(ItemListener.COMMAND_FIELD, ItemListener.ADD_COMMAND);
                update.put(ItemListener.KEY_FIELD, ""+ob.getPrice());
                update.put("qty", ""+ob.getQuantity());
                
                logger.debug("ADD command for buy order " + ob.getPrice() + " x " + ob.getQuantity());
                
                // If we have a listener create a new Runnable to be used as a task to pass the new update to the listener
                Runnable updateTask = new Runnable() {
                    @Override
                    public void run() {                   
                        l.snapshot(update, null);
                    }
                };

                // We add the task on the executor to pass to the listener the actual status
                executor.execute(updateTask);
            }
            
            // Signal end of snapshot for COMMAND mode
            executor.execute(() -> l.endOfSnapshot());
        }

        private void sendSellOrderSnapshot(Iterator<OrderSell> list, ItemListener itemListener) {
            
            if ( itemListener == null ) {
                return ;
            }
            
            final ItemListener l = itemListener;
            
            while ( list.hasNext() ) {
                OrderSell os = list.next();
                final HashMap<String, String> update = new HashMap<String, String>();
            
                update.put(ItemListener.COMMAND_FIELD, ItemListener.ADD_COMMAND);
                update.put(ItemListener.KEY_FIELD, ""+os.getPrice());
                update.put("qty", ""+os.getQuantity());
                
                logger.debug("ADD command for sell order " + os.getPrice() + " x " + os.getQuantity());
                
                //If we have a listener create a new Runnable to be used as a task to pass the new update to the listener
                Runnable updateTask = new Runnable() {
                    @Override
                    public void run() {                   
                        l.snapshot(update, null);
                    }
                };

                //We add the task on the executor to pass to the listener the actual status
                executor.execute(updateTask);
            }
            
            // Signal end of snapshot for COMMAND mode
            executor.execute(() -> l.endOfSnapshot());
        }
    }
    
    public void newTradingProposal(String symbol, double price, long qty, boolean buy) throws PriceOutOfBoundException, RejectProposalException, QtyOutOfBoundException {
        if (availableStocks.containsKey(symbol)) {
            Stock myStock = availableStocks.get(symbol);
            
            myStock.newTradingProposal(qty, price, buy);
        } else {
            throw new RejectProposalException("Symbol not available");
        }
    }
    
    @Override
    public void newOrderLevel(String symbol, OrderBase ob) {
        if (availableStocks.containsKey(symbol)) {
            Stock myStock = availableStocks.get(symbol);
            final HashMap<String, String> update = new HashMap<String, String>();
            
            if ( myStock.getLs_handle() == null ) {
                return ;
            }

            update.put(ItemListener.COMMAND_FIELD, ItemListener.ADD_COMMAND);
            update.put(ItemListener.KEY_FIELD, ""+ob.getPrice());
            update.put("qty", ""+ob.getQuantity());
            
            if (ob instanceof OrderBuy ) {
                logger.debug("ADD command for buy order " + ob.getPrice() + " x " + ob.getQuantity());
                
                ItemListener buyListener = myStock.getLs_buy_handle();
                if ( buyListener != null ) {
                    buyListener.update(update);
                }    
            } else {
                logger.debug("ADD command for sell order " + ob.getPrice() + " x " + ob.getQuantity());
                
                ItemListener sellListener = myStock.getLs_sell_handle();
                if ( sellListener != null ) {
                    sellListener.update(update);
                }
            }
            
            myStock.getLs_handle().update(myStock.synUpdate());
        }
    }
    
    @Override
    public void orderLevelChange(String symbol, OrderBase ob) {
        if (availableStocks.containsKey(symbol)) {
            Stock myStock = availableStocks.get(symbol);
            final HashMap<String, String> update = new HashMap<String, String>();

            update.put(ItemListener.COMMAND_FIELD, ItemListener.UPDATE_COMMAND);
            update.put(ItemListener.KEY_FIELD, ""+ob.getPrice());
            update.put("qty", ""+ob.getQuantity());
            
            if (ob instanceof OrderBuy ) {
                logger.debug("UPDATE command for buy order " + ob.getPrice() + " x " + ob.getQuantity());
                
                ItemListener buyListener = myStock.getLs_buy_handle();
                if ( buyListener != null ) {
                    buyListener.update(update);
                }
            } else {
                                
                logger.debug("UPDATE command for sell order " + ob.getPrice() + " x " + ob.getQuantity());
                ItemListener sellListener = myStock.getLs_sell_handle();
                if ( sellListener != null ) {
                    sellListener.update(update);
                }
            }
        }
    }
    
    @Override
    public void deleteOrderLevel(String symbol, OrderBase ob) {
        if (availableStocks.containsKey(symbol)) {
            Stock myStock = availableStocks.get(symbol);
            final HashMap<String, String> update = new HashMap<String, String>();
        
            if ( myStock.getLs_handle() == null ) {
                return ;
            }

            update.put(ItemListener.COMMAND_FIELD, ItemListener.DELETE_COMMAND);
            update.put(ItemListener.KEY_FIELD, ""+ ob.getPrice());
        
            if (ob instanceof OrderSell ) {
                logger.debug("DELETE command for sell order " + ob.getPrice() + " x " + ob.getQuantity());
                
                ItemListener sellListener = myStock.getLs_sell_handle();
                if ( sellListener != null ) {
                    sellListener.update(update);
                }
            } else {
                logger.debug("DELETE command for buy order " + ob.getPrice() + " x " + ob.getQuantity());
                
                ItemListener buyListener = myStock.getLs_buy_handle();
                if ( buyListener != null ) {
                    buyListener.update(update);
                }
            }
            
            myStock.getLs_handle().update(myStock.synUpdate());
        }
    }
    
    @Override
    public void newExecution(String symbol) {
        Stock myStock = availableStocks.get(symbol);
        
        logger.debug("Syn update for " + symbol + ".");
        
        if ( myStock.getLs_handle() != null ) {
            myStock.getLs_handle().update(myStock.synUpdate());
        }
        
        // For Time&Sales push of the new deal.
    }
    
    @Override
    public void endOfDay(String symbol) {
        Stock myStock = availableStocks.get(symbol);
        
        if (myStock != null ) {
            if ( myStock.getLs_handle() != null ) {
                myStock.getLs_handle().update(myStock.synUpdate());
            }
        
            ItemListener buyListener = myStock.getLs_buy_handle();
            if ( buyListener != null ) {
                logger.info("Buy side " + myStock.getSymbol() + ": clearSnapshot.");
                
                buyListener.clearSnapshot();
            }
            ItemListener sellListener = myStock.getLs_sell_handle();
            if ( sellListener != null ) {
                logger.info("Sell side " + myStock.getSymbol() + ": clearSnapshot.");
                
                sellListener.clearSnapshot();
            }
        }
    }

}
