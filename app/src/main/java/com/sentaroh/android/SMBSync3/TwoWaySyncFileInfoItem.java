package com.sentaroh.android.SMBSync3;

/*
The MIT License (MIT)
Copyright (c) 2020 Sentaroh

Permission is hereby granted, free of charge, to any person obtaining a copy of
this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights to use,
copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software,
and to permit persons to whom the Software is furnished to do so, subject to
the following conditions:

The above copyright notice and this permission notice shall be included in all copies or
substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED,
INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR
PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
OTHER DEALINGS IN THE SOFTWARE.

*/

// Two way sync is not yet complete, so do not use it.

class TwoWaySyncFileInfoItem {
    private boolean isPairAReferenced =false;
    private boolean isPairAUpdated =false;
    private long pairALastSyncTime =0L;
    private boolean isPairADeleted =false;
    private boolean isPairADeleteRequired =false;

    private String pairAFilePath ="";
    private boolean isPairADirectory =false;
    private long pairAFileSize =0L;
    private long pairAFileLastModified =0L;

//    private boolean isPairBReferenced =false;
//    private boolean isPairBUpdated =false;
//    private long pairBLastSyncTime =0L;
//    private boolean isPairBDeleted =false;
//    private boolean isPairBDeleteRequired =false;
//
//    private String pairBFilePath ="";
//    private long pairBFileSize =0L;
//    private long pairBFileLastModified =0L;

    public TwoWaySyncFileInfoItem() {}

    public TwoWaySyncFileInfoItem(boolean a_ref, boolean a_upd, boolean a_directory, long a_sync_time, String a_fp, long a_file_size, long a_last_modified) {
        pairALastSyncTime =a_sync_time;
        isPairAReferenced =a_ref;
        isPairAUpdated =a_upd;
        isPairADirectory=a_directory;

        pairAFilePath =a_fp;
        pairAFileSize =a_file_size;
        pairAFileLastModified =a_last_modified;

    }

//    public TwoWaySyncFileInfoItem(boolean a_ref, boolean a_upd, long a_sync_time, String a_fp, long a_file_size, long a_last_modified,
//                                  boolean b_ref, boolean b_upd, long b_sync_time, String b_fp, long b_file_size, long b_last_modified) {
//        pairALastSyncTime =a_sync_time;
//        isReferenced =a_ref;
//        isUpdated =a_upd;
//
//        pairAFilePath =a_fp;
//        pairAFileSize =a_file_size;
//        pairAFileLastModified =a_last_modified;
//
//        pairBLastSyncTime =b_sync_time;
//        isPairBReferenced =b_ref;
//        isPairBUpdated =b_upd;
//
//        pairBFilePath =b_fp;
//        pairBFileSize =b_file_size;
//        pairBFileLastModified =b_last_modified;
//    }

    public long getLastSyncTime() {return pairALastSyncTime;}
    public void setLastSyncTime(long last_sync_time) {
        pairALastSyncTime =last_sync_time;}

    public long getFileSize() {return pairAFileSize;}
    public void setFileSize(long file_size) {
        pairAFileSize =file_size;}

    public long getFileLastModified() {return pairAFileLastModified;}
    public void setFileLastModified(long last_modified) {
        pairAFileLastModified =last_modified;}

    public boolean isReferenced() {return isPairAReferenced;}
    public void setReferenced(boolean referenced) {
        isPairAReferenced = referenced;}

    public boolean isUpdated() {return isPairAUpdated;}
    public void setUpdated(boolean updated) {
        isPairAUpdated = updated;}

//    public boolean isDeleted() {return isPairADeleted;}
//    public void setDeleted(boolean deleted) {
//        isPairADeleted = deleted;}

    public boolean isDirectory() {return isPairADirectory;}
    public void setDirectory(boolean directory) {
        isPairADirectory = directory;}

//    public boolean isDeleteRequired() {return isPairADeleteRequired;}
//    public void setDeleteRequired(boolean delete_required) {
//        this.isPairADeleteRequired = delete_required;}

    public String getFilePath() {return pairAFilePath;}
    public void setFilePath(String file_path) {
        pairAFilePath =file_path;}
    public String getParent() {
        String[] path_parts=pairAFilePath.split("/");
        String last_parts=path_parts[path_parts.length-1];
        String parent=pairAFilePath.substring(0, (pairAFilePath.length()-last_parts.length()-1));
        return parent;
    }

//    public long getPairBLastSyncTime() {return pairBLastSyncTime;}
//    public void setPairBLastSyncTime(long last_sync_time) {
//        pairBLastSyncTime =last_sync_time;}
//
//    public long getPairBFileSize() {return pairBFileSize;}
//    public void setPairBFileSize(long file_size) {
//        pairBFileSize =file_size;}
//
//    public long getPairBFileLastModified() {return pairBFileLastModified;}
//    public void setPairBFileLastModified(long last_modified) {
//        pairBFileLastModified =last_modified;}
//
//    public boolean isPairBReferenced() {return isPairBReferenced;}
//    public void setPairBReferenced(boolean referenced) {
//        isPairBReferenced = referenced;}
//
//    public boolean isPairBUpdated() {return isPairBUpdated;}
//    public void setPairBUpdated(boolean updated) {
//        isPairBUpdated = updated;}
//
//    public boolean isPairBDeleted() {return isPairBDeleted;}
//    public void setPairBDeleted(boolean deleted) {
//        isPairBDeleted = deleted;}
//
//    public boolean isPairBDeleteRequired() {return isPairBDeleteRequired;}
//    public void setPairBDeleteRequired(boolean delete_required) {
//        isPairBDeleteRequired = delete_required;}
//
//    public String getPairBFilePath() {return pairBFilePath;}
//    public void setPairBFilePath(String file_path) {
//        pairBFilePath =file_path;}
        
}
