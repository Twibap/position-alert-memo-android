package org.iptime.twd.mymemoalamapplication.edit.folder;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;

import org.iptime.twd.mymemoalamapplication.R;
import org.iptime.twd.mymemoalamapplication.edit.Memo;

import java.util.ArrayList;
import java.util.Map;

/**
 * Created by tky476 on 2017. 9. 17..
 */

public class FolderAdapter
        extends RecyclerView.Adapter<FolderAdapter.ViewHolder>
        implements View.OnClickListener
{

    private final String TAG = this.getClass().getSimpleName();

    Context             mContext;
    RecyclerView        mView;
    ArrayList<Folder>   mData = new ArrayList<>();
    boolean             mIsAllCecked = false;

    SharedPreferences   mFolderStorage;

    Gson gson = new Gson();

    private OnFolderChangedListener mChangedListener;

    interface OnFolderChangedListener{
        void onFolderRenamed(Folder folder);
        void onFolderRemoved(Folder folder);
    }

    FolderAdapter(Context context, RecyclerView view){

        mContext = context;
        mView = view;

        mFolderStorage = mContext.getSharedPreferences("folder", Context.MODE_PRIVATE);

        loadFolder();
    }

    FolderAdapter(Context context, RecyclerView view, OnFolderChangedListener listener){

        mContext = context;
        mView = view;
        mChangedListener = listener;

        mFolderStorage = mContext.getSharedPreferences("folder", Context.MODE_PRIVATE);

    }

    // 메모리 재활용
    class ViewHolder extends RecyclerView.ViewHolder {

        TextView mTitle;
        CheckBox mIsChecked;

        ViewHolder(View itemView) {
            super(itemView);

            mTitle      = itemView.findViewById(R.id.card_folder_title);
            mIsChecked  = itemView.findViewById(R.id.card_folder_checkbox);

        }

    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        View view = LayoutInflater.from(mContext).inflate(R.layout.item_folder, parent, false);
        view.setOnClickListener(this);

        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {

        Folder currentFolder = mData.get(position);

        holder.mTitle.setText(currentFolder.mTitle);
        holder.mIsChecked.setChecked(currentFolder.isChecked);

    }

    @Override
    public int getItemCount() {
        return mData.size();
    }

    // 폴더 생성
    void addFolder(String folderName) {
        Folder folder = new Folder();
        folder.mTitle = folderName;

        mData.add(folder);

        notifyDataSetChanged();

        // 폴더 명 DB 저장
        String data = gson.toJson(folder);
        mFolderStorage.edit().putString(folder.getStrId(), data).apply();
    }

    @Override
    public void onClick(View view) {
        int index = mView.getChildLayoutPosition(view);
        mData.get(index).toggle();

        notifyDataSetChanged();
    }   // TODO 폴더 명 수정은 롱클릭으로 하자

    // 생성될 때 실행
    private void loadFolder(){
        Map<String, ?> folders = mFolderStorage.getAll();

        for (Map.Entry<String, ?> entry : folders.entrySet()){
            if (entry.getValue() instanceof String){
                mData.add(gson.fromJson((String) entry.getValue(), Folder.class));
            }
        }

        notifyDataSetChanged();
    }

    void checkAll(){
        if (!mIsAllCecked) {
            for (int i = 0; i < mData.size(); i++) {
                Folder currentFolder = mData.get(i);
                if (!currentFolder.isChecked) {
                    currentFolder.isChecked = true;

                }
                mIsAllCecked = true;
            }
        } else {
            for (int i = 0; i < mData.size(); i++) {
                mData.get(i).isChecked = false;
                mIsAllCecked = false;
            }
        }

        notifyDataSetChanged();
    }
    
    void deleteFolder(){
        boolean isAnyFolderDeleted = false;
        ArrayList<Integer> checkedIndex = new ArrayList<>();

        for (int i = 0; i < mData.size(); i++) {
            if (mData.get(i).isChecked) {
                // delete from db
                mFolderStorage.edit().remove(mData.get(i).getStrId()).apply();

                final Folder folder = mData.get(i);
                Runnable work = new Runnable() {
                    @Override
                    public void run() {
                        // 해당 분류의 메모 삭제
                        SharedPreferences sf = mContext.getSharedPreferences("memo", Context.MODE_PRIVATE);
                        for (Map.Entry<String, ?> entry : sf.getAll().entrySet()) {
                            Memo memo = gson.fromJson((String) entry.getValue(), Memo.class);

                            if (memo.mCategory == folder.getId()){
                                sf.edit().remove(entry.getKey()).apply();
                            }
                        }
                    }
                };

                Thread worker = new Thread(work);
                worker.start();

                isAnyFolderDeleted = true;
            }
        }


        if (!isAnyFolderDeleted){
            Toast.makeText(mContext, "선택된 폴더가 없습니다.", Toast.LENGTH_SHORT).show();
        } else {
            // reload
            mData.clear();
            loadFolder();
        }

        notifyDataSetChanged();
    }

}
