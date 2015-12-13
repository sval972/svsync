package com.altoukhov.svsync;

import java.util.HashMap;
import java.util.Map;
import com.altoukhov.svsync.engines.Analyzer;
import com.altoukhov.svsync.engines.Restorer;
import com.altoukhov.svsync.engines.Syncer;

//TODO: 1. do retries on read? read-write, not scan, maybe not required
//TODO: 2. Lock specific profile, if it's running can't run more instances

/**
 * @author Sval
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
        System.out.println("USAGE: svsync -profile <profile> [-analyze] [-restore <>]");
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