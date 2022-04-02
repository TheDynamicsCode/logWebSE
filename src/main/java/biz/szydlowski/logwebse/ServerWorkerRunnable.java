/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package biz.szydlowski.logwebse;

import static biz.szydlowski.logwebse.HtmlStyle.ALERTSTYLE;
import static biz.szydlowski.logwebse.HtmlStyle.FORM;
import static biz.szydlowski.logwebse.HtmlStyle.INFOSTYLE;
import static biz.szydlowski.logwebse.HtmlStyle.LINKSTYLE;
import static biz.szydlowski.logwebse.HtmlStyle.OKSTYLE;
import static biz.szydlowski.logwebse.HtmlStyle.SCRIPTHIDDEN;
import static biz.szydlowski.logwebse.HtmlStyle.SCRIPTREMADD;
import static biz.szydlowski.logwebse.HtmlStyle.TABLE_STYLE_NEW;
import static biz.szydlowski.logwebse.HtmlStyle.TOPNAV_STYLE;
import static biz.szydlowski.logwebse.LogSearchEngineServer.absolutePath;
import static biz.szydlowski.logwebse.LogSearchEngineServer.browseCommandApi;
import static biz.szydlowski.logwebse.LogSearchEngineServer.helplink;
import static biz.szydlowski.logwebse.LogSearchEngineServer.logApi;
import static biz.szydlowski.logwebse.LogSearchEngineServer.modules;
import static biz.szydlowski.logwebse.LogSearchEngineServer.predefinedWord;
import static biz.szydlowski.logwebse.LogSearchEngineServer.restartApi;
import static biz.szydlowski.logwebse.LogSearchEngineServer.searchlock;
import biz.szydlowski.utils.OSValidator;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.Socket;
import java.net.URLDecoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 *
 * @author Dominik
 */
public class ServerWorkerRunnable implements Runnable {
   
      static final Logger logger =  LogManager.getLogger(ServerWorkerRunnable.class);
      protected Socket clientSocket = null;
      protected SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
      String userInput = "default";
      boolean isGet=true;
      boolean isPost=false;
      int port=8080;    
      private boolean isApi=false;

      
                         
      public ServerWorkerRunnable(Socket clientSocket, boolean api, int port) {
            this.clientSocket = clientSocket;
            this.isApi=api;
            this.port=port;
      }


