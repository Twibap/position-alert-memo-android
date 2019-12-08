package org.iptime.twd.mymemoalamapplication.edit.folder;

import java.util.Comparator;
import java.util.Date;

/**
 * Created by tky476 on 2017. 9. 17..
 */

public class Folder {

    private int id = hashCode();

    public String mTitle;
    boolean isChecked = false;
    long    mCreatedTime;

    public Folder(){
        mCreatedTime = new Date().getTime();
    }

    public int getId() {
        return id;
    }

    public String getStrId(){
        return String.valueOf(id);
    }

    void toggle(){
        isChecked = !isChecked;
    }

    static class SortByTime implements Comparator<Folder>{

        @Override
        public int compare(Folder folder, Folder t1) {
            if (folder.mCreatedTime > t1.mCreatedTime)
                return 1;
            else if (folder.mCreatedTime < t1.mCreatedTime)
                return -1;
            else
                return 0;
        }
    }
}
