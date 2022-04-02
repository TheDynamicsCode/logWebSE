/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package biz.szydlowski.logwebse;

import biz.szydlowski.utils.OSValidator;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 *
 * @author dkbu
 */
public class WebParams {
  
    private List<String> allowedConn = new ArrayList<>(); 
    private List<String> predefinedWord = new ArrayList<>(); 
    private int webConsolePort;
    private String restartScriptPath;
    private String helplink;
     
    private String _setting="setting/web.properties";
    
    static final Logger logger =  LogManager.getLogger(WebParams.class);

       
     /** Konstruktor pobierający parametry z pliku konfiguracyjnego "config.xml"
     */
    public WebParams(){
         
         
         if (OSValidator.isUnix()){
              String absolutePath = getClass().getProtectionDomain().getCodeSource().getLocation().getPath();
                   absolutePath = absolutePath.substring(0, absolutePath.lastIndexOf("/"));
              _setting = absolutePath + "/" + _setting;
         }

        webConsolePort=9090;
     
	InputStream input = null;
        Properties prop = new Properties();

	try {

		input = new FileInputStream(_setting );
		// load a properties file
		prop.load(input);
                
                Set<Object> keys = getAllKeys( prop );
                keys.stream().map((k) -> (String)k).forEachOrdered((key) -> {
                    if ( getPropertyValue(prop , key).length()==0){ 
                    } else if (key.startsWith("web.allowed.host")) {
                        allowedConn.add(getPropertyValue(prop, key));
                    }  else if (key.startsWith("web.console.port")) {
                        webConsolePort = Integer.parseInt(getPropertyValue(prop, key));
                    }  else if (key.startsWith("restart.script")) {
                        restartScriptPath = getPropertyValue(prop, key);
                    } else if (key.startsWith("predefined.word")) {
                        predefinedWord.add(getPropertyValue(prop, key));
                    }  else if (key.startsWith("web.helplink")) {
                        helplink=getPropertyValue(prop, key);
                    } 
             }); 
                
                prop.clear();


	} catch (IOException ex) {
		logger.error(ex);
	} finally {
		if (input != null) {
			try {
				input.close();
			} catch (IOException e) {
				logger.error(e);
			}
		}
	}

  
    }

    public  List<String> getAllowedConn (){
          return  allowedConn;
    }  
   
    
    public int getWebConsolePort(){
          return webConsolePort;
    } 
 
   
    private Set<Object> getAllKeys(Properties prop){
            Set<Object> keys = prop.keySet();
            return keys;
    }

    private String getPropertyValue(Properties prop, String key){
            return prop.getProperty(key);
    }  

    /**
     * @return the restartScriptPath
     */
    public String getRestartScriptPath() {
        return restartScriptPath;
    }

    /**
     * @param restartScriptPath the restartScriptPath to set
     */
    public void setRestartScriptPath(String restartScriptPath) {
        this.restartScriptPath = restartScriptPath;
    }

    /**
     * @return the predefinedPhase
     */
    public List<String> getPredefinedWord() {
        return predefinedWord;
    }

    /**
     * @return the helplink
     */
    public String getHelplink() {
        return helplink;
    }

    /**
     * @param helplink the helplink to set
     */
    public void setHelplink(String helplink) {
        this.helplink = helplink;
    }
              
   
}
