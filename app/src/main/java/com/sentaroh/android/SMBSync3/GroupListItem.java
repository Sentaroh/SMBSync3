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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

class GroupListItem implements Cloneable, Serializable {
    public String groupName="";
    public boolean enabled=true;
    public boolean isChecked=false;
    public int position=0;
    public boolean autoTaskOnly=false;
    public String taskList="";

    final static public int BUTTON_NOT_ASSIGNED=0;
    final static public int BUTTON_SHORTCUT1=1;
    final static public int BUTTON_SHORTCUT2=2;
    final static public int BUTTON_SHORTCUT3=3;
    final static public int BUTTON_SYNC_BUTTON =9;
    public int button =BUTTON_NOT_ASSIGNED;

    public GroupListItem() {
        //NOP
    }

    public boolean isSame(GroupListItem new_gi) {
        boolean result=false;
        if (this.groupName.equalsIgnoreCase(new_gi.groupName)
                && this.enabled==new_gi.enabled
                && this.autoTaskOnly==new_gi.autoTaskOnly
                && this.position==new_gi.position
                && this.button ==new_gi.button) {
            if (this.taskList.equalsIgnoreCase(new_gi.taskList)) result=true;
        }
        return result;
    }

    @Override
    public GroupListItem clone() {
        GroupListItem new_gi = null;
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(baos);
            oos.writeObject(this);

            oos.flush();
            oos.close();

            baos.flush();
            byte[] ba_buff = baos.toByteArray();
            baos.close();

            ByteArrayInputStream bais = new ByteArrayInputStream(ba_buff);
            ObjectInputStream ois = new ObjectInputStream(bais);

            new_gi = (GroupListItem) ois.readObject();
            ois.close();
            bais.close();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }

        return new_gi;
    }


}
