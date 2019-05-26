package fish2;



import com.google.gson.JsonElement;
import com.google.gson.JsonIOException;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSyntaxException;
import com.google.gson.internal.Streams;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.MalformedJsonException;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;

/**
 * Created by Administrator on 2017/4/28.
 */

public class JsonParser {
    public JsonParser() {
    }

    public JsonElement parse(String json) throws JsonSyntaxException {
        return this.parse((Reader)(new StringReader(json)));
    }

    public JsonElement parse(Reader json) throws JsonIOException, JsonSyntaxException {
        try {
            JsonReader e = new JsonReader(json);
            JsonElement element = this.parse(e);
            if(!element.isJsonNull() && e.peek() != JsonToken.END_DOCUMENT) {
                throw new JsonSyntaxException("Did not consume the entire document.");
            } else {
                return element;
            }
        } catch (MalformedJsonException var4) {
            throw new JsonSyntaxException(var4);
        } catch (IOException var5) {
            throw new JsonIOException(var5);
        } catch (NumberFormatException var6) {
            throw new JsonSyntaxException(var6);
        }
    }

    public JsonElement parse(JsonReader json) throws JsonIOException, JsonSyntaxException {
        boolean lenient = json.isLenient();
        json.setLenient(true);

        JsonElement e;
        try {
            e = Streams.parse(json);
        } catch (StackOverflowError var8) {
            throw new JsonParseException("Failed parsing JSON source: " + json + " to Json", var8);
        } catch (OutOfMemoryError var9) {
            throw new JsonParseException("Failed parsing JSON source: " + json + " to Json", var9);
        } finally {
            json.setLenient(lenient);
        }

        return e;
    }
    public static String parseIatResult(String json) {
        StringBuffer ret = new StringBuffer() ;
        try {
            JSONTokener tokener = new JSONTokener(json) ;
            JSONObject joResult = new JSONObject(tokener) ;

            JSONArray words = joResult.getJSONArray("ws" );
            for (int i = 0; i < words.length(); i++) {
                // 转写结果词，默认使用第一个结果
                JSONArray items = words.getJSONObject(i).getJSONArray("cw" );
                JSONObject obj = items.getJSONObject(0 );
                ret.append(obj.getString("w" ));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return ret.toString();
    }

    public static String parseGrammarResult(String json) {
        StringBuffer ret = new StringBuffer() ;
        try {
            JSONTokener tokener = new JSONTokener(json) ;
            JSONObject joResult = new JSONObject(tokener) ;

            JSONArray words = joResult.getJSONArray("ws" );
            for (int i = 0; i < words.length(); i++) {
                JSONArray items = words.getJSONObject(i).getJSONArray("cw" );
                for (int j = 0; j < items.length() ; j++)
                {
                    JSONObject obj = items.getJSONObject(j);
                    if (obj.getString("w").contains( "nomatch"))
                    {
                        ret.append( "没有匹配结果.") ;
                        return ret.toString();
                    }
                    ret.append( "【结果】" + obj.getString("w" ));
                    ret.append("【置信度】 " + obj.getInt("sc" ));
                    ret.append("\n ");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            ret.append(" 没有匹配结果 .");
        }
        return ret.toString();
    }
}
