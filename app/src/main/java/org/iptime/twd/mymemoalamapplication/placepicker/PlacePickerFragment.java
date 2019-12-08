/*
* Copyright 2015 The Android Open Source Project
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*     http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

package org.iptime.twd.mymemoalamapplication.placepicker;


import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import androidx.fragment.app.Fragment;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.ui.PlacePicker;

import org.iptime.twd.mymemoalamapplication.R;

/**
 * Sample demonstrating the use of {@link PlacePicker}.
 * This sample shows the construction of an {@link Intent} to open the PlacePicker from the
 * Google Places API for Android and select a {@link Place}.
 *
 * {@link PlacePicker} 사용 예제입니다.
 * Android 용 Google Place API 를 사용하여
 * Place Picker 를 열기위한 인텐트 생성하는 것과
 * 장소를 선택하는 것을 보여줍니다.
 *
 *
 * This sample uses the CardStream sample template to create the UI for this demo, which is not
 * required to use the PlacePicker API. (Please see the Readme-CardStream.txt file for details.)
 *
 * 이 예제는 CardStream template 예제를 사용합니다.
 * 이것은 PlacePicker API를 사용하는데 꼭 필요하지 않습니다.
 * (자세한 사항은 Readme-CardStream.txt 를 참조하십시오.)
 *
 *
 * @see com.google.android.gms.location.places.ui.PlacePicker.IntentBuilder
 * @see com.google.android.gms.location.places.ui.PlacePicker
 * @see com.google.android.gms.location.places.Place
 */
public class PlacePickerFragment extends Fragment implements View.OnClickListener{

    private static final String TAG = "PlacePickerSample";

    // Buffer used to display list of place types for a place
    private final StringBuffer mPlaceTypeDisplayBuffer = new StringBuffer();

    /**
     * Request code passed to the PlacePicker intent to identify its result when it returns.
     */
    private static final int REQUEST_PLACE_PICKER = 1;

    // 구현된 Listener를 가리킨다.
    private OnPlaceSelectedListener mListener;
    /**
     * 장소 선택 시 이벤트 콜백 메서드를 선언
     */
    public interface OnPlaceSelectedListener{
        void onPlaceSelected(Place place);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        try {
            // Activity로부터 구현된 OnPlaceSelectedListener 등록
            mListener = (OnPlaceSelectedListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString() + " must implement OnPlaceSelectedListener");
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    /**
     * Extracts data from PlacePicker result.
     * This method is called when an Intent has been started by calling
     * {@link #startActivityForResult(android.content.Intent, int)}. The Intent for the
     * {@link com.google.android.gms.location.places.ui.PlacePicker} is started with
     * {@link #REQUEST_PLACE_PICKER} request code. When a result with this request code is received
     * in this method, its data is extracted by converting the Intent data to a {@link Place}
     * through the
     * {@link com.google.android.gms.location.places.ui.PlacePicker#getPlace(android.content.Intent,
     * android.content.Context)} call.
     *
     * @param requestCode
     * @param resultCode
     * @param data
     */
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        // BEGIN_INCLUDE(activity_result)
        if (requestCode == REQUEST_PLACE_PICKER) {
            // This result is from the PlacePicker dialog.

            if (resultCode == Activity.RESULT_OK) {
                /* User has picked a place, extract data.
                   Data is extracted from the returned intent by retrieving a Place object from
                   the PlacePicker.
                 */
                Place place = PlacePicker.getPlace(getActivity(), data);
                mListener.onPlaceSelected(place); // Activity 로 place 전달

                /* A Place object contains details about that place, such as its name, address
                and phone number. Extract the name, address, phone number, place ID and place types.
                 */
                final CharSequence name = place.getName();
                final String placeId = place.getId();

                // Print data to debug log
                Log.d(TAG, "Place selected: " + placeId + " (" + name.toString() + ")");

            } else {
                Log.e(TAG, "Place is not selected");
            }

        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
        // END_INCLUDE(activity_result)
    }

    /**
     * 알람 장소 지정버튼 클릭 시 호출된다.
     * @param view
     */
    @Override
    public void onClick(View view) {
        int bt_id = view.getId();

        if (bt_id == R.id.edit_memo_place_bt) {
            // BEGIN_INCLUDE(intent)
            /* Use the PlacePicker Builder to construct an Intent.
            Note: This sample demonstrates a basic use case.
            The PlacePicker Builder supports additional properties such as search bounds.
             */
            try {
                PlacePicker.IntentBuilder intentBuilder = new PlacePicker.IntentBuilder();
                Intent intent = intentBuilder.build(getActivity());
                // Start the Intent by requesting a result, identified by a request code.
                startActivityForResult(intent, REQUEST_PLACE_PICKER);

            } catch (GooglePlayServicesRepairableException e) {
                GooglePlayServicesUtil
                        .getErrorDialog(e.getConnectionStatusCode(), getActivity(), 0);
            } catch (GooglePlayServicesNotAvailableException e) {
                Toast.makeText(getActivity(), "Google Play Services is not available.",
                        Toast.LENGTH_LONG)
                        .show();
            }

            // END_INCLUDE(intent)
        }
    }
}
