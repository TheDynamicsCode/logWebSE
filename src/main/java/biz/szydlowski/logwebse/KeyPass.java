/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package biz.szydlowski.logwebse;

import static biz.szydlowski.logwebse.WebParams.logger;
import biz.szydlowski.utils.OSValidator;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.Set;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 *
 * @author dkbu
 */
public class KeyPass {
   
    private String _setting="setting/keypass";
    
    static final Logger logger =  LogManager.getLogger(WebParams.class);
    Properties prop = new Properties();
    String sreturn = "def";
   
    public KeyPass(){
         
         if (OSValidator.isUnix()){
              String absolutePath = getClass().getProtectionDomain().getCodeSource().getLocation().getPath();
                   absolutePath = absolutePath.substring(0, absolutePath.lastIndexOf("/"));
              _setting = absolutePath + "/" + _setting;
         }
     
	InputStream input = null;
       

	try {

		input = new FileInputStream(_setting );
		// load a properties file
		prop.load(input);                
              


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
    
    
    public String replace (String toReplace){
         sreturn=toReplace;   
         Set<Object> keys = getAllKeys( prop );
         keys.stream().map((k) -> (String)k).forEachOrdered((key) -> {

                if (key.equals(toReplace)) {
                    sreturn=prop.getProperty(key);
                }

         }); 


         return sreturn;
    }
    
    private Set<Object> getAllKeys(Properties prop){
            Set<Object> keys = prop.keySet();
            return keys;
    }
    
}
