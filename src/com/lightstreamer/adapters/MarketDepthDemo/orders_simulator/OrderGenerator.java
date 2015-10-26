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

import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

import com.lightstreamer.adapters.MarketDepthDemo.Constants;

public class OrderGenerator {
    
    private Random myGenerator;
    
    private Stock stockbase;
    
    private double basePrice = 0.0;
    
    private Timer dispatcher = null;
    
    private long frequencyFactor = 50;
    
    private boolean active = false;
    
    private boolean restart = true;
    
    public void setBasePrice(double basePrice) {
        this.basePrice = basePrice;
    }

    public OrderGenerator(Stock s) {
        this.basePrice = s.getReferencePrice();
        this.frequencyFactor = s.getRnd_gen();
        this.stockbase = s;
        
        this.myGenerator = new Random(System.currentTimeMillis()*this.frequencyFactor);
    }
    
    public OrderBuy initBuyOrder() {
        double rnd;
        long base = (long)Math.ceil(this.basePrice * 100);
        
        rnd = myGenerator.nextDouble();
        while ( (rnd < 0.75) || (rnd > 0.98) ) {
            rnd = myGenerator.nextDouble();    
        }
        
        return new OrderBuy(Math.ceil(base*rnd)/100, myGenerator.nextInt(((int)Constants.MAX_QTY)));
    }
    
    public OrderSell initSellOrder() {
        double rnd;
        long base = (long)Math.ceil(this.basePrice * 100);
        
        rnd = myGenerator.nextDouble() + 1.0;
        while ( (rnd < 1.02) || (rnd > 1.25) ) {
            rnd = myGenerator.nextDouble() + 1.0;    
        }
        
        return new OrderSell(Math.ceil(base*rnd)/100, myGenerator.nextInt(((int)Constants.MAX_QTY)));
    }

    public OrderBase negOrder() {
        double rnd,newPrice;
        long base = (long)Math.ceil(this.basePrice * 100);
        
        if ( basePrice == 0 ) {
            return null;
        }
        
        if ( this.myGenerator.nextDouble() < 0.5 ) {
            // BUY ORDER
            
            //rnd = myGenerator.nextDouble() + 0.1;
            rnd = myGenerator.nextGaussian() + 1.0;
            while ( (rnd > 1.1) || (rnd < 0.75) ) {
                //rnd = myGenerator.nextDouble() + 0.1;
                rnd = myGenerator.nextGaussian() + 1.0;
            }
            newPrice = Math.ceil(base*rnd)/100;
            if (newPrice < 0.20) {
                newPrice = 0.20;
            } else if (newPrice > 200.0) {
                newPrice = 200.0;
            }
             
            return new OrderBuy(newPrice, myGenerator.nextInt(((int)Constants.MAX_QTY)));
        } else {
            // SELL ORDER
            
            //rnd = myGenerator.nextDouble() + 0.25;
            rnd = myGenerator.nextGaussian() + 1.0;
            while ( (rnd > 1.25) || (rnd < 0.9) ) {
                //rnd = myGenerator.nextDouble() + 0.25;
                rnd = myGenerator.nextGaussian() + 1.0;
            }
            newPrice = Math.ceil(base*rnd)/100;
            if (newPrice < 0.20) {
                newPrice = 0.20;
            } else if (newPrice > 200.0) {
                newPrice = 200.0;
            }
            
            return new OrderSell(newPrice, myGenerator.nextInt(((int)Constants.MAX_QTY)));
        }
    }
    
    public void startSimulation(boolean eod) {
        
        if (eod) {
            restart = true;
        } else {
        
            if (dispatcher == null) {
                dispatcher = new Timer();
            }
                    
            active = true;
            
            scheduleGenerator((int)Math.ceil(myGenerator.nextDouble()*this.frequencyFactor)+1500);
            
        }
        return ;
    }
    
    public void stopSimulation(boolean eod) {
        if (eod) {
            restart = false;
        } else {
            
            active = false;
        
            if (dispatcher != null) {
                dispatcher.cancel();
                dispatcher = null;
            }
            
        }
    }
    
    public void overnight() {
        dispatcher = new Timer();
        restart = true;
        
        dispatcher.schedule(new TimerTask() {
            @Override
            public void run() {
                synchronized (this) {
                    stockbase.restartTradingDay();
                    if (restart) {
                        startSimulation(false);
                    }
                }
            }
        },  15000);
    }
    
    private void scheduleGenerator(int waitTime) {
        
        if ( active ) {
            
            dispatcher.schedule(new TimerTask() {
                @Override
                public void run() {
                    int nextWaitTime;
                    try {
                        OrderBase ob = negOrder();
                        stockbase.newDemoProposal(ob);
                    } catch (Exception e) {
                        // Skip.
                    }
                    
                    nextWaitTime = (int)Math.ceil(myGenerator.nextDouble()*frequencyFactor);
                    scheduleGenerator(nextWaitTime);
                }
            }, waitTime);
            
        }
    }

}