      @Override
      public void run() {
        try {
       
                    
          InputStream input = this.clientSocket.getInputStream();
         

          BufferedReader stdIn = null;
          try {
                stdIn = new BufferedReader(new InputStreamReader(this.clientSocket.getInputStream()));
                userInput = stdIn.readLine();
          } catch (IOException ex) {
                logger.error(ex);
          }

          if (userInput == null) userInput = "DEFAULT";

          PrintWriter out = new PrintWriter(this.clientSocket.getOutputStream(), true);
                        
          //System.out.println(userInput);
          if (userInput.length() == 0){
              return;
           }
         
         isGet = userInput.contains("GET");
          
         if (!isApi) {
             logger.debug("Rejected Client : Address - " + clientSocket.getInetAddress().getHostAddress());
           
             String data="<h1>401 UNAUTHORIZED. Authentication required.</h1><b><br/>Sorry, you are not allowed to access this page.</b><br/>";
             String response = "HTTP/1.1 401 UNAUTHORIZED\r\n" +
                    "Content-Length: "+data.length()+"\r\n" +
                    "Content-Type: text/html\r\n\r\n" +
                    data;
              
             out.print(response);
             out.flush();

             out.close();
             input.close();
          
             return;
         } 
         
          logger.debug("Accepted Client : Address - " + clientSocket.getInetAddress().getHostName());
          
          String postData = "";
          if ( userInput.contains("POST")){
               isPost=true;
             
               String line;
               int postDataI=0;
                while ((line = stdIn.readLine()) != null && line.length() != 0) {
                        logger.debug("HTTP-HEADER: " + line);
                        if (line.contains("Content-Length:")) {
                            postDataI = Integer.parseInt(line.substring(line.indexOf("Content-Length:") + 16, line.length()));
                        }
                    
                }
                postData = "";
                // read the post data
                if (postDataI > 0) {
                    char[] charArray = new char[postDataI];
                    stdIn.read(charArray, 0, postDataI);
                    postData = new String(charArray);
                }
                
               logger.debug("post DATA before replace " + postData); 
                               
                postData =  replaceURL(postData );                                
                 
               logger.debug("post DATA after replace " + postData); 
             
          } else {
              isPost=false;
          }
                
            if (isGet || isPost){
              out.println("HTTP/1.1 200 OK");
              out.println("Content-Type: text/html");
              out.println("<html>\n");
              out.println("<head>");
              out.println("<title>LogWeb Search Engine</title>");
              out.println("</head>\n");
          }
      
         if (userInput.length()>0) userInput = userInput.replace("HTTP/1.1", "");
         if (userInput.length()>0) userInput = userInput.replace("%20", " ");
         if (userInput.length()>0) userInput = userInput.replaceFirst("GET", "");      
         if (userInput.length()>0) userInput = userInput.replaceFirst("/", "");  
         if (userInput.length()>0) userInput = userInput.replaceAll("\\s+", " ");
         if (userInput.length()>0) userInput = userInput.replaceAll("favicon.ico ", "");

         if (userInput.length() == 0){
              return;
         }
       
         
        if (isPost){
            
            if ( userInput.contains("browse.api") ){
                
                String[] parm = postData.split("&");
                    //System.out.println(postData);
                switch (parm.length) {
                    case 1:
                        out.println("");
                        out.flush();
                        break;
                    case 3:
                        String[] lockidstr  = parm[1].split("=");
                        int lockid=0;
                        if (lockidstr.length==2){
                            lockid=Integer.parseInt(lockidstr[1]);
                        }
                        String[] lockbridstr  = parm[2].split("=");
                        int lockbrid=0;
                        if (lockbridstr.length==2){
                            lockbrid=Integer.parseInt(lockbridstr[1]);            
                        }
                        if (!logApi.get(lockid).getLock(lockbrid)){
                            String[] _sp = parm[0].split("=");
                            if (_sp.length==2){
                                logApi.get(lockid).setLock(lockbrid,true);
                                logger.info("Execution start " + _sp[1]);
                                                                
                                out.println(OKSTYLE);
                                out.println("</br><div class=\"ok\"><strong>Execute: </strong>");
                                out.println(_sp[1]);
                                out.println("</div></br>");
                                out.flush();                              
                                 
                                if (logApi.get(lockid).isRemote()){
                                        SshCommander sshCommander = new SshCommander(); 
                                        sshCommander.execCommand(logApi.get(lockid).getHostname(),logApi.get(lockid).getPort(),logApi.get(lockid).getUser(), logApi.get(lockid).getPassword(), _sp[1], out, false, "NULL");
                                      
                                        out.flush();
                                       
                                        logApi.get(lockid).setLock(lockbrid,false);
                                } else {
                                
                                    Process p=null;
                                    boolean timeout = false;

                                    try {

                                        p = Runtime.getRuntime().exec(_sp[1]);
                                        long start = System.currentTimeMillis();
                                        long current;

                                        BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));

                                        String line = "";                                    

                                        while ((line = reader.readLine())!= null && !timeout) {
                                            current = System.currentTimeMillis();
                                            if (current - start > 15000) timeout = true;
                                            out.println("  ");
                                            lineCut(line, out, 1000);
                                            out.flush();
                                        }

                                    } catch (Exception e) {
                                        out.println("  ");
                                        out.println(e.toString());
                                        out.println("</br>");
                                        //logger.error("executeCommand "+e);
                                    } finally {

                                        logApi.get(lockid).setLock(lockbrid,false);
                                        try {
                                            if (p!=null) {
                                                p.waitFor(5, TimeUnit.SECONDS);
                                                if (p.isAlive()) p.destroyForcibly();
                                            }
                                        } catch (InterruptedException o){
                                            logger.error(o);
                                        }
                                        out.println(OKSTYLE);
                                        out.println("</br><div class=\"ok\"><strong>Info.</strong> The tail is complete.</div></br>");

                                        out.flush();


                                    }
                                }
                                
                                
                            } else {
                                out.println("_sp.length!=2</br>");
                                out.flush();
                                logger.error("_sp.length!=2");
                            }
                        }
                        else {
                            out.println(ALERTSTYLE);
                            out.println("</br><div class=\"alert\">\n" +
                            "  <strong>Error!</strong> (LOCKED) Indicates a dangerous or potentially negative action.\n" +
                            "</div></br>");
                            out.flush();
                        }
                        break;
                    default:
                        out.println("</br></br>INTERNAL ERROR</br></br>");
                        out.flush();
                        break;
                }
            } else if ( userInput.contains("search.api") ){
                    
                   // System.out.println(postData);
                    String[] parms = postData.split("\n");
                                    
                    List<String> word_tmp = new ArrayList<>();
                    String grep_and = "or";
                    String grep_not = "false";
                    String before = "2";
                    String after = "2";  
                    String last  = "0"; 
                    String entirefile  = "false";
                    String savetofile  = "false";
                    int i_last  = 0;
                    boolean isRemote=false;
                    List<String> logfile = new ArrayList<>();
                    String tmp_str="";
                                      
                    for (String s : parms){
                       //BUG 20201220
                        if (s.startsWith("word")){
                            s = s.replaceFirst("word=", "");
                                if (s.replaceAll("\\s+", "").length()>0){
                                    tmp_str = s.substring(0, s.length()-2);
                                    tmp_str = tmp_str+s.substring(s.length()-2).replaceAll("\\s+", "");
                                    //word_tmp.add(tmp[1].replaceAll("\\s+", "#"));
                                     word_tmp.add( tmp_str);
                                }
                            
                        } else {
                        String[] tmp = s.split("=");
                        if (tmp.length==2){
                            if (tmp[0].equalsIgnoreCase("before")) before = tmp[1].replaceAll("\\s+", "");
                            else if (tmp[0].equalsIgnoreCase("after")) after = tmp[1].replaceAll("\\s+", "");
                            else if (tmp[0].equalsIgnoreCase("last")) last = tmp[1].replaceAll("\\s+", "");
                            else if (tmp[0].equalsIgnoreCase("entirefile")) entirefile = tmp[1].replaceAll("\\s+", "");
                            else if (tmp[0].equalsIgnoreCase("savetofile")) savetofile = tmp[1].replaceAll("\\s+", "");
                            else if (tmp[0].equalsIgnoreCase("logfile")) logfile.add(tmp[1]);  
                            else if (tmp[0].equalsIgnoreCase("grep_and")) grep_and = tmp[1].replaceAll("\\s+", "");
                            else if (tmp[0].equalsIgnoreCase("grep_not")) grep_not = tmp[1].replaceAll("\\s+", "");
                            
                          }   
                       }                     
                    }
                    
                    List<String> word = new ArrayList<>();
                    for (String ww:word_tmp){
                        boolean exist=false;
                        for (String ww_uniq : word){
                           if (ww_uniq.equals(ww)){
                               exist=true;
                           }  
                        }
                        if (!exist) {
                            //System.out.println(ww);
                            word.add(ww);
                        }
                    }
                   
                                        
                    if (searchlock>1){
                        out.println(ALERTSTYLE);
                        out.println("</br><div class=\"alert\"><strong>Error!</strong> SEARCH ENGINE IS LOCKED.... PLEASE WAIT AND RETRY LATER...</div></br>");
                        out.flush();
                    } else if ( logfile.isEmpty()){
                        out.println(ALERTSTYLE);
                        out.println("</br><div class=\"alert\"><strong>Error!</strong> PLEASE SELECT A LOGFILE.</div></br>");
                        out.flush();
                        printHomeAndBack(out,false,false,false);
                    } else if ( word.isEmpty()){
                        out.println(ALERTSTYLE);
                        out.println("</br><div class=\"alert\"><strong>Error!</strong> PLEASE SPECIFY A PHRASE.</div></br>");
                        out.flush();
                        printHomeAndBack(out,false,false,false);
                    } else {         
                  
                        out.println("<h3><b>SEARCH RESULTS</b></h3>");
                        printHomeAndBack(out,false,true,false);
                        String logpath="";
                        
                        searchlock++;
                        out.println(INFOSTYLE);
                        
                        long startTime = System.currentTimeMillis();
                        String saveFileName = getFileName();
                         
                        for (String log : logfile){
                                                     
                            out.println("<hr>");
                            
                            Process p=null; 
                             
                            out.println("</br><div class=\"info\"><strong>Info.</strong> Search in file\n");
                            out.println(log);
                            if (savetofile.equals("true")) out.println(". The results will only be saved to a file..."); 
             
                            out.println("</div></br>");
                            
                            if (savetofile.equals("true"))  dumpToFile("Search in file: " + log, saveFileName);
                            
                            out.flush();
                            String hostname="";
                            String user="";
                            String pswd="";
                            String port="";
                            
                            for (int i=0; i<logApi.size();i++){                               
                                if (logApi.get(i).getAlias().replaceAll("\\s+", "").equalsIgnoreCase(log.replaceAll("\\s+", ""))){
                                    logpath=logApi.get(i).getPath();
                                    isRemote=logApi.get(i).isRemote();
                                    if (isRemote){
                                        user = logApi.get(i).getUser();
                                        hostname = logApi.get(i).getHostname();
                                        pswd = logApi.get(i).getPassword();
                                        port = logApi.get(i).getPort();
                                    }
                                }
                            }
                            
                            int ile=0;
                            boolean error=false;
                            String error_info="";                            
                           
                            String c="";

                            try {
                                i_last = Integer.parseInt(last);
                            } catch (Exception ignore){

                            }


                            if (entirefile.equals("true")) i_last=0;

                            String grep="";
                                
                                if (grep_not.equals("true")){
                                    if (word.size()==1) {
                                        grep =  " | grep -v \"" + word.get(0) + "\" -A " + after + " -B " + before;
                                    } else {
                                        if (grep_and.equals("and")){
                                            for (String w:word){
                                                grep = grep + " | grep -v \"" + w + "\" -A " + after + " -B " + before;
                                            }

                                        } else if (grep_and.equals("or")){
                                                grep="bez sensu break";
                                                error=true;
                                                error_info="The choice makes no sense (grep or & not)";
                                        }
                                    }
                                    
                                } else {
                                
                                    if (word.size()==1) {
                                        grep =  " | grep \"" + word.get(0) + "\" -A " + after + " -B " + before;
                                    } else {
                                        if (grep_and.equals("and")){
                                            for (String w:word){
                                                grep = grep + " | grep \"" + w + "\" -A " + after + " -B " + before;
                                            }

                                        } else if (grep_and.equals("or")){
                                            grep = " | grep -E \"";
                                            for (int q=0; q<word.size(); q++){
                                                if (q==0) grep = grep + word.get(q);
                                                else  grep = grep + "|" + word.get(q);
                                            }
                                            grep = grep + "\" -A " + after + " -B " + before;
                                        }
                                    }
                                }
  
                                
                                if (error){
                                       out.println(ALERTSTYLE);
                                       out.println("</br><div class=\"alert\"><strong>Error!</strong>"+error_info+"</div></br>");
                                       out.flush();
                                } else {
                                    long startLogTime = System.currentTimeMillis();
                                    if (i_last==0) {
                                          c = "cat "  + logpath + grep;
                                    } else {
                                          c = "tail -n " + last + " " + logpath + grep;
                                    }  
                                  
                                    logger.info(c); 
                                    saveCmdToAuditFile(clientSocket.getInetAddress().getHostAddress() + " " + c);
                                             
                                    if (isRemote){ //wykonywane przez klienta ssh
                                        SshCommander sshCommander = new SshCommander(); 
                                        if (savetofile.equals("true")) sshCommander.execCommand(hostname,port,user,pswd, c, out, true,saveFileName);
                                        else sshCommander.execCommand(hostname,port,user,pswd, c, out, false,saveFileName);
                                        ile =  sshCommander.getLines();
                                        
                                        long stopLogTime = System.currentTimeMillis();
                                        long elapsedLogTime = stopLogTime - startLogTime;
                                        double searchLogtime=elapsedLogTime/1000;

                                        out.println(OKSTYLE);
                                        if (ile==0){
                                            if (savetofile.equals("true"))  dumpToFile("The search is complet. Nothing found. Search in "+searchLogtime+" sec.", saveFileName);
                                          out.println("</br><div class=\"ok\"><strong>OK.</strong> The search is complet. Nothing found. Search in "+searchLogtime+" sec.</div></br>");
                                        } else {
                                            if (savetofile.equals("true"))  dumpToFile("The search is complet. Search in "+searchLogtime+" sec. Found " + ile +" lines", saveFileName);
                                           out.println("</br><div class=\"ok\"><strong>OK.</strong> The search is complet. Search in "+searchLogtime+" sec. Lines " + ile +"</div></br>"); 
                                        }

                                        out.flush();
                                    } else {
                                            try {


                                                dumpScript(c);
                                               
                                                 p = Runtime.getRuntime().exec(absolutePath + "/" + "script.sh");                                                                     

                                            
                                                BufferedReader reader =
                                                new BufferedReader(new InputStreamReader(p.getInputStream()));

                                                String line = "";

                                                while ((line = reader.readLine())!= null) {
                                                    if (savetofile.equals("true"))  dumpToFile(line+"\n", saveFileName);
                                                    else lineCut(line, out, 1000);                                                   
                                                    ile++;
                                                    out.flush();
                                                }
                                           
                                              

                                            } catch (IOException | NumberFormatException e) { 
                                                    out.println("  ");
                                                    out.println(e.toString());
                                                    out.println("</br>"); 
                                                     if (savetofile.equals("true"))  dumpToFile(e.toString()+"\n", saveFileName);
                                                    logger.error("executeCommand "+e);
                                            } finally {
                                                 long stopLogTime = System.currentTimeMillis();
                                                 long elapsedLogTime = stopLogTime - startLogTime;
                                                 double searchLogtime=elapsedLogTime/1000;

                                                out.println(OKSTYLE);
                                                if (ile==0){
                                                    if (savetofile.equals("true"))  dumpToFile("The search is complet. Nothing found. Search in "+searchLogtime+" sec.", saveFileName);
                                                   out.println("</br><div class=\"ok\"><strong>OK.</strong> The search is complet. Nothing found. Search in "+searchLogtime+" sec.</div></br>");
                                                } else {
                                                    if (savetofile.equals("true"))  dumpToFile("The search is complet. Search in "+searchLogtime+" sec. Found " + ile +" lines", saveFileName);
                                                    out.println("</br><div class=\"ok\"><strong>OK.</strong> The search is complet. Search in "+searchLogtime+" sec. Found " + ile +" lines</div></br>"); 
                                                }

                                                out.flush();

                                                try {
                                                    if (p!=null) {
                                                       p.waitFor(30, TimeUnit.SECONDS);
                                                       if (p.isAlive()) p.destroyForcibly();
                                                    }
                                                } catch (InterruptedException o){
                                                    logger.error(o);
                                                }
                                            }
                                    } //else not remote
                             }
                        }
                         long stopTime = System.currentTimeMillis();
                         long elapsedTime = stopTime - startTime;
                         double searchtime= elapsedTime/1000;
                         
                         out.println("</br><div class=\"ok\"><strong>OK.</strong> Search in "+ searchtime +" sec.</div></br>"); 
                                      
                         if (savetofile.equals("true")) {                        
                            out.println("<h3>DOWNLOAD SEARCH RESULTS:  <a href=\"search-results/" + saveFileName  +"\" download>"+saveFileName+"</a></h3>");
                         }
                             
                         searchlock--;
                         printHomeAndBack(out,false,false,false);
                }

                
            } else if ( userInput.contains("restart.api") ){ 
                        logger.info("RESTART API");
                        String[] _sp = postData.split("=");
                         if (_sp.length==2){
                               if (_sp[1].equals("restart")){
                                    out.println(ALERTSTYLE);
                                    out.println("</br><div class=\"alert\"> RESTART application LogSE. Refresh this page.</div></br>");                                   
                                    out.flush();
                                    restartApi.restart();
                               }  else {
                                   out.write(new StringBuilder().append("<font color=\"red\">DO NOTHING</font>").toString());
                               }

                        } else {
                             logger.error("_sp.length!=2");
                        }
                 
           } 
       } //end if post  
        
        
        else if (userInput.contains("all-search-results")  ) {  
         
           printAllSearchResults(out);      
           
       } else if (userInput.contains("search-results")  ) {  
           String data  = userInput.replace("search-results/", "").replaceAll(" ", "");
           printSearchResults(out, data);
       }  else if (userInput.contains("browse")  ) {  
           
                 int Imodule = 0;
                 try {
                        Imodule = Integer.parseInt(userInput.replace("browse_", "").replaceAll("\\s+", ""));
                 } catch (Exception ee){}

                printDefinedCommands(out, Imodule);        
              
           
        } else if (userInput.contains("search")  ) {  
          
                printSearch(out);        
           
        }  else if (userInput.contains("diagtools")  ) {  
          
                printDiagTools(out);          
              
        } else  if (userInput.contains("version")){
                  printVersion(out); 
        } 
        else if (userInput.replaceAll("\\s+", "").equals("ping")){ 
             if (isGet) out.println(new StringBuilder().append("PONG").append("<br/>").toString());
             else out.println(new StringBuilder().append("PONG").append("<br/>").toString());
        }  else { 
                out.println("<h2><b>Log Search Engine</b></h2>");
                out.println("<ul>");
                out.println(new StringBuilder().append(" <li> <a href=\"search\"> SEARCH</a></li>").toString());
                out.println(new StringBuilder().append(" <li> <a href=\"browse_0\"> BROWSE </a></li>").toString());
                out.println(new StringBuilder().append(" <li> <a href=\"all-search-results\"> SEARCH RESULTS </a></li>").toString());
                out.println("</ul>");
                //out.println(new StringBuilder().append("<br/>").toString());
                out.println("<ul>");
                out.println(new StringBuilder().append(" <li> <a href=\"").append(helplink).append("\"> HELP </a></li>").toString()); 
                out.println(new StringBuilder().append(" <li> <a href=\"diagtools\"> DIAGTOOLS </a></li>").toString());   
                out.println(new StringBuilder().append(" <li> <a href=\"version\"> VERSION </a></li>").toString()); 
                out.println("</ul>");
                out.println(LINKSTYLE);
                printHomeAndBack(out,false,true,true);               
        }
      
