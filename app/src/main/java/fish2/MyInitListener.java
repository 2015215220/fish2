package fish2;

import com.iflytek.cloud.ErrorCode;
import com.iflytek.cloud.InitListener;

/**
 * Created by Administrator on 2019/5/4.
 */

public class MyInitListener implements InitListener {
    @Override
    public void onInit(int code) {
        if (code != ErrorCode.SUCCESS) {
            showTip("初始化失败 ");
        }
    }
    void showTip(String data) {
        // Toast.makeText( this, data, Toast.LENGTH_SHORT).show() ;
    }
}
