package fish2;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.app.TimePickerDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.BitmapFactory;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.speech.RecognitionListener;
import android.speech.SpeechRecognizer;
import android.support.v7.app.AppCompatActivity;
import android.telephony.SmsManager;
import android.util.AndroidRuntimeException;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TimePicker;
import android.widget.Toast;

import com.baidu.speech.EventListener;
import com.baidu.speech.EventManager;
import com.baidu.speech.EventManagerFactory;
import com.baidu.speech.VoiceRecognitionService;
import com.google.gson.JsonIOException;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import com.iflytek.cloud.RecognizerListener;
import com.iflytek.cloud.RecognizerResult;
import com.iflytek.cloud.SpeechConstant;
import com.iflytek.cloud.SpeechError;
import com.iflytek.cloud.SpeechSynthesizer;
import com.iflytek.cloud.SpeechUtility;
import com.iflytek.cloud.ui.RecognizerDialog;
import com.iflytek.cloud.ui.RecognizerDialogListener;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Random;
import java.util.Set;
import java.util.UUID;


public class MainActivity extends AppCompatActivity implements RecognitionListener {//RecognitionListener语音接口
    private MediaPlayer mp,mp1;
    private SpeechRecognizer speechRecognizer;
    private EventManager wakeupManager;
    private Calendar mCalendar ;
    int abnormal=0;
    JsonObject json;
    private Handler handle;
    private static final String TAG = MainActivity.class.getSimpleName();
    BluetoothDevice mmDevice;//蓝牙设备
    BluetoothAdapter mmBluetoothAdapter;//蓝牙适配器
    BluetoothSocket mmBluetoothSocket;//客户端之类的
    OutputStream mmOutputStream;
    InputStream mmInputStream;
    ImageView tu;
    Calendar c=Calendar.getInstance();//使用日历类
    private HashMap<String, String> mIatResults = new LinkedHashMap<String, String>();

