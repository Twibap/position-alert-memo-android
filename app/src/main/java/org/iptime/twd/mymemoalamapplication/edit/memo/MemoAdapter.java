package org.iptime.twd.mymemoalamapplication.edit.memo;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.google.gson.Gson;

import org.iptime.twd.mymemoalamapplication.BaseApplication;
import org.iptime.twd.mymemoalamapplication.MainActivity;
import org.iptime.twd.mymemoalamapplication.R;
import org.iptime.twd.mymemoalamapplication.edit.Memo;
import org.iptime.twd.mymemoalamapplication.geofence.GeofenceAlertService;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Map;

import static java.lang.String.valueOf;

/**
 * Created by tky476 on 2017. 8. 30..
 */

public class MemoAdapter extends RecyclerView.Adapter<MemoAdapter.ViewHolder> implements View.OnClickListener, View.OnLongClickListener{

    private static final String TAG = MemoAdapter.class.getSimpleName();
    static final int    MAX_EMS = 7;    // 제목 글자 보이기 제한

    private Context         mContext;
    private RecyclerView    mView;
    private ArrayList<Memo> mData;          // 보여 줄 데이터
    private ArrayList<Memo> mSelectedData;  // 선별 된 데이터(임시 보관)
    private ArrayList<Memo> mBackupData;    // 원본 데이터 백업

    private SharedPreferences mStorage;
    private SharedPreferences.Editor mStorageManager;

    private Gson gson = new Gson();

    public MemoAdapter(Context context, RecyclerView view) {

        mContext = context;
        mView    = view;
        mData    = new ArrayList<>();
        mSelectedData = new ArrayList<>();

        mStorage = mContext.getSharedPreferences("memo", Context.MODE_PRIVATE);
        mStorageManager = mStorage.edit();
        mStorageManager.apply(); // 무의미, mStorage.Edit()의 경고 지운다.

    }

    // 메모리 재활용
    class ViewHolder extends RecyclerView.ViewHolder {

        View background;

        TextView title;
        TextView location;
        TextView time;

        ViewHolder(View itemView) {
            super(itemView);

            background = itemView.findViewById(R.id.card_memo_background);

            title = itemView.findViewById(R.id.card_memo_title);
            location = itemView.findViewById(R.id.card_memo_location);
            time = itemView.findViewById(R.id.card_memo_time);

        }
    }

    /**
     * ViewHolder 를 새로 생성한다.
     * @param parent
     * @param viewType
     * @return
     */
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        View view = LayoutInflater.from(mContext).inflate(R.layout.item_memo, parent, false);
        view.setOnClickListener(this);
        view.setOnLongClickListener(this);

