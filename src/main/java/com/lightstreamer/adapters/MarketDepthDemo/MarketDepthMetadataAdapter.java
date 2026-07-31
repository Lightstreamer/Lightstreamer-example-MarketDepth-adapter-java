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
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.io.File;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.lightstreamer.adapter.common.InitException;
import com.lightstreamer.adapter.metadata.CreditsException;
import com.lightstreamer.adapter.metadata.LoginInfo;
import com.lightstreamer.adapter.metadata.MessageException;
import com.lightstreamer.adapter.metadata.MetadataAdapter;
import com.lightstreamer.adapter.metadata.SessionRequestHandler;
import com.lightstreamer.adapter.metadata.SubscriptionHandler;
import com.lightstreamer.adapter.metadata.SubscriptionInfo;

public class MarketDepthMetadataAdapter implements MetadataAdapter {

    private static final String LOGGER_CAT = "LS_demos_Logger.MarketDepthDemo.adapters";

    private Logger logger;
    
    private MarketDepthDataAdapter dataAdapter;

    private String dataAdapterName;

    private ExecutorService messageProcessingPool = Executors.newCachedThreadPool();

    private Map<String, String> params;

    @Override
    public void init(Map<String, String> params, File configDir) throws InitException {
        
        logger = LogManager.getLogger(LOGGER_CAT);
        
        this.params = params;

        logger.info("MarketDepth Demo Metadata Adapter start.");
    }

    @Override
    public void onDataAdapterAvailable(String dataAdapterName, Object dataAdapter) {
        if (!(dataAdapter instanceof MarketDepthDataAdapter)) {
            return;
        }
        if (this.dataAdapter != null) {
            logger.warn("A MarketDepthDataAdapter instance is already available as \"" 
                    + this.dataAdapterName + "\"; ignoring \"" + dataAdapterName + "\".");
            return;
        }
        this.dataAdapterName = dataAdapterName;
        this.dataAdapter = (MarketDepthDataAdapter) dataAdapter;
        this.dataAdapter.configure(params);
    }

    @Override
    public void onDataAdapterUnavailable(String dataAdapterName) {
        if (dataAdapterName.equals(this.dataAdapterName)) {
            this.dataAdapter = null;
            this.dataAdapterName = null;
        }
    }

    @Override
    public CompletionStage<SessionRequestHandler> loginAsync(LoginInfo loginInfo) {
        //////////////////////////////////////////////////////////////////////////
        // WARNING: No authentication logic is implemented; any login is accepted.
        // This is suitable for demos only and MUST NOT be used in production.
        //////////////////////////////////////////////////////////////////////////
        return CompletableFuture.completedFuture(new SessionRequestHandler() {

            @Override
            public Double getAllowedMaxBandwidth() {
                return null;
            }

            @Override
            public CompletionStage<SubscriptionHandler> newSubscription(SubscriptionInfo info) {
                return CompletableFuture.completedFuture(new SubscriptionHandler(info));
            }

            @Override
            public CompletionStage<String> userMessageAsync(String message) throws CreditsException, MessageException {

                //NOTE: since the order processing is potentially blocking (in a real scenario), we have 
                //configured a dedicated ExecutorService. Moreover, to provide backpressure to the Server
                //when the number of pending operations is too high, we have properly configured the
                //messages thread pool in the adapters.xml configuration file for this adapter.

                if (message == null) {
                    logger.warn("Null message received");
                    throw new MessageException("Null message received");
                }
                
                CompletableFuture<String> future = new CompletableFuture<>();
                messageProcessingPool.execute(() -> {
                    try {
                        String[] pieces = message.split("\\|");
                        handlePortfolioMessage(pieces);
                        future.complete("SUBMITTED");
                    } catch (PriceOutOfBoundException poobe) {
                        future.completeExceptionally(new CreditsException(-10025, poobe.getMessage()));
                    } catch (QtyOutOfBoundException qoobe) {
                        future.completeExceptionally(new CreditsException(-10026, qoobe.getMessage()));
                    } catch (RejectProposalException rpe) {
                        future.completeExceptionally(new CreditsException(-10027, rpe.getMessage()));
                    } catch (NumberFormatException nfe) {
                        future.completeExceptionally(new CreditsException(-10027, "Number Format Error " + nfe.getMessage()));
                    } catch (MessageException me) {
                        future.completeExceptionally(me);
                    } catch (Throwable t) {
                        future.completeExceptionally(t);
                    }
                    assert (future.isDone());
                });

                return future;
            }

            private void handlePortfolioMessage(String[] splits) throws PriceOutOfBoundException, RejectProposalException, QtyOutOfBoundException, NumberFormatException, MessageException {
                
                if ( splits.length == 4 ) {
                    double priceP = 0.0;
                    long qtyP = 0;

                    logger.info("New trading proposal: " + splits[0] + ", " + splits[1] + ", " + splits[2]);   

                    priceP = Double.parseDouble(splits[3]);

                    qtyP = Long.parseLong(splits[2]);

                    String op = splits[0];
                    if (op.equals("BUY") || op.equals("SELL")) {
                        dataAdapter.newTradingProposal(splits[1], priceP, qtyP, op.equals("BUY"));
                    } else {
                        throw new MessageException("Wrong operation received");
                    }
                } else {
                    logger.warn("Wrong formatted message received. No pieces: " + splits.length);
                    throw new MessageException("Wrong message received");
                }
            }
        });
    }

    @Override
    public void shutdown() {
        messageProcessingPool.shutdown();
    }

}
