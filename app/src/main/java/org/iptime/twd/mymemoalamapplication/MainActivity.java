package org.iptime.twd.mymemoalamapplication;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.appcompat.widget.SwitchCompat;
import androidx.appcompat.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.google.gson.Gson;

import org.iptime.twd.mymemoalamapplication.edit.Memo;
import org.iptime.twd.mymemoalamapplication.edit.folder.EditFolderActivity;
import org.iptime.twd.mymemoalamapplication.edit.folder.Folder;
import org.iptime.twd.mymemoalamapplication.edit.memo.EditMemoActivity;
import org.iptime.twd.mymemoalamapplication.edit.memo.MemoAdapter;
import org.iptime.twd.mymemoalamapplication.geofence.GeofenceAlertService;
import org.iptime.twd.mymemoalamapplication.geofence.GeofenceManager;
import org.iptime.twd.mymemoalamapplication.map.MemoMapActivity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class MainActivity extends BaseActivity implements NavigationView.OnNavigationItemSelectedListener{

    final static String TAG = MainActivity.class.getSimpleName();

    public final static int MAKE_MEMO = 1001;
    public final static int EDIT_MEMO = 1002;

    private RecyclerView                mMemoList;
    private MemoAdapter mMemoListAdapter;
    private RecyclerView.LayoutManager  mMemoLayoutManager;

    SharedPreferences mStorage;
    SharedPreferences.Editor mStorageManager;

    GeofenceManager mManager;
//    NotificationActionButtonReceiver mReceiver;

    private NavigationView mNavigationView;
    private SwitchCompat mNavAlertSwitch;

    HashMap<Integer, Folder> mMenuItems = new HashMap<>();

    private ActionBar mActionBar;
    private String DEFALT_TITLE = "전체 보기";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 액션바
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        mActionBar = getSupportActionBar();
        mActionBar.setTitle(DEFALT_TITLE);

        // 메모 추가 버튼
        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // EditMemoActivity로 이동
                Intent intent = new Intent(MainActivity.this, EditMemoActivity.class);
                startActivityForResult(intent, MAKE_MEMO);
            }
        });

        // GeofenceManager : Alert을 울린다.
        mManager = new GeofenceManager(this);
        ((BaseApplication) getApplicationContext()).setGeofenceManager(mManager);

        // 네비게이션 바 생성
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        mNavigationView = findViewById(R.id.nav_view);
        mNavigationView.getMenu().findItem(R.id.nav_alert).setActionView(new SwitchCompat(this));    // Setting ActionView for menuItem
        mNavigationView.setNavigationItemSelectedListener(this);

        mNavAlertSwitch = (SwitchCompat) mNavigationView.getMenu().findItem(R.id.nav_alert).getActionView(); // Alert Switch
        mNavAlertSwitch.setClickable(false);

        // 목록 UI 생성
        mMemoList = findViewById(R.id.view_memo_list);
        mMemoListAdapter = new MemoAdapter(this, mMemoList);
        mMemoLayoutManager = new LinearLayoutManager(this);
        mMemoList.setAdapter(mMemoListAdapter);
        mMemoList.setLayoutManager(mMemoLayoutManager);

        // DB 관리자 생성
        mStorage = getSharedPreferences("memo", MODE_PRIVATE);
        mStorageManager = mStorage.edit();
        mStorageManager.apply();

    }

    @Override
    protected void onResume() {
        super.onResume();

        // 쌓이지 않게 비우고
        mMemoListAdapter.clear();
        mManager.clearGeofence();
        Intent intent = new Intent(this, GeofenceAlertService.class);
//        intent.putExtra("method", "clearAlert");
//        startService(intent);

        // UI에 출력할 데이터 불러오기
        mMemoListAdapter.loadMemo();

        // 알람 등록
        ArrayList<Memo> data = mMemoListAdapter.getData();
        Log.e(TAG, "MemoList Size is "+data.size());

        for (int i = 0; i < data.size(); i++){
            Memo memo = data.get(i);

            if (memo.mCategory != Memo.COMPLETE) mManager.addGeofence(memo);

//            intent.putExtra("method", "addAlert");
//            intent.putExtra("memo", memo);
//            startService(intent);
        }

        // 알람 시작 설정에 따른 세팅
        boolean isAlertOn = new GeofenceManager(this).getGeofencesAdded();
        if (isAlertOn){
            mNavAlertSwitch.setChecked(true);
            mManager.startGeofencesAlert();
//            intent.putExtra("method", "startAlert");
            startService(intent);
        } else {
            mNavAlertSwitch.setChecked(false);
            mManager.stopGeofencesAlert();
//            intent.putExtra("method", "stopAlert");
            stopService(intent);
        }

        // 폴더 선택 메뉴 설정
        Menu menu = mNavigationView.getMenu();
        Menu nav_folders = null;
        for (int i = 0; i < menu.size() ; i++){
            MenuItem item = menu.getItem(i);
            if (item.getItemId() == R.id.nav_folders)
                nav_folders = item.getSubMenu();
        }

        nav_folders.clear();

        nav_folders.add(Menu.NONE, R.id.nav_folder_all, Menu.NONE, "전체보기").setIcon(R.drawable.ic_nav_folder_filled);
        nav_folders.add(Menu.NONE, R.id.nav_folder_complete, Menu.NONE, "완료된 메모").setIcon(R.drawable.ic_nav_folder_filled);

        SharedPreferences sf = getSharedPreferences("folder", MODE_PRIVATE);
        Map<String, ?> folderList = sf.getAll();
        for (Map.Entry<String, ?> entry : folderList.entrySet()){
            Gson gson = new Gson();
            Folder folder = gson.fromJson((String) entry.getValue(), Folder.class);
            MenuItem item = nav_folders
                    .add(Menu.NONE, folder.getId(), Menu.NONE, folder.mTitle)
                    .setIcon(R.drawable.ic_nav_folder_filled);
            mMenuItems.put(item.getItemId(), folder);

            Log.e(TAG, "Folder - "+gson.toJson(folder));
        }

        nav_folders.add(Menu.NONE, R.id.nav_folder_add, Menu.NONE, "폴더 관리").setIcon(R.drawable.ic_nav_folder_add);
    }

    /**
     * 뒤로가기
     */
    @Override
    public void onBackPressed() {
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    /**
     * ActionBar 생성
     * @param menu
     * @return
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.

        getMenuInflater().inflate(R.menu.main, menu);

        return true;
    }

    /**
     * ActionBar 버튼 클릭 시 기능
     * @param item
     * @return
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        switch (id){
            case R.id.action_sort_by_time_asc:
                Toast.makeText(this, getString(R.string.sort_by_time_asc), Toast.LENGTH_SHORT).show();
                mMemoListAdapter.sortBy(Memo.REQUEST_SORT_BY_TIME_ASCENDING);
                return true;
            case R.id.action_sort_by_time_desc:
                Toast.makeText(this, getString(R.string.sort_by_time_desc), Toast.LENGTH_SHORT).show();
                mMemoListAdapter.sortBy(Memo.REQUEST_SORT_BY_TIME_DESCENDING);
                return true;
            case R.id.action_sort_by_range_asc:
                Toast.makeText(this, getString(R.string.sort_by_range_asc), Toast.LENGTH_SHORT).show();
                mMemoListAdapter.sortBy(Memo.REQUEST_SORT_BY_RANGE_ASCENDING);
                return true;
            case R.id.action_sort_by_range_desc:
                Toast.makeText(this, getString(R.string.sort_by_range_desc), Toast.LENGTH_SHORT).show();
                mMemoListAdapter.sortBy(Memo.REQUEST_SORT_BY_RANGE_DESCENDING);
                return true;
            case R.id.action_sort_by_title_asc:
                Toast.makeText(this, getString(R.string.sort_by_title_asc), Toast.LENGTH_SHORT).show();
                mMemoListAdapter.sortBy(Memo.REQUEST_SORT_BY_TITLE_ASCENDING);
                return true;
            case R.id.action_sort_by_title_desc:
                Toast.makeText(this, getString(R.string.sort_by_time_desc), Toast.LENGTH_SHORT).show();
                mMemoListAdapter.sortBy(Memo.REQUEST_SORT_BY_TITLE_DESCENDING);
                return true;
        }

        // default
        return super.onOptionsItemSelected(item);
    }

    /**
     * 네비게이션 메뉴 선택 시 기능
     * @param item
     * @return
     */
    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        switch (id){
            // show Map
            case R.id.nav_view_map:
                Intent intent = new Intent(this, MemoMapActivity.class);
//                intent.putExtra("memoData", mMemoListAdapter.getData());
                startActivity(intent);
                break;

            // Folder
            case R.id.nav_folder_all:
                mActionBar.setTitle(DEFALT_TITLE);
                mMemoListAdapter.selectBy(0);
                break;
            case R.id.nav_folder_complete:
                mActionBar.setTitle("완료된 메모");
                mMemoListAdapter.selectBy(Memo.COMPLETE);
                break;
            case R.id.nav_folder_add:
                Toast.makeText(this, "폴더 만들기", Toast.LENGTH_SHORT).show();
                makeFolder();
                break;

            // Setting
            case R.id.nav_alert:
                mNavAlertSwitch.toggle();
                if (mNavAlertSwitch.isChecked()){   // 알람 스위치 on
                    mManager.startGeofencesAlert();
                    Intent geoService = new Intent(this, GeofenceAlertService.class);
//                    geoService.putExtra("method", "startAlert");
                    startService(geoService);
                    item.setIcon(R.drawable.ic_nav_alert_switch_on);                    // 아이콘 수정
                } else {                            // 알람 스위치 off
                    mManager.stopGeofencesAlert();
                    Intent geoService = new Intent(this, GeofenceAlertService.class);
//                    geoService.putExtra("method", "stopAlert");
                    stopService(geoService);
                    item.setIcon(R.drawable.ic_nav_alert_switch_off);
                }
                return true;


            // 임의의 메모 대량 생산하는 버튼. 테스트용이다.
//            case R.id.nav_random:
//                final EditText editor = new EditText(this);
//                editor.setInputType(InputType.TYPE_CLASS_NUMBER);
//                new AlertDialog.Builder(this)
//                        .setTitle("메모 임의 생성기")
//                        .setMessage("갯수를 입력하세요.")
//                        .setView(editor)
//                        .setNegativeButton("취소", null)
//                        .setPositiveButton("확인", new DialogInterface.OnClickListener() {
//                            @Override
//                            public void onClick(DialogInterface dialogInterface, int i) {
//                                Runnable work = new Runnable() {
//                                    @Override
//                                    public void run() {
//                                        int count = Integer.parseInt(editor.getText().toString());
//                                        double minLat = 33.1;
//                                        double minLng = 126.126;
//                                        double maxLat = 33.6;
//                                        double maxLng = 127.0;
//
//                                        SharedPreferences sf = getSharedPreferences("folder", MODE_PRIVATE);
//                                        Gson gson = new Gson();
//                                        // 임의 폴더 생성
//                                        Folder folder = new Folder();
//                                        folder.mTitle = "임의 생성 폴더";
//                                        sf.edit().putString(folder.getStrId(), gson.toJson(folder)).apply();
//
//                                        sf = getSharedPreferences("memo", MODE_PRIVATE);
//                                        for (int j = 0; j < count; j++) {
//                                            // 임의 메모 생성
//                                            Memo memo = new Memo();
//                                            memo.mCategory = folder.getId();
//                                            memo.mTitle = "Random "+ j;
//                                            memo.mPlace = new LatLng((Math.random()*(maxLat-minLat+1))+minLat,
//                                                    (Math.random()*(maxLng-minLng+1))+minLng);
//                                            memo.mAlertDistance = 150;
//                                            memo.mGeofenceTransition = 3;
//
//                                            sf.edit().putString(memo.getStringId(), gson.toJson(memo)).apply();
//                                        }
//                                    }
//                                };
//
//                                Thread worker = new Thread(work);
//                                worker.start();
//                            }
//                        })
//                        .create()
//                        .show();
//                break;

            // 폴더 선택 시
            default:
                if (mMenuItems.containsKey(item.getItemId())) {
                    Folder folder = mMenuItems.get(item.getItemId());
                    mActionBar.setTitle(folder.mTitle);
                    mMemoListAdapter.selectBy(folder.getId());
                }
                break;
        }

        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    private void makeFolder(){
        Intent intent = new Intent(this, EditFolderActivity.class);
        startActivity(intent);
    }

    /**
     * 새 메모 및 수정사항 처리(List 반영 및 저장)
     * @param requestCode
     * @param resultCode
     * @param data
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.e(TAG, "onActivityResult : RequestCode = "+requestCode + ", ResultCode = "+ resultCode +", data="+ String.valueOf(data != null));
        if (resultCode == RESULT_OK) {
            switch (requestCode) {
                case MAKE_MEMO:
                    // TODO 리스트에 추가
                    Log.e(TAG, "onActivityResult : MakeMemo Success");
                    addMemo(data);
                    break;
                case EDIT_MEMO:
                    // TODO 기존에 있던 메모 찾아 업데이트
                    Log.e(TAG, "onActivityResult : EditMemo Success");
                    editMemo(data);
                    break;
            }
        }
    }

    /**
     * 작성된 메모 List 추가 및 저장
     * @param data
     */
    private void addMemo(Intent data){
        if (data == null)
            return;

        if (data.hasExtra("memo")){
            Memo memo = data.getParcelableExtra("memo");
            mMemoListAdapter.addMemo(memo);

            mManager.addGeofence(memo);
//            Intent geoService = new Intent(this, GeofenceAlertService.class);
//            geoService.putExtra("method", "addAlert");
//            geoService.putExtra("memo", memo);
//            startService(geoService);
        }

    }

    /**
     * 수정된 메모 List 반영 및 저장
     * @param data
     */
    private void editMemo(Intent data){
        if (data == null)
            return;

        if (data.hasExtra("memo")){
            Memo memo = data.getParcelableExtra("memo");
            mMemoListAdapter.setMemo(memo);

            mManager.addGeofence(memo);
//            Intent geoService = new Intent(this, GeofenceAlertService.class);
//            geoService.putExtra("method", "addAlert");
//            geoService.putExtra("memo", memo);
//            startService(geoService);
        }
    }

}
