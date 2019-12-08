package org.iptime.twd.mymemoalamapplication;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.Iterator;

/**
 * Created by Twibap(tky476@gmail.com) on 2017. 5. 10..
 *
 *
 */

public abstract class TWDhttpATask extends AsyncTask<JSONObject, Void, JSONObject> {


    private Context mContext;

    private String taskID;
    private String strUrl;
    private HttpURLConnection conn = null;
    private int    mDelay;

    /**
     *
     * @param mTaskID : 로그 출력 시 메시지를 구별하기 위한 변수
     * @param mStrUrl : 연결할 서버 주소
     */
    public TWDhttpATask(String mTaskID, String mStrUrl, int delay){

        taskID = mTaskID;   //로그 출력 시 어떤 Task의 메시지인지 확인하기 위한 ID
        strUrl = mStrUrl;  // 연결 할 서버 주소
        mDelay = delay;
    }

    public TWDhttpATask(Context context, String mTaskID, String mStrUrl, int delay){
        mContext = context;

        taskID = mTaskID;   //로그 출력 시 어떤 Task의 메시지인지 확인하기 위한 ID
        strUrl = "http://twd.iptime.org/"+mStrUrl;  // 연결 할 서버 주소
        mDelay = delay;
    }

//    public static int getmShowLoadingCounter() {
//        return mShowLoadingCounter;
//    }
//
//    public static void upShowLoadingCounter() {
//        TWDhttpATask.mShowLoadingCounter++;
//    }
//
//    public static void downShowLoadingCounter(){
//        TWDhttpATask.mShowLoadingCounter--;
//    }


    @Override
    protected void onPreExecute() {

        super.onPreExecute();
        try {
            URL url = new URL(strUrl);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");

            conn.setDoInput(true);
            conn.setDoOutput(true);

            conn.setConnectTimeout(10000);  // miliseconds
            conn.setReadTimeout(10000);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected JSONObject doInBackground(JSONObject... params) {

        Log.e(taskID, "task params check - " + Arrays.toString(params));

        try {

            Thread.sleep(mDelay);

            int responseCode = conn.getResponseCode();

            if (responseCode == HttpURLConnection.HTTP_OK){

                Log.e(taskID, "Connection Ok");
                BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuffer sb = new StringBuffer("");
                String line;

                while ((line = in.readLine()) != null){
                    sb.append(line);
                }

                in.close();
                String result = sb.toString();
                Log.e(taskID, "Return result - " + result);

                if (result != null) {
                    Log.e(taskID, "Result - " + result);

                    return new JSONObject(result);
                } else {
                    Log.e(taskID, "result is Null");
                }

            } else {
                String errMsg = conn.getResponseMessage();
                Log.e(taskID, "Connection Fail - "+responseCode +" - "+errMsg);

                BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuffer sb = new StringBuffer("");
                String line;

                while ((line = in.readLine()) != null){
                    sb.append(line);
                }

                in.close();
                String result = sb.toString();

                Log.e(this.taskID, result);

//                return result;

//                JSONObject resultJSON = new JSONObject(result);
//
//                return resultJSON;
            }

        } catch (Exception e) {
            e.printStackTrace();

//            return e.toString();
        }
        return null;
    }

    /**
     * TWDhttpATask 클래스를 상속 또는 객체생성 시 onPostExecute()를 재정의해서 사용한다.
     * 재정의 시 Json 객체로부터 데이터를 추출한다.
     * @param fromServerData : 서버로부터 받은 데이터
     */
    @Override
    protected abstract void onPostExecute(JSONObject fromServerData);

    private String getPostDataString(JSONObject params) throws Exception {
        Log.e(getmTaskID(), "getPostDataString Json param - " + params);

        StringBuilder result = new StringBuilder();
        boolean first = true;

        Iterator<String> itr = params.keys();

        while(itr.hasNext()){

            String key= itr.next();
            Object value = params.get(key);

            if (first)
                first = false;
            else
                result.append("&");

            result.append(URLEncoder.encode(key, "UTF-8"));
            result.append("=");
            result.append(URLEncoder.encode(value.toString(), "UTF-8"));

        }

        Log.e(getmTaskID(), "getPostDataString String result - " + result.toString());
        return result.toString();
    }

    public String getmTaskID() {
        return taskID;
    }
}
