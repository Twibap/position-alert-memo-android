package org.iptime.twd.mymemoalamapplication.edit.folder;

import android.content.DialogInterface;
import android.os.Bundle;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.appcompat.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;

import org.iptime.twd.mymemoalamapplication.R;
import org.iptime.twd.mymemoalamapplication.edit.EditActivity;

public class EditFolderActivity extends EditActivity{
    private final String TAG = this.getClass().getSimpleName();

    RecyclerView                mFolderList;
    FolderAdapter               mFolderAdapter;
    RecyclerView.LayoutManager  mLayoutManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_folder);

        // Build The ActionBar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);  // 뒤로가기 버튼 생성
        getSupportActionBar().setTitle("폴더 관리");

        // 메모 추가 버튼
        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                AlertDialog.Builder builder = new AlertDialog.Builder(EditFolderActivity.this);

                final EditText folderName = new EditText(EditFolderActivity.this);
                folderName.setHint("폴더 명");
                builder.setTitle("폴더 만들기")
                        .setView(folderName)
                        .setNegativeButton("취소", null)
                        .setPositiveButton("확인", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                mFolderAdapter.addFolder(folderName.getText().toString());
                            }
                        })
                        .create();

                builder.show();
            }
        });

        // 목록 UI 생성
        mFolderList = findViewById(R.id.view_folder_list);
        mFolderAdapter = new FolderAdapter(this, mFolderList);
        mLayoutManager = new LinearLayoutManager(this);
        mFolderList.setAdapter(mFolderAdapter);
        mFolderList.setLayoutManager(mLayoutManager);

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        Log.e(TAG, "onCreateOptionMenu");

        super.onCreateOptionsMenu(menu);

        getMenuInflater().inflate(R.menu.edit_folder, menu);

        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        Log.e(TAG, "onPrepareOptionMenu");
        super.onPrepareOptionsMenu(menu);

        return true;

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()){
            case android.R.id.home:
                onBackPressed();
                break;
            case R.id.edit_folder_check_all:
                mFolderAdapter.checkAll();
                break;
            case R.id.edit_folder_delete:
                mFolderAdapter.deleteFolder();
                break;
        }

        return super.onOptionsItemSelected(item);
    }
}