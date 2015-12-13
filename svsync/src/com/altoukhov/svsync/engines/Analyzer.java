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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import com.altoukhov.svsync.Diff;
import com.altoukhov.svsync.FileSpaceFactory;
import com.altoukhov.svsync.Snapshot;
import com.altoukhov.svsync.SourceInfo;
import com.altoukhov.svsync.TargetInfo;
import com.altoukhov.svsync.fileviews.IReadableFileSpace;
import com.altoukhov.svsync.fileviews.IScannableFileSpace;
import com.altoukhov.svsync.fileviews.IWriteableFileSpace;

/**
 * @author Alex Altoukhov
 */
public class Analyzer {
    
    private SourceInfo sourceInfo;
    private TargetInfo targetInfo;
    
    private IReadableFileSpace source;
    private IWriteableFileSpace target;

    private Snapshot sourceSnapshot;
    private Snapshot targetSnapshot;
   
    private Thread targetScanThread;
    
    public Analyzer(SourceInfo sourceInfo, TargetInfo targetInfo) {
        this.sourceInfo = sourceInfo;
        this.targetInfo = targetInfo;
        
        targetScanThread = new Thread(new TargetScanJob());
    }
    
    public IReadableFileSpace getSource() {
        return source;
    }

    public IWriteableFileSpace getTarget() {
        return target;
    }
    
    public IWriteableFileSpace getCache() {
        return targetSnapshot;
    }
    
    public boolean init() {
        
        source = FileSpaceFactory.create(sourceInfo);
        if (source == null) {
            System.out.println("Failed to initialize source: " + sourceInfo.getPath());
            return false;
        }
        
        target = FileSpaceFactory.create(targetInfo, sourceInfo.getName());
        if (target == null) {
            System.out.println("Failed to initialize target: " + targetInfo.getPath());
            return false;
        }
        
        return true;
    }
    
    public Diff analyze() {

        System.out.println("Analyzing " + sourceInfo.getName());
        
        targetScanThread.start();
        
        sourceSnapshot = loadFromCacheOrScan(sourceInfo, source);
        if (sourceSnapshot == null) {
            System.out.println("Failed to scan source: " + sourceInfo.getPath());
            return null;
        }
        try {
            targetScanThread.join();
        }
        catch (InterruptedException ex) {
            System.out.println("Target scan thread was interrupted: " + ex.getMessage());
        }
        
        if (targetSnapshot == null) {
            System.out.println("Failed to scan target: " + targetInfo.getPath());
            return null;
        }

        return Snapshot.diff(sourceSnapshot, targetSnapshot, target.isMoveFileSupported());
    }        
    
    public void updateCache() {
        boolean isCacheEnabled = targetInfo.getParams().containsKey("cache-days");
        
        if (isCacheEnabled) {
            targetSnapshot.toFile(snapshotFileName(targetInfo, sourceInfo.getName()));
        }
    }
    
    private static Snapshot loadFromCacheOrScan(SourceInfo sourceInfo, IReadableFileSpace source) {
        if (sourceInfo == null) return null;
        return loadFromCacheOrScan(sourceInfo.getParams(), snapshotFileName(sourceInfo), (IScannableFileSpace)source, sourceInfo.getFilters());
    }
    
    private static Snapshot loadFromCacheOrScan(TargetInfo targetInfo, String sourceName, IWriteableFileSpace target) {
        if (targetInfo == null) return null;
        return loadFromCacheOrScan(targetInfo.getParams(), snapshotFileName(targetInfo, sourceName), (IScannableFileSpace)target, new ArrayList<String>());
    }
    
    private static Snapshot loadFromCacheOrScan(Map<String, String> infoParams, String snapshotFilePath, IScannableFileSpace fileSpace, Collection<String> filters) {
        boolean isCacheEnabled = infoParams.containsKey("cache-days");
        boolean shouldScan = true;
        
        Snapshot snap = Snapshot.fromFile(snapshotFilePath);
        
        if (snap != null) {
            if (isCacheEnabled) {
                DateTime cacheExpiration = snap.getTimestamp().plusDays(Integer.parseInt(infoParams.get("cache-days")));
                shouldScan = cacheExpiration.isBefore(DateTime.now(DateTimeZone.UTC));
            }
        }
        
        if (shouldScan) {
            snap = fileSpace.scan(filters);
            if ((snap != null) && isCacheEnabled) {
                snap.toFile(snapshotFilePath);
            }
        }
        
        return snap;        
    }
    
    private static String snapshotFileName(TargetInfo targetInfo, String sourceName) {
        return snapshotFileName(targetInfo.getPath(), sourceName);
    }
    
    private static String snapshotFileName(SourceInfo sourceInfo) {
        return snapshotFileName(sourceInfo.getPath(), sourceInfo.getName());
    }
    
    private static String snapshotFileName(String path, String name) {
        return path.replaceAll("[^a-zA-Z0-9]", "_") + "_" + name + ".snap";
    }
    
    private class TargetScanJob implements Runnable {
        
        @Override
        public void run() {
            targetSnapshot = loadFromCacheOrScan(targetInfo, sourceInfo.getName(), target);
        }
    }
}
