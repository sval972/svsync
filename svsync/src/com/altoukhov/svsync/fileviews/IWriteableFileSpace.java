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

package com.altoukhov.svsync.fileviews;

import java.io.InputStream;
import com.altoukhov.svsync.FileSnapshot;

/**
 * @author Alex Altoukhov
 */
public interface IWriteableFileSpace {
    boolean createDirectory(String path);
    boolean deleteDirectory(String path);
    
    boolean deleteFile(String path);
    boolean writeFile(InputStream fileStream, FileSnapshot file);
    
    boolean isMoveFileSupported();
    boolean moveFile(String oldPath, String newPath);
}
