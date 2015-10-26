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

import java.util.Map;

public class Constants {
    public static final String LOGGER_CAT = "LS_demos_Logger.MarketDepthDemo.adapters";
    
    public static final long MAX_QTY = 100000;
    
    public static final String BUY_SUFFIX = "_BUYSIDE";
    
    public static final String SELL_SUFFIX = "_SELLSIDE";
    
    public static long TRADING_DAY_LASTING = 750;
    
    public static long RND_GENERATOR_1 = 70;
    
    public static long RND_GENERATOR_2 = 700;
    
    public static long RND_GENERATOR_3 = 7000;
    
    public static long RND_GENERATOR_4 = 25000;
    
    public static void init(Map params) {
        
        if (params.containsKey("TRADING_DAY_LASTING")) {
            TRADING_DAY_LASTING = new Long((String)params.get("TRADING_DAY_LASTING")).longValue();
        }
        if (params.containsKey("RND_GENERATOR_SUPERFAST")) {
            RND_GENERATOR_1 = new Long((String)params.get("RND_GENERATOR_SUPERFAST")).longValue();
        }
        if (params.containsKey("RND_GENERATOR_FAST")) {
            RND_GENERATOR_2 = new Long((String)params.get("RND_GENERATOR_FAST")).longValue();
        }
        if (params.containsKey("RND_GENERATOR_REGULAR")) {
            RND_GENERATOR_3 = new Long((String)params.get("RND_GENERATOR_REGULAR")).longValue();
        }
        if (params.containsKey("RND_GENERATOR_SLOW")) {
            RND_GENERATOR_3 = new Long((String)params.get("RND_GENERATOR_SLOW")).longValue();
        }
        
        return ;
    }

}
