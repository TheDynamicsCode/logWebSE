/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package biz.szydlowski.logwebse.configuration;

import biz.szydlowski.logwebse.KeyPass;
import biz.szydlowski.logwebse.LogApi;
import static biz.szydlowski.logwebse.LogSearchEngineServer.logApi;
import biz.szydlowski.utils.OSValidator;
import biz.szydlowski.utils.template.TemplateFile;
import java.io.File;
import java.io.IOException;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 *
 * @author dkbu
 */
public class ReadLogInterface {
        
    
   private String _setting="setting/log-interfaces.xml";
   private  KeyPass _KeyPass;
   static final Logger logger =  LogManager.getLogger(ReadBrowseCommands.class);

       
     /** Konstruktor pobierający parametry z pliku konfiguracyjnego "config.xml"
     */
     public  ReadLogInterface(){
         
         if (OSValidator.isUnix()){
              String absolutePath = getClass().getProtectionDomain().getCodeSource().getLocation().getPath();
                   absolutePath = absolutePath.substring(0, absolutePath.lastIndexOf("/"));
              _setting = absolutePath + "/" + _setting;
         }
         _KeyPass = new KeyPass();
         
         readSetting( _setting);
         TemplateFile _Template = new TemplateFile("default");
         
         for (String file : _Template.getFilenames()){
             readSetting(file);
          }
     }
   
    private void readSetting(String filename){
        
          
         try {
                        
		File fXmlFile = new File(filename);
		DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
		Document doc = dBuilder.parse(fXmlFile);
		doc.getDocumentElement().normalize();
              
                NodeList  nList = doc.getElementsByTagName("log"); 
                
		for (int temp = 0; temp < nList.getLength(); temp++) {
                
		   Node nNode = nList.item(temp);
		   if (nNode.getNodeType() == Node.ELEMENT_NODE) {
                          Element eElement = (Element) nNode;
                                                
                          LogApi _logApi = new LogApi();
                          
                          _logApi.setAlias(getTagValue("alias", eElement));
                          _logApi.setTemplatePath(getTagValue("path", eElement));
                          _logApi.setGroup(getTagValue("group", eElement));
                          _logApi.setModule(getTagValue("module", eElement));
                          _logApi.setBrowseInterface(getTagValue("browse", eElement));
                          _logApi.setRemote(getTagValue("remote", eElement));
                        
                          if (eElement.getElementsByTagName("path").item(0).hasAttributes()){

                              NamedNodeMap  baseElmnt_attr = eElement.getElementsByTagName("path").item(0).getAttributes();
                                    for (int i = 0; i <  baseElmnt_attr.getLength(); ++i)
                                    {
                                        Node attr =  baseElmnt_attr.item(i);

                                        if (attr.getNodeName().equalsIgnoreCase("dateFormat")){
                                            logger.debug("dateFormat=" + attr.getNodeValue());
                                            _logApi.setDateFormat(attr.getNodeValue());
                                        }

                                  }
                             }
                           
                          if (_logApi.isRemote()){
                              _logApi.setPassword(_KeyPass.replace(_logApi.getPassword()));
                          }
                            
                          logApi.add(_logApi);
		   }
		}
                
                logger.debug("Read logApi done");
                                
         }  catch (ParserConfigurationException | SAXException | IOException e) {         
                logger.fatal("commands.xml::XML Exception/Error:", e);
                System.exit(-1);
				
	 }
    }
    

  
      private static String getTagValue(String sTag, Element eElement) {
            try {
                NodeList nlList = eElement.getElementsByTagName(sTag).item(0).getChildNodes();
                Node nValue = (Node) nlList.item(0);
                return nValue.getNodeValue();
            } catch (Exception e){
                if (!sTag.equals("remote")) logger.error("getTagValue error " + sTag + " "+ e);
                return "ERROR";
            }

      }

 
}


