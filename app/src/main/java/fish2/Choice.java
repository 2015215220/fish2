package fish2;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class Choice extends AppCompatActivity {
    private Button btn1_zhuce,btn1_face;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_choice);
        btn1_zhuce=(Button)findViewById(R.id.btn1_zhuce);
        btn1_face=(Button)findViewById(R.id.btn1_face);
        btn1_zhuce.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent=new Intent(Choice.this,Login.class);
                startActivity(intent);
            }
        });
        btn1_face.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent=new Intent(Choice.this,Face_login.class);
                startActivity(intent);
            }
        });
    }
}
