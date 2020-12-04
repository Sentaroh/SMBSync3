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

import android.content.Context;

import com.sentaroh.android.Utilities3.NotifyEvent;
import com.sentaroh.android.Utilities3.SafFile3;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.Externalizable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import static com.sentaroh.android.SMBSync3.Constants.GENERAL_IO_BUFFER_SIZE;
import static com.sentaroh.android.SMBSync3.Constants.LOCAL_FILE_LAST_MODIFIED_FILE_LIST_NAME_V1;


public class FileLastModifiedTime {
    final public static boolean isCurrentListWasDifferent(
            ArrayList<FileLastModifiedTimeEntry> curr_last_modified_list,
            ArrayList<FileLastModifiedTimeEntry> new_last_modified_list,
            String fp, long l_lm, long r_lm, int timeDifferenceLimit, boolean ignore_dst_time, int dstOffset) {
        boolean result = false;
        if (curr_last_modified_list.size() != 0) {
            int idx = Collections.binarySearch(curr_last_modified_list,
                    new FileLastModifiedTimeEntry(fp, 0, 0, false),
                    new Comparator<FileLastModifiedTimeEntry>() {
                        @Override
                        public int compare(FileLastModifiedTimeEntry ci,
                                           FileLastModifiedTimeEntry ni) {
                            return ci.getFilePath().compareToIgnoreCase(ni.getFilePath());
                        }
                    });
            if (idx >= 0) {
                long diff_lcl = Math.abs(curr_last_modified_list.get(idx).getLocalFileLastModified() - l_lm);
                long diff_rmt = Math.abs(curr_last_modified_list.get(idx).getRemoteFileLastModified() - r_lm);
                if (diff_lcl > timeDifferenceLimit || diff_rmt > timeDifferenceLimit) {
                    if (ignore_dst_time) {
                        if ((Math.abs((diff_lcl - dstOffset)) > timeDifferenceLimit) || (Math.abs((diff_rmt - dstOffset)) > timeDifferenceLimit))
                            result = true;
                    } else {
                        result=true;
                    }
                }
                curr_last_modified_list.get(idx).setReferenced(true);
            } else {
                result = isAddedListWasDifferent(
                        curr_last_modified_list, new_last_modified_list,
                        fp, l_lm, r_lm, timeDifferenceLimit, ignore_dst_time, dstOffset);
            }
        } else {
            result = isAddedListWasDifferent(
                    curr_last_modified_list, new_last_modified_list,
                    fp, l_lm, r_lm, timeDifferenceLimit, ignore_dst_time, dstOffset);
        }
        return result;
    }

    final public static FileLastModifiedTimeEntry isFileItemExists(
            ArrayList<FileLastModifiedTimeEntry> curr_last_modified_list,
            ArrayList<FileLastModifiedTimeEntry> new_last_modified_list,
            String fp) {
        FileLastModifiedTimeEntry result = null;
        if (curr_last_modified_list.size() != 0) {
            int idx = Collections.binarySearch(curr_last_modified_list,
                    new FileLastModifiedTimeEntry(fp, 0, 0, false),
                    new Comparator<FileLastModifiedTimeEntry>() {
                        @Override
                        public int compare(FileLastModifiedTimeEntry ci,
                                           FileLastModifiedTimeEntry ni) {
                            return ci.getFilePath().compareToIgnoreCase(ni.getFilePath());
                        }
                    });
            if (idx >= 0) {
                result=curr_last_modified_list.get(idx);
            }
        }
        if (result==null) {
            for (FileLastModifiedTimeEntry fli : new_last_modified_list) {
                if (fli.getFilePath().equals(fp)) {
                    result=fli;
                    break;
                }
            }
        }
        return result;
    }

