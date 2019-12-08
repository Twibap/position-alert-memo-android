package org.iptime.twd.mymemoalamapplication.edit;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.google.gson.Gson;

import java.util.Date;
import java.util.Map;

/**
 * Created by tky476 on 2017. 9. 9..
 *
 * 메모를 저장하는 SharedPreferences 를 관리한다.
 */

public class MemoManager {
    private final String TAG = this.getClass().getSimpleName();
    private final String CATEGORY_LIST = TAG;

    private Context mContext;
    private Gson gson = new Gson();

    public MemoManager(Context context) {
        mContext = context;
    }

    /**
     * 분류 생성하기
     * 메모를 담고있는 SharedPreferences 목록을 저장한다.
     * @param categoryName
     * @return
     */
    public boolean createCategory(String categoryName){
        SharedPreferences sf = mContext.getSharedPreferences(CATEGORY_LIST, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sf.edit();

        editor.putLong(categoryName, new Date().getTime());

        return editor.commit();
    }

    /**
     * 분류 삭제하기
     * 분류에 해당하는 메모 모두 삭제된다.
     * @param categoryName
     * @return
     */
    public boolean deleteCategory(String categoryName){
        SharedPreferences sf = mContext.getSharedPreferences(CATEGORY_LIST, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sf.edit();

        editor.remove(categoryName);

        if (editor.commit()){
            // 해당 카테고리 비우기
            SharedPreferences category = mContext.getSharedPreferences(categoryName, Context.MODE_PRIVATE);
            if (category.edit().clear().commit()){
                Log.e(TAG, "Delete Category and Data is Success");
                return true;
            } else {
                // 카테고리 데이터 삭제 실패
                Log.e(TAG, "DeleteCategory data is fail");
                return false;
            }
        } else {
            // 카테고리 명단 삭제 실패
            Log.e(TAG, "DeleteCategory is fail");
            return false;
        }

    }

    /**
     * 메모 저장하기
     * @param memo
     * @return
     */
    public boolean writeMemo(Memo memo){
        SharedPreferences sf = mContext.getSharedPreferences(String.valueOf(memo.mCategory), Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sf.edit();

        editor.putString(memo.getStringId(), gson.toJson(memo));

        return editor.commit();
    }

    /**
     * 메모 삭제하기
     * @param memo
     * @return
     */
    public boolean deleteMemo(Memo memo){
        SharedPreferences sf = mContext.getSharedPreferences(String.valueOf(memo.mCategory), Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sf.edit();

        editor.remove(memo.getStringId());

        return editor.commit();
    }

    /**
     * 모든 분류에서 메모 찾기
     * @param id
     * @return
     */
    public Memo getMemo(String id){
        Memo result = null;

        SharedPreferences sf = mContext.getSharedPreferences(CATEGORY_LIST, Context.MODE_PRIVATE);
        Map<String, ?> categoryList = sf.getAll();

        for (Map.Entry<String, ?> entry : categoryList.entrySet()) {
            String category = entry.getKey();

            result = getMemo(category, id);
        }

        return result;
    }

    /**
     * 특정 분류에서 메모 찾기
     * @param category
     * @param id
     * @return
     */
    public Memo getMemo(String category, String id){
        Memo result = null;
        SharedPreferences sf = mContext.getSharedPreferences(category, Context.MODE_PRIVATE);

        if (sf.contains(id)) {
            String memoData = sf.getString(id, null);
            result = gson.fromJson(memoData, Memo.class);
        }

        return result;
    }
}
