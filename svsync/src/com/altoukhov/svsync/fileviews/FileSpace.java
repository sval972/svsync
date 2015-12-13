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

package com.altoukhov.svsync.fileviews;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import com.altoukhov.svsync.Snapshot;

/**
 * @author Alex Altoukhov
 */
public abstract class FileSpace {
    
    protected final List<String> excludes = new ArrayList<>();
    
    public Snapshot scan(Collection<String> filters) {
        return scan(compileFilters(filters));
    }

    protected abstract Snapshot scan(List<Pattern> filters);
    
    protected static List<Pattern> compileFilters(Collection<String> filters) {
        List<Pattern> compiled = new ArrayList<>();
        
        for (String filter : filters) {
            compiled.add(Pattern.compile(filter));
        }
        
        return compiled;
    }
    
    protected static boolean isFiltered(String path, Collection<Pattern> filters) {
     
        for (Pattern pattern : filters) {
            Matcher m = pattern.matcher(path);
            
            if (m.find()) {
                return true;
            }
        }
        
        return false;
    }
    
    protected boolean isExcluded(String path) {
        
        for (String exclude : excludes) {
            if (path.startsWith(exclude)) {
                return true;
            }
        }
        
        return false;
    }
}
