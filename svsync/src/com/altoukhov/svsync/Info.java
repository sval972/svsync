/*
Copyright 2015 Alex Altoukhov

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

package com.altoukhov.svsync;

import java.util.Map;

/**
 * @author Alex Altoukhov
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