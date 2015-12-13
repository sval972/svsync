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

/**
 * @author Alex Altoukhov
 */
public class Stat {
    
    private long sourceSize = 0;
    private long updateSize = 0;
    private long newSpaceSize = 0;
    private long transferSize = 0;
    
    public long getSourceSize() {
        return sourceSize;
    }

    public long getUpdateSize() {
        return updateSize;
    }

    public long getNewSpaceSize() {
        return newSpaceSize;
    }

    public long getTransferSize() {
        return transferSize;
    }    
    
    public Stat() {
    }
    
    public Stat(Snapshot source, Snapshot target, Diff diff) {
        
        for (FileSnapshot file : source.getFiles().values()) {
            sourceSize += file.getFileSize();
        }        
        
        for (FileSnapshot file : diff.getFileChanges(Diff.DiffType.CHANGED)) {
            updateSize += file.getFileSize();
        }
        
        long addedSize = 0;
        for (FileSnapshot file : diff.getFileChanges(Diff.DiffType.ADDED)) {
            addedSize += file.getFileSize();
        }
        
        long deletedSize = 0;
        for (FileSnapshot file : diff.getFileChanges(Diff.DiffType.DELETED)) {
            deletedSize += file.getFileSize();
        }
        
        long changeSize = 0;
        for (FileSnapshot file : diff.getFileChanges(Diff.DiffType.CHANGED)) {
            changeSize += target.getFiles().get(file.getRelativePath()).getFileSize();
        }        
        
        transferSize = addedSize + updateSize;
        newSpaceSize = addedSize - deletedSize + (updateSize - changeSize);
    }
    
    public Stat merge(Stat other) {
        
        Stat merged = new Stat();
        merged.sourceSize = this.sourceSize + other.sourceSize;
        merged.updateSize = this.updateSize + other.updateSize;
        merged.newSpaceSize = this.newSpaceSize + other.newSpaceSize;
        merged.transferSize = this.transferSize + other.transferSize;
        
        return merged;
    }
    
    @Override
    public String toString() {
        return "Stat{" 
                + "sourceSize=" + Utils.readableFileSize(sourceSize)
                + ", updateSize=" + Utils.readableFileSize(updateSize)
                + ", newSpaceSize=" + Utils.readableFileSize(newSpaceSize)
                + ", transferSize=" + Utils.readableFileSize(transferSize)
                + '}';
    }
    
}
