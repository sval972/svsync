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

package com.altoukhov.svsync.engines;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import com.altoukhov.svsync.SourceInfo;
import com.altoukhov.svsync.TargetInfo;

/**
 * @author Alex Altoukhov
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
