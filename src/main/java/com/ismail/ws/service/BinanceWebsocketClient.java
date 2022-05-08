package com.ismail.ws.service;

import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;
import okhttp3.Dispatcher;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;

/**
 * Binance Websocket Client
 * 
 * This service responsible to connect to Binance stream to get live data
 * 
 * TODO
 * - handle disconnect from ws every 24 hours; by reconnecting again
 * - have subscribers dictacte when we subscribe to binance? (but when we run algos; we need to be up). Hence; use a market data service to do so; 
 * 
 * @author ismail
 */
@Service
@EnableScheduling
@Slf4j
public class BinanceWebsocketClient
{
    // TODO: get this from properties
    private static final String binance_ws_bookticker_url = "wss://stream.binance.com:9443/ws/${symbol}@bookTicker";
    //String binance_ws_bookticker_url = "wss://stream.binance.com:9443/ws/btcbusd@bookTicker/btcusdt@bookTicker";

    private OkHttpClient okHttpClient = new OkHttpClient();

    private WebSocket webSocket = null;

    private boolean subscribedToBinance = false;

    // TODO: have subscribers to this binance ws instead
    @Autowired
    private SimpMessagingTemplate simpMsgTemplate;

    public BinanceWebsocketClient()
    {
        Dispatcher dispatcher = new Dispatcher();
        dispatcher.setMaxRequestsPerHost(500);
        dispatcher.setMaxRequests(500);

        okHttpClient = new OkHttpClient.Builder().dispatcher(dispatcher).pingInterval(20, TimeUnit.SECONDS).build();

        subscribeToBinanceBookTicker();
    }

    private void subscribeToBinanceBookTicker()
    {
        log.info("subscribeToBinanceBookTicker()");

        // double check if we are already subscribed
        if (subscribedToBinance == true && webSocket != null)
        {
            log.warn("subscribeToBinanceBookTicker() we are already subscribed!");
            
            return;
        }
        
        try
        {
            String url = binance_ws_bookticker_url.replace("${symbol}", "btcbusd");

            Request request = new Request.Builder().url(url).build();

            WebSocketListener socketListener = new WebSocketListener()
            {
                private int updateCount = 0;

                @Override
                public void onMessage(WebSocket webSocket, String text)
                {
                    updateCount++;

                    if (updateCount % 1000 == 0)
                        log.info("onMessage() " + updateCount + ": " + text);

                    //System.out.println(text);

                    // notify subscribers                
                    simpMsgTemplate.convertAndSend("/topic/public", text);

                }

                @Override
                public void onClosing(final WebSocket webSocket, final int code, final String reason)
                {
                    log.info("onClosing() " + reason);

                }

                @Override
                public void onFailure(WebSocket webSocket, Throwable t, Response response)
                {
                    log.info("onFailure()");

                }

                @Override
                public void onOpen(WebSocket webSocket, Response response)
                {
                    super.onOpen(webSocket, response);

                    log.info("onOpen() " + response.body());
                }

                @Override
                public void onClosed(WebSocket webSocket, int code, String reason)
                {
                    super.onClosed(webSocket, code, reason);

                    log.info("onClosed() " + reason);

                }

            };

            webSocket = okHttpClient.newWebSocket(request, socketListener);

            subscribedToBinance = true;
        }
        catch (Throwable t)
        {
            log.warn("error subscribing to binance ws: " + t.getMessage(), t);
        }
    }

    private void unsubscribeFromBinanceBookTicker()
    {
        if (subscribedToBinance == true)
        {
            if (webSocket != null)
            {
                try
                {
                    webSocket.close(1000, "byebye");
                }
                catch (Throwable t)
                {
                    log.warn("error closing ws: " + t.getMessage(), t);
                }
                
                webSocket = null;
            }
        }

        subscribedToBinance = false;
    }

    /**
     * Binance requires clients to disconnect/connect at least once every 24 hours
     * we do it every 23 hours here
     */
    @Scheduled(initialDelay = 23 * 60 * 60 * 1000, fixedRate = 23 * 60 * 60 * 1000)
    public void regularShutdown()
    {
        log.info("regularShutdown() ");
        
        try
        {
            unsubscribeFromBinanceBookTicker();
            
            Thread.sleep(2000L);
            
            subscribeToBinanceBookTicker();
        }
        catch (Throwable t)
        {
            log.warn("regularShutdown error: " + t.getMessage(), t);
        }
    }
}