        return new ViewHolder(view);
    }

    /**
     * ViewHolder 에 Item 정보를 출력한다.
     * @param holder
     * @param position
     */
    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {

        Memo currentMemo = mData.get(position);

        // 분류에 따른 배경 설정
        if (currentMemo.mCategory == Memo.COMPLETE)
            holder.background.setBackgroundColor(Color.GRAY);
        else
            holder.background.setBackgroundColor(Color.WHITE);

        // 제목 출력
        holder.title.setText(currentMemo.mTitle);

        // 위치 출력
        if (currentMemo.mPlaceId == null)
            holder.location.setText(currentMemo.mPlaceAddr);
        else
            holder.location.setText(currentMemo.mPlaceName);

        // 날짜 설정
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
        String date = formatter.format(new Date(currentMemo.mCreatedTime));
        holder.time.setText(date);

    }

    /**
     * RecyclerView Adapter 기본 메소드
     * 아이템 총 갯수를 반환한다.
     * @return
     */
    @Override
    public int getItemCount() {
        return mData.size();
    }

    /**
     * 세부내용 확인 하거나, 수정할 때 ShortClick 한다.
     * @param view
     */
    @Override
    public void onClick(View view) {

        Memo currentMemo = getCurrentMemo(view);

        // EditMemoActivity 이동
        Intent intent = new Intent(mContext, EditMemoActivity.class);
        intent.putExtra("memo", currentMemo);
        ((Activity) mContext).startActivityForResult(intent, MainActivity.EDIT_MEMO);

    }

    /**
     * 삭제하기 위해서 항목을 LongClick 한다.
     * @param view
     * @return
     */
    @Override
    public boolean onLongClick(View view) {

        final Memo currentMemo = getCurrentMemo(view);

        // 확인 버튼 기능 정의
        DialogInterface.OnClickListener actionOkBt = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                MemoAdapter.this.delMemo(currentMemo);
            }
        };

        // 삭제 확인 검사를 위한 안내문
        AlertDialog.Builder delAlert = new AlertDialog.Builder(mContext);
        delAlert.setTitle("삭제")
                .setMessage("삭제되면 복구할 수 없습니다.\n정말 삭제하시겠습니까?")
                .setPositiveButton("확인", actionOkBt)
                .setNegativeButton("취소", null)
                .create();

        delAlert.show();
        return false;
    }

    // 메모 추가
    public void addMemo(Memo memo){
        Log.e(TAG, "new Memo added");
        // DB 추가
//        saveMemo(memo);

        // UI 추가
        mData.add(memo);
        notifyDataSetChanged();
    }

    // 메모 수정
    public void setMemo(Memo memo) {
        Log.e(TAG, "edited Memo Updated");
        // DB 수정
//        saveMemo(memo);

        // UI 수정
        for (int i = 0; i < mData.size(); i++) {
            Memo temp = mData.get(i);
            if (temp.mId == memo.mId){
                mData.set(i, memo);
                notifyDataSetChanged();
            }
        }
    }

    /**
     * DB에 데이터 저장
     * SharedPreferences 사용
     * @param memo
     */
    void saveMemo(Memo memo){
        String key = valueOf(memo.mId);
        String value = gson.toJson(memo);
        mStorageManager.putString(key, value);
        mStorageManager.apply();
    }

    /**
     * DB로부터 데이터 인출
     * MainActivity 의 Resume 단계에서 호출됨
     * Load 하기 전 반드시 Clear 필요
     */
    public void loadMemo() {
        final Map<String, ?> memos = mStorage.getAll();
        final Gson gson = new Gson();

        for (Map.Entry<String, ?> entry : memos.entrySet()) {
            if (entry.getValue() instanceof String) {
                mData.add(gson.fromJson((String) entry.getValue(), Memo.class));
            }
        }

        ((BaseApplication) mContext.getApplicationContext()).setMemoData(mData);

        notifyDataSetChanged();
    }

    public ArrayList<Memo> getData(){
        if (mBackupData != null)
            mBackupData = mData;

            return mData;
    }

    // Adapter 의 데이터를 비운다.
    public void clear(){
        mData.clear();
    }

    /**
     * Click 또는 LongClick 시 어떤 메모가 클릭되었는지 확인한다.
     * @param view
     * @return
     */
    private Memo getCurrentMemo(View view){
        int currentPosition = mView.getChildLayoutPosition(view);

        return mData.get(currentPosition);
    }

    /**
     * 해당 메모를 지운다.
     * DB, 메모리(UI), 등록된 알람을 해제한다.
     *
     * Item LongClick 시 호출된다.
     *
     * @param memo
     */
    private void delMemo(Memo memo){
        // DB에서 삭제
        String key = memo.getStringId();
        SharedPreferences sf = mContext.getSharedPreferences("memo", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sf.edit();

        editor.remove(key);
        editor.apply();

        // UI에서 삭제
        for (int i = 0; i < mData.size(); i++) {
            Memo temp = mData.get(i);
            if (temp.mId == memo.mId){
                mData.remove(i);
                notifyDataSetChanged();
            }
        }

        // 등록된 알람 삭제
        Intent rmAlert = new Intent(mContext, GeofenceAlertService.class);
        rmAlert.putExtra("method", "rmAlert");
        rmAlert.putExtra("memo", memo);
        mContext.startService(rmAlert);

    }

    // 정렬
    public void sortBy(int type){
        switch (type){
            // 시간 순 정렬
            case Memo.REQUEST_SORT_BY_TIME_ASCENDING:
                Collections.sort(mData, new Memo.SortByTime());
                notifyDataSetChanged();
                break;
            case Memo.REQUEST_SORT_BY_TIME_DESCENDING:
                Collections.sort(mData, new Memo.SortByTime());
                Collections.reverse(mData);
                notifyDataSetChanged();
                break;

            // 거리 순 정렬
            case Memo.REQUEST_SORT_BY_RANGE_ASCENDING:
                break;
            case Memo.REQUEST_SORT_BY_RANGE_DESCENDING:
                break;

            // 이름 순 정렬
            case Memo.REQUEST_SORT_BY_TITLE_ASCENDING:
                Collections.sort(mData, new Memo.SortByTitle());
                notifyDataSetChanged();
                break;
            case Memo.REQUEST_SORT_BY_TITLE_DESCENDING:
                Collections.sort(mData, new Memo.SortByTitle());
                Collections.reverse(mData);
                notifyDataSetChanged();
                break;
        }
    }

    // 분류
    public void selectBy(int categoryId){
        if (mBackupData == null) {
            mBackupData = mData;
        }
        mSelectedData.clear();

        if (categoryId == 0){
            // 전체 보기
            if(mBackupData != null)
                mData = mBackupData;
        } else {
            // 카테고리 분류
            for (int i = 0; i < mBackupData.size(); i++) {
                if (mBackupData.get(i).mCategory == categoryId) mSelectedData.add(mBackupData.get(i));
            }
            mData = mSelectedData;
        }

        notifyDataSetChanged();
    }

}
