package top.narozeol.thoughts;

import org.json.JSONObject;
import java.net.HttpURLConnection;
import java.net.URL;
import java.io.InputStream;
import java.io.ByteArrayOutputStream;

final class Api {
    static final String ORIGIN="https://narozeol.top";
    static final class Failure extends Exception {final int code;Failure(int code,String message){super(message);this.code=code;}}
    static JSONObject request(String path,String method,JSONObject body,String token) throws Exception {
        HttpURLConnection connection=(HttpURLConnection)new URL(ORIGIN+"/api"+path).openConnection();
        connection.setConnectTimeout(12000);connection.setReadTimeout(30000);connection.setInstanceFollowRedirects(false);connection.setRequestMethod(method);
        connection.setRequestProperty("Accept","application/json");
        if(!token.isEmpty())connection.setRequestProperty("Authorization","Bearer "+token);
        try{
            if(body!=null){connection.setDoOutput(true);connection.setRequestProperty("Content-Type","application/json");try(java.io.OutputStream out=connection.getOutputStream()){out.write(body.toString().getBytes("UTF-8"));}}
            int status=connection.getResponseCode();InputStream stream=status>=400?connection.getErrorStream():connection.getInputStream();
            String text="";
            if(stream!=null){try(InputStream in=stream;ByteArrayOutputStream out=new ByteArrayOutputStream()){byte[] buffer=new byte[4096];int n;while((n=in.read(buffer))!=-1){out.write(buffer,0,n);if(out.size()>16*1024*1024)throw new Exception("响应过大，请使用网页管理");}text=out.toString("UTF-8");}}
            JSONObject result;
            try{result=new JSONObject(text);}catch(Exception e){throw new Failure(status,"服务器暂时无法使用，本地记录已保留");}
            if(status<200||status>=300)throw new Failure(status,result.optString("error","同步失败"));
            return result;
        }finally{connection.disconnect();}
    }
}
