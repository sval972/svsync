package com.altoukhov.svsync;

import java.util.Map;

/**
 * @author Sval
 */
public class Info {
    
    protected String path;
    protected Map<String, String> params;

    protected Info(Map<String, String> params) {        
        this.params = params;
        
        if (params.containsKey("path")) {
            path = params.get("path");
        }
    }
    
    public String getPath() {
        return path;
    }
    
    public Map<String, String> getParams() {
        return params;
    }
}