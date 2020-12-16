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
    
    @Override
    public void notifyUserMessage(String user, String session, String message) throws CreditsException, NotificationException {
        
        if (message == null) {
            logger.warn("Null message received");
            throw new NotificationException("Null message received");
        }
        
        String[] pieces = message.split("\\|");
            
        try {
            this.handlePortfolioMessage(pieces);
        } catch (PriceOutOfBoundException poobe) {
            throw new CreditsException(-10025, poobe.getMessage());
        } catch (QtyOutOfBoundException qoobe) {
            throw new CreditsException(-10026, qoobe.getMessage());
        } catch (RejectProposalException rpe) {
            throw new CreditsException(-10027, rpe.getMessage());
        } catch (NumberFormatException nfe) {
            throw new CreditsException(-10027, "Number Format Error " + nfe.getMessage());
        }
        
        return ;
    }
    
    private void handlePortfolioMessage(String[] splits) throws PriceOutOfBoundException, RejectProposalException, QtyOutOfBoundException, NumberFormatException {
        
        if ( splits.length == 4 ) {
            double priceP = 0.0;
            long qtyP = 0;

            logger.info("New trading proposal: " + splits[0] + ", " + splits[1] + ", " + splits[2]);   

            priceP = new Double(splits[3]).doubleValue();

            qtyP = new Long(splits[2]).longValue();

            dataAdapter.newTradingProposal(splits[1], priceP, qtyP, (splits[0].equals("BUY")));
        } else {
            logger.warn("Wrong formatted message received. No pieces: " + splits.length);
        }
        
        return ;
    }

}

