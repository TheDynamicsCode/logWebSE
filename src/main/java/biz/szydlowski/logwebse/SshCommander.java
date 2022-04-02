/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package biz.szydlowski.logwebse;

import static biz.szydlowski.logwebse.LogSearchEngineServer.absolutePath;
import static biz.szydlowski.logwebse.ServerWorkerRunnable.logger;
import biz.szydlowski.utils.OSValidator;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpException;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.List;

/**
 *
 * @author Dominik
 */
public class SshCommander  {


    private boolean running=true;

    private final int sshBufferSize = 8192;
    private int lines = 0;
    
    
    public int execCommand(String hostname, String sPort, String username, String password, String command, PrintWriter out, boolean saveTofile, String FILENAME){
         
           lines=0; 
           int TIMEOUT = 120000;
           
           if (command.contains("tail -f")) TIMEOUT = 15000;
                  
           try {
                    JSch conn = new JSch();
                    int port = 22;
                    try {
                        port = Integer.parseInt(sPort);
                    } catch (Exception e){}

                 
                    Session session = conn.getSession(username, hostname, port);
                    session.setPassword(password);
                    session.setConfig("StrictHostKeyChecking", "no");
                    session.setConfig("PreferredAuthentications",  "password, publickey,keyboard-interactive");
                
                    
                    session.setTimeout(3000);

                    session.connect();
            
                
                if (!session.isConnected()){
                     out.append("Session not connected").append("</br>");
                     return 0;                    
                }
                 
               ChannelExec channelExec = (ChannelExec)session.openChannel("exec");
               out.append("</br>CONNECTED TO HOST: ").append(username).append("@").append(hostname).append("</br></br>");    
               
               
               boolean timeout = false;
                           
               long start = System.currentTimeMillis();
               long current;
               
            ///   if (saveTofile) out.append("The results will only be saved to a file...").append("</br>"); 
             
               try (InputStream in = channelExec.getInputStream()) {
                   channelExec.setCommand(command);
                 //  logger.debug("Exec command " + command);
                   channelExec.connect(30000);
                   try (BufferedReader reader = new BufferedReader(new InputStreamReader(in))) {
                        String line;
                        while ((line = reader.readLine()) != null  && !timeout) {
                            current = System.currentTimeMillis();
                            if (saveTofile) dumpToFile(line+"\n", FILENAME);
                            else out.append(line).append("</br>");                            
                            if (current - start > TIMEOUT) {
                                out.append("</br>Stopping follow tail").append("</br>");
                                timeout = true;
                            }
                            lines++;
                        }  
                    }
               }
                
                channelExec.disconnect();
             
                session.disconnect();
                
                
                /*if (saveTofile) {
                     if (lines==0){
                       dumpToFile("Nothing found.\n", FILENAME);
                    } else {
                       dumpToFile("Lines: " + lines +".\n", FILENAME); 
                    }
                }*/

                out.append("</br>DISCONNECTED").append("</br>");
            }
            catch(JSchException | IOException e) {
                out.append("ERROR " + e.getMessage()).append("</br>");
            }
           
           return lines;
           
    }
    
    
      public int getLines(){
          return lines;
      }
      
     public boolean isSSHReadDir (String hostname, String sPort, String username, String password, String file) {
     
        boolean ret = false;
        
        try {

            JSch conn = new JSch();
            Session session = null;
            
            int port = 22;
            try {
                port = Integer.parseInt(sPort);
            } catch (Exception e){}

                                 
            session = conn.getSession(username, hostname, port);
            session.setPassword(password);
            
            session.setConfig("StrictHostKeyChecking", "no");
            session.setConfig("PreferredAuthentications",  "password, publickey,keyboard-interactive");
                
            session.connect();

            ChannelSftp channel = (ChannelSftp)session.openChannel("sftp");
            channel.connect();
          
            InputStream in = channel.get(file);
            
            if (in!=null){
                ret = true;
            } else ret=false;
            
            in.close();

            channel.disconnect();
            session.disconnect();
        
        } catch (JSchException j){ }
        
        catch (SftpException s){  } 
        
        catch (IOException e){   } 
        
        return ret;
        
    }   
      
      
    private void dumpToFile(String content, String FileName){         
          
      
            if (OSValidator.isUnix()){
                     absolutePath = getClass().getProtectionDomain().getCodeSource().getLocation().getPath();
                           absolutePath = "/" + absolutePath.substring(0, absolutePath.lastIndexOf("/"))+"/";
            }


            String dumpfile="results/"+FileName;
            String response="results";

            if (OSValidator.isUnix()){
                  dumpfile = absolutePath + "/" + dumpfile;
                  response = absolutePath + "/" + response ;
            }  
            

            File f = new File(response);

            if (!f.exists()) {
                f.mkdirs();
            }   

           BufferedWriter bw = null;
           FileWriter fw = null;

           try {

                    fw = new FileWriter(dumpfile, true); //append
                    bw = new BufferedWriter(fw);
                    bw.write(content);

            } catch (IOException e) {
                    logger.error(e);

            } finally {

                    try {

                            if (bw != null)
                                    bw.close();

                            if (fw != null)
                                    fw.close();

                    } catch (IOException ex) {
                        logger.error(ex);
                    }

            }                        
    }
 
   

}