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

import java.text.DecimalFormat;

/**
 * @author Alex Altoukhov
 */
public class Utils {

    public static String readableFileSize(long size) {
        if(size <= 0) return "0";
        final String[] units = new String[] { "B", "kB", "MB", "GB", "TB" };
        int digitGroups = (int) (Math.log10(size)/Math.log10(1024l));
        return new DecimalFormat("#,##0.#").format(size/Math.pow(1024l, digitGroups)) + " " + units[digitGroups];
    }    
   
    public static String readableTransferRate(long size, long millis) {
        double rate = size / ((double)millis);
        long rateInt = Math.round(rate) * 1000l;
        return readableFileSize(rateInt);
    }    
}