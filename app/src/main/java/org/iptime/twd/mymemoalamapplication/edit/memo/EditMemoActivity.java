package org.iptime.twd.mymemoalamapplication.edit.memo;

import android.app.NotificationManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.core.content.ContextCompat;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.cardview.widget.CardView;
import androidx.appcompat.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.places.GeoDataClient;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.PlaceDetectionClient;
import com.google.android.gms.location.places.Places;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.gson.Gson;

import org.iptime.twd.mymemoalamapplication.R;
import org.iptime.twd.mymemoalamapplication.TWDhttpATask;
import org.iptime.twd.mymemoalamapplication.edit.EditActivity;
import org.iptime.twd.mymemoalamapplication.edit.Memo;
import org.iptime.twd.mymemoalamapplication.edit.folder.Folder;
import org.iptime.twd.mymemoalamapplication.placepicker.PlacePickerFragment;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class EditMemoActivity extends EditActivity implements OnMapReadyCallback, PlacePickerFragment.OnPlaceSelectedListener, View.OnClickListener, SeekBar.OnSeekBarChangeListener {

    private final String TAG = this.getClass().getSimpleName();

    // UI
    EditText mEditTitle;
    EditText mEditContents;
    CardView mEditPlace;    // Button 으로 사용
    ToggleButton mEditDirectionIn;
    ToggleButton mEditDirectionOut;
    SeekBar  mEditDistance;
    TextView mTextRange;

    int mCircleFillColor = Color.argb(64, 0, 255, 0);
    int mCircleLineColor = Color.argb(192, 0, 255, 0);

    private static final int ALERT_RANGE_MIN = 50;
    private static final int ALERT_RANGE_MAX = 1000;

    // Memo Data
    private Memo mMemo;
    private Place mPlace;
    private int mGeofenceTransition = Geofence.GEOFENCE_TRANSITION_ENTER;
    private int mAlertRange = ALERT_RANGE_MIN;

    private GoogleMap mMap;
    private CameraPosition mCameraPosition;

    // The entry points to the Places API.
    private GeoDataClient mGeoDataClient;
    private PlaceDetectionClient mPlaceDetectionClient;

    // The entry point to the Fused Location Provider.
    private FusedLocationProviderClient mFusedLocationProviderClient;

    // A default location (Sydney, Australia) and default zoom to use when location permission is
    // not granted.
    private final LatLng mDefaultLocation = new LatLng(37.576006, 126.976914);   // 광화문
    private static final int DEFAULT_ZOOM = 15;
    private static final int PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 1;
    private boolean mLocationPermissionGranted;

    // The geographical location where the device is currently located. That is, the last-known
    // location retrieved by the Fused Location Provider.
    private Location mLastKnownLocation;
    private LatLng mLastCoordinate;

    // Keys for storing activity state.
    private static final String KEY_CAMERA_POSITION = "camera_position";
    private static final String KEY_LOCATION = "location";

    // Place UI
    private TextView mPlaceTitle;
    private TextView mPlaceDetailsText;
    private TextView mPlaceAttribution;

    private MarkerOptions mMarkerOption;
    private Marker mMarker;
    private Circle mCircle;

    // Place Picker
    final static String FRAGTAG = "PlacePickerFragment";

    // Folder
    private ArrayList<Folder> mFolderList = new ArrayList<>();

    private ActionBar mActionBar;
    private String mActivityTitle = "메모 작성";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_memo);

        // Build The ActionBar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);  // 뒤로가기 버튼 생성
        mActionBar = getSupportActionBar();
        mActionBar.setTitle(mActivityTitle);

        // Retrieve location and camera position from saved instance state.
        if (savedInstanceState != null) {
            mLastKnownLocation = savedInstanceState.getParcelable(KEY_LOCATION);
            mCameraPosition = savedInstanceState.getParcelable(KEY_CAMERA_POSITION);
        }

        // Construct a GeoDataClient.
        mGeoDataClient = Places.getGeoDataClient(this, null);

        // Construct a PlaceDetectionClient.
        mPlaceDetectionClient = Places.getPlaceDetectionClient(this, null);

        // Construct a FusedLocationProviderClient.
        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);

        // Build The Map
        MapFragment mapFragment = (MapFragment) getFragmentManager().findFragmentById(R.id.edit_memo_map);
        mapFragment.getMapAsync(this);

        // Place Picker
        FragmentManager fm = getSupportFragmentManager();
        PlacePickerFragment fragment = (PlacePickerFragment) fm.findFragmentByTag(FRAGTAG);

        if (fragment == null) {
            FragmentTransaction transaction = fm.beginTransaction();
            fragment = new PlacePickerFragment();
            transaction.add(fragment, FRAGTAG);
            transaction.commit();
        }

        // Build Ui
        mEditTitle = findViewById(R.id.edit_memo_title);
        mEditContents = findViewById(R.id.edit_memo_contents);

        mPlaceTitle = findViewById(R.id.place_title);
        mPlaceDetailsText = findViewById(R.id.place_details);

        mEditPlace = findViewById(R.id.edit_memo_place_bt);
        mEditPlace.setOnClickListener(fragment);

        mEditDirectionIn = findViewById(R.id.edit_memo_direction_inbound);
        mEditDirectionOut = findViewById(R.id.edit_memo_direction_outbound);
        mEditDirectionIn.setOnClickListener(this);
        mEditDirectionOut.setOnClickListener(this);

        mTextRange = findViewById(R.id.print_memo_range);
        mEditDistance = findViewById(R.id.edit_memo_range);
        mEditDistance.setOnSeekBarChangeListener(this);
        mEditDistance.setMax(ALERT_RANGE_MAX);  // max 1,000 m
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            mEditDistance.setMin(ALERT_RANGE_MIN);  // min 50 m
        }

    }

    @Override
    protected void onStart() {
        super.onStart();
        // 폴더 목록
        SharedPreferences sf = getSharedPreferences("folder", MODE_PRIVATE);
        Map<String, ?> folderList = sf.getAll();
        for (Map.Entry<String, ?> entry : folderList.entrySet()){
            Gson gson = new Gson();
            Folder folder = gson.fromJson((String) entry.getValue(), Folder.class);
            mFolderList.add(folder);
        }

    }

    @Override
    protected void onResume() {
        super.onResume();

        // GetData from Intent
        Intent intent = getIntent();
        if (intent.hasExtra("memo")) {
            mMemo = intent.getParcelableExtra("memo");
            // set Title
            mEditTitle.setText(mMemo.mTitle);

            // set Category
            if (mMemo.mCategory != 0){
                for (int i = 0 ; i < mFolderList.size() ; i++){
                    Folder folder = mFolderList.get(i);
                    if (folder.getId() == mMemo.mCategory)
                        mActionBar.setTitle(mActivityTitle+"("+folder.mTitle+")");
                }
            } else {
                mActionBar.setTitle(mActivityTitle);
            }

            // set Place info
            if (mMemo.mPlaceId != null) {
                mPlaceTitle.setText(mMemo.mPlaceName);
                mPlaceDetailsText.setText(mMemo.mPlaceAddr);
            } else {
                mPlaceTitle.setText("당시 위치");
//                mPlaceDetailsText.setText(mMemo.mPlaceLat + ", " + mMemo.mPlaceLng);
                mPlaceDetailsText.setText(mMemo.mPlaceAddr);
            }
            // set Direction Info
            mGeofenceTransition = mMemo.mGeofenceTransition;
            switch (mGeofenceTransition){
                case Geofence.GEOFENCE_TRANSITION_ENTER:
                    mEditDirectionIn.setChecked(true);
                    mEditDirectionOut.setChecked(false);
                    break;
                case Geofence.GEOFENCE_TRANSITION_EXIT:
                    mEditDirectionIn.setChecked(false);
                    mEditDirectionOut.setChecked(true);
                    break;
                case Geofence.GEOFENCE_TRANSITION_ENTER
                        | Geofence.GEOFENCE_TRANSITION_EXIT:
                    mEditDirectionIn.setChecked(true);
                    mEditDirectionOut.setChecked(true);
                    break;
                default:
                    mEditDirectionIn.setChecked(false);
                    mEditDirectionOut.setChecked(false);
                    break;
            }

            // set AlertDistance Info
            mTextRange.setText(mMemo.mAlertDistance + " m");
            mEditDistance.setProgress(mMemo.mAlertDistance);

            // set Contents
            if (mMemo.mContent != null) mEditContents.setText(mMemo.mContent);
        } else {
            mMemo = new Memo();
            Log.e(TAG, "New Memo Created");

            mEditDirectionIn.setChecked(true);
            mEditDirectionOut.setChecked(false);
            mEditDistance.setProgress(mAlertRange);
            mTextRange.setText(mAlertRange + " m");
        }

        // 화면 실행시 Notification Cancel 확인
        if (intent.hasExtra("cancel")){
            int cancel_id = intent.getIntExtra("cancel", 0);
            NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            manager.cancel(cancel_id);
        }


    }

    private void addMarker(double latitude, double longitude, String placeName, String placeAddress) {
        mMap.addMarker(new MarkerOptions().position(new LatLng(latitude, longitude)).title(placeName).snippet(placeAddress));
    }

    /**
     * Saves the state of the map when the activity is paused.
     */
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        if (mMap != null) {
            outState.putParcelable(KEY_CAMERA_POSITION, mMap.getCameraPosition());
            outState.putParcelable(KEY_LOCATION, mLastKnownLocation);
            super.onSaveInstanceState(outState);
        }

    }


    HashMap<Integer, Folder> mMenuItems = new HashMap<>();
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        Log.e(TAG, "onCreateOptionMenu");
        getMenuInflater().inflate(R.menu.edit_memo, menu);

        Menu nav_folders = null;
        for (int i = 0; i < menu.size() ; i++){
            MenuItem item = menu.getItem(i);
            if (item.getItemId() == R.id.action_set_folder) {
                nav_folders = item.getSubMenu();
            }
        }


        for(int i = 0 ; i < mFolderList.size(); i++){
            Folder folder = mFolderList.get(i);
            // MenuItem의 Order에 Folder Id 저장
            MenuItem item = nav_folders
                    .add(Menu.NONE, folder.getId(), Menu.NONE, folder.mTitle)
                    .setIcon(R.drawable.ic_nav_folder_filled);
            mMenuItems.put(item.getItemId(), folder);
        }

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        switch (id) {
            // 뒤로가기
            case android.R.id.home:
                onBackPressed();
                break;
            // 저장
            case R.id.action_done:
                onClickSaveBt();
                break;

            // 폴더 선택
            case R.id.action_set_folder_all:
                mActionBar.setTitle(mActivityTitle);
                mMemo.mCategory = 0;
                break;
            case R.id.action_set_folder:
                // Do nothing
                break;
            default:
                mActionBar.setTitle(mActivityTitle + "("+ item.getTitle() +")");
                mMemo.mCategory = mMenuItems.get(item.getItemId()).getId();
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * 저장버튼 클릭 시 유효성 검사 및 메모 생성
     */
    private void onClickSaveBt() {

        // 제목 입력 검사
        if (mEditTitle.length() == 0) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage("제목은 반드시 입력되어야합니다.").setCancelable(true).setPositiveButton("확인", null).create();
            builder.show();
            return;
        } else {
            mMemo.mTitle = mEditTitle.getText().toString();
        }

        // 내용 입력 있으면 메모에 저장
        if (mEditContents.length() != 0) mMemo.mContent = mEditContents.getText().toString();

        // 위치 저장
        if (mPlace != null) {
            mMemo.mPlaceId = mPlace.getId();
            mMemo.mPlace = mPlace.getLatLng();
            mMemo.mPlaceAddr = mPlace.getAddress().toString();
            mMemo.mPlaceName = mPlace.getName().toString();
        } else {
            if (mLastCoordinate != null) {
                mMemo.mPlace = mLastCoordinate;
                mMemo.mPlaceAddr = mPlaceDetailsText.getText().toString();
            }
        }

        if (mAlertRange == ALERT_RANGE_MIN){
            // 기본값 저장, 기존에 설정 된 값 있으면 유지
            if (mMemo.mAlertDistance == 0)
                mMemo.mAlertDistance = ALERT_RANGE_MIN;

        } else {
            mMemo.mAlertDistance = mAlertRange;

        }

        // 알람 방향 저장
        mMemo.mGeofenceTransition = mGeofenceTransition;  // 기본값 Geofence.Enter

        // 내용 확인
        Log.e(TAG, mMemo.toString());

        // SharedPreferences 저장
        SharedPreferences sf = getSharedPreferences("memo", MODE_PRIVATE);
        SharedPreferences.Editor editor = sf.edit();
        Gson gson = new Gson();
        editor.putString(mMemo.getStringId(), gson.toJson(mMemo));
        editor.apply();

        // MainActivity로 결과 전송
        Intent intent = new Intent();
        intent.putExtra("memo", mMemo);
        setResult(RESULT_OK, intent);


        // Activity 종료
        finish();
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        Log.e(TAG, "onMapReady");

        mMap = googleMap;

        // Prompt the user for permission.
        getLocationPermission();

        // Turn on the My Location layer and the related control on the map.
        updateLocationUI();

        // Get the current location of the device and set the position of the map.
        // 현재 위치를 받아오거나, mMemo 에 저장된 위치 정보를 표시한다.
        if (mMemo != null && mMemo.mPlace != null) {
            moveMapLocation(mMemo.mPlace.latitude, mMemo.mPlace.longitude);
            if (mMemo.mPlaceId != null) {
                addMarker(mMemo.mPlace.latitude, mMemo.mPlace.longitude, mMemo.mPlaceName, mMemo.mPlaceAddr);
                setCircle(mMemo.mPlace.latitude, mMemo.mPlace.longitude, mMemo.mAlertDistance, mMemo.mGeofenceTransition);
            }
            else {
                addMarker(mMemo.mPlace.latitude, mMemo.mPlace.longitude, "당시 위치", null);
                setCircle(mMemo.mPlace.latitude, mMemo.mPlace.longitude, mMemo.mAlertDistance, mMemo.mGeofenceTransition);
            }
        }
        else
            getDeviceLocation();

        Log.e(TAG, "onMapReady End");
    }

    private void setCircle(double latitude, double longitude, double radius, int geofenceTransition){
        if (mCircle != null){
            mCircle.setFillColor(Color.alpha(0));
            mCircle.setStrokeColor(Color.alpha(0));
            mCircle = null;
        }

        CircleOptions circleOption = new CircleOptions();
        // 위치, 색, 크기 설정
        circleOption.center(new LatLng(latitude, longitude))
                .fillColor(mCircleFillColor)
                .strokeColor(mCircleLineColor)
                .strokeWidth(32)
                .radius(radius);

        switch (geofenceTransition){
            case Geofence.GEOFENCE_TRANSITION_ENTER:
                // 원 내부 색만 보인다.
                circleOption
                        .strokeColor(Color.alpha(0));
                break;
            case Geofence.GEOFENCE_TRANSITION_EXIT:
                // 원 테두리 색만 보인다.
                circleOption
                        .fillColor(Color.alpha(0));
                break;
            case Geofence.GEOFENCE_TRANSITION_ENTER
                    | Geofence.GEOFENCE_TRANSITION_EXIT:
                // 원 내부, 테두리 모두 보이게 한다.
                circleOption
                        .fillColor(mCircleFillColor)
                        .strokeColor(mCircleLineColor);
                break;
        }

        mCircle = mMap.addCircle(circleOption);
    }

    private void setInvertCircle(int geofenceTransition){

        switch (geofenceTransition){
            case Geofence.GEOFENCE_TRANSITION_ENTER:
                // 원 내부 색만 보인다.
                mCircle.setFillColor(mCircleFillColor);
                mCircle.setStrokeColor(Color.alpha(0));
                break;
            case Geofence.GEOFENCE_TRANSITION_EXIT:
                // 원 테두리 색만 보인다.
                mCircle.setFillColor(Color.alpha(0));
                mCircle.setStrokeColor(mCircleLineColor);
                break;
            case Geofence.GEOFENCE_TRANSITION_ENTER
                    | Geofence.GEOFENCE_TRANSITION_EXIT:
                // 원 내부, 테두리 모두 보이게 한다.
                mCircle.setFillColor(mCircleFillColor);
                mCircle.setStrokeColor(mCircleLineColor);
                break;
            default:
                mCircle.setFillColor(Color.alpha(0));
                mCircle.setStrokeColor(Color.alpha(0));
                break;
        }

    }

    /**
     * Gets the current location of the device, and positions the map's camera.
     * 단말기 현재위치 가져오기 지도에 현재위치 표시하기
     */
    private void getDeviceLocation() {
        Log.e(TAG, "getDeviceLocation");
        /*
         * Get the best and most recent location of the device, which may be null in rare
         * cases when a location is not available.
         */
        try {
            if (mLocationPermissionGranted) {
                Task<Location> locationResult = mFusedLocationProviderClient.getLastLocation();
                locationResult.addOnCompleteListener(this, new OnCompleteListener<Location>() {
                    @Override
                    public void onComplete(@NonNull Task<Location> task) {
                        Log.e(TAG, "getDeviceLocation - OnComplete");
                        if (task.isSuccessful()) {
                            // Set the map's camera position to the current location of the device.
                            // 현재 위치로 맵 카메라 이동
                            mLastKnownLocation = task.getResult();

                            Toast.makeText(EditMemoActivity.this, mLastKnownLocation.getLatitude()+", "+mLastKnownLocation.getLongitude(), Toast.LENGTH_SHORT).show();

                            mLastCoordinate = new LatLng(mLastKnownLocation.getLatitude(), mLastKnownLocation.getLongitude());
                            mMemo.mPlace = mLastCoordinate;
                            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(mLastCoordinate, DEFAULT_ZOOM));

                            // TODO 네트워크 상태에 따른 대응 필요
                            getAddr();

                            setCircle(mLastCoordinate.latitude, mLastCoordinate.longitude, mAlertRange, mGeofenceTransition);
                        } else {
                            Log.d(TAG, "Current location is null. Using defaults.");
                            Log.e(TAG, "Exception: %s", task.getException());
                            mMap.moveCamera(CameraUpdateFactory
                                    .newLatLngZoom(mDefaultLocation, DEFAULT_ZOOM));
                            mMap.getUiSettings().setMyLocationButtonEnabled(false);
                        }
                    }
                });
            }
        } catch (SecurityException e)  {
            Log.e("Exception: %s", e.getMessage());
        }

        Log.e(TAG, "getDeviceLocation End");
    }

    private void getAddr(){
        // 좌표의 주소 표시
        double latitude;
        double longitude;
        if (mMemo != null) {
            latitude = mMemo.mPlace.latitude;
            longitude = mMemo.mPlace.longitude;
        } else {
            latitude = mLastCoordinate.latitude;
            longitude = mLastCoordinate.longitude;
        }
        final String geoCordUrl = "https://maps.googleapis.com/maps/api/geocode/json?" +
                "latlng="+latitude+","+longitude +
                "&key=AIzaSyBYLzCTnpzeNLYYoaFvdIiAe4mOq6j-KK8" +
                "&language=ko";
        TWDhttpATask getAddrFromLocation = new TWDhttpATask("InvertGeoCordTask", geoCordUrl, 0) {
            @Override
            protected void onPostExecute(JSONObject fromServerData) {
                try {
                    if( fromServerData != null
                            && !fromServerData.isNull("status")
                            && fromServerData.getString("status").equals("OK")){
                        JSONArray results = fromServerData.getJSONArray("results");

                        Log.e(TAG, geoCordUrl);
                        Log.e(TAG, results.toString());

                        String addr = results.getJSONObject(0).getString("formatted_address");

                        mPlaceDetailsText.setText(addr);
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        };

        getAddrFromLocation.execute();
    }

    private void moveMapLocation(double Latitude, double Longitude){
        if (mLocationPermissionGranted){
            LatLng targetLocation = new LatLng(Latitude, Longitude);
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(targetLocation, DEFAULT_ZOOM));
        }
    }

    /**
     * Prompts the user for permission to use the device location.
     * 권한 요청하기
     */
    private void getLocationPermission() {
        /*
         * Request location permission, so that we can get the location of the
         * device. The result of the permission request is handled by a callback,
         * onRequestPermissionsResult.
         */
        if (ContextCompat.checkSelfPermission(this.getApplicationContext(),
                android.Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            mLocationPermissionGranted = true;
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                    PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
        }
    }

    /**
     * Handles the result of the request for location permissions.
     * 권한 요청 결과 처리
     */
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String permissions[],
                                           @NonNull int[] grantResults) {
        mLocationPermissionGranted = false;
        switch (requestCode) {
            case PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    mLocationPermissionGranted = true;
                }
            }
        }
        updateLocationUI();
    }

    /**
     * Updates the map's UI settings based on whether the user has granted location permission.
     */
    private void updateLocationUI() {
        if (mMap == null) {
            return;
        }
        try {
            if (mLocationPermissionGranted) {
                mMap.setMyLocationEnabled(true);
                mMap.getUiSettings().setMyLocationButtonEnabled(true);
            } else {
                mMap.setMyLocationEnabled(false);
                mMap.getUiSettings().setMyLocationButtonEnabled(false);
                mLastKnownLocation = null;
                getLocationPermission();
            }
        } catch (SecurityException e)  {
            Log.e("Exception: %s", e.getMessage());
        }
    }

    /**
     * PlacePickerFragment.OnPlaceSelectedListener
     * @param place
     */
    @Override
    public void onPlaceSelected(Place place) {
        mPlace = place;
        mPlaceTitle.setText(place.getName());
        mPlaceDetailsText.setText(place.getAddress());
        mPlaceDetailsText.setVisibility(View.VISIBLE);

        // 마커 제거
        mMap.clear();

        // 지도에 표시
        if (mMarkerOption == null)
            mMarkerOption = new MarkerOptions();

        mMarkerOption.position(mPlace.getLatLng())
                .title(mPlace.getName().toString())
                .snippet(mPlace.getAddress().toString());

        if (mMemo != null)
            setCircle(mPlace.getLatLng().latitude, mPlace.getLatLng().longitude, mMemo.mAlertDistance, mGeofenceTransition);
        else
            setCircle(mPlace.getLatLng().latitude, mPlace.getLatLng().longitude, mAlertRange, mGeofenceTransition);

        mMarker = mMap.addMarker(mMarkerOption);
        moveMapLocation(mPlace.getLatLng().latitude, mPlace.getLatLng().longitude);
    }

    /**
     * ToggleButton 제어
     * @param view
     */
    @Override
    public void onClick(View view) {
        int buttonId = view.getId();
        switch (buttonId){
            case R.id.edit_memo_direction_inbound:
                if (mEditDirectionIn.isChecked())
                    mGeofenceTransition |= Geofence.GEOFENCE_TRANSITION_ENTER;  // 해당 비트 1 up
                else
                    mGeofenceTransition ^= Geofence.GEOFENCE_TRANSITION_ENTER;  // 해당 비트 반전
                setInvertCircle(mGeofenceTransition);
                break;
            case R.id.edit_memo_direction_outbound:
                if (mEditDirectionOut.isChecked())
                    mGeofenceTransition |= Geofence.GEOFENCE_TRANSITION_EXIT;
                else
                    mGeofenceTransition ^= Geofence.GEOFENCE_TRANSITION_EXIT;
                setInvertCircle(mGeofenceTransition);
                break;
        }

    }

    /**
     * 알람 범위 설정
     * @param seekBar
     * @param i The current progress level.
     *          This will be in the range min..max
     *          where min and max were set by setMin(int) and setMax(int), respectively.
     *          (The default values for min is 0 and max is 100.)
     * @param b True if the progress change was initiated by the user.
     */
    @Override
    public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
        if (b){
            mAlertRange = i;
            mTextRange.setText(mAlertRange+" m");
            if (mCircle != null){
                mCircle.setRadius(i);
            } else {
                setCircle(mLastCoordinate.latitude, mLastCoordinate.longitude, mAlertRange, mGeofenceTransition);
            }
        }
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
    }
}
