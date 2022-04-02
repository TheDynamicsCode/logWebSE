/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package biz.szydlowski.logwebse;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 *
 * @author dkbu
 */
public class LogApi {
    
   
    private String dateFormat;
    private String alias;
    private String template_path;
    private String return_path;
    private String module;
    private String group;     
    private String remote; 
    private boolean isRemote; 
    private String hostname; 
    private String user;
    private String password;
    private String port;    
    private List<String> browse;
    private List<Boolean> lock;
    
    /**
     * @param password the password to set
     */
    public void setPassword(String password) {
        this.password = password;
    }

    /**
     * @return the lock
     */
    public boolean getLock(int i) {
        return  this.lock.get(i);
    }

    /**
     * @param lock the lock to set
     */
    public void setLock(int i, boolean lock) {
        this.lock.set(i, lock);
    }

    public LogApi (){
        browse = new ArrayList<>();
        lock=new ArrayList<>();
        hostname="hostname"; 
        user="user";
        password="password";
        port="22";
        isRemote=false;
        module="default";
        dateFormat="default";
    }
    
    /**
     * @return the alias
     */
    public String getAlias() {
        return alias;
    }

    /**
     * @param alias the alias to set
     */
    public void setAlias(String alias) {
        this.alias = alias;
    }

    /**
     * @return the group
     */
    public String getGroup() {
        return group;
    }

    /**
     * @param group the group to set
     */
    public void setGroup(String group) {
        this.group = group;
    }

    /**
     * @return the path
     */
    public String getPath() {
        String dateformat = getDateFormat();
         
        return_path = template_path;
                                
        if (!dateformat.equals("default")){
                SimpleDateFormat dateFormat = new SimpleDateFormat(dateformat);

                try {
                    String stringDate = dateFormat.format(new Date());
                     return_path = template_path.replaceAll("#DATE", stringDate);
                } catch (Exception e){}
         }
        
        return return_path;
    }

    /**
     * @param path the path to set
     */
    public void setTemplatePath(String path) {
        this.template_path = path;
    }
    
       /**
     * @return the browse
     */
    public List<String> getBrowseInterface() {
        return browse;
    }

    /**
     * @param browse the browse to set
     */
    public void setBrowseInterface(String browse) {        
        for (String s :  browse.split(",")){
          this.browse.add(s);
          this.lock.add(false);
        }
    }

    /**
     * @param remote the remote to set
     */
    public void setRemote(String remote) {
        this.remote = remote;
        if (!remote.equals("ERROR")){
            //user@adres:port:password
            String tmp [] =  remote.split("@");
            if (tmp.length==2){
                user=tmp[0];
                String tmp2 [] =  tmp[1].split(":");
                if (tmp2.length==3){
                    hostname=tmp2[0];
                    port=tmp2[1];
                    setPassword(tmp2[2]);
                }

            }
            isRemote=true;
        } else isRemote=false;
    }

    /**
     * @return the isRemote
     */
    public boolean isRemote() {
        return isRemote;
    }

    /**
     * @return the hostname
     */
    public String getHostname() {
        return hostname;
    }

    /**
     * @return the user
     */
    public String getUser() {
        return user;
    }

    /**
     * @return the password
     */
    public String getPassword() {
        return password;
    }

    /**
     * @return the port
     */
    public String getPort() {
        return port;
    }

    /**
     * @return the module
     */
    public String getModule() {
        return module;
    }

    /**
     * @param module the module to set
     */
    public void setModule(String module) {
        this.module = module;
    }

    /**
     * @return the dateFormat
     */
    public String getDateFormat() {
        return dateFormat;
    }

    /**
     * @param dateFormat the dateFormat to set
     */
    public void setDateFormat(String dateFormat) {
        this.dateFormat = dateFormat;
    }
 
}
