package fish2;

import android.content.ContentValues;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import DB.DBHelper;


public class Zhuce extends AppCompatActivity implements View.OnClickListener{

    Button btn3,btn4;//注册和返回
    TextView Epname,Epassword,Eprepassword,Epaddress,Epphone;
    //数据库操作
    private DBHelper mHelper; //是一个类，主要是存放数据库的
    private SQLiteDatabase db;//
    private ContentValues values;
    //      implements中使用方法监听
    //      第一步：申明继承接口
    //      第二步：alt+enter键 产生一个方法如下onClick
    //      第三步：注册监听，如  名字.setOnClickListener(this);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_zhuce);

        Epname=(TextView) findViewById(R.id.name);
        Epassword=(TextView)findViewById(R.id.password);
        Eprepassword=(TextView)findViewById(R.id.repassword);
        Epaddress=(TextView)findViewById(R.id.address);
        Epphone=(TextView)findViewById(R.id.phone);
        btn3=(Button)findViewById(R.id.zhuce);
        btn4=(Button)findViewById(R.id.fanhui);
        mHelper=new DBHelper(this);//添加注册
        btn3.setOnClickListener(this);
        btn4.setOnClickListener(this);
    }
    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.zhuce:
                //注册数据进数据库
                String name=Epname.getText().toString();
                String password=Epassword.getText().toString();
                String repassword=Eprepassword.getText().toString();
                String address=Epaddress.getText().toString();
                String phone=Epphone.getText().toString();
                if((name.length()==0) || (password.length()==0) || (repassword.length()==0) || (address.length()==0) || (phone.length()==0)){
                    Toast.makeText(this, "不能为空", Toast.LENGTH_SHORT).show();
                }else if(password.equals(repassword)){
                    db = mHelper.getWritableDatabase();//获取可读写SQLiteDatabse对象
                    values = new ContentValues();       // 创建ContentValues对象
                    values.put("name", name);           // 将数据添加到ContentValues对象
                    values.put("password", password);
                    values.put("address",address);
                    values.put("phone",phone);
                    db.insert("h", null, values);
                    Toast.makeText(this, "信息已添加", Toast.LENGTH_SHORT).show();
                    db.close();
                    Intent intent=new Intent(Zhuce.this,Login.class);
                    startActivity(intent);
                }else{
                    Toast.makeText(this, "密码不对，请重新输入", Toast.LENGTH_SHORT).show();
                }
                break;
            case R.id.fanhui:
                Intent intent=new Intent(Zhuce.this,Login.class);
                startActivity(intent);
                break;
        }
    }
}
