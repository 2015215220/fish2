package fish2;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.MediaPlayer;

public class CallAlarm extends BroadcastReceiver {
    private MediaPlayer mediaPlayer;
    @Override
    public void onReceive(Context context, Intent intent) {
//        Intent intent1 = new Intent(context,AlarmAlert.class);
//        context.startActivity(intent1);
        mediaPlayer = MediaPlayer.create(context,R.raw.yao);
        mediaPlayer.start();

    }
}