    final public static boolean isAddedListWasDifferent(
            ArrayList<FileLastModifiedTimeEntry> curr_last_modified_list,
            ArrayList<FileLastModifiedTimeEntry> new_last_modified_list,
            String fp, long l_lm, long r_lm, int timeDifferenceLimit, boolean ignore_dst_time, int dstOffset) {
        boolean result = false, found = false;
        if (new_last_modified_list.size() == 0) result = true;
        else for (FileLastModifiedTimeEntry fli : new_last_modified_list) {
            if (fli.getFilePath().equals(fp)) {
                found = true;
                long diff_lcl = Math.abs(fli.getLocalFileLastModified() - l_lm);
                long diff_rmt = Math.abs(fli.getRemoteFileLastModified() - r_lm);
                if (diff_lcl > timeDifferenceLimit || diff_rmt > timeDifferenceLimit) {
                    if (ignore_dst_time) {
                        if ((Math.abs((diff_lcl - dstOffset)) > timeDifferenceLimit) || (Math.abs((diff_rmt - dstOffset)) > timeDifferenceLimit))
                            result = true;
                    } else {
                        result=true;
                    }
                }
                break;
            }
        }
        if (!found) addLastModifiedItem(
                curr_last_modified_list, new_last_modified_list, fp, l_lm, r_lm);
        return result;
    }

    final public static void addLastModifiedItem(
            ArrayList<FileLastModifiedTimeEntry> curr_last_modified_list,
            ArrayList<FileLastModifiedTimeEntry> new_last_modified_list,
            String fp, long l_lm, long r_lm) {
//		Thread.dumpStack();
        if (new_last_modified_list.size() > 1000)
            mergeLastModifiedList(curr_last_modified_list, new_last_modified_list);
        FileLastModifiedTimeEntry fli = new FileLastModifiedTimeEntry
                (fp, l_lm, r_lm, true);
        new_last_modified_list.add(fli);
    }

    final public static boolean deleteLastModifiedItem(
            ArrayList<FileLastModifiedTimeEntry> curr_last_modified_list,
            ArrayList<FileLastModifiedTimeEntry> new_last_modified_list,
            String fp) {
        boolean deleted=false;
        int idx = Collections.binarySearch(curr_last_modified_list,
                new FileLastModifiedTimeEntry(fp, 0, 0, false),
                new Comparator<FileLastModifiedTimeEntry>() {
                    @Override
                    public int compare(FileLastModifiedTimeEntry ci,
                                       FileLastModifiedTimeEntry ni) {
                        return ci.getFilePath().compareToIgnoreCase(ni.getFilePath());
                    }
                });
        if (idx >= 0) {
            curr_last_modified_list.remove(idx);
            deleted=true;
        }
        else for (FileLastModifiedTimeEntry fli : new_last_modified_list) {
            if (fli.getFilePath().equals(fp)) {
                new_last_modified_list.remove(fli);
                deleted=true;
                break;
            }
        }
        return deleted;
    }

    final public static boolean updateLastModifiedList(
            ArrayList<FileLastModifiedTimeEntry> curr_last_modified_list,
            ArrayList<FileLastModifiedTimeEntry> new_last_modified_list,
            String target_path, long l_lm, long r_lm) {
//		long b_time=System.currentTimeMillis();
        boolean result = false;
        int idx = Collections.binarySearch(curr_last_modified_list,
                new FileLastModifiedTimeEntry(target_path, 0, 0, false),
                new Comparator<FileLastModifiedTimeEntry>() {
                    @Override
                    public int compare(FileLastModifiedTimeEntry ci,
                                       FileLastModifiedTimeEntry ni) {
                        return ci.getFilePath().compareToIgnoreCase(ni.getFilePath());
                    }
                });
//		long et1=System.currentTimeMillis()-b_time;
        if (idx >= 0) {
            curr_last_modified_list.get(idx).setLocalFileLastModified(l_lm);
            curr_last_modified_list.get(idx).setRemoteFileLastModified(r_lm);
            curr_last_modified_list.get(idx).setReferenced(true);
            result = true;
        } else for (FileLastModifiedTimeEntry fli : new_last_modified_list) {
            if (fli.getFilePath().equals(target_path)) {
                fli.setLocalFileLastModified(l_lm);
                fli.setRemoteFileLastModified(r_lm);
                result = true;
                break;
            }
        }
        return result;

    }

