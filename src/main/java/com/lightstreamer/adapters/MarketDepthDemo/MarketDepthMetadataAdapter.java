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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.io.File;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.lightstreamer.adapters.metadata.LiteralBasedProvider;
import com.lightstreamer.interfaces.metadata.CreditsException;
import com.lightstreamer.interfaces.metadata.MetadataProviderException;
import com.lightstreamer.interfaces.metadata.NotificationException;

public class MarketDepthMetadataAdapter extends LiteralBasedProvider {

    private Logger logger;
    
    private static MarketDepthDataAdapter dataAdapter;
    
    @Override
    public void init(Map params, File configDir) throws MetadataProviderException {
        
        logger = LogManager.getLogger(Constants.LOGGER_CAT);
        
        Constants.init(params);
        
        logger.debug("Rnd generator time (1): " + Constants.RND_GENERATOR_1);
        logger.debug("Rnd generator time (2):" + Constants.RND_GENERATOR_2);
        logger.debug("Rnd generator time (3): " + Constants.RND_GENERATOR_3);
        logger.debug("Rnd generator time (4): " + Constants.RND_GENERATOR_4);
        
        logger.info("MarketDepth Demo Metadata Adapter start.");
    }
    
    public static void setDataDadapter(MarketDepthDataAdapter d) {
        dataAdapter = d;
    }
    

    @Override
    public boolean wantsTablesNotification(java.lang.String user) {
        return false;
    }

    private static ExecutorService messageProcessingPool = Executors.newCachedThreadPool();

    @Override
    public CompletableFuture<String> notifyUserMessage(String user, String session, String message) throws CreditsException, NotificationException {

        //NOTE: since the order processing is potentially blocking (in a real scenario), we have 
        //configured a dedicated ExecutorService. Moreover, to provide backpressure to the Server
        //when the number of pending operations is too high, we have properly configured the
        //messages thread pool in the adapters.xml configuration file for this adapter.

        if (message == null) {
            logger.warn("Null message received");
            throw new NotificationException("Null message received");
        }
        
        CompletableFuture<String> future = new CompletableFuture<>();
        messageProcessingPool.execute(() -> {
            try {
                String[] pieces = message.split("\\|");
                this.handlePortfolioMessage(pieces);
                future.complete(null);
            } catch (PriceOutOfBoundException poobe) {
                future.completeExceptionally(new CreditsException(-10025, poobe.getMessage()));
            } catch (QtyOutOfBoundException qoobe) {
                future.completeExceptionally(new CreditsException(-10026, qoobe.getMessage()));
            } catch (RejectProposalException rpe) {
                future.completeExceptionally(new CreditsException(-10027, rpe.getMessage()));
            } catch (NumberFormatException nfe) {
                future.completeExceptionally(new CreditsException(-10027, "Number Format Error " + nfe.getMessage()));
            } catch (Throwable t) {
                future.completeExceptionally(t);
            }
            assert (future.isDone());
        });

        return future;
    }
    
    private void handlePortfolioMessage(String[] splits) throws PriceOutOfBoundException, RejectProposalException, QtyOutOfBoundException, NumberFormatException, NotificationException {
        
        if ( splits.length == 4 ) {
            double priceP = 0.0;
            long qtyP = 0;

            logger.info("New trading proposal: " + splits[0] + ", " + splits[1] + ", " + splits[2]);   

            priceP = new Double(splits[3]).doubleValue();

            qtyP = new Long(splits[2]).longValue();

            String op = splits[0];
            if (op.equals("BUY") || op.equals("SELL")) {
                dataAdapter.newTradingProposal(splits[1], priceP, qtyP, op.equals("BUY"));
            } else {
                throw new NotificationException("Wrong operation received");
            }
        } else {
            logger.warn("Wrong formatted message received. No pieces: " + splits.length);
            throw new NotificationException("Wrong message received");
        }
    }

}

