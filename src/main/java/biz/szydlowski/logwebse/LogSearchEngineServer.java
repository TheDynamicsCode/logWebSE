/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package biz.szydlowski.logwebse;

import biz.szydlowski.logwebse.configuration.ReadBrowseCommands;
import biz.szydlowski.logwebse.configuration.ReadLogInterface;
import biz.szydlowski.utils.OSValidator;
import biz.szydlowski.utils.api.RestartApi;
import java.io.File;
import java.net.URLDecoder;
import java.security.CodeSource;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import org.apache.commons.daemon.Daemon;
import org.apache.commons.daemon.DaemonContext;
import org.apache.commons.daemon.DaemonInitException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 *
 * @author szydlowskidom
 */
public class LogSearchEngineServer implements Daemon { 
    
    static {
        try {
            System.setProperty("log4j.configurationFile", getJarContainingFolder(LogSearchEngineServer.class)+"/setting/log4j/log4j2.xml");
        } catch (Exception ex) {
        }
    }
    
    static final Logger logger =  LogManager.getLogger(LogSearchEngineServer.class);
     
    public static List<String> allowedConn = null;
    public static List<String> predefinedWord= null; 
    public static List<BrowseCmdApi> browseCommandApi = new ArrayList<>();
   // public static List<String> group = new ArrayList<>();
    public static RestartApi restartApi=null;
    public static List<LogApi> logApi = new ArrayList<>();
    public static List<String> modules = new ArrayList<>();
    
    private static boolean stop = false;
    public static SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy HH-mm-ss"); 
    public static String absolutePath ="";
 
    private Timer Maintenance = null;
     
    private LogSEWebServer WebServer = null;
    private int webport=8080;
    public static String helplink;
    public static int searchlock=0;
  
    public LogSearchEngineServer (){		
    } 
    
     public LogSearchEngineServer (boolean test, boolean win){
          if (test || win){
            if (!win) System.out.println("****** TESTING MODE  ********"); 
            else System.out.println("****** WINDOWS MODE  ********"); 
            try {
               initialize();
               start();               
            } catch (Exception ex) {
                logger.error(ex);
            }
        }
     }
    
        
            
     public static void main(String[] args) {
       
         if (args.length>0){
             if (args[0].equalsIgnoreCase("testing")){
                 LogSearchEngineServer  jobber  = new LogSearchEngineServer (true, false);
             }

         }
         
		
     }
   
     
     public void initialize() {
      
            if (OSValidator.isUnix()){
                 absolutePath = getClass().getProtectionDomain().getCodeSource().getLocation().getPath();
                       absolutePath = "/" + absolutePath.substring(0, absolutePath.lastIndexOf("/"))+"/";
            }
            
             
          
            Maintenance = new Timer("Maintenance", true);
 
            logger.info(new Version().getAllInfo()); 
         
            ReadLogInterface readLogInterface = new ReadLogInterface();
            
            ReadBrowseCommands readCommands = new ReadBrowseCommands();
            
            for (int i=0; i<logApi.size(); i++){
                boolean exist=false;
                for (int j=0; j<modules.size(); j++){
                    if (modules.get(j).equals(logApi.get(i).getModule())) exist=true;
                }
                if (!exist) modules.add(logApi.get(i).getModule());
            }     
           
            
            
            WebParams _WebParams = new WebParams();
            webport = _WebParams.getWebConsolePort();
            helplink = _WebParams.getHelplink();
            restartApi = new RestartApi(_WebParams.getRestartScriptPath());
            allowedConn = _WebParams.getAllowedConn();
            predefinedWord  = _WebParams.getPredefinedWord();
            WebServer = new LogSEWebServer(webport);
           
     }


    /**
     *
     * @param dc
     * @throws DaemonInitException
     * @throws Exception
     */
    @Override
    public void init(DaemonContext dc) throws DaemonInitException, Exception {
          //String[] args = dc.getArguments();
          initialize();
         
    }

  
    @Override
    public void start() throws Exception {
          logger.info("Starting server");
          WebServer.start(); 
          logger.info("Started server");
    }

   
    @Override
    public void stop() throws Exception {
        logger.info("Stopping daemon");
        
        WebServer.stopSever();        
        Maintenance.cancel();
             
        logger.info("Stopped daemon");
    }
    
    //for windows
    public static void start(String[] args) {
        System.out.println("start");
        LogSearchEngineServer jobber = new LogSearchEngineServer(false, true);
        
        while (!stop) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
            }
        }
    }
  
    public static void stop(String[] args) {
        System.out.println("stop");
        stop = true;
       
        logger.info("Stoppping daemon");
  
           
        int active = Thread.activeCount();
        Thread all[] = new Thread[active];
        Thread.enumerate(all);

        for (int i = 0; i < active; i++) {
            if (!all[i].getName().contains("RESTART")){
               logger.info("Thread to interrupt " + i + ": " + all[i].getName() + " " + all[i].getState());
               all[i].interrupt();
            } else {
                logger.info("Thread alive " + i + ": " + all[i].getName() + " " + all[i].getState());
            }
        }
    
        logger.info("Stopped daemon");  
        
        System.exit(0);
                
    }
 

   
    @Override
    public void destroy() { 
        logger.info("Destroy daemon");
        
        Maintenance = null;
     
        logger.info("*********** Destroyed daemon  ****************");
    }
      
   
          
    public class MaintenanceTask extends TimerTask {
           
            int tick=0;
            int mb = 1024 * 1024; 
            Runtime runtime = Runtime.getRuntime();
                 
            @Override
            public void run() {
                        
                     
                   long maxMemory = runtime.maxMemory();
                   long allocatedMemory = runtime.totalMemory();
                   long freeMemory = runtime.freeMemory();
                   long usedMem = allocatedMemory - freeMemory;
                   long totalMem = runtime.totalMemory();
                   
                    if (tick==50){
                        logger.info("***** Heap utilization statistics [MB] *****");
                        // available memory
                        logger.info("Total Memory: " + totalMem / mb);
                        // free memory
                        logger.info("Free Memory: " + freeMemory / mb);
                        // used memory
                        logger.info("Used Memory: " + usedMem / mb);
                        // Maximum available memory
                        logger.info("Max Memory: " + maxMemory / mb);
                       
                        tick=0;
                        
                        System.gc();
                    }
  
                 
            }
    }
    
    public static String getJarContainingFolder(Class aclass) throws Exception {
          CodeSource codeSource = aclass.getProtectionDomain().getCodeSource();

          File jarFile;

          if (codeSource.getLocation() != null) {
            jarFile = new File(codeSource.getLocation().toURI());
          }
          else {
            String path = aclass.getResource(aclass.getSimpleName() + ".class").getPath();
            String jarFilePath = path.substring(path.indexOf(":") + 1, path.indexOf("!"));
            jarFilePath = URLDecoder.decode(jarFilePath, "UTF-8");
            jarFile = new File(jarFilePath);
          }
          return jarFile.getParentFile().getAbsolutePath();
     }

  
       
}
