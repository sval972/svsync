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

import com.google.common.base.Stopwatch;
import com.google.common.collect.Iterables;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import com.altoukhov.svsync.Diff;
import com.altoukhov.svsync.FileSnapshot;
import com.altoukhov.svsync.SourceInfo;
import com.altoukhov.svsync.TargetInfo;
import com.altoukhov.svsync.Utils;
import com.altoukhov.svsync.fileviews.IReadableFileSpace;
import com.altoukhov.svsync.fileviews.IWriteableFileSpace;

/**
 * @author Alex Altoukhov
 */
public class Syncer {
    
    public static void sync(SourceInfo sourceInfo, TargetInfo targetInfo) {

        Analyzer analyzer = new Analyzer(sourceInfo, targetInfo);
        if (!analyzer.init()) {
            System.out.println("Analyzer failed to init");
            return;
        }

        Diff diff = analyzer.analyze();
        System.out.println("Diff for " + sourceInfo.getName());
        System.out.println(diff.toString());
                
        IReadableFileSpace source = analyzer.getSource();
        IWriteableFileSpace target = analyzer.getTarget();
        IWriteableFileSpace cache = analyzer.getCache();
        
        System.out.println("Syncing " + sourceInfo.getName());
        
        // Create added directories
        List<String> addedDirectories = new ArrayList<>(diff.getDirectoryChanges(Diff.DiffType.ADDED));
        Collections.sort(addedDirectories);
        
        for (String dir : addedDirectories) {
            System.out.println("Creating directory " + dir);
            target.createDirectory(dir);
            cache.createDirectory(dir);
        }

        // Add + update files
        for (FileSnapshot file : Iterables.concat(diff.getFileChanges(Diff.DiffType.ADDED), diff.getFileChanges(Diff.DiffType.CHANGED))) {
            writeFile(source, target, file);
            cache.writeFile(null, file);
        }

        // Delete files
        for (FileSnapshot file : diff.getFileChanges(Diff.DiffType.DELETED)) {
            System.out.println("Deleting file " + file.getRelativePath());
            target.deleteFile(file.getRelativePath());
            cache.deleteFile(file.getRelativePath());
        }
        
        // Move files
        for (FileSnapshot file : diff.getFileChanges(Diff.DiffType.MOVED)) {
            System.out.println("Moving file from " + file.getPreviousPath() + " to " + file.getRelativePath());
            target.moveFile(file.getPreviousPath(), file.getRelativePath());
            cache.moveFile(file.getPreviousPath(), file.getRelativePath());
        }

        // Delete removed directories (should be empty by this point)
        List<String> deletedDirectories = new ArrayList<>(diff.getDirectoryChanges(Diff.DiffType.DELETED));
        Collections.sort(deletedDirectories, Collections.reverseOrder());        
        
        for (String dir : deletedDirectories) {
            System.out.println("Deleting directory " + dir);
            target.deleteDirectory(dir);
            cache.deleteDirectory(dir);
        }
        
        analyzer.updateCache();
    }
    
    private static void writeFile(IReadableFileSpace source, IWriteableFileSpace target, FileSnapshot file) {
        
        System.out.println("Writing file " + file.getRelativePath() + ", " + Utils.readableFileSize(file.getFileSize()));
        
        Stopwatch stopwatch = Stopwatch.createStarted();
        boolean success = target.writeFile(source.readFile(file.getRelativePath()), file);
        stopwatch.stop();
        
        if (success) {
            System.out.println("Write speed was " + Utils.readableTransferRate(file.getFileSize(), stopwatch.elapsed(TimeUnit.MILLISECONDS)) + "/s");
        }
        else {
            System.out.println("Failed to write file " + file.getRelativePath());
        }
    }
}
