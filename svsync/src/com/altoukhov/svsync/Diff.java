package com.altoukhov.svsync;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

/**
 * @author Sval
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
