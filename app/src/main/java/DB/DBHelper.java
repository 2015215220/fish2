package DB;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
public class DBHelper extends SQLiteOpenHelper {

    public DBHelper(Context context) {
        super(context, "hy.db", null, 1);
    }
    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE h(_id INTEGER PRIMARY KEY AUTOINCREMENT, name VARCHAR(20),  password VARCHAR(20),address VARCHAR(20),phone VARCHAR(20))");

        //h表中一共有姓名，密码，天气，紧急联系人
    }
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    }
}