        if (isGet || isPost) {
             out.println("</html>");
        }
          
         out.flush();

         out.close();
         input.close();

          logger.debug("Request processed/completed...");
        }
        catch (IOException e) {
          logger.error(e);
        }
    }
      
      
      public void lineCut(String line, PrintWriter out, int size){
                
          for (int start=0;start<line.length();start+=size){
              out.println(line.substring(start, Math.min(line.length(), start+size)));
              out.println("</br>");            
          }
          
          
      }
     private void printVersion(PrintWriter out)  {
        out.println("<b><h3> VERSION " +Version.getName()+ " </h3>" );
        out.println(" </b>");
        out.println(Version.getVersion());
        out.println("<br/>");
        out.println(Version.getAuthor());
        out.println("<br/>");
        out.println("DEV_VERSION_EXPIRE_STR: ");
        out.println(Version.DEV_VERSION_EXPIRE_STR);
        out.println("<br/>");
        //out.println(UPDATE_STR.toString());        
        printHomeAndBack(out, false,false,true);
    }
      
    private void printDiagTools(PrintWriter out){
       out.print("<h2>DiagTools</h2>\n");  
       ThreadGroup currentGroup = Thread.currentThread().getThreadGroup();
       int noThreads = currentGroup.activeCount();
       Thread[] lstThreads = new Thread[noThreads];
       currentGroup.enumerate(lstThreads);
       
        out.println("<style>\n table, th, td {\n border: 1px solid black;\n border-collapse: collapse;\n }\n th, td {\n padding: 5px;\n");
        out.println(" text-align: left;   \n}\n </style>");  
          
        
        out.println("<table style=\"width:90%\">");      
        out.println("<tr>\n <th>ID</th>\n <th>Thread ID</th>\n  <th>Thread name</th>\n  <th>Thread state</th>\n  <th>Thread priority</th>\n </tr>");
       
      
        for (int i = 0; i < noThreads; i++){
                     
                 out.println("<tr>"); 
                 
                 out.println("<td>");
                 out.print(i);
                 out.println("</td>"); 
                 
                 out.println("<td>");
                 out.print(lstThreads[i].getId());
                 out.println("</td>"); 
                 
                 
                 out.println("<td>");
                 out.print(lstThreads[i].getName());
                 out.println("</td>"); 
                 
                 out.println("<td>");
                 out.print(lstThreads[i].getState());
                 out.println("</td>"); 
                 
                 out.println("<td>");
                 out.print(lstThreads[i].getPriority());
                 out.println("</td>"); 
               
                 out.println("</tr>"); 
                 
        }
        
       
        out.println("</table></br>");
        
        out.println("<table style=\"width:90%\">");      
        out.println("<tr>\n <th>File</th>\n <th>Status</th>\n </tr>");
       
      
        for (int i=0; i<logApi.size(); i++){
                     
                 out.println("<tr>"); 
                 
                 out.println("<td>");
                 out.print(logApi.get(i).getPath());
                 out.println("</td>"); 
                 
            
                 if (logApi.get(i).isRemote()){
                     SshCommander sshCommander = new SshCommander(); 
                     if (sshCommander.isSSHReadDir(logApi.get(i).getHostname(),logApi.get(i).getPort(),logApi.get(i).getUser(), logApi.get(i).getPassword(),logApi.get(i).getPath())){
                        out.println("<td>");
                        out.print("OK");
                        out.println("</td>"); 
                     } else {
                          out.println("<td>");
                          out.print("ERROR");
                          out.println("</td>"); 
                     }
                      
                 } else {
                     File tmpDir = new File(logApi.get(i).getPath());
                     if (tmpDir.exists()){
                        out.println("<td>");
                        out.print("OK");
                        out.println("</td>"); 
                     } else {
                            out.println("<td>");
                            out.print("ERROR");
                            out.println("</td>"); 
                     }
                 }
                         
               
                 out.println("</tr>"); 
                 
        }
               
        out.println("</table>");
     
        out.println("<b><h3>Locks</h3></b>" );
        out.println("<b>SearchLock: " );
        out.println(searchlock);
        out.println("<br/>");

          //out.println("<h3>"+group.get(i)+"</h3>"); 
          
         out.println("</br><table>");
         out.println("<tr> <th>Alias</th><th>Browse interface</th><th> Locked </th>");
         out.println("</tr>");

          for (int j = 0; j <  logApi.size(); j++) {
        
                      for (int k = 0; k <  logApi.get(j).getBrowseInterface().size(); k++) {                    
                           
                            out.println("<tr>");
                            out.println("<td>" + logApi.get(j).getAlias()  + "</td><td>"+ logApi.get(j).getBrowseInterface().get(k) + "</td>");
                            out.println("<td>"+logApi.get(j).getLock(k)+"</td>");
                            out.println("</tr>");
                       
                    }
              
         }

      
        out.println("</table>");  
            
            
        Runtime runtime = Runtime.getRuntime(); 
        
        
        long maxMemory = runtime.maxMemory();
        long allocatedMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        long usedMem = allocatedMemory - freeMemory;
        long totalMem = runtime.totalMemory();
        long mb=1024;
        
        out.println("<b><h3>Heap utilization statistics [MB] </h3></b>");
        out.println("Total Memory: " + totalMem / mb);
        out.println("<br/>");
        out.println("Free Memory:  " + freeMemory / mb);
        out.println("<br/>");
        out.println("Used Memory:  " + usedMem / mb);
        out.println("<br/>");
        out.println("Max Memory:   " + maxMemory / mb);
        out.println("<br/>");
     
        
       printHomeAndBack(out, false,false,true);
      
      
    }
      
    private void saveCmdToAuditFile(String text)  {
          if (OSValidator.isUnix()){
             absolutePath = getClass().getProtectionDomain().getCodeSource().getLocation().getPath();
             absolutePath = "/" + absolutePath.substring(0, absolutePath.lastIndexOf("/"))+"/";
          }

     
        FileWriter fw = null; 
        BufferedWriter bw = null;
        PrintWriter pw = null; 
        try { 
            fw = new FileWriter(absolutePath+"audit.log", true);
            bw = new BufferedWriter(fw);
            pw = new PrintWriter(bw); 
            pw.print(getCurrentTimeStamp());
            pw.print(" ");
            pw.println(text);
            pw.flush(); 
        }    catch (IOException ex) { 
            logger.error(ex);
        } finally  { 
            try {
               pw.close();
               bw.close();
               fw.close(); 
            } catch (IOException io) {
              // can't do anything 
           } 
        }
  }  

 private static String getCurrentTimeStamp() {
    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    Date now = new Date();
    String strDate = sdf.format(now);
    return strDate;
  }
 
    private void dumpScript(String content){ 
     
           if (OSValidator.isUnix()){
                     absolutePath = getClass().getProtectionDomain().getCodeSource().getLocation().getPath();
                           absolutePath = "/" + absolutePath.substring(0, absolutePath.lastIndexOf("/"))+"/";
           }


           BufferedWriter bw = null;
           FileWriter fw = null;

           try {


                    fw = new FileWriter(absolutePath + "/" + "script.sh");
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
      
     
    public void printSearch(PrintWriter out) {
      
        out.println("<script src=\"https://ajax.googleapis.com/ajax/libs/jquery/1.11.1/jquery.min.js\"></script>");
        out.println("<form action=\"search.api\" method=\"post\" enctype=\"text/plain\">\n" +
            "<table>\n" +
            "<tr><td>Search</td><td><span id=\"fooBar\"><input size=80 list=\"words\" name=\"word\" id=\"word\" class=\"szukaj\" required=\"required\">"
                + "<INPUT type=\"button\" value=\"Add\" onclick=\"add()\" class=\"szukaj\"/>" +
            "<INPUT type= \"button\" value=\"Remove\" onclick=\"rem()\" class=\"szukaj\"/></span></td>" + 
            " <datalist id=\"words\">");
            for (String s:predefinedWord) {
                    out.println("<option value=\""+s+"\">");
            }
            out.println("</datalist></tr>");
            out.println("<td>Grep AND/OR</td><td>" +
                " <select id=\"grep_and\" name=\"grep_and\" class=\"szukaj\" >" +
                "  <option value=\"or\">OR</option>" +
                "  <option value=\"and\">AND</option>" +
                "</select></td></tr>" +
                "<tr>\n" +
                "	<td>Grep NOT</td><td><input type=\"checkbox\" name=\"grep_not\" class=\"szukaj\" value=\"true\" ></td>\n" +
                "</tr>");        
            out.println("<tr><td>Add BEFORE (lines)</td><td><input type=\"number\" name=\"before\" min=\"0\" max=\"1000\" class=\"szukaj\" value=\"0\" required=\"required\"></td>\n" +
            "</tr>   \n" +
            "<tr>\n" +
            "	<td>Add AFTER (lines)</td><td><input type=\"number\" name=\"after\" min=\"0\" max=\"1000\" class=\"szukaj\" value=\"0\" required=\"required\"></td>\n" +
            "</tr>\n" +
            "<tr>\n" +
            "	<td>Only last (lines)</td><td><input id=\"inp_last\"  type=\"number\" name=\"last\" min=\"0\" max=\"1000000\" class=\"szukaj\" value=\"100\" required=\"required\"></td>\n" +
            "</tr>\n" + 
            "<tr>\n" +
            "	<td>Search all over</td><td><input id=\"ch_entirefile\" type=\"checkbox\" name=\"entirefile\" class=\"szukaj\" value=\"true\" ></td>\n" +
            "</tr>\n" +  
            "<tr>\n" +
            "	<td>Save results to file</td><td><input type=\"checkbox\" name=\"savetofile\" class=\"szukaj\" value=\"true\" ></td>\n" +
            "</tr>\n" +
            "<tr>\n" +
            "	<td>Select Logfile</td><td>	\n" +
            "	<select name=\"logfile\" multiple size=20 required=\"required\" style=\"width: 650px;\">\n");
            
            
            for (String mod : modules){
                List<String> group = getGroupInModule(mod);
                for (int i = 0; i <  group.size(); i++) {
                  out.println(" <optgroup label=\""+ mod + " | " + group.get(i)+"\">\n"); 

                  for (int j = 0; j <  logApi.size(); j++) {
                  if (logApi.get(j).getGroup().equals(group.get(i)))  {
                      out.println("<option value=\""+logApi.get(j).getAlias()+"\">"+logApi.get(j).getAlias().toUpperCase()+"</option>\n");                
                    }
                  }
                  out.println("</optgroup>");
                }
            }
         
          out.println( "</select> </td>\n" +
            "</tr><tr>\n" +
            "	<td></td><td><input type=\"submit\" value=\"Search\" class=\"wyslij\"></td>\n" +
            "</tr>\n" +
            "</table>\n ");       
           //out.println("<input type=\"hidden\" id=\"isRemote\" name=\"isRemote\" value=\"3487\">");      
                    
        out.println("</form>");
          
        out.println(SCRIPTREMADD);
        out.println(SCRIPTHIDDEN);
        
        out.println(FORM);
        
        
        printHomeAndBack(out, false,false,false);
    }
    
    public void printDefinedCommands(PrintWriter out, int mdl) {
        
        
        out.println(TOPNAV_STYLE );
        
        printDefinedModules(out, mdl);
        
        out.println(TABLE_STYLE_NEW);
        
        out.println("<div class=\"tab\">");
        
        if (mdl>=modules.size() || mdl<0) mdl=0;
        
        List<String> group = getGroupInModule(modules.get(mdl));
                  
        for (int i = 0; i <  group.size(); i++) {
          out.println("<button class=\"tablinks\" onclick=\"exec(event, '"+group.get(i)+"')\" id=\"defaultOpen\">"+group.get(i)+"</button>");        
        }               
        out.println("</div>");  
        
        for (int i = 0; i <  group.size(); i++) {
          out.println("<div id=\""+group.get(i)+"\" class=\"tabcontent\">"); 
          //out.println("<h3>"+group.get(i)+"</h3>"); 
          
            out.println("</br><table>");
            out.println("<tr> <th>Alias</th><th>Command</th><th> Tail </th>");
            out.println("</tr>");
         

            for (int j = 0; j <  logApi.size(); j++) {
              if ( logApi.get(j).getGroup().equals(group.get(i)) && logApi.get(j).getModule().equals(modules.get(mdl)) ) {
                      for (int k = 0; k <  logApi.get(j).getBrowseInterface().size(); k++) {
                        String command="";
                        String description="";
                        boolean found=false;
                        
                        for (int l=0; l< browseCommandApi.size(); l++){
                           if (browseCommandApi.get(l).getName().equalsIgnoreCase(logApi.get(j).getBrowseInterface().get(k))){
                                found=true;
                                command = browseCommandApi.get(l).getCommand();
                                description = browseCommandApi.get(l).getDescription();
                           }
                        }
                        
                        if (found){
                            command = command.replaceAll("#PATH", logApi.get(j).getPath()).replaceAll("#ALIAS", logApi.get(j).getAlias());
                            description = description.replaceAll("#PATH", logApi.get(j).getPath()).replaceAll("#ALIAS", logApi.get(j).getAlias());
                           
                            out.println("<tr>");
                            out.println("<td>" +   description + "</td><td>"+ command+"</td>");
                            out.println("<td><button onclick=\"execute('"+j+"','"+ k + "','"+ command+"')\">RUN</button></td>");
                            out.println("</tr>");
                        }
                      }
              }
            }

      
            out.println("</table>");            
            out.println("</div>");  
          
        }  
        
       
        out.println("<script>\n" +
            "function exec(evt, group) {\n" +
            "  var i, tabcontent, tablinks;\n" +
            "  tabcontent = document.getElementsByClassName(\"tabcontent\");\n" +
            "  for (i = 0; i < tabcontent.length; i++) {\n" +
            "    tabcontent[i].style.display = \"none\";\n" +
            "  }\n" +
            "  tablinks = document.getElementsByClassName(\"tablinks\");\n" +
            "  for (i = 0; i < tablinks.length; i++) {\n" +
            "    tablinks[i].className = tablinks[i].className.replace(\" active\", \"\");\n" +
            "  }\n" +
            "  document.getElementById(group).style.display = \"block\";\n" +
            "  evt.currentTarget.className += \" active\";\n" +
            "}\n" +
            "// Get the element with id=\"defaultOpen\" and click on it\n" +
            "document.getElementById(\"defaultOpen\").click();\n" +
            "</script>"); 

        printApiFunction(out);
        
        
    }
   
 
  
      private void printHomeAndBack(PrintWriter out, boolean printClear, boolean printRestart, boolean printSupport){
          
          out.println("<br/><button onclick=\"goHome()\">HOME</button>\n");
          out.println("  "); 
          out.println("<script>\n");
          out.println("function goHome() {\n");
          out.println("   window.location = '/';\n");
          out.println("}\n" );
          out.println( "</script>");
          
          if (printClear) out.println("<button onclick=\"goBack()\">Go Back</button>\n");
          else out.println("<button onclick=\"goBack()\">Go Back</button><br/>\n");
          out.println("\n");
          out.println("<script>\n");
          out.println("function goBack() {\n");
          out.println("    window.history.back();\n");
          out.println("}\n" );
          out.println( "</script>"); 
          
          if (printClear){
              out.println("<button onclick=\"executeClean(-1, 'clear')\">Clear</button><br/>\n");          
          }
          
          if (printRestart){
               printRestartAndKill(out);          
          }
          
          if (printSupport) out.println("</br> &copy; 2021 Landevant Research Center <b> <a href=\"mailto:support@szydlowski.biz?subject="+ Version.getVersion()+"\">support@szydlowski.biz</a></b>");
     }
      
     private void printApiFunction(PrintWriter out){
          //out.println("<h3>Tail log</h3><p style=\"color:Lime;background-color:black;width:60%;\" id=\"info\"></p>");
          out.println("<h3>Tail log</h3>");
          
          printHomeAndBack(out, true,false,true);
          
          out.println("<p style=\"width:80%;\" id=\"info\"></p>");
          
          out.println(new StringBuilder().append("<script>\n").append("function execute(id, brid, command) {\n")
                              .append( "        var xhttp = new XMLHttpRequest();\n")
                              .append( "        xhttp.onreadystatechange = function() {\n")
                              .append( "           if (this.status == 200) {")
                              .append( "               document.getElementById(\"info\").innerHTML = this.responseText;\n")
                              .append( "           }\n")
                              .append( "       };\n")
                              .append( "       xhttp.open(\"POST\", \"browse.api\", true);")
                              .append( "       xhttp.setRequestHeader(\"Content-type\", \"application/x-www-form-urlencoded\");\n")
                              .append( "       xhttp.send(\"command=\"+command+\"&id=\"+id+\"&brid=\"+brid);\n ")

                              .append(  " }\n")
                              .append( " </script>").toString());
          
        out.println(new StringBuilder().append("<script>\n").append("function executeClean(id, command) {\n")
                              .append( "        var xhttp = new XMLHttpRequest();\n")
                              .append( "        xhttp.onreadystatechange = function() {\n")
                              .append( "           if (this.status == 200) {")
                              .append( "               document.getElementById(\"info\").innerHTML = this.responseText;\n")
                              .append( "           }\n")
                              .append( "       };\n")
                              .append( "       xhttp.open(\"POST\", \"browse.api\", true);")
                              .append( "       xhttp.setRequestHeader(\"Content-type\", \"application/x-www-form-urlencoded\");\n")
                              .append( "       xhttp.send(\"command=\"+command);\n ")
                              .append(  " }\n")
                              .append( " </script>").toString());
            
    } 
     
    private void printRestartAndKill(PrintWriter out){         
          
          out.println("");
          out.println("<br/><button onclick=\"Restart('restart')\">Restart application</button>");
          
          printRestartApiFunction(out);
    }
      
     
    private void printRestartApiFunction(PrintWriter out){
          out.println("<font color=\"red\"><p id=\"info\"></p></font>");
      
          out.println(new StringBuilder().append("<script>\n").append("function Restart(type) {\n")
                              .append( "    var txt = \'Action\';\n")
                              .append( "    if (type === 'restart') {\n")
                              .append( "        txt = \"Are you sure you want to restart daemon?\";\n")
                              .append( "    } else if (type === 'kill') {\n")
                              .append( "        txt = \"Are you sure you want to kill daemon?\";\n")
                              .append( "    } \n")
                              .append( "    var r = confirm(txt);\n")
                              .append( "    if (r == true) {\n")
                              .append( "        var xhttp = new XMLHttpRequest();\n")
                              .append( "        xhttp.onreadystatechange = function() {\n")
                              .append( "           if (this.status == 200) {")
                              .append( "               document.getElementById(\"info\").innerHTML = this.responseText;\n")
                              .append( "           }\n")
                              .append( "       };\n")
                              .append( "       xhttp.open(\"POST\", \"restart.api\", true);")
                              .append( "       xhttp.setRequestHeader(\"Content-type\", \"application/x-www-form-urlencoded\");\n")
                              .append( "       xhttp.send(\"action=\"+type);\n ")
                              .append( "    } else {\n")
                              .append( "    }\n")
                              .append(  " }\n")
                              .append( " </script>").toString());
            
    }
    
    public void printDefinedModules(PrintWriter out, int k) {
  
        out.println("<div class=\"topnav\">");
        for (int i=0; i<modules.size(); i++){
             if (i==k) out.println("<a class=\"active\" href=\"browse_"+i+"\">"+modules.get(i)+"</a>");
             else out.println("<a href=\"browse_"+i+"\">"+modules.get(i)+"</a>");
        }
        
        out.println("</div>");
       
    }
    
    public List<String> getGroupInModule(String module){
             List<String> group = new ArrayList<>();
        
             for (int i=0; i<logApi.size(); i++){
                if (logApi.get(i).getModule().equals(module)){
                    boolean exist=false;
                    for (int j=0; j<group.size(); j++){
                        if (group.get(j).equals(logApi.get(i).getGroup())) exist=true;
                    }
                    if (!exist) group.add(logApi.get(i).getGroup());
                }
            } 
            return group;
    }
  
    
   private String getFileName(){
      SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyyMMddHHmmss");
      String date = simpleDateFormat.format(new Date());
      return "search-"+date+".log";
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
   
   private void printSearchResults(PrintWriter out, String data){
        
           BufferedReader br = null;
           FileReader fr = null;
           
           String file = "results/"+data;
           
           if (OSValidator.isUnix()){
                file = absolutePath + "/" + file;
            }  

            try {

                    //br = new BufferedReader(new FileReader(FILENAME));
                    fr = new FileReader(file);
                    br = new BufferedReader(fr);
                    String sCurrentLine;

                    while ((sCurrentLine = br.readLine()) != null) {
                        out.println(sCurrentLine);
                    }

            } catch (IOException e) {
                out.println("Results " + data + " not found!!!!");
                logger.error(e);

            } finally {

                    try {

                            if (br != null)
                                    br.close();

                            if (fr != null)
                                    fr.close();

                    } catch (IOException ex) {
                        out.println(ex);
                    }

        }
     
    }
       
    public void printAllSearchResults(PrintWriter out) {
        if (OSValidator.isUnix()){
             absolutePath = getClass().getProtectionDomain().getCodeSource().getLocation().getPath();
             absolutePath = "/" + absolutePath.substring(0, absolutePath.lastIndexOf("/"))+"/";
        }
               
        File folder = new File(absolutePath+"results");
        File[] listOfFiles = folder.listFiles();
        out.println("<table border=\"1\" style=\"width:50%\">");
        out.println("<tr> <th>Modification date</th><th>name</th>");
        out.println("</tr>");
        if (listOfFiles!=null){
        for (int i = 0; i < listOfFiles.length; i++) {
          out.println("<tr>");
          if (listOfFiles[i].isFile()) {
            out.println("<td>" +  new Date(listOfFiles[i].lastModified()) + "</td><td><a href=\"search-results/" + listOfFiles[i].getName() +"\" download>"+listOfFiles[i].getName()+"</a></td>");
          } else if (listOfFiles[i].isDirectory()) {
          }
          out.println("</tr>");
        }
        } 
        out.println("</table>");
        printHomeAndBack(out,true,false,false); 
        
    }
  
    
   private String replaceURL(String postData){
       
          postData = postData.replaceAll("\\+", "#PLUS");
          
         try {
              postData = URLDecoder.decode(postData, "UTF-8");
          } catch (UnsupportedEncodingException ex) {
              logger.error("replaceURL decoder");
          }
              
          postData = postData.replaceAll("%C4%84", "A");
          postData = postData.replaceAll("%C4%85", "a");
          postData = postData.replaceAll("%C4%87", "C");
          postData = postData.replaceAll("%C4%88", "c");
          postData = postData.replaceAll("%C4%98", "E");
          postData = postData.replaceAll("%C4%99", "e");
          postData = postData.replaceAll("%C5%81", "L");
          postData = postData.replaceAll("%C5%82", "l");

          postData = postData.replaceAll("%C5%83", "N");
          postData = postData.replaceAll("%C5%84", "n");

          postData = postData.replaceAll("%C3%B3", "o");
          postData = postData.replaceAll("%C3%93", "O");

          postData = postData.replaceAll("%C5%9A", "S");
          postData = postData.replaceAll("%C5%9B", "s");
          postData = postData.replaceAll("%C5%B9", "Z");
          postData = postData.replaceAll("%C5%BA", "z");
          postData = postData.replaceAll("%C5%BB", "Z");
          postData = postData.replaceAll("%C5%BC", "z");
          postData = postData.replaceAll("%20", " ");

         
          postData = postData.replaceAll("%5B", "[");
          postData = postData.replaceAll("%5C", "\\");
          postData = postData.replaceAll("%5D", "]");
          postData = postData.replaceAll("%21", "!");
          postData = postData.replaceAll("%22", "\"");
          postData = postData.replaceAll("%23", "#");
          postData = postData.replaceAll("%24", "$");
          postData = postData.replaceAll("%28", "(");
          postData = postData.replaceAll("%29", ")");
          postData = postData.replaceAll("%40", "@");
          postData = postData.replaceAll("%3F", "?");
          postData = postData.replaceAll("%25", "%");
          
          postData = postData.replaceAll("#PLUS", "+");

          return postData;          
                
    }

  
      
}