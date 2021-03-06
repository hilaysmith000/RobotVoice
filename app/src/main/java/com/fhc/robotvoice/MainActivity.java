package com.fhc.robotvoice;


import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.Window;
import android.widget.Toast;

import com.fhc.alarmManage.Actions;
import com.fhc.alarmManage.SettingsActivity;
import com.fhc.utils.ServiceName;
import com.iflytek.cloud.ErrorCode;
import com.iflytek.cloud.InitListener;
import com.iflytek.cloud.RecognizerResult;
import com.iflytek.cloud.SpeechConstant;
import com.iflytek.cloud.SpeechError;
import com.iflytek.cloud.SpeechRecognizer;
import com.iflytek.cloud.SpeechSynthesizer;
import com.iflytek.cloud.SpeechUnderstander;
import com.iflytek.cloud.SpeechUnderstanderListener;
import com.iflytek.cloud.TextUnderstander;
import com.iflytek.cloud.UnderstanderResult;
import com.iflytek.cloud.ui.RecognizerDialog;
import com.iflytek.cloud.ui.RecognizerDialogListener;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Random;

import trikita.jedux.Action;

public class MainActivity extends Activity {


    private String preSetSentence[] = {"你可以说，提醒我10分钟后开会。","需要帮助，请说我需要帮助。","想要设置软件，就说打开设置。","想要取消提醒，就说取消提醒。"};

    private static final String TAG = "MainActivity";
//    private CircleButton cbStart;
//    private TextView tvTips;
//    private EditText etShow;

    //语音控件
    private SpeechUnderstander mSpeechUnderstander;  // 语义理解对象(语音到语义)
    private TextUnderstander mTextUnderstander;      // 语义理解对象(文本到语义)
    private SpeechRecognizer mIat;  				 // 语音听写对象
    private SpeechSynthesizer mTts;  				 // 语音合成对象
    private RecognizerDialog mIatDialog;            // 语音听写UI

    // 用HashMap存储听写结果
    private HashMap<String, String> mIatResults = new LinkedHashMap<String, String>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.main);

        initVoice();
        setRecognizerParam();
        setTtsParam();

        mIatDialog.setParameter("asr_sch", "1");
        mIatDialog.setParameter("nlp_version", "2.0");

        // 显示听写对话框
        mIatDialog.setListener(mRecognizerDialogListener);
//        mIatDialog.setCanceledOnTouchOutside(false); //设置背景色后就管用了

        mIatDialog.setOnDismissListener(dialogInterface -> finish());
        mIatDialog.show();



