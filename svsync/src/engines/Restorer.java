package svsync.engines;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import svsync.SourceInfo;
import svsync.TargetInfo;

/**
 * @author Sval
 */
public class Restorer {
    
    public static void restore(TargetInfo target, String restoreToPath, String sourceName) {
        
        Map<String, String> sourceParams = target.getParams();
        sourceParams.put("name", sourceName);
        
        String path = sourceParams.get("_original_path_");        
        
        path = path.endsWith("/")? path + sourceName : path + "/" + sourceName;
        sourceParams.put("path", path);
        SourceInfo sourceInfo = SourceInfo.createSource(sourceParams, new ArrayList<String>(), new ArrayList<String>());
        
        Map<String, String> targetParams = new HashMap<>();
        targetParams.put("path", restoreToPath);
        TargetInfo targetInfo = TargetInfo.createTarget(targetParams);
        
        File restoreDir = new File(restoreToPath);
        if (!restoreDir.exists()) {
            restoreDir.mkdirs();
        }
        
        Syncer.sync(sourceInfo, targetInfo);
    }
}