    final public static boolean isSetLastModifiedFunctional(String app_specific_dir) {
        boolean result = false;
        File lf = new File(app_specific_dir + "/" + "SMBSyncLastModifiedTest.temp");
        try {
            if (lf.exists()) lf.delete();
            lf.createNewFile();
            result = lf.setLastModified(1000*1000);
            lf.delete();
        } catch (IOException e) {
			e.printStackTrace();
        }
        return result;
    }

    final static private void mergeLastModifiedList(
            ArrayList<FileLastModifiedTimeEntry> curr_last_modified_list,
            ArrayList<FileLastModifiedTimeEntry> new_last_modified_list) {
        curr_last_modified_list.addAll(new_last_modified_list);
        new_last_modified_list.clear();
        Collections.sort(curr_last_modified_list,
            new Comparator<FileLastModifiedTimeEntry>() {
                @Override
                public int compare(FileLastModifiedTimeEntry ci, FileLastModifiedTimeEntry ni) {
                    return ci.getFilePath().compareToIgnoreCase(ni.getFilePath());
                }
            });
    }

    final public static void saveLastModifiedList(Context c, String dir,
                                                  ArrayList<FileLastModifiedTimeEntry> curr_last_modified_list,
                                                  ArrayList<FileLastModifiedTimeEntry> new_last_modified_list) {
        if (new_last_modified_list.size() != 0) {
            mergeLastModifiedList(curr_last_modified_list, new_last_modified_list);
        }
        try {
//			long b_time=System.currentTimeMillis();
            SafFile3 lf_tmp = new SafFile3(c, dir + "/lflm/");
            if (!lf_tmp.exists()) lf_tmp.mkdirs();
            String fn = LOCAL_FILE_LAST_MODIFIED_FILE_LIST_NAME_V1;
            lf_tmp = new SafFile3(c, dir + "/lflm/" + fn + ".tmp");
            lf_tmp.deleteIfExists();
            lf_tmp.createNewFile();

            SafFile3 lf_save = new SafFile3(c, dir + "/lflm/" + fn);

            OutputStream fos = lf_tmp.getOutputStream();
            BufferedOutputStream bos = new BufferedOutputStream(fos, GENERAL_IO_BUFFER_SIZE);
            ZipOutputStream zos = new ZipOutputStream(bos);
            ZipEntry ze = new ZipEntry("list.txt");
            zos.putNextEntry(ze);
            OutputStreamWriter osw = new OutputStreamWriter(zos, "UTF-8");
            BufferedWriter bw = new BufferedWriter(osw, GENERAL_IO_BUFFER_SIZE);

            StringBuffer pl = new StringBuffer(512);
            String last_fp = "";
            String new_fp = "";
            for (FileLastModifiedTimeEntry lfme : curr_last_modified_list) {
                new_fp = lfme.getFilePath();
                if (!last_fp.equals(new_fp)) {
                    boolean f_exists = true;
                    if (!lfme.isReferenced()) {
                        last_fp = new_fp;
                        File slf = new File(last_fp);
                        f_exists = slf.exists();
                    }
                    if (f_exists || new_fp.equals(LOCAL_FILE_LAST_MODIFIED_FILE_LIST_NAME_V1)) {
                        pl.append(new_fp)
                                .append("\t")
                                .append(String.valueOf(lfme.getLocalFileLastModified()))
                                .append("\t")
                                .append(String.valueOf(lfme.getRemoteFileLastModified()))
                                .append("\n");
                        bw.append(pl);
                        pl.setLength(0);
                    } else {
                    }
                } else {
                }
            }
		    bw.flush();
            bw.close();
            lf_save.deleteIfExists();
            lf_tmp.renameTo(lf_save);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    final public static boolean isLastModifiedFileV1Exists(String dir) {
        boolean exists = false;

        File lf = new File(dir + "/lflm/" +LOCAL_FILE_LAST_MODIFIED_FILE_LIST_NAME_V1);
        exists = lf.exists();

        return exists;
    }

    final public static boolean loadLastModifiedList(Context c, String dir,
                                                     ArrayList<FileLastModifiedTimeEntry> curr_last_modified_list,
                                                     ArrayList<FileLastModifiedTimeEntry> new_last_modified_list, NotifyEvent p_ntfy) {
        boolean list_was_corrupted = false;
        curr_last_modified_list.clear();
        new_last_modified_list.clear();

        File lf1 = new File(dir + "/lflm/" +LOCAL_FILE_LAST_MODIFIED_FILE_LIST_NAME_V1);
        if (lf1.exists()) loadLastModifiedListV1(c, dir,
                curr_last_modified_list, new_last_modified_list, p_ntfy);


        return list_was_corrupted;
    }

    final public static boolean loadLastModifiedListV1(Context c, String dir,
                                                       ArrayList<FileLastModifiedTimeEntry> curr_last_modified_list,
                                                       ArrayList<FileLastModifiedTimeEntry> new_last_modified_list, NotifyEvent p_ntfy) {
        curr_last_modified_list.clear();
        new_last_modified_list.clear();
        boolean list_was_corrupted = false;
        try {
//			long b_time=System.currentTimeMillis();
            SafFile3 lf = new SafFile3(c, dir + "/lflm/" +LOCAL_FILE_LAST_MODIFIED_FILE_LIST_NAME_V1);
            InputStream fis = lf.getInputStream();
            BufferedInputStream bis = new BufferedInputStream(fis, 1024 * 1024 * 1);
            ZipInputStream zis = new ZipInputStream(bis);
            zis.getNextEntry();
            InputStreamReader isr = new InputStreamReader(zis, "UTF-8");
            BufferedReader br = new BufferedReader(isr, 1024 * 1024 * 4);
            String line = null;
            String[] l_array = null;
            String last_fp = "";
            while ((line = br.readLine()) != null) {
                l_array = line.split("\t");
                if (l_array != null && l_array.length == 3) {
                    if (!last_fp.equals(l_array[0])) {
                        curr_last_modified_list.add(new FileLastModifiedTimeEntry(
                                l_array[0], Long.valueOf(l_array[1]), Long.valueOf(l_array[2]), false));
                        last_fp = l_array[0];
                    } else {
                        if (p_ntfy != null) p_ntfy.notifyToListener(false, new Object[]{last_fp});
                        list_was_corrupted = true;
                    }
                }
            }
            br.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list_was_corrupted;
    }

    static class FileLastModifiedTimeEntry implements Externalizable {
        private static final long serialVersionUID = 1L;
        private String file_path = "";
        private long local_last_modified_time = 0;
        private long remote_last_modified_time = 0;
        private boolean referenced = false;

        FileLastModifiedTimeEntry(String fp, long l_lm, long r_lm, boolean ref) {
            file_path = fp;
            local_last_modified_time = l_lm;
            remote_last_modified_time = r_lm;
            referenced = ref;
        }

        public boolean isReferenced() {
            return referenced;
        }

        public void setReferenced(boolean p) {
            referenced = p;
        }

        public String getFilePath() {
            return file_path;
        }

        public long getLocalFileLastModified() {
            return local_last_modified_time;
        }

        public long getRemoteFileLastModified() {
            return remote_last_modified_time;
        }

        public void setFilePath(String p) {
            file_path = p;
        }

        public void setLocalFileLastModified(long p) {
            local_last_modified_time = p;
        }

        public void setRemoteFileLastModified(long p) {
            remote_last_modified_time = p;
        }

        @Override
        public void readExternal(ObjectInput input) throws IOException,
                ClassNotFoundException {
            file_path = input.readUTF();
            local_last_modified_time = input.readLong();
            remote_last_modified_time = input.readLong();
        }

        @Override
        public void writeExternal(ObjectOutput output) throws IOException {
            output.writeUTF(file_path);
            output.writeLong(local_last_modified_time);
            output.writeLong(remote_last_modified_time);
        }
    }
}