//        tvTips = (TextView) findViewById(R.id.tvTips);
//        etShow = (EditText) findViewById(R.id.etShow);
//
//        cbStart = (CircleButton) findViewById(R.id.cbStart);
//
//        cbStart.setOnClickListener(view -> {
//
//            mIatDialog.setParameter("asr_sch", "1");
//            mIatDialog.setParameter("nlp_version", "2.0");
//
//            // 显示听写对话框
//            mIatDialog.setListener(mRecognizerDialogListener);
//            mIatDialog.show();
//        });

    }

    private void initVoice() {

        // 初始化语义理解对象(语音到语义)
        mSpeechUnderstander = SpeechUnderstander.createUnderstander(MainActivity.this, mSpeechUdrInitListener);

        // 初始化语义理解对象(文本到语义)
        mTextUnderstander =  TextUnderstander.createTextUnderstander(MainActivity.this, textUnderstanderListener);

        // 初始化语音合成对象
        mTts = SpeechSynthesizer.createSynthesizer(MainActivity.this, mTtsInitListener);

        // 初始化语音识别对象
        mIat = SpeechRecognizer.createRecognizer(MainActivity.this, mInitListener);

        //
        mIatDialog = new RecognizerDialog(MainActivity.this, mInitListener);

    }


    /***start*************************************语音控件初始化回调**************************************************/

    /**
     * 初始化语义理解监听器(语音到语义)
     */
    private InitListener mSpeechUdrInitListener = new InitListener() {

        @Override
        public void onInit(int code) {
//            Log.d(TAG, "speechUnderstanderListener init() code = " + code);
            if (code != ErrorCode.SUCCESS) {
//                showTip("初始化失败,错误码："+code);
            }
        }
    };


    /**
     * 初始化语义理解监听器(文本到语义)
     */
    private InitListener textUnderstanderListener = new InitListener() {

        @Override
        public void onInit(int code) {
//            Log.d(TAG, "textUnderstanderListener init() code = " + code);
            if (code != ErrorCode.SUCCESS) {
//                showTip("初始化失败,错误码："+code);
            }
        }
    };

    /**
     * 初始化语音识别监听器
     */
    private InitListener mInitListener = new InitListener() {

        @Override
        public void onInit(int code) {
//            Log.d(TAG, "SpeechRecognizer init() code = " + code);
            if (code != ErrorCode.SUCCESS) {
//                showTip("初始化失败，错误码：" + code);
            }
        }
    };



    /**
     * 初始化语音合成监听器
     */
    private InitListener mTtsInitListener = new InitListener() {
        @Override
        public void onInit(int code) {
            Log.d(TAG, "InitListener init() code = " + code);
//            Toast.makeText(MainActivity.this,"InitListener init() code = " + code,Toast.LENGTH_SHORT).show();
            if (code != ErrorCode.SUCCESS) {
//                showTip("初始化失败,错误码："+code);
//                Log.d(TAG, "初始化失败,错误码："+code);
            } else {
                // 初始化成功，之后可以调用startSpeaking方法
                // 注：有的开发者在onCreate方法中创建完合成对象之后马上就调用startSpeaking进行合成，
                // 正确的做法是将onCreate中的startSpeaking调用移至这里
            }
        }
    };


    /***end*************************************语音控件初始化回调***************************************************/

    /***start***********************************语音参数设置********************************************************/
    /**
     * 语音识别参数设置
     *
     */
    public void setRecognizerParam() {
        // 清空参数
        mIat.setParameter(SpeechConstant.PARAMS, null);
        mSpeechUnderstander.setParameter(SpeechConstant.PARAMS, null);
        mTextUnderstander.setParameter(SpeechConstant.PARAMS, null);
        // 设置听写引擎
        mIat.setParameter(SpeechConstant.ENGINE_TYPE, SpeechConstant.TYPE_CLOUD);
        mSpeechUnderstander.setParameter(SpeechConstant.ENGINE_TYPE, SpeechConstant.TYPE_CLOUD);
        mTextUnderstander.setParameter(SpeechConstant.ENGINE_TYPE, SpeechConstant.TYPE_CLOUD);

        mTextUnderstander.setParameter("nlp_version", "2.0");

        // 设置返回结果格式
        mIat.setParameter(SpeechConstant.RESULT_TYPE, "json");
        mSpeechUnderstander.setParameter(SpeechConstant.RESULT_TYPE, "json");
        mTextUnderstander.setParameter(SpeechConstant.RESULT_TYPE, "json");


        mIat.setParameter(SpeechConstant.LANGUAGE, "zh_cn");
        mSpeechUnderstander.setParameter(SpeechConstant.LANGUAGE, "zh_cn");
        mTextUnderstander.setParameter(SpeechConstant.LANGUAGE, "zh_cn");
        // 设置语言区域
        mIat.setParameter(SpeechConstant.ACCENT, "mandarin");
        mSpeechUnderstander.setParameter(SpeechConstant.ACCENT, "mandarin");
        mTextUnderstander.setParameter(SpeechConstant.ACCENT, "mandarin");

        mIat.setParameter(SpeechConstant.VAD_ENABLE, "1");
        mSpeechUnderstander.setParameter(SpeechConstant.VAD_ENABLE, "1");
        mTextUnderstander.setParameter(SpeechConstant.VAD_ENABLE, "1");

        // 设置语音前端点:静音超时时间，即用户多长时间不说话则当做超时处理
        mIat.setParameter(SpeechConstant.VAD_BOS, "6000");
        mSpeechUnderstander.setParameter(SpeechConstant.VAD_BOS, "6000");
        mTextUnderstander.setParameter(SpeechConstant.VAD_BOS, "6000");

        // 设置语音后端点:后端点静音检测时间，即用户停止说话多长时间内即认为不再输入， 自动停止录音
        mIat.setParameter(SpeechConstant.VAD_EOS, "1000");
        mSpeechUnderstander.setParameter(SpeechConstant.VAD_EOS, "1000");
        mTextUnderstander.setParameter(SpeechConstant.VAD_EOS, "1000");

        // 设置标点符号,设置为"0"返回结果无标点,设置为"1"返回结果有标点
        mIat.setParameter(SpeechConstant.ASR_PTT, "0");
        mSpeechUnderstander.setParameter(SpeechConstant.ASR_PTT, "0");
        mTextUnderstander.setParameter(SpeechConstant.ASR_PTT, "0");

        // 设置音频保存路径，保存音频格式支持pcm、wav，设置路径为sd卡请注意WRITE_EXTERNAL_STORAGE权限
        // 注：AUDIO_FORMAT参数语记需要更新版本才能生效
        mIat.setParameter(SpeechConstant.AUDIO_FORMAT,"wav");
        mIat.setParameter(SpeechConstant.ASR_AUDIO_PATH, Environment.getExternalStorageDirectory()+"/msc/iat.wav");
        mSpeechUnderstander.setParameter(SpeechConstant.AUDIO_FORMAT,"wav");
        mSpeechUnderstander.setParameter(SpeechConstant.ASR_AUDIO_PATH, Environment.getExternalStorageDirectory()+"/msc/sud.wav");
        mTextUnderstander.setParameter(SpeechConstant.AUDIO_FORMAT,"wav");
        mTextUnderstander.setParameter(SpeechConstant.ASR_AUDIO_PATH, Environment.getExternalStorageDirectory()+"/msc/sud.wav");

        // 设置听写结果是否结果动态修正，为“1”则在听写过程中动态递增地返回结果，否则只在听写结束之后返回最终结果
        // 注：该参数暂时只对在线听写有效
        mIat.setParameter(SpeechConstant.ASR_DWA, "0");
        mSpeechUnderstander.setParameter(SpeechConstant.ASR_DWA, "0");
        mTextUnderstander.setParameter(SpeechConstant.ASR_DWA, "0");
    }


    /**
     * 语音合成参数设置
     *
     */
    private void setTtsParam(){
        // 清空参数
        mTts.setParameter(SpeechConstant.PARAMS, null);
        // 根据合成引擎设置相应参数

        mTts.setParameter(SpeechConstant.ENGINE_TYPE, SpeechConstant.TYPE_CLOUD);
        // 设置在线合成发音人
        mTts.setParameter(SpeechConstant.VOICE_NAME, "jiajia");

        //设置合成语速
        mTts.setParameter(SpeechConstant.SPEED, "50");
        //设置合成音调
        mTts.setParameter(SpeechConstant.PITCH, "50");
        //设置合成音量
        mTts.setParameter(SpeechConstant.VOLUME, "100");

        //设置播放器音频流类型
        mTts.setParameter(SpeechConstant.STREAM_TYPE, "3");
        // 设置播放合成音频打断音乐播放，默认为true
        mTts.setParameter(SpeechConstant.KEY_REQUEST_FOCUS, "true");

        // 设置音频保存路径，保存音频格式支持pcm、wav，设置路径为sd卡请注意WRITE_EXTERNAL_STORAGE权限
        // 注：AUDIO_FORMAT参数语记需要更新版本才能生效
        mTts.setParameter(SpeechConstant.AUDIO_FORMAT, "wav");
        mTts.setParameter(SpeechConstant.TTS_AUDIO_PATH, Environment.getExternalStorageDirectory()+"/msc/tts.wav");
    }

    /***end***********************************语音参数设置********************************************************/


    /***start**********************************语音控件回调*******************************************************/


    /**
     * 听写UI监听器
     */
    private RecognizerDialogListener mRecognizerDialogListener = new RecognizerDialogListener() {


        /*
         * 注意：语音识别的过程中会多次调用这个函数，所以请在判断识别完成后，再对识别结果进行操作
         *
         */
        public void onResult(RecognizerResult results, boolean isLast) {

            if (null != results) {

                final String jsonString = results.getResultString();

                String sn = null;
                // 读取json结果中的sn字段
                try {
                    JSONObject resultJson = new JSONObject(results.getResultString());
                    sn = resultJson.optString("sn");
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                mIatResults.put(sn, jsonString);


                if (isLast) {

                    StringBuffer resultBuffer = new StringBuffer();
                    for (String key : mIatResults.keySet()) {
                        resultBuffer.append(mIatResults.get(key));
                    }
                    parseResult(resultBuffer);
                }

            } else {
//                Log.d("MainActivity", "understander result:null");
//                etShow.append("null");
            }

        }

        /**
         * 识别回调错误.
         */
        public void onError(SpeechError error) {
//            showTip(error.getPlainDescription(true));
        }

    };

    public static int hour = 0;
    public static int minute = 0;
    /*
     * 根据语音识别的结果设置闹钟，或者应答
     */
    private void parseResult(StringBuffer results) {

        JSONObject root;    //获取Json整体对象
        String service = null;     //获取service字段
        JSONObject answer ;  //获取answer字段
        String answerText ;  //获取answer字段中的text字段
        String dateOrig = null;
        JSONObject weatherJsonObject = null;
        String textOrig ;
        String timeOrig ;
        String speakString = null;

        try {

            root = new JSONObject(results.toString());

            textOrig = root.getString("text");

//                    etShow.append(textOrig);
//                    showTip(textOrig);

            if (textOrig.contains("设置")){

                openSettings();
                mTts.startSpeaking("好的，以为您打开设置。",null);

                return;
            }else if (textOrig.contains("取消")){

                App.dispatch(new Action<>(Actions.Alarm.DISMISS)); //turn off alarm
                App.dispatch(new Action<>(Actions.Alarm.OFF));     //stop alarm
                mTts.startSpeaking("好的，以为您取消。",null);
                return;
            }else if (textOrig.contains("帮助")){

                Intent startIntent = new Intent(MainActivity.this, Help.class);
                startActivity(startIntent); // 启动服务

                mTts.startSpeaking("好的，以为您打开帮助。",null);

                return;
            }

            // respond code
            int rc = root.getInt("rc");

            switch (rc) {
                case 0:

                    service  = root.getString("service");

//                            showTip(service);

                    break;

                case 1:
                    showTip("无效请求");
                    break;

                case 2:
                    showTip("服务器内部错误");
                    break;

                case 3:
                    showTip("业务操作失败");
                    break;

                case 4:
                    showTip("文本没有匹配的服务场景");
                    break;
            }


            switch (ServiceName.getServiceName(service)) {

                case openQA:

                    answer = root.getJSONObject("answer");

                    speakString = answer.getString("text");

//                            showTip(speakString);
                    mTts.startSpeaking(speakString,null);

                    break;


                case schedule:

                    JSONObject schSemantic = root.getJSONObject("semantic");
                    JSONObject schSolt = schSemantic.getJSONObject("slots");

                    String name = schSolt.getString("name");
                    String content = schSolt.getString("content");

                    App.remindString = content;

                    JSONObject schDatatime = schSolt.getJSONObject("datetime");
//
//                            if (null == schDatatime){
//
//                                long time=System.currentTimeMillis();
//                                final Calendar mCalendar=Calendar.getInstance();
//                                mCalendar.setTimeInMillis(time);
//                                hour = mCalendar.get(Calendar.HOUR);
//                                etShow.append(hour+"");
//                                minute = mCalendar.get(Calendar.MINUTE) + 1;
//                                etShow.append(minute+"");

//                            }else{

                    timeOrig = schDatatime.getString("timeOrig");

                    String time = schDatatime.getString("time");

                    System.out.print("my alarm time is : " + time);
                    Toast.makeText(MainActivity.this,"my alarm time is : " + time,Toast.LENGTH_LONG).show();


                    String a[] = time.split(":");
                    hour = Integer.valueOf( a[0]);
                    minute =  Integer.valueOf( a[1]);
//                            }

                    if (textOrig.contains("闹钟")){

                        speakString = "好的，"+"已为您"+content;

//                                tvTips.setText("已为您"+content);

                    }else if ("reminder".equals(name)){

                        speakString = "好的，"+timeOrig+"提醒您"+content;

//                                tvTips.setText(timeOrig+"提醒您"+content);

                    }else if("clock".equals(name)){

                        speakString  = "好的，"+timeOrig+"叫您"+content;

//                                tvTips.setText(timeOrig+"叫您"+content);

                    }

                    mTts.startSpeaking(speakString,null);

                    App.dispatch(new Action<>(Actions.Alarm.ON));


                    break;

                case calc:// 计算

                    answer = root.getJSONObject("answer");
                    answerText = answer.getString("text");
                    mTts.startSpeaking(answerText, null);
                    break;

//                        case weather:// 天气
//
//                            JSONObject semantic = root.getJSONObject("semantic");  //semantic Json对象，语义理解后返回
//
//                            JSONObject slots = semantic.getJSONObject("slots");
//                            // 时间
//                            JSONObject datetime = slots.getJSONObject("datetime");  //datetime Json对象，语义理解后返回
//                            dateOrig = datetime.getString("dateOrig");
//
//                            // 地点
//                            JSONObject location = slots.getJSONObject("location");  //location Json对象，语义理解后返回
//                            String city = location.getString("city");               //city字段，包含地点信息
//
//                            int ret;
//                            if("CURRENT_CITY".equals(city)){
//
//                                if(mTextUnderstander.isUnderstanding()){
//                                    mTextUnderstander.cancel();
//                                    showTip("取消");
//                                }else {
//
//                                    ret = mTextUnderstander.understandText(baiduCity+dateOrig+"天气怎么样？", textListener);
//
//                                    if(ret != 0)
//                                    {
//                                        showTip("语义理解失败,错误码:"+ ret);
//                                    }
//                                }
//
//                            }else{
//
//                                // 获取天气信息
//                                JSONObject weatherData = root.getJSONObject("data");
//                                JSONArray weatherResult = weatherData.getJSONArray("result");
//
//                                if("昨天".equals(dateOrig)){
//                                    weatherJsonObject = weatherResult.getJSONObject(0);
//                                }else if("明天".equals(dateOrig)){
//                                    weatherJsonObject = weatherResult.getJSONObject(2);
//                                }else if("后天".equals(dateOrig)){
//                                    weatherJsonObject = weatherResult.getJSONObject(3);
//                                }else if("大后天".equals(dateOrig)){
//                                    weatherJsonObject = weatherResult.getJSONObject(4);
//                                }else if("大大后天".equals(dateOrig)){
//                                    weatherJsonObject = weatherResult.getJSONObject(5);
//                                }else{
//                                    weatherJsonObject = weatherResult.getJSONObject(1);
//                                }
//
//                                // 天气
//                                String weather = weatherJsonObject.getString("weather");
//                                // 风力
//                                String wind = weatherJsonObject.getString("wind");
//                                // 温度
//                                String tempRange = weatherJsonObject.getString("tempRange");
//
//                                // 播报天气
//                                mTts.startSpeaking(city+dateOrig+weather+","+"风力："+wind+","+"温度"+tempRange, null);
//                            }
//
//                            break;

                case baike:// 百科
                    answer = root.getJSONObject("answer");
                    answerText = answer.getString("text");
                    mTts.startSpeaking(answerText, null);

                    break;

                case datetime:// 日期
                    answer = root.getJSONObject("answer");
                    answerText = answer.getString("text");
                    mTts.startSpeaking(answerText, null);
                    break;

                case faq:// 社区问答
                    answer = root.getJSONObject("answer");
                    answerText = answer.getString("text");
                    mTts.startSpeaking(answerText, null);
                    break;

                case chat:// 聊天
                    answer = root.getJSONObject("answer");
                    answerText = answer.getString("text");
                    mTts.startSpeaking(answerText, null);
                    break;

                default:

                    mTts.startSpeaking("抱歉，我没有听清楚，请再说一遍好吗", null);

                    App.insignificanceCount++;

//                            showTip(App.insignificanceCount+"");

                    if (App.insignificanceCount%3 == 0){

                        Random random = new Random();
                        int i = random.nextInt(4);
                        mTts.startSpeaking(preSetSentence[i], null);

                    }

                    break;
            }

        } catch (JSONException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }


    }

    /**
     * 语义理解回调。
     */
    private SpeechUnderstanderListener mSpeechUnderstanderListener = new SpeechUnderstanderListener() {

        @Override
        public void onVolumeChanged(int i, byte[] bytes) {

        }

        @Override
        public void onBeginOfSpeech() {

        }

        @Override
        public void onEndOfSpeech() {

        }

        @Override
        public void onResult(UnderstanderResult understanderResult) {

        }

        @Override
        public void onError(SpeechError speechError) {

        }

        @Override
        public void onEvent(int i, int i1, int i2, Bundle bundle) {

        }
    };

    /***end**********************************语音控件回调*******************************************************/


    private void showTip(String str) {

        Toast.makeText(MainActivity.this,str,Toast.LENGTH_SHORT).show();

    }

    public void openSettings() {
        startActivity(new Intent(this, SettingsActivity.class));
    }


    @Override
    protected void onStop() {
        super.onStop();
    }
}
