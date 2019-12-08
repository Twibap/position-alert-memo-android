package org.iptime.twd.mymemoalamapplication.edit;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.android.gms.maps.model.LatLng;
import com.google.gson.Gson;
import com.google.maps.android.clustering.ClusterItem;

import java.util.Comparator;
import java.util.Date;

/**
 * Created by tky476 on 2017. 8. 30..
 */

public class Memo implements Parcelable, ClusterItem {
    static final String TAG = Memo.class.getSimpleName();
    public static final int COMPLETE = -1;

    public int mId;

    public String mTitle;
    public String mContent;
    public int    mCategory = 0;
    public String mPhoto;

    // Place : id, 위/경도, 이름, 주소
    public String mPlaceId;
    public LatLng mPlace;
//    public double mPlaceLat;
//    public double mPlaceLng;
    public String mPlaceName;
    public String mPlaceAddr;

    // Alert
    public int mAlertDistance;
    public int mGeofenceTransition;
//    public boolean mIsInbound;

    public final long mCreatedTime;
    public long mEditedTime;

    public Memo() {
        mId = this.hashCode();

        Date createdTime = new Date();
        mCreatedTime = createdTime.getTime();
    }

    protected Memo(Parcel in) {
        mId = in.readInt();
        mTitle = in.readString();
        mContent = in.readString();
        mCategory = in.readInt();
        mPhoto = in.readString();
        mPlaceId = in.readString();
        mPlace = in.readParcelable(LatLng.class.getClassLoader());
        mPlaceName = in.readString();
        mPlaceAddr = in.readString();
        mAlertDistance = in.readInt();
        mGeofenceTransition = in.readInt();
        mCreatedTime = in.readLong();
        mEditedTime = in.readLong();
    }

    public static final Creator<Memo> CREATOR = new Creator<Memo>() {
        @Override
        public Memo createFromParcel(Parcel in) {
            return new Memo(in);
        }

        @Override
        public Memo[] newArray(int size) {
            return new Memo[size];
        }
    };

    public void updateEditTime(Date editDateTime) {
        mEditedTime = editDateTime.getTime();
    }

    public String getStringId() {
        return String.valueOf(mId);
    }

    @Override
    public LatLng getPosition() {
        return mPlace;
    }

    @Override
    public String getTitle() {
        return mTitle;
    }

    @Override
    public String getSnippet() {
        if (mPlaceId != null)
            return mPlaceName;
        else
            return mPlaceAddr;
    }

    @Override
    public String toString() {

        Gson gson = new Gson();
        String result = gson.toJson(this);

        return result;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeInt(mId);
        parcel.writeString(mTitle);
        parcel.writeString(mContent);
        parcel.writeInt(mCategory);
        parcel.writeString(mPhoto);
        parcel.writeString(mPlaceId);
        parcel.writeParcelable(mPlace, i);
        parcel.writeString(mPlaceName);
        parcel.writeString(mPlaceAddr);
        parcel.writeInt(mAlertDistance);
        parcel.writeInt(mGeofenceTransition);
        parcel.writeLong(mCreatedTime);
        parcel.writeLong(mEditedTime);
    }

    // 정렬
    // 마지막 자리 숫자만 ^(XOR) 연산을 통해 ASC, DESC 를 바꿀 수 있다.
    public final static int REQUEST_SORT_BY_TIME_ASCENDING     = 0b0010;   // 2
    public final static int REQUEST_SORT_BY_TIME_DESCENDING    = 0b0011;   // 3
    public final static int REQUEST_SORT_BY_RANGE_ASCENDING    = 0b0100;   // 4
    public final static int REQUEST_SORT_BY_RANGE_DESCENDING   = 0b0101;   // 5
    public final static int REQUEST_SORT_BY_TITLE_ASCENDING    = 0b1000;   // 8
    public final static int REQUEST_SORT_BY_TITLE_DESCENDING   = 0b1001;   // 9

    public static class SortByTime implements Comparator<Memo> {

        @Override
        public int compare(Memo memo, Memo t1) {
            if (memo.mCreatedTime > t1.mCreatedTime)
                return 1;
            else if (memo.mCreatedTime < t1.mCreatedTime)
                return -1;
            else
                return 0;
        }

    }

    public static class SortByTitle implements Comparator<Memo>{

        @Override
        public int compare(Memo memo, Memo t1) {
            return memo.mTitle.compareTo(t1.mTitle);
        }

    }
}
