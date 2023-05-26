# Lightstreamer - Market Depth Demo - Java Adapter

The *Market Depth Demo* is a very simple market depth application based on Lightstreamer for its real-time communication needs.<br>

This project includes the server-side part of this demo, the Metadata and Data Adapters implementation.
The client-side part of this demo, a web client front-end, is covered in this project: [Lightstreamer - Market Depth Demo - HTML Client](https://github.com/Lightstreamer/Lightstreamer-example-MarketDepth-client-javascript).

## Details

Market depth is an electronic list of buy and sell orders, organized by price level and updated to reflect real-time market activity.<br>
Market depth data are also known as Level II, Depth Of Market (DOM), and the order book.<br>
<br>
Lightstreamer has semi-native support for market depth, which can be managed very easily via COMMAND mode subscriptions. Basically, the bid and ask lists for an instrument will be two items subscribed to in COMMAND mode.<br>
You will be able to add, update, and remove rows, where each row is identified by a key, which is the price. Lightstreamer will take care of aggregating updates to market depth in the most optimized way, based on bandwidth and frequency constraints.<br>
This way, you will be able to manage full market depths even on unreliable networks and also benefit from update resampling with conflation, if needed.<br>

### Dig the Code

The project is comprised of source code and a deployment example. The main elements of the code are the Data Adapter, the MetaData Adapter, and the simulator of market orders.

#### The Data Adapter

The Data Adpter is implemented by the *MarketDepthDataAdapter* class. The class implement two interfaces:
- *SmartDataProvider* that concerns relations with <b>Lightstreamer server</b>;
- *MarketMaker* that models a very simple set of possible events from the trading market.

#### The Metadata Adapter

The Metadata Adpter is implemented by the *MarketDepthMetadataAdapter* class. The class inherits from the reusable [LiteralBasedProvider](https://github.com/Lightstreamer/Lightstreamer-lib-adapter-java-inprocess/tree/v7.4.1#literalbasedprovider-metadata-adapter) and just adds a simple support for order entry by implementing the NotifyUserMessage method, to handle "sendMessage" requests from the client.
The communication to the Market Orders Simulator, through the Data Adapter, is handled here.

It should not be used as a reference for a real case of client-originated message handling, as no guaranteed delivery and no clustering support is shown.

#### The Market Orders Simulator

The *com.lightstreamer.adapters.MarketDepthDemo.orders_simulator* package contains the implementation of a very simple market orders simulator.<br>
Orders are randomly generated for ask or bid around a reference price that is also randomly determined. Four stocks are managed, each with a different order generation frequency.
The stocks are listed below in ascending order (from slower to faster):
- *Old&Slow Company ...*, Item name: "OS_C";
- *BTTQ Industries Sa* Item name: "BTTQ_I";
- *AXY COMPANY Ltd*, Item name: "AXY_COMP";
- *SuperFast Tech.*, Item name: "SF_TECH".

Once a fixed number of executions has been reached, the stock simulates the "End of Day" phase, in which both the order books, for buy and sell, are cleaned up, and the reference price is recalculated (this phase lasts 15 seconds).<br>
<br>
See the source code comments for further details.

### The Adapter Set Configuration

This Adapter Set is configured and will be referenced by the clients as `MARKETDEPTH`. 

The `adapters.xml` file for the Market Depth Demo, should look like:
```xml
<?xml version="1.0"?>
<adapters_conf id="MARKETDEPTH">

    <metadata_adapter_initialised_first>Y</metadata_adapter_initialised_first>

    <metadata_provider>
        <adapter_class>com.lightstreamer.adapters.MarketDepthDemo.MarketDepthMetadataAdapter</adapter_class>
        
        <param name="RND_GENERATOR_SUPERFAST">70</param>
		  <param name="RND_GENERATOR_FAST">700</param>
		  <param name="RND_GENERATOR_REGULAR">7000</param>
		  <param name="RND_GENERATOR_SLOW">35000</param>
		  
		  <!-- Number of executions after which the "End Of Day" starts. -->
		  <param name="TRADING_DAY_LASTING">5000</param>
        
    </metadata_provider>
    
    <data_provider>
	 
        <adapter_class>com.lightstreamer.adapters.MarketDepthDemo.MarketDepthDataAdapter</adapter_class>
          
    </data_provider>
</adapters_conf>
```

<i>NOTE: not all configuration options of an Adapter Set are exposed by the file suggested above. 
You can easily expand your configurations using the generic template, see the [Java In-Process Adapter Interface Project](https://github.com/Lightstreamer/Lightstreamer-lib-adapter-java-inprocess/tree/v7.4.1#configuration) for details.</i><br>
<br>
Please refer [here](https://lightstreamer.com/docs/ls-server/latest_7_3/General%20Concepts.pdf) for more details about Lightstreamer Adapters.

## Install

If you want to install a version of this demo in your local Lightstreamer Server, follow these steps:
* Download *Lightstreamer Server* (Lightstreamer Server comes with a free non-expiring demo license for 20 connected users) from [Lightstreamer Download page](https://lightstreamer.com/download/), and install it, as explained in the `GETTING_STARTED.TXT` file in the installation home directory.
* Get the `deploy.zip` file of the ["Release for Lightstreamer 7.3" release](https://github.com/Lightstreamer/Lightstreamer-example-MarketDepth-adapter-java/releases), unzip it, and copy the just unzipped `MarketDepthDemo` folder into the `adapters` folder of your Lightstreamer Server installation.
* [Optional] Customize the logging settings in log4j configuration file: `MarketDepthDemo/classes/log4j2.xml`.
* Launch Lightstreamer Server.
* Test the Adapter, launching one of the clients listed in [Clients Using This Adapter](#clients-using-this-adapter).


## Build

To build your own version of `example-MarketDepth-adapter-java-x.y.z-SNAPSHOT.jar` instead of using the one provided in the `deploy.zip` file from the [Install](#install) section above, you have two options:
either use [Maven](https://maven.apache.org/) (or other build tools) to take care of dependencies and building (recommended) or gather the necessary jars yourself and build it manually.
For the sake of simplicity only the Maven case is detailed here.

### Maven

You can easily build and run this application using Maven through the pom.xml file located in the root folder of this project. As an alternative, you can use an alternative build tool (e.g. Gradle, Ivy, etc.) by converting the provided pom.xml file.

Assuming Maven is installed and available in your path you can build the demo by running
```sh 
 mvn install dependency:copy-dependencies 
```

## See Also

### Clients Using This Adapter

* [Lightstreamer - Market Depth Demo - JavaScript Client](https://github.com/Lightstreamer/Lightstreamer-example-MarketDepth-client-javascript)

### Related Projects

* [LiteralBasedProvider Metadata Adapter](https://github.com/Lightstreamer/Lightstreamer-lib-adapter-java-inprocess#literalbasedprovider-metadata-adapter)
* [Lightstreamer - Stock-List Demos - HTML Clients](https://github.com/Lightstreamer/Lightstreamer-example-StockList-client-javascript)
* [Lightstreamer - Portfolio Demos - HTML Clients](https://github.com/Lightstreamer/Lightstreamer-example-Portfolio-client-javascript)

## Lightstreamer Compatibility Notes

- Compatible with Lightstreamer SDK for Java In-Process Adapters version 7.3 to 7.4.
- For a version of this example compatible with Lightstreamer SDK for Java Adapters version 6.0, please refer to [this tag](https://github.com/Lightstreamer/Lightstreamer-example-MarketDepth-adapter-java/tree/pre_mvn).
