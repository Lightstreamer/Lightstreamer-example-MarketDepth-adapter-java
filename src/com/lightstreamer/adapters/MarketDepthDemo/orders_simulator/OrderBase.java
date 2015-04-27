/*
  Copyright 2015 Weswit Srl

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

public class OrderBase {
    
    private double price;
    
    private long quantity;
    
    public double getPrice() {
        return price;
    }
    public void setPrice(double price) {
        this.price = price;
    }
    
    public long getQuantity() {
        return quantity;
    }
    public void setQuantity(long quantity) {
        this.quantity = quantity;
    }
    
    public OrderBase(double price, long quantity) {
        super();
        this.price = price;
        this.quantity = quantity;
    }
    
    public void add(OrderBase newOrder) {
        this.quantity += newOrder.getQuantity();
    }
    
    public void remove(OrderBase newOrder) {
        this.quantity -= newOrder.getQuantity();
    }
}