    private static final Random RANDOM = new Random();
    private static final int MAX_RANGE=12;//故事个数

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mCalendar = Calendar.getInstance();
        final Button timeBtn = (Button)findViewById(R.id.timeBtn);
        timeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                c.setTimeInMillis(System.currentTimeMillis());
                int mHour = c.get(Calendar.HOUR_OF_DAY);
                int mMinute = c.get(Calendar.MINUTE);
                new TimePickerDialog(MainActivity.this,
                        new TimePickerDialog.OnTimeSetListener() {
                            public void onTimeSet(TimePicker view, int hourOfDay,
                                                  int minute) {
                                c.setTimeInMillis(System.currentTimeMillis());
                                c.set(Calendar.HOUR_OF_DAY, hourOfDay);
                                c.set(Calendar.MINUTE, minute);
                                c.set(Calendar.SECOND, 0);
                                c.set(Calendar.MILLISECOND, 0);
                                Intent intent = new Intent(MainActivity.this, CallAlarm.class);
                                PendingIntent sender = PendingIntent.getBroadcast(
                                        MainActivity.this, 0, intent, 0);
                                AlarmManager am;
                                am = (AlarmManager) getSystemService(ALARM_SERVICE);
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                                    am.setExact(AlarmManager.RTC_WAKEUP, c.getTimeInMillis(), sender);
                                }
                                String tmpS = format(hourOfDay) + "：" + format(minute);
                                timeBtn.setText(tmpS);
                                Toast.makeText(MainActivity.this, "设置吃药时间为" + tmpS,
                                        Toast.LENGTH_SHORT)
                                        .show();
                                speekText("设置吃药时间为" + tmpS);
                            }
                        }, mHour, mMinute, true).show();
            }
        });
        initNotification();
        initSpeech();
        mp = MediaPlayer.create(MainActivity.this, R.raw.nvfuma);
        mp1=MediaPlayer.create(MainActivity.this,R.raw.tianxianpei);
        findBT();
        try {
            openBT();
        } catch (Exception e) {
            e.printStackTrace();
        }
        tu=(ImageView)findViewById(R.id.tu);
        tu.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startSpeechDialog();
            }
        });
        // 创建识别器
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this, new ComponentName(this, VoiceRecognitionService.class));
        // 注册监听器
        speechRecognizer.setRecognitionListener(this);
        // 语音唤醒
        wakeupManager = EventManagerFactory.create(MainActivity.this, "wp");
        wakeupManager.registerListener(new EventListener() {
            @Override
            public void onEvent(String name, String params, byte[] data, int offset, int length) {
                try {
                    //解析json文件
                    JSONObject json = new JSONObject(params);
                    if ("wp.data".equals(name)) { // 每次唤醒成功, 将会回调name=wp.data的时间, 被激活的唤醒词在params的word字段
                        String word = json.getString("word"); // 唤醒词
                        Log.e("自定义标签", "类名==MainActivity" + "方法名==onEvent=====:" + word);
                        if(word.equals("晓爱救命")){
                            ProgressDialog dialog = ProgressDialog.show(MainActivity.this, "提示", "发送消息成功", false, true);
                            dialog.show();
                            //传的数据
                            Intent intent1=getIntent();
                            String phone=intent1.getStringExtra("hy_phone");//电话
                            Intent intent = new Intent();
                            intent.setAction("android.intent.action.CALL");
                            intent.setData(Uri.parse("tel:"+phone));
                            startActivity(intent);
                        }
                        Toast.makeText(MainActivity.this, word, Toast.LENGTH_SHORT).show();
                        // 开始语音识别
                        startASR();
                        startSpeechDialog();//打开讯飞的接口
                    }
                } catch (JSONException e) {
                    throw new AndroidRuntimeException(e);
                }
            }
        });
        HashMap<String, String> params = new HashMap<>();
        params.put("kws-file", "assets:///WakeUp.bin"); // 设置唤醒资源, 唤醒资源请到 http://yuyin.baidu.com/wake#m4 来评估和导出  你好晓爱  晓爱救命 云杰
        wakeupManager.send("wp.start", new JSONObject(params).toString(), null, 0, 0);
        //wakeupManager.send("wp.stop", null, null, 0, 0);
        //如果没有注释上一行  手机6.0以上需要  如果注释上一行，使用开发板进行烧录程序
}
    private String format(int x)
    {
        String s=""+x;
        if(s.length()==1) s="0"+s;
        return s;
    }

    public void startASR() {
        Intent intent = new Intent();
        bindParams(intent);
        speechRecognizer.startListening(intent);
    }
    public void bindParams(Intent intent) {
        // 设置一些提示音有关的常量
        intent.putExtra(ConstantVoice.EXTRA_SOUND_START, R.raw.bdspeech_recognition_start); // 开始说话提示音
        intent.putExtra(ConstantVoice.EXTRA_SOUND_END, R.raw.bdspeech_speech_end);          // 解析语音完成提示音
        intent.putExtra(ConstantVoice.EXTRA_SOUND_SUCCESS, R.raw.bdspeech_recognition_success); // 解析成功的提示音
        intent.putExtra(ConstantVoice.EXTRA_SOUND_ERROR, R.raw.bdspeech_recognition_error);     // 解析错误的提示音
        intent.putExtra(ConstantVoice.EXTRA_SOUND_CANCEL, R.raw.bdspeech_recognition_cancel);   // 解析取消的提示音
        intent.putExtra(ConstantVoice.EXTRA_GRAMMAR, "assets:///baidu_speech_grammar.bsg");
        intent.putExtra(ConstantVoice.EXTRA_LANGUAGE, "cmn-Hans-CN");        // 普通话
        intent.putExtra(ConstantVoice.EXTRA_NLU, "enable"); // 开启语义解析
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main1, menu);
        return true;
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_open: {
                Toast.makeText(this, "你点击了连接蓝牙", Toast.LENGTH_SHORT).show();
                findBT();
                break;
            }
            case R.id.action_find: {
                Toast.makeText(this, "你点击了打开蓝牙", Toast.LENGTH_SHORT).show();
                // findBT();
                try {
                    openBT();
                    startSpeechDialog();//对话框
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;
            }
        }
        return true;
    }
    private void initSpeech() {
        SpeechUtility.createUtility(this, SpeechConstant.APPID + "=5a504a51");
    }
    private void initNotification(){
        Intent intent = new Intent(this, NotificationActivity.class);
        PendingIntent pi = PendingIntent.getActivity(this, 0, intent, 0);
        NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        Notification notification = new android.support.v4.app.NotificationCompat.Builder(this)
                .setContentTitle("语音小杰")
                .setContentText("你是最棒的")
                .setWhen(System.currentTimeMillis())
                .setSmallIcon(R.mipmap.ic_launcher)
                .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher))
                .setContentIntent(pi)
                .setDefaults(android.support.v4.app.NotificationCompat.DEFAULT_ALL)
                .setStyle(new android.support.v4.app.NotificationCompat.BigPictureStyle().bigPicture(BitmapFactory.decodeResource(getResources(), R.drawable.big_image)))
                .setPriority(android.support.v4.app.NotificationCompat.PRIORITY_MAX)
                .build();
        manager.notify(1, notification);
    }
    void findBT() {
        mmBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();//获得蓝牙适配器
        if (mmBluetoothAdapter == null) {
            Toast.makeText(MainActivity.this, "无法打开手机蓝牙，请确认手机是否有蓝牙功能！", Toast.LENGTH_SHORT).show();
            finish();
        }
        new Thread() {
            public void run() {
                if (mmBluetoothAdapter.isEnabled() == false) {
                    mmBluetoothAdapter.enable();
                } else {
                    mmBluetoothAdapter.enable();//修改
                }
            }
        }.start();
        Set<BluetoothDevice> paireDevices = mmBluetoothAdapter.getBondedDevices();
        ArrayList list = new ArrayList();
        if (paireDevices.size() > 0) {
            for (BluetoothDevice device : paireDevices) {
                //如何选取
                Toast.makeText(MainActivity.this, device.getName(), Toast.LENGTH_SHORT).show();
                mmDevice = device;
                break;
            }
        }
    }
    void openBT() throws IOException {
        UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
            if (mmDevice != null) {
                mmBluetoothSocket = mmDevice.createInsecureRfcommSocketToServiceRecord(uuid);
                mmBluetoothSocket.connect();
                mmOutputStream = mmBluetoothSocket.getOutputStream();
                mmInputStream = mmBluetoothSocket.getInputStream();
                speekText("我已经连上蓝牙了");
                Toast.makeText(MainActivity.this, "Bluetooth open:" + mmDevice.getName() + " " + mmDevice.getAddress(), Toast.LENGTH_SHORT).show();

            } else {
                Toast.makeText(getApplicationContext(), "没有链接到蓝牙", Toast.LENGTH_LONG).show();
            }
    }
    private void startSpeechDialog() {
        //1. 创建RecognizerDialog对象
        RecognizerDialog mDialog = new RecognizerDialog(this, new MyInitListener());
        //2. 设置accent、 language等参数
        mDialog.setParameter(SpeechConstant.LANGUAGE, "zh_cn");// 设置中文
        mDialog.setParameter(SpeechConstant.ACCENT, "mandarin");
        mDialog.setListener(new MyRecognizerDialogListener());
        //4. 显示dialog，接收语音输入
        mDialog.show();
    }
    private String speekText(String test) {
        SpeechSynthesizer mTts = SpeechSynthesizer.createSynthesizer(this, null);
        mTts.setParameter(SpeechConstant.VOICE_NAME, "xiaoqi");//设置发音人
        mTts.setParameter(SpeechConstant.SPEED, "50");// 设置语速
        mTts.setParameter(SpeechConstant.VOLUME, "80");// 设置音量，范围 0~100
        mTts.setParameter(SpeechConstant.ENGINE_TYPE, SpeechConstant.TYPE_CLOUD); //设置云端
        //3.开始合成
        mTts.startSpeaking(test.toString(), new MySynthesizerListener());
        return test.toString();
    }
    @Override
   public void onReadyForSpeech(Bundle params) {
        Log.e("自定义标签", "类名==MainActivity" + "方法名==onReadyForSpeech=====:" + "");
    }
    /**
     * 开始说话回调
     */
    @Override
    public void onBeginningOfSpeech() {
        Log.e("自定义标签", "类名==MainActivity" + "方法名==onBeginningOfSpeech=====:" + "");
    }
    @Override
    public void onRmsChanged(float rmsdB) {
        Log.e("自定义标签", "类名==MainActivity" + "方法名==onRmsChanged=====:" + rmsdB);
    }
    @Override
    public void onBufferReceived(byte[] buffer) {
        Log.e("自定义标签", "类名==MainActivity" + "方法名==onBufferReceived=====:" + "");
    }
    /**
     * 说话结束回调
     */
    @Override
    public void onEndOfSpeech() {
        Log.e("自定义标签", "类名==MainActivity" + "方法名==onEndOfSpeech=====:" + "");
    }
    @Override
    public void onError(int error) {
        StringBuilder sb = new StringBuilder();
        switch (error) {
            case SpeechRecognizer.ERROR_AUDIO:
                sb.append("音频问题");
                break;
            case SpeechRecognizer.ERROR_SPEECH_TIMEOUT:
                sb.append("没有语音输入");
                break;
            case SpeechRecognizer.ERROR_CLIENT:
                sb.append("其它客户端错误");
                break;
            case SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS:
                sb.append("权限不足");
                break;
            case SpeechRecognizer.ERROR_NETWORK:
                sb.append("网络问题");
                break;
            case SpeechRecognizer.ERROR_NO_MATCH:
                sb.append("没有匹配的识别结果");
                break;
            case SpeechRecognizer.ERROR_RECOGNIZER_BUSY:
                sb.append("引擎忙");
                break;
            case SpeechRecognizer.ERROR_SERVER:
                sb.append("服务端错误");
                break;
            case SpeechRecognizer.ERROR_NETWORK_TIMEOUT:
                sb.append("连接超时");
                break;
        }
    }
    @Override
    public void onResults(Bundle results) {
        // 获取截取到的词的集合
        ArrayList<String> nbest = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
        if (nbest != null) {
            Log.e("自定义标签", "类名==MainActivity" + "方法名==onResults=====nbest:" + nbest);
        }

        // 获取到不知道干嘛的东西,好像是认证的分数，但是一直为空
        float[] array = results.getFloatArray(SpeechRecognizer.CONFIDENCE_SCORES);
        if (array != null) {
            String str = "";
            for (float anArray : array) {
                str += anArray;
            }
            Log.e("自定义标签", "类名==MainActivity" + "方法名==onResults=====array:" + str);
        } else {
            Log.e("自定义标签", "类名==MainActivity" + "方法名==onResults=====array:" + "为空");
        }

        // 获取到Json数据
        String json = results.getString("origin_result");
        Log.e("自定义标签", "类名==MainActivity" + "方法名==onResults=====:" + json);

    }
    /**
     * 临时结果处理,这里可以截取到一些关键词
     *
     * @param results 这里保存着一些说话的关键词
     */
    @Override
    public void onPartialResults(Bundle results) {
        // 获取截取到的词的集合
        ArrayList<String> nbest = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
        if (nbest != null) {
            Log.e("自定义标签", "类名==MainActivity" + "方法名==onPartialResults=====nbest:" + nbest);
        }

        // 获取到不知道干嘛的东西，好像是认证的分数，但是一直为空
        float[] array = results.getFloatArray(SpeechRecognizer.CONFIDENCE_SCORES);
        if (array != null) {
            Log.e("自定义标签", "类名==MainActivity" + "方法名==onPartialResults=====array:" + array.toString());
        } else {
            Log.e("自定义标签", "类名==MainActivity" + "方法名==onPartialResults=====array:" + "为空");
        }

        // 获取到Json数据
        String json = results.getString("origin_result");
        Log.e("自定义标签", "类名==MainActivity" + "方法名==onPartialResults=====:" + json);
    }
    /**
     * 处理事件回调,为将来的一些事件保留的一些东西
     *
     * @param eventType 事件类型
     * @param params    这个可能和上面回调结果的一样，用同样的key去获取
     */
    @Override
    public void onEvent(int eventType, Bundle params) {
        Log.e("自定义标签", "类名==MainActivity" + "方法名==onEvent=====:" + eventType);
    }




    class MyRecognizerDialogListener implements RecognizerDialogListener {
        @Override
        public void onResult(RecognizerResult results, boolean isLast) {
            String result = results.getResultString(); //为解析的
            showTip(result);
            String text = JsonParser.parseIatResult(result);//解析过后的
            String sn = null;
            // 读取json结果中的 sn字段
            try {
                JSONObject resultJson = new JSONObject(results.getResultString());
                sn = resultJson.optString("sn");
            } catch (JSONException e) {
                e.printStackTrace();
            }
            mIatResults.put(sn, text);//没有得到一句，添加到
            StringBuffer resultBuffer = new StringBuffer();
            for (String key : mIatResults.keySet()) {
                resultBuffer.append(mIatResults.get(key));
            }
            final String[] shuju = new String[200];
            if (resultBuffer.toString().equals("打开女驸马。") || resultBuffer.toString().equals("点播女驸马。") || resultBuffer.toString().equals("女驸马。")) {
                final ProgressDialog pd = new ProgressDialog(MainActivity.this);
                pd.setTitle("请稍后：");
                pd.setMessage("正在查找女驸马歌曲...");
                speekText("正在查找女驸马歌曲...");
                pd.show();
                new Thread() {
                    @Override
                    public void run() {
                        try {
                            Thread.sleep(4000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
//                        pd.setMessage("查找女驸马成功...");
//                        speekText("查找女驸马成功...");
//                        try {
//                            Thread.sleep(4000);
//                        } catch (InterruptedException e) {
//                            e.printStackTrace();
//                        }
                        pd.dismiss();
                        mp.start();
                    }
                }.start();
            } else if (resultBuffer.toString().equals("打开天仙配。") || resultBuffer.toString().equals("点播天仙配。")) {
                final ProgressDialog pd = new ProgressDialog(MainActivity.this);
                pd.setTitle("请稍后：");
                pd.setMessage("正在查找天仙配歌曲...");
                speekText("正在查找天仙配歌曲...");
                pd.show();
                new Thread() {
                    @Override
                    public void run() {
                        try {
                            Thread.sleep(4000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
//                        pd.setMessage("查找天仙配成功...");
//                        speekText("查找天仙配成功...");
//                        try {
//                            Thread.sleep(4000);
//                        } catch (InterruptedException e) {
//                            e.printStackTrace();
//                        }
                        pd.dismiss();
                        mp1.start();
                    }
                }.start();
            } else if (resultBuffer.toString().equals("讲个故事。") || resultBuffer.toString().equals("说个故事呗。") || resultBuffer.toString().equals("讲故事。")) {
                switch (random()) {
                    case 0:
                        final ProgressDialog pd = new ProgressDialog(MainActivity.this);
                            pd.setTitle("请稍后：");
                            pd.setMessage("正在查找故事中...");
                            speekText("正在查找故事中...");
                            pd.show();
                            new Thread() {
                                @Override
                                public void run() {
                                    try {
                                        Thread.sleep(4000);
                                    } catch (InterruptedException e) {
                                        e.printStackTrace();
                                    }
//                                    pd.setMessage("找到故事...");
//                                    speekText("找到故事...");
//                                    try {
//                                        Thread.sleep(4000);
//                                    } catch (InterruptedException e) {
//                                        e.printStackTrace();
//                                    }
                                    pd.dismiss();
                                    speekText("刚才走在路上，接了一个陌生电话，一个女的，开口就说：“你好！恭喜你中了我们公司二等奖30万！”" +
                                            "我还没说话，她自己哈哈哈大笑又说：“不好意思，第一次骗人，没忍住……”然后，" +
                                            "她挂断了……只留下我站在风中凌乱-_-#如果你养一只鹦鹉，教他什么话？ 救我，我被变成鹦鹉了！");
                                }
                            }.start();
                        break;
                    case 1:
                        final ProgressDialog pd1 = new ProgressDialog(MainActivity.this);
                        pd1.setTitle("请稍后：");
                        pd1.setMessage("正在查找故事中...");
                        speekText("正在查找故事中...");
                        pd1.show();
                        new Thread() {
                            @Override
                            public void run() {
                                try {
                                    Thread.sleep(4000);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
//                                pd1.setMessage("找到故事...");
//                                speekText("找到故事...");
//                                try {
//                                    Thread.sleep(4000);
//                                } catch (InterruptedException e) {
//                                    e.printStackTrace();
//                                }
                                pd1.dismiss();
                                speekText("一女生因上课迟到被罚操场跑圈，不料天下起雨，女生只得淋雨跑步。" +
                                        "这是一个男生撑着伞到她身边一同跑步，并把伞移到女生头顶。" +
                                        "女生认出男生已经在一旁注视她好久，瞬间脸就红了，低声不好意思说：“对不起，我有男朋友了……”" +
                                        "男生低头沉思了一下，深情地对女生说：“要吗？这伞十块……..\n！");
                            }
                        }.start();
                        break;
                    case 2:
                        final ProgressDialog pd2 = new ProgressDialog(MainActivity.this);
                        pd2.setTitle("请稍后：");
                        pd2.setMessage("正在查找故事中...");
                        speekText("正在查找故事中...");
                        pd2.show();
                        new Thread() {
                            @Override
                            public void run() {
                                try {
                                    Thread.sleep(4000);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
//                                pd2.setMessage("找到故事...");
//                                speekText("找到故事...");
//                                try {
//                                    Thread.sleep(4000);
//                                } catch (InterruptedException e) {
//                                    e.printStackTrace();
//                                }
                                pd2.dismiss();
                                speekText( "有个老人在河边钓鱼，一个小孩走过去看他钓鱼。老人技巧纯熟，所以没多久就钓上了满篓的鱼，" +
                                        "老人见小孩很可爱，要把整篓的鱼送给他，小孩摇摇头，老人惊异地问道：你为何不要？小孩回答：“我想要你" +
                                        "手中的钓竿。”老人问：“你要钓竿做什么？”小孩说：“这篓鱼没多久就吃完了，要是我有钓竿，我就可以自己" +
                                        "钓，一辈子也吃不完。"
                                );
                            }
                        }.start();
                        break;
                    case 3:
                        final ProgressDialog pd3 = new ProgressDialog(MainActivity.this);
                        pd3.setTitle("请稍后：");
                        pd3.setMessage("正在查找故事中...");
                        speekText("正在查找故事中...");
                        pd3.show();
                        new Thread() {
                            @Override
                            public void run() {
                                try {
                                    Thread.sleep(4000);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
//                                pd3.setMessage("找到故事...");
//                                speekText("找到故事...");
//                                try {
//                                    Thread.sleep(4000);
//                                } catch (InterruptedException e) {
//                                    e.printStackTrace();
//                                }
                                pd3.dismiss();
                                speekText( "男人买了一条鱼回家让老婆煮，然后自己跑去看电影，老婆也想一起去。男人说：“两个人看浪费钱，" +
                                        "你把鱼煮好，等我看完回来，边吃边和你分享故事情节。”\n" +
                                        "待男人看完回来时，没见到鱼，就问老婆：“鱼呢？”老婆淡定地找了把椅子坐了下来说：" +
                                        "“鱼我全吃了，来，坐下来我给你讲讲鱼的味道。”\n");
                            }
                        }.start();
                        break;
                    case 4:
                        final ProgressDialog pd4 = new ProgressDialog(MainActivity.this);
                        pd4.setTitle("请稍后：");
                        pd4.setMessage("正在查找故事中...");
                        speekText("正在查找故事中...");
                        pd4.show();
                        new Thread() {
                            @Override
                            public void run() {
                                try {
                                    Thread.sleep(4000);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
//                                pd4.setMessage("找到故事...");
//                                speekText("找到故事...");
//                                try {
//                                    Thread.sleep(4000);
//                                } catch (InterruptedException e) {
//                                    e.printStackTrace();
//                                }
                                pd4.dismiss();
                                speekText( "一个姑娘上了高铁，见自己的座位上坐着一男士。\n" +
                                        "她核对自己的票，客气地说：“先生，您坐错位置了吧？”\n" +
                                        "男士拿出票，大声嚷嚷：“看清楚点，这是我的座位，你瞎了眼吗？！”\n" +
                                        "女孩仔细看了他的票，不再做声，默默地站在他的身旁。\n" +
                                        "一会儿火车开动了，女孩低头轻轻地对男士说：“先生，您没坐错位，但您坐错了车！这是开往上海的，" +
                                        "你的车票是去哈尔滨的。”\n");
                            }
                        }.start();
                        break;
                    case 5:
                        final ProgressDialog pd5 = new ProgressDialog(MainActivity.this);
                        pd5.setTitle("请稍后：");
                        pd5.setMessage("正在查找故事中...");
                        speekText("正在查找故事中...");
                        pd5.show();
                        new Thread() {
                            @Override
                            public void run() {
                                try {
                                    Thread.sleep(4000);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
//                                pd5.setMessage("找到故事...");
//                                speekText("找到故事...");
//                                try {
//                                    Thread.sleep(4000);
//                                } catch (InterruptedException e) {
//                                    e.printStackTrace();
//                                }
                                pd5.dismiss();
                                speekText( "父子两住山上，每天都要赶牛车下山卖柴。老父较有经验，坐镇驾车，山路崎岖，弯道特多，儿子眼神较好，总是在要转弯时提醒道：“爹，转弯啦!”\n" +
                                        "　　有一次父亲因病没有下山，儿子一人驾车。到了弯道，牛怎么也不肯转弯，儿子用尽各种方法，下车又推又拉，用青草诱之，牛一动不动。\n" +
                                        "　　到底是怎么回事?儿子百思不得其解。最后只有一个办法了，他左右看看无人，贴近牛的耳朵大声叫道：“爹，转弯啦!”\n" +
                                        "　　牛应声而动。\n" +
                                        "　　牛用条件反射的方式活着，而人则以习惯生活。一个成功的人晓得如何培养好的习惯来代替坏的习惯，当好的习惯积累多了，自然会有一个好的人生。\n");
                            }
                        }.start();
                        break;
                    case 6:
                        final ProgressDialog pd6 = new ProgressDialog(MainActivity.this);
                        pd6.setTitle("请稍后：");
                        pd6.setMessage("正在查找故事中...");
                        speekText("正在查找故事中...");
                        pd6.show();
                        new Thread() {
                            @Override
                            public void run() {
                                try {
                                    Thread.sleep(4000);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
//                                pd6.setMessage("找到故事...");
//                                speekText("找到故事...");
//                                try {
//                                    Thread.sleep(4000);
//                                } catch (InterruptedException e) {
//                                    e.printStackTrace();
//                                }
                                pd6.dismiss();
                                speekText( "五岁的汉克和爸爸妈妈哥哥一起到森林干活，突然间下起雨来，可是他们只带了一块雨披。\n" +
                                        "　　爸爸将雨披给了妈妈，妈妈给了哥哥，哥哥又给了汉克。\n" +
                                        "　　汉克问道：“为什么爸爸给了妈妈，妈妈给了哥哥，哥哥又给了我呢?”\n" +
                                        "　　爸爸回答道：“因为爸爸比妈妈强大，妈妈比哥哥强大，哥哥又比你强大呀。我们都会保护比较弱小的人。”\n" +
                                        "　　汉克左右看了看，跑过去将雨披撑开来挡在了一朵风雨中飘摇的娇弱小花上面。\n" +
                                        "　　这个告诉我们，真正的强者不一定是多有力，或者多有钱，而是他对别人多有帮助。\n" +
                                        "　　感悟：责任可以让我们将事做完整，爱可以让我们将事情做好。\n");
                            }
                        }.start();
                        break;
                    case 7:
                        final ProgressDialog pd7 = new ProgressDialog(MainActivity.this);
                        pd7.setTitle("请稍后：");
                        pd7.setMessage("正在查找故事中...");
                        speekText("正在查找故事中...");
                        pd7.show();
                        new Thread() {
                            @Override
                            public void run() {
                                try {
                                    Thread.sleep(4000);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
//                                pd7.setMessage("找到故事...");
//                                speekText("找到故事...");
//                                try {
//                                    Thread.sleep(4000);
//                                } catch (InterruptedException e) {
//                                    e.printStackTrace();
//                                }
                                pd7.dismiss();
                                speekText( "在动物园里的小骆驼问妈妈：“妈妈，妈妈，为什么我们的睫毛那么地长?”\n" +
                                        "　　骆驼妈妈说：“当风沙来的时候，长长的睫毛可以让我们在风暴中都能看得到方向。”\n" +
                                        "　　小骆驼又问：“妈妈妈妈，为什么我们的背那么驼，丑死了!”\n" +
                                        "　　骆驼妈妈说：“这个叫驼峰，可以帮我们储存大量的水和养分，让我们能在沙漠里耐受十几天的无水无食条件。”\n" +
                                        "　　小骆驼又问：“妈妈妈妈，为什么我们的脚掌那么厚?”\n" +
                                        "　　骆驼妈妈说：“那可以让我们重重的身子不至于陷在软软的沙子里，便于长途跋涉啊。”\n" +
                                        "　　小骆驼高兴坏了：“哗，原来我们这么有用啊!!可是妈妈，为什么我们还在动物园里，不去沙漠远足呢?”\n" +
                                        "　　天生我才必有用，可惜现在没人用。一个好的心态+一本成功的教材+一个无限的舞台=成功。每人的潜能是无限的，" +
                                        "关键是要找到一个能充分发挥潜能的舞台。\n");
                            }
                        }.start();
                        break;
                    case 8:
                        final ProgressDialog pd8 = new ProgressDialog(MainActivity.this);
                        pd8.setTitle("请稍后：");
                        pd8.setMessage("正在查找故事中...");
                        speekText("正在查找故事中...");
                        pd8.show();
                        new Thread() {
                            @Override
                            public void run() {
                                try {
                                    Thread.sleep(4000);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
//                                pd8.setMessage("找到故事...");
//                                speekText("找到故事...");
//                                try {
//                                    Thread.sleep(4000);
//                                } catch (InterruptedException e) {
//                                    e.printStackTrace();
//                                }
                                pd8.dismiss();
                                speekText( "在台湾旅游时，听过这么一段感人的：台南有位着名作家，童年时家境贫寒，父母以卖豆腐维持生计。每天早上天尚" +
                                        "未破晓时，他便与弟弟起身丁作，两人治街叫卖。他告诉弟弟说：”我们把卖豆腐所赚的钱，拿回家给母亲，帮助家人过活。我" +
                                        "们给自己的奖励品是你我共享一块豆腐，你一半，我一半。");
                            }
                        }.start();
                        break;
                    case 9:
                        final ProgressDialog pd9 = new ProgressDialog(MainActivity.this);
                        pd9.setTitle("请稍后：");
                        pd9.setMessage("正在查找故事中...");
                        speekText("正在查找故事中...");
                        pd9.show();
                        new Thread() {
                            @Override
                            public void run() {
                                try {
                                    Thread.sleep(4000);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
//                                pd9.setMessage("找到故事...");
//                                speekText("找到故事...");
//                                try {
//                                    Thread.sleep(4000);
//                                } catch (InterruptedException e) {
//                                    e.printStackTrace();
//                                }
                                pd9.dismiss();
                                speekText( "　对于一只盲目航行的船而言，所有方向的风都是逆风。\n" +
                                        "　　有个人一心一意想升官发财，可是从年轻熬到白发，却还只是个小公务员。" +
                                        "这个人为此极不快乐，每次想起来就掉泪，有一天竟然号陶大哭了。办公室有个新来的年轻人觉得很奇怪，" +
                                        "便问他到底因为什么难过。他说：“我怎么不难过?年轻的时候，我的上司爱好文学，我便学着作诗写文章，" +
                                        "想不到刚觉得有点小成绩了，却又换了一位爱好科学的上司。我赶紧又改学数学、研究物理，不料上司嫌我学历太浅，不够老成，" +
                                        "还是不重用我。后来换了现在这位上司，我自认文武兼备，人也老成了，谁知上司喜欢青年才俊，我……我眼看年龄渐高，" +
                                        "就要被迫退休了，一事无成，怎么不难过呢?”\n");
                            }
                        }.start();
                        break;
                    case 10:
                        final ProgressDialog pd10 = new ProgressDialog(MainActivity.this);
                        pd10.setTitle("请稍后：");
                        pd10.setMessage("正在查找故事中...");
                        speekText("正在查找故事中...");
                        pd10.show();
                        new Thread() {
                            @Override
                            public void run() {
                                try {
                                    Thread.sleep(4000);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
//                                pd10.setMessage("找到故事...");
//                                speekText("找到故事...");
//                                try {
//                                    Thread.sleep(4000);
//                                } catch (InterruptedException e) {
//                                    e.printStackTrace();
//                                }
                                pd10.dismiss();
                                speekText( "一只小灰兔得到了兔王奖励的第一根胡萝卜，这件事在整个兔群中激起了轩然大波。" +
                                        "兔王没想到反响如此强烈，而且居然是效果适得其反的反响。\n" +
                                        "　　有几只老兔子前来找他谈话，数落小灰兔的种种不是，质问兔王凭什么奖励小灰兔?兔王说：" +
                                        "“我认为小灰兔的工作表现不错。如果你们也能积极表现，自然也会得到奖励。”\n");
                            }
                        }.start();
                        break;
                    case 11:
                        final ProgressDialog pd11 = new ProgressDialog(MainActivity.this);
                        pd11.setTitle("请稍后：");
                        pd11.setMessage("正在查找故事中...");
                        speekText("正在查找故事中...");
                        pd11.show();
                        new Thread() {
                            @Override
                            public void run() {
                                try {
                                    Thread.sleep(4000);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
//                                pd11.setMessage("找到故事...");
//                                speekText("找到故事...");
//                                try {
//                                    Thread.sleep(4000);
//                                } catch (InterruptedException e) {
//                                    e.printStackTrace();
//                                }
                                pd11.dismiss();
                                speekText( "一只小灰兔得到了兔王奖励的第一根胡萝卜，这件事在整个兔群中激起了轩然大波。" +
                                        "兔王没想到反响如此强烈，而且居然是效果适得其反的反响。\n" +
                                        "　　有几只老兔子前来找他谈话，数落小灰兔的种种不是，质问兔王凭什么奖励小灰兔?兔王说：" +
                                        "“我认为小灰兔的工作表现不错。如果你们也能积极表现，自然也会得到奖励。”\n");
                            }
                        }.start();
                        break;
                    case 12:
                        final ProgressDialog pd12 = new ProgressDialog(MainActivity.this);
                        pd12.setTitle("请稍后：");
                        pd12.setMessage("正在查找故事中...");
                        speekText("正在查找故事中...");
                        pd12.show();
                        new Thread() {
                            @Override
                            public void run() {
                                try {
                                    Thread.sleep(4000);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
//                                pd12.setMessage("找到故事...");
//                                speekText("找到故事...");
//                                try {
//                                    Thread.sleep(4000);
//                                } catch (InterruptedException e) {
//                                    e.printStackTrace();
//                                }
                                pd12.dismiss();
                                speekText( "一个人在高山之巅的鹰巢里，抓到了一只幼鹰，他把幼鹰带回家，养在鸡笼里。这只幼鹰和鸡" +
                                        "一起啄食、嬉闹和休息。它以为自己是一只鸡。这只鹰渐渐长大，羽翼丰 满了，主人想把它训练成猎鹰，" +
                                        "可是由于终日和鸡混在一起，它已经变得和鸡完全一样，根本没有飞的愿望了。主人试了各种办法，都毫无效果，" +
                                        "最后把它带到山顶 上，一把将它扔了出去。这只鹰像块石头似的，直掉下去，慌乱之中它拼命地扑打翅膀，就这样，" +
                                        "它终于飞了起来!\n");
                            }
                        }.start();
                        break;
                }
            }
            else if(resultBuffer.toString().equals("关闭音乐。")) {
                mp1.pause();
                mp.pause();
                speekText("音乐已经关闭");
            }
            else if(resultBuffer.toString().equals("救命。") || resultBuffer.toString().equals("救命啊。")) {
                Intent intent = new Intent();
                intent.setAction("android.intent.action.CALL");
                intent.setData(Uri.parse("tel:"+"15755081825"));
                startActivity(intent);
            }

            else if(resultBuffer.toString().equals("短信。") || resultBuffer.toString().equals("发短信。")) {
                SmsManager smsManager = SmsManager.getDefault();// 获得短信管理器
                ArrayList<String> texts = smsManager.divideMessage("您好：\n" +
                        "检测到您的母亲有异常语音，建议您致电询问详情。");// 对短信内容进行拆分
                for (String te : texts)//text会显示错误
                {
                    smsManager.sendTextMessage("15755081825", null, te, null, null); // 短信发送
                }
            }
            else if(resultBuffer.toString().equals("打"+"开"+"电"+"视。") || resultBuffer.toString().equals("把电视打开。")|| resultBuffer.toString().equals("开电视。")) {
                shuju[0] = "{d01,01}";
                try {
                    mmOutputStream.write(shuju[0].getBytes());
                    speekText("电视已打开");
                } catch (Exception e) {
                        speekText("网关没有连接上，请稍后在试");
                        e.printStackTrace();
                }
            }
            else if(resultBuffer.toString().equals("找钥匙。") || resultBuffer.toString().equals("钥匙在哪呢。")) {
                shuju[40] = "{d07,01}";
                try {
                    mmOutputStream.write(shuju[40].getBytes());
                    speekText("正在查找钥匙中");
                } catch (Exception e) {
                        speekText("网关没有连接上，请稍后在试");
                    e.printStackTrace();
                }
            }
            else if(resultBuffer.toString().equals("找眼镜。")) {
                shuju[46] = "{d07,02}";
                try {
                    mmOutputStream.write(shuju[46].getBytes());
                    speekText("正在查找眼镜中");
                } catch (Exception e) {
                        speekText("网关没有连接上，请稍后在试");
                    e.printStackTrace();
                }

            }
            else if(resultBuffer.toString().equals("找钱包。")) {
                shuju[47] = "{d07,03}";
                try {
                    mmOutputStream.write(shuju[47].getBytes());
                    speekText("正在查找钱包中");
                } catch (Exception e) {
                    speekText("网关没有连接上，请稍后在试");
                    e.printStackTrace();
                }
            }
            else if(resultBuffer.toString().equals("关闭电视。")||  resultBuffer.toString().equals("关电视。")) {
                shuju[1] = "{d01,01}";
                try {
                    mmOutputStream.write(shuju[1].getBytes());
                    speekText("电视已经关闭");
                } catch (Exception e) {
                    speekText("网关没有连接上，请稍后在试");
                    e.printStackTrace();
                }
            }
            else if(resultBuffer.toString().equals("加台。")||  resultBuffer.toString().equals("频道加。")) {
                shuju[7] = "{d01,02}";
                try {
                    mmOutputStream.write(shuju[7].getBytes());
                    speekText("正在加台");
                } catch (Exception e) {
                    speekText("网关没有连接上，请稍后在试");
                }
            }
            else if(resultBuffer.toString().equals("减台。")||  resultBuffer.toString().equals("频道减。")) {
                shuju[8] = "{d01,03}";
                try {
                    mmOutputStream.write(shuju[8].getBytes());
                    speekText("正在减台");
                }  catch (Exception e) {
                    speekText("网关没有连接上，请稍后在试");
                }
            }
            else if(resultBuffer.toString().equals("调高音量。")||  resultBuffer.toString().equals("音量加。")|| resultBuffer.toString().equals("多加音量")) {
                shuju[9] = "{d01,04}";
                try {
                    mmOutputStream.write(shuju[9].getBytes());
                    speekText("正在减台");
                } catch (Exception e) {
                    speekText("网关没有连接上，请稍后在试");
                    e.printStackTrace();
                }
            }
            else if(resultBuffer.toString().equals("调低音量。")||  resultBuffer.toString().equals("音量减。")) {
                shuju[10] = "{d01,05}";
                try {
                    mmOutputStream.write(shuju[10].getBytes());
                    speekText("正在调低音量");
                } catch (Exception e) {
                    speekText("网关没有连接上，请稍后在试");
                    e.printStackTrace();
                }
            }
            else if(resultBuffer.toString().equals("频道一。")||  resultBuffer.toString().equals("调到一。")) {
                shuju[11] = "{d01,06}";
                try {
                    mmOutputStream.write(shuju[11].getBytes());
                    speekText("正在打开频道一");
                } catch (Exception e) {
                    speekText("网关没有连接上，请稍后在试");
                    e.printStackTrace();
                }
            }
            else if(resultBuffer.toString().equals("频道二。")||  resultBuffer.toString().equals("调到二。")) {
                shuju[12] = "{d01,07}";
                try {
                    mmOutputStream.write(shuju[12].getBytes());
                    speekText("正在打开频道二");
                } catch (Exception e) {
                    speekText("网关没有连接上，请稍后在试");
                    e.printStackTrace();
                }
            }

            else if(resultBuffer.toString().equals("频道三。")||  resultBuffer.toString().equals("调到三。")) {
                shuju[13] = "{d01,08}";
                try {
                    mmOutputStream.write(shuju[13].getBytes());
                    speekText("正在打开频道三");
                } catch (Exception e) {
                    speekText("网关没有连接上，请稍后在试");
                    e.printStackTrace();
                }
            }

            else if(resultBuffer.toString().equals("频道四。")||  resultBuffer.toString().equals("调到四。")) {
                shuju[14] = "{d01,09}";
                try {
                    mmOutputStream.write(shuju[14].getBytes());
                    speekText("正在打开频道四");
                } catch (Exception e) {
                    speekText("网关没有连接上，请稍后在试");
                }
            }
            else if(resultBuffer.toString().equals("频道五。")||  resultBuffer.toString().equals("调到五。")) {
                shuju[15] = "{d01,10}";
                try {
                    mmOutputStream.write(shuju[15].getBytes());
                    speekText("正在打开频道五");
                } catch (Exception e) {
                    speekText("网关没有连接上，请稍后在试");
                    e.printStackTrace();
                }
            }
            else if(resultBuffer.toString().equals("频道六。")||  resultBuffer.toString().equals("调到六。")) {
                shuju[16] = "{d01,11}";
                try {
                    mmOutputStream.write(shuju[16].getBytes());
                    speekText("正在打开频道六");
                } catch (Exception e) {
                    speekText("网关没有连接上，请稍后在试");
                    e.printStackTrace();
                }
            }
            else if(resultBuffer.toString().equals("频道七。")||  resultBuffer.toString().equals("调到七。")) {
                shuju[17] = "{d01,12}";
                try {
                    mmOutputStream.write(shuju[17].getBytes());
                    speekText("正在打开频道七");
                } catch (Exception e) {
                    speekText("网关没有连接上，请稍后在试");
                    e.printStackTrace();
                }
            }
            else if(resultBuffer.toString().equals("频道八。")||  resultBuffer.toString().equals("调到八。")) {
                shuju[18] = "{d01,13}";
                try {
                    mmOutputStream.write(shuju[18].getBytes());
                    speekText("正在打开频道八");
                } catch (Exception e) {
                    speekText("网关没有连接上，请稍后在试");
                    e.printStackTrace();
                }
            }
            else if(resultBuffer.toString().equals("频道九。")||  resultBuffer.toString().equals("调到九。")) {
                shuju[19] = "{d01,14}";
                try {
                    mmOutputStream.write(shuju[19].getBytes());
                    speekText("正在打开频道九");
                } catch (Exception e) {
                    speekText("网关没有连接上，请稍后在试");
                    e.printStackTrace();
                }
            }
            else if(resultBuffer.toString().equals("频道零。")||  resultBuffer.toString().equals("调到零。")) {
                shuju[20] = "{d01,15}";
                try {
                    mmOutputStream.write(shuju[20].getBytes());
                    speekText("正在打开频道零");
                } catch (Exception e) {
                    speekText("网关没有连接上，请稍后在试");
                    e.printStackTrace();
                }
            }
            else  if(resultBuffer.toString().equals("返回。")||  resultBuffer.toString().equals("返回到上一个。")) {
                shuju[21] = "{d01,16}";
                try {
                    mmOutputStream.write(shuju[21].getBytes());
                    speekText("正在打开返回上一个台");
                } catch (Exception e) {
                    speekText("网关没有连接上，请稍后在试");
                    e.printStackTrace();
                }
            }
            else  if(resultBuffer.toString().equals("静音。")||  resultBuffer.toString().equals("关闭声音。")) {
                shuju[22] = "{d01,17}";
                try {
                    mmOutputStream.write(shuju[22].getBytes());
                    speekText("正在打开静音按钮");
                } catch (Exception e) {
                    speekText("网关没有连接上，请稍后在试");
                    e.printStackTrace();
                }
            }
            else if(resultBuffer.toString().equals("輸入切換。")||  resultBuffer.toString().equals("切換。")) {
                shuju[23] = "{d01,18}";
                try {
                    mmOutputStream.write(shuju[23].getBytes());
                    speekText("正在打开輸入切換按钮");
                } catch (Exception e) {
                    speekText("网关没有连接上，请稍后在试");
                    e.printStackTrace();
                }
            }
            else if(resultBuffer.toString().equals("上。")||  resultBuffer.toString().equals("调上一个台。")) {
                shuju[24] = "{d01,19}";
                try {
                    mmOutputStream.write(shuju[24].getBytes());
                    speekText("正在打开上按钮");
                } catch (Exception e) {
                    speekText("网关没有连接上，请稍后在试");
                    e.printStackTrace();
                }
            }
            else if(resultBuffer.toString().equals("下。")||  resultBuffer.toString().equals("调下一个台。")) {
                shuju[25] = "{d01,20}";
                try {
                    mmOutputStream.write(shuju[25].getBytes());
                    speekText("正在打开下按钮");
                } catch (Exception e) {
                    speekText("网关没有连接上，请稍后在试");
                    e.printStackTrace();
                }
            }
            else if(resultBuffer.toString().equals("左。") ){
                shuju[26] = "{d01,21}";
                try {
                    mmOutputStream.write(shuju[26].getBytes());
                    speekText("正在打开左按钮");
                } catch (Exception e) {
                    speekText("网关没有连接上，请稍后在试");
                    e.printStackTrace();
                }
            }
            else if(resultBuffer.toString().equals("右。") ){
                shuju[27] = "{d01,22}";
                try {
                    mmOutputStream.write(shuju[27].getBytes());
                    speekText("正在打开右按钮");
                } catch (Exception e) {
                    speekText("网关没有连接上，请稍后在试");
                    e.printStackTrace();
                }
            }
            else if(resultBuffer.toString().equals("OK。") ){
                shuju[28] = " {d01,23}";
                try {
                    mmOutputStream.write(shuju[28].getBytes());
                    speekText("正在打开OK按钮");
                } catch (Exception e) {
                    speekText("网关没有连接上，请稍后在试");
                    e.printStackTrace();
                }
            }
            else if(resultBuffer.toString().equals("打开空调。") || resultBuffer.toString().equals("把空调打开。") || resultBuffer.toString().equals("开空调。")) {
                shuju[2] = "{d02,24}";
                try {
                    mmOutputStream.write(shuju[2].getBytes());
                    speekText("空调已打开");
                } catch (Exception e) {
                    speekText("网关没有连接上，请稍后在试");
                    e.printStackTrace();
                }
            }
            else if(resultBuffer.toString().equals("关闭空调。") || resultBuffer.toString().equals("把空调关闭。") || resultBuffer.toString().equals("关空调。")) {
                shuju[3] = "{d02,59}";
                try {
                    mmOutputStream.write(shuju[3].getBytes());
                    speekText("空调已经关闭");
                } catch (Exception e) {
                    speekText("网关没有连接上，请稍后在试");
                    e.printStackTrace();
                }
            }
            else if(resultBuffer.toString().equals("打开灯。") || resultBuffer.toString().equals("开灯。")) {
                shuju[55] = "{d04,01}";
                try {
                    mmOutputStream.write(shuju[55].getBytes());
                    speekText("灯以打开");
                } catch (Exception e) {
                    speekText("网关没有连接上，请稍后在试");
                    e.printStackTrace();
                }
            }
            else  if(resultBuffer.toString().equals("关闭灯。") || resultBuffer.toString().equals("关灯。")) {
                shuju[56] = "{d04,02}";
                try {
                    mmOutputStream.write(shuju[56].getBytes());
                    speekText("灯以关闭");
                } catch (Exception e) {
                    speekText("网关没有连接上，请稍后在试");
                    e.printStackTrace();
                }
            }
            //厨房气阀
            else if(resultBuffer.toString().equals("打开窗户。") || resultBuffer.toString().equals("打开厨房窗户。")) {
                shuju[50] = "{d06,off}";
                try {
                    mmOutputStream.write(shuju[50].getBytes());
                    speekText("正在打开厨房窗户");
                } catch (Exception e) {
                    speekText("网关没有连接上，请稍后在试");
                    e.printStackTrace();
                }
            }
            else if(resultBuffer.toString().equals("关闭窗户。") || resultBuffer.toString().equals("关闭厨房窗户。")) {
                shuju[51] = "{d06,on}";
                try {
                    mmOutputStream.write(shuju[51].getBytes());
                    speekText("正在关闭厨房窗户");
                } catch (Exception e) {
                    speekText("网关没有连接上，请稍后在试");
                    e.printStackTrace();
                }
            }
            else if(resultBuffer.toString().equals("打开阀门。") || resultBuffer.toString().equals("打开厨房阀门。")) {
                shuju[52] = "{d08,on}";
                try {
                    mmOutputStream.write(shuju[52].getBytes());
                    speekText("正在打开厨房阀门");
                } catch (Exception e) {
                    speekText("网关没有连接上，请稍后在试");
                    e.printStackTrace();
                }
            }
            else if(resultBuffer.toString().equals("关闭阀门。") || resultBuffer.toString().equals("关闭厨房阀门。")) {
                shuju[53] = "{d08,off}";
                try {
                    mmOutputStream.write(shuju[53].getBytes());
                    speekText("正在关闭厨房阀门");
                } catch (Exception e) {
                    speekText("网关没有连接上，请稍后在试");
                    e.printStackTrace();
                }
            }
//            else if(resultBuffer.toString().equals("拖鞋归位。") ) {
//                ProgressDialog dialog = ProgressDialog.show(MainActivity.this, "提示", "拖鞋正在归位中", false, true);
//                speekText("拖鞋正在归位中");
//                dialog.cancel();
//            }
            else {
                final String juti_address = resultBuffer.toString().substring(0, 2);//取值，主要是对于天气的
                //我需要对值进行处理 判断是不是天气
                if (resultBuffer.toString().equals(juti_address + "天气怎么样？") || resultBuffer.toString().equals(juti_address + "今天天气怎么样？") ) {
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            json = getResult(juti_address);
                            Message m = handle.obtainMessage();
                            handle.sendMessage(m);
                        }
                    }).start();
                    handle = new Handler() {
                        public void handleMessage(Message msg) {
                            if (json != null && json.get("resultcode").getAsInt() == 200) {
                                JsonObject json2 = json.get("result").getAsJsonObject();
                                JsonObject json3 = json2.get("today").getAsJsonObject();
                                JsonObject json4 = json2.get("sk").getAsJsonObject();
                                String[] string = new String[]{"温度：" + json4.get("temp") + "度", "今日温度：" + json3.get("temperature").getAsString(),
                                        "今日天气：" + json3.get("weather").getAsString(),
                                        json4.get("wind_direction").getAsString() + json4.get("wind_strength").getAsString(),
                                        "湿度：" + json4.get("humidity").getAsString()};
                                speekText(juti_address + "今日温度：" + json4.get("temp").toString() + "度" + "今日温度：" + json3.get("temperature").getAsString()
                                        + "今日天气：" + json3.get("weather").getAsString() + json4.get("wind_direction").getAsString() + json4.get("wind_strength").getAsString() +
                                        "湿度：" + json4.get("humidity").getAsString());
                            } else {
                                Toast.makeText(MainActivity.this, "天气信息获取失败！", Toast.LENGTH_LONG).show();
                                speekText("天气信息获取失败,请重试");
                            }
                            super.handleMessage(msg);
                        }
                    };
                }else{
                    //异常分析结果
                    abnormal++;
                    if (abnormal % 5 == 0) {
                        Intent intent1 = getIntent();
                        String phone = intent1.getStringExtra("hy_phone");//电话
                        SmsManager smsManager = SmsManager.getDefault();// 获得短信管理器
                        ArrayList<String> texts = smsManager.divideMessage("您好：检测到您的母亲有异常语音，建议您致电询问详情。");
                        for (String te : texts)
                        {
                            smsManager.sendTextMessage(phone, null, te, null, null);
                        }
                    }
                }
            }
        }



    @Override
        public void onError(SpeechError speechError) {
        }
    }
    private int random() {
        return RANDOM.nextInt(MAX_RANGE);
    }

    // 听写监听器
    private RecognizerListener mRecoListener = new RecognizerListener() {
        public void onResult(RecognizerResult results, boolean isLast) {
            Log.e(TAG, results.getResultString());
            System.out.println(results.getResultString());
            showTip(results.getResultString());
        }
        // 会话发生错误回调接口
        public void onError(SpeechError error) {
            showTip(error.getPlainDescription(true));
            // 获取错误码描述
            Log.e(TAG, "error.getPlainDescription(true)==" + error.getPlainDescription(true));
        }
        // 开始录音
        public void onBeginOfSpeech() {
            showTip(" 开始录音 ");
        }

        //volume 音量值0~30， data音频数据
        public void onVolumeChanged(int volume, byte[] data) {
            showTip(" 声音改变了 ");
        }

        // 结束录音
        public void onEndOfSpeech() {
            showTip(" 结束录音 ");
        }

        // 扩展用接口
        public void onEvent(int eventType, int arg1, int arg2, Bundle obj) {
        }
    };
    void showTip(String data) {
        // Toast.makeText( this, data, Toast.LENGTH_SHORT).show() ;
    }

    //根据城市名，获得天气信息方法，返回保存在String类型中的JSON数据
    private JsonObject getResult (String city){
        String cityName;
        String it;
        String result = "";
        JsonParser parse =new JsonParser();  //创建json解析器
        JsonObject json = null;
        try{
            cityName = java.net.URLEncoder.encode(city, "UTF-8"); //将城市名转化为UTF-8格式
            it = "http://v.juhe.cn/weather/index?format=2&cityname=" + cityName + "&key=91a781a05b0884ed700be2569481e317";
            //75c6c0a4c52e3a2dd7b614cd91237ced  天气接口华杨
            //91a781a05b0884ed700be2569481e317   马继斌
            //网络获取GPS数据代码开始
            URL url = new URL(it);
            HttpURLConnection uRLConnection = (HttpURLConnection) url.openConnection();
            uRLConnection.setDoOutput(true);
            uRLConnection.connect();
            InputStream is = uRLConnection.getInputStream();
            BufferedReader br = new BufferedReader(new InputStreamReader(is, "UTF-8"));
            String readLine;
            while ((readLine = br.readLine()) != null)
            {
                result += readLine;
            }
            is.close();
            br.close();
            uRLConnection.disconnect();
            json=(JsonObject) parse.parse(result);  //创建jsonObject对象
            return json;
        }catch (UnsupportedEncodingException e1){
            e1.printStackTrace();
            Toast.makeText(MainActivity.this,"城市名转码失败！",Toast.LENGTH_LONG).show();
        }catch (MalformedURLException e2){
            e2.printStackTrace();
            Toast.makeText(MainActivity.this,"获取天气信息失败！",Toast.LENGTH_LONG).show();
        }
        catch (IOException e3){
            e3.printStackTrace();
            Toast.makeText(MainActivity.this,"获取天气失败！",Toast.LENGTH_LONG).show();
        }catch (JsonIOException e) {
            e.printStackTrace();
            Toast.makeText(MainActivity.this,"天气数据处理失败！",Toast.LENGTH_LONG).show();
        } catch (JsonSyntaxException e) {
            e.printStackTrace();
            Toast.makeText(MainActivity.this,"天气数据处理失败！",Toast.LENGTH_LONG).show();
        }
        return json;
    }
}

