/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package biz.szydlowski.logwebse;


import static biz.szydlowski.logwebse.LogSearchEngineServer.allowedConn;
import biz.szydlowski.utils.MonitorThread;
import biz.szydlowski.utils.RejectedExecutionHandlerImpl;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Iterator;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 *
 * @author Dominik
 */
public class LogSEWebServer extends Thread {
   
    protected int serverPort   = 8080;
    protected ServerSocket serverSocket = null;
    protected boolean isStopped    = false;
    protected Thread runningThread = null;
    private ThreadPoolExecutor executorPool = null; 
    //private  Socket clientSocket = null;
    
    static final Logger logger =  LogManager.getLogger(LogSEWebServer.class);
    
    public LogSEWebServer (int port){
        this.serverPort = port;
    }

    @Override
    public void run(){
        synchronized(this){
            this.runningThread = Thread.currentThread();
            this.runningThread.setName("WebServer");
        }
        try {
            openServerSocket();           
        } catch (Exception e){
            isStopped = true;
            logger.error(e);
            
        } 
        
        RejectedExecutionHandlerImpl rejectionHandler = new RejectedExecutionHandlerImpl();
        
        
        ThreadFactory threadFactory = Executors.defaultThreadFactory();
        //creating the ThreadPoolExecutor
        executorPool = new ThreadPoolExecutor(4, 20, 300, TimeUnit.SECONDS, new ArrayBlockingQueue<>(10), threadFactory, rejectionHandler);
        executorPool.allowCoreThreadTimeOut(true);
                     
        
         //start the monitoring thread
        MonitorThread monitor = new MonitorThread(executorPool, 60);
        Thread monitorThread = new Thread(monitor);
        monitorThread.setName("monitorThread");
        monitorThread.start();
        
        while(!isStopped()){ 
            boolean api=false;
            Socket clientSocket = null;
                 
            try {
               clientSocket = this.serverSocket.accept();
               clientSocket.setKeepAlive(true);
              
               String address = clientSocket.getInetAddress().getHostAddress();
                 
               Iterator<String> iteratorweb = allowedConn.iterator();
              
               while(iteratorweb.hasNext()){
                    String obj = iteratorweb.next();
                    if (obj.equals("all")){
                         api=true;
                         break;
                    } else {
                        if (obj.startsWith("*") || obj.endsWith("*")){
                            //gwiazdka
                            Pattern r = Pattern.compile(obj);
                            if ( r.matcher(address).find()){
                              api=true;
                              break;
                            } 
                            
                        } else {
                             if (obj.equalsIgnoreCase(address)){
                                 api=true;
                                 break;
                             }
                        }
                       
                    }
               }  
              //A client has connected to this server. Send welcome message
            } catch (IOException e) {
                if(isStopped()) {
                    logger.info("Server Stopped.") ;
                    return;
                }
                throw new RuntimeException("Error accepting client connection", e);
            }   
        
             executorPool.execute(new Thread(new ServerWorkerRunnable(clientSocket, api, serverPort)));
      
         
        }
        logger.info("Server Stopped.") ;
    }


    private synchronized boolean isStopped() {
        return this.isStopped;
    }
   
    public void stopSever() {
        this.isStopped = true;
        try {
            logger.info("Currently active threads: " + Thread.activeCount());
            if (serverSocket!=null) serverSocket.close();
            interruptAll();
            this.serverSocket.close();
        } catch (IOException e) {
            logger.error("Error closing server", e);
        }
    }

    
   private void interruptAll(){ 
      executorPool.shutdownNow();
   }

    private void openServerSocket() {
        try {
            this.serverSocket = new ServerSocket(this.serverPort);
        } catch (IOException e) {
            throw new RuntimeException("Cannot open port " + this.serverPort, e);
        }
    }

}