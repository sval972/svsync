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

import java.util.Objects;
import org.joda.time.DateTime;

/**
 * @author Alex Altoukhov
 */
public class FileSnapshot {

    private String fileName;
    private DateTime modifiedTimestamp;
    private long fileSize;
    private String relativePath;
    private String previousPath;
    
    public String getFileName() {
        return fileName;
    }

    public DateTime getModifiedTimestamp() {
        return modifiedTimestamp;
    }

    public long getFileSize() {
        return fileSize;
    }

    public String getRelativePath() {
        return relativePath;
    }
    
    public String getPreviousPath() {
        return previousPath;
    }

    public void setPreviousPath(String previousPath) {
        this.previousPath = previousPath;
    }
    
    public FileSnapshot(String name, long size, DateTime lastModified, String relativePath) {
        this.fileName = name;
        this.fileSize = size;
        this.modifiedTimestamp = lastModified.minusMillis(lastModified.getMillisOfSecond());
        this.relativePath = relativePath;
    }

    @Override
    public String toString() {
        return "FileSnapshot{" + "fileName=" + fileName + ", modifiedTimestamp=" + modifiedTimestamp + ", fileSize=" + fileSize + ", relativePath=" + relativePath + ((previousPath != null)?(", previousPath=" + previousPath) : "") + '}';
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 53 * hash + Objects.hashCode(this.fileName);
        hash = 53 * hash + Objects.hashCode(this.modifiedTimestamp);
        hash = 53 * hash + (int) (this.fileSize ^ (this.fileSize >>> 32));
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final FileSnapshot other = (FileSnapshot) obj;
        if (!Objects.equals(this.fileName, other.fileName)) {
            return false;
        }
        if (!Objects.equals(this.modifiedTimestamp, other.modifiedTimestamp)) {
            return false;
        }
        if (this.fileSize != other.fileSize) {
            return false;
        }
        return true;
    }
    
    public Boolean isLargeFile() {
        return fileSize > Integer.MAX_VALUE;
    }
}
