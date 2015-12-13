package svsync;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author Sval
 */
public class SourceInfo extends Info {

    private String name;
    private List<String> excludes;
    private List<String> filters;
    
    private SourceInfo(Map<String, String> params, List<String> excludes, List<String> filters) {
        super(params);
                
        if (params.containsKey("name")) {
            name = params.get("name");
        }

        if ((path != null) && path.endsWith(File.separator)) {
            path = path.substring(0, path.length()-1);
        }            
        
        this.excludes = normalizeExcludes(excludes);
        this.filters = filters;
    }

    public static SourceInfo createSource(Map<String, String> params, List<String> excludes, List<String> filters) {
        return new SourceInfo(params, excludes, filters);
    }
    
    public String getName() {
        return name;
    }

    public List<String> getExcludes() {
        return excludes;
    }

    public List<String> getFilters() {
        return filters;
    }
    
    private List<String> normalizeExcludes(List<String> excludes) {

        List<String> normalizedExcludes = new ArrayList<>();
        for (String exclude : excludes) {
            normalizedExcludes.add(normalizeExcludePath(exclude));
        }

        return normalizedExcludes;
    }
    
    private String normalizeExcludePath(String absolute) {
        if (!absolute.startsWith(path)) return absolute;
        
        String relative = absolute.substring(path.length());
        if (relative.startsWith(File.separator)) {
            relative = relative.substring(1);
        }
        
        return relative.replace("\\", "/");
    }    
}