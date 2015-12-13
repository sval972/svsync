package com.altoukhov.svsync.fileviews;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import com.altoukhov.svsync.Snapshot;

/**
 * @author Sval
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
