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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

/**
 * @author Alex Altoukhov
 */
public class Diff {
    
    public enum DiffType {
        ADDED,
        DELETED,
        CHANGED,
        MOVED
    }
    
    private Map<DiffType, Collection<FileSnapshot>> fileChanges;
    private Map<DiffType, Collection<String>> directoryChanges;
    private Stat stat;

    public Diff() {
        
        fileChanges = new HashMap<>();
        directoryChanges = new HashMap<>();
        
        for (DiffType type : DiffType.values()) {
            fileChanges.put(type, new ArrayList<FileSnapshot>());
            directoryChanges.put(type, new HashSet<String>());
        }
    }
    
    public Stat getStat() {
        return stat;
    }

    public void setStat(Stat stat) {
        this.stat = stat;
    }
    
    public Collection<FileSnapshot> getFileChanges(DiffType type) {
        return fileChanges.get(type);
    }

    public Collection<String> getDirectoryChanges(DiffType type) {
        return directoryChanges.get(type);
    }
    
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        
        builder.append("[FILES]\n\n");
        for (DiffType type : DiffType.values()) {
            builder.append(type).append(": \n");
            for (FileSnapshot file : fileChanges.get(type)) {
                builder.append("    ").append(file).append("\n");
            }
        }
        
        builder.append("\n[DIRECTORIES]\n\n");
        for (DiffType type : DiffType.values()) {
            builder.append(type).append(": \n");
            for (String dir : directoryChanges.get(type)) {
                builder.append("    ").append(dir).append("\n");
            }
        }
        
        return builder.toString();
    }
}
