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

import java.util.HashMap;
import java.util.Map;
import com.altoukhov.svsync.engines.Analyzer;
import com.altoukhov.svsync.engines.Restorer;
import com.altoukhov.svsync.engines.Syncer;

/**
 * @author Alex Altoukhov
 */
public class Svsync {
    
    public static void main(String[] args) {
        System.out.println("Starting...");
        
        if (args.length < 1) {
            printUsage();
            return;
        }
        
        Map<String, String> params = parseParameters(args);
        if (!params.containsKey("profile")) {
            printUsage();
            return;
        }
        
        Profile profile;
        try {
            profile = Profile.load(params.get("profile"));
        }
        catch (Exception ex) {            
            System.out.println("Failed to load profile: " + ex.getMessage());
            return;
        }
        
        if (params.containsKey("analyze")) {
            analyze(profile);
        }
        else if (params.containsKey("restore")) {
            restore(profile, params.get("restore"));
        }
        else {
            sync(profile);
        }
    }

    private static void printUsage() {
        System.out.println("USAGE: svsync -profile <profile> [-analyze] [-restore <local_path>]");
    }
    
    private static Map<String, String> parseParameters(String[] args) {
        Map<String, String> params = new HashMap<>();

        String key = "";
        for (int i=0; i<args.length; i++) {
            if (args[i].startsWith("-")) {
                if (!key.isEmpty()) {
                    params.put(key, "");
                }
                
                key = args[i].substring(1);
            }
            else {
                if (!key.isEmpty()) {
                    params.put(key, args[i]);
                    key = "";
                }
            }
        }
        
        if (!key.isEmpty()) {
            params.put(key, "");
        }

        return params;
    }

    private static void analyze(Profile profile) {
                        
        Stat totalStat = new Stat();
        
        for (SourceInfo source : profile.getSources()) {
            
            Analyzer analyzer = new Analyzer(source, profile.getTarget());
            if (!analyzer.init()) {
                System.out.println("Analyzer failed to init");
                return;
            }
            
            Diff diff = analyzer.analyze();
            System.out.println("Diff for " + source.getName());
            System.out.println(diff.toString());
            
            Stat stat = diff.getStat();
            System.out.println("Stat for " + source.getName());
            System.out.println(stat.toString());
            
            totalStat = totalStat.merge(stat);
        }
        
        System.out.println("Total Stat: " + totalStat.toString());
    }
    
    private static void sync(Profile profile) {
        for (SourceInfo source : profile.getSources()) {
            Syncer.sync(source, profile.getTarget());
        }
    }
    
    private static void restore(Profile profile, String path) {
        
        Map<String, String> params = profile.getTarget().getParams();        
        params.put("_original_path_", params.get("path"));
        
        for (SourceInfo source : profile.getSources()) {
            Restorer.restore(profile.getTarget(), path, source.getName());
        }
    }
}