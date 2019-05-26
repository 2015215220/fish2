package fish2;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import DB.DBHelper;

public class Login extends AppCompatActivity {
    private static final String TAG = "Login";
    Button btn1,btn2;
    EditText Epusername,Eppassword;
    DBHelper dbHelper;//数据库资料
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);


        btn1= (Button) findViewById(R.id.btn1);//主要是登录模块
        btn2= (Button) findViewById(R.id.btn2);//主要是注册模块
        Epusername=(EditText)findViewById(R.id.name);
        Eppassword=(EditText)findViewById(R.id.phone);
        dbHelper=new DBHelper(this);//数据库，很重要，没写闪退


        btn1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String name=Epusername.getText().toString();
                String password=Eppassword.getText().toString();
                SQLiteDatabase sdb=dbHelper.getReadableDatabase();
                String sql="select * from h where name=? and password=?";
                Cursor cursor=sdb.rawQuery(sql, new String[]{name,password});
                if(cursor.moveToFirst()==true){
                    String address=cursor.getString(cursor.getColumnIndex("address"));//读出的是滁州,合肥
                    String phone=cursor.getString(cursor.getColumnIndex("phone"));//
                    Log.e(TAG,address+phone);
                    cursor.close();
                    Intent intent=new Intent(Login.this,MainActivity.class);
                    intent.putExtra("hy_address",address);//带地点数据
                    intent.putExtra("hy_phone",phone);//带电话数据
                    startActivity(intent);//启动
                }else{
                    Toast.makeText(Login.this, "密码错误,请重新输入", Toast.LENGTH_SHORT).show();
                }
            }
        });

        btn2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent;
                intent = new Intent(Login.this, Zhuce.class);
                startActivity(intent);
            }
        });


    }
}
