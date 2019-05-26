package fish2;

import android.os.Bundle;

import com.iflytek.cloud.SpeechError;
import com.iflytek.cloud.SynthesizerListener;

/**
 * Created by Administrator on 2019/5/4.
 */

public  class MySynthesizerListener implements SynthesizerListener {
    @Override
    public void onSpeakBegin() {
        showTip(" 开始播放 ");
    }
    @Override
    public void onSpeakPaused() {
        showTip(" 暂停播放 ");
    }
    @Override
    public void onSpeakResumed() {
        showTip(" 继续播放 ");
    }
    @Override
    public void onBufferProgress(int percent, int beginPos, int endPos,
                                 String info) {
        // 合成进度
    }
    @Override
    public void onSpeakProgress(int percent, int beginPos, int endPos) {
        // 播放进度
    }
    @Override
    public void onCompleted(SpeechError error) {
        if (error == null) {
            showTip("播放完成 ");
        } else if (error != null) {
            showTip(error.getPlainDescription(true));
        }
    }
    @Override
    public void onEvent(int eventType, int arg1, int arg2, Bundle obj) {

    }
    void showTip(String data) {
        // Toast.makeText( this, data, Toast.LENGTH_SHORT).show() ;
    }
}
