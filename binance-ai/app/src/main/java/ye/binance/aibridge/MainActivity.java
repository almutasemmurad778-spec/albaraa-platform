package ye.binance.aibridge;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.Gravity;
import android.view.WindowManager;
import android.graphics.Color;
import android.widget.*;
import org.json.*;
import java.net.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public class MainActivity extends Activity {
  static final int YELLOW=Color.rgb(240,185,36), DARK=Color.rgb(14,19,26);
  EditText ai,key,secret,command; TextView output; Button go;
  Handler ui=new Handler(Looper.getMainLooper());
  ExecutorService worker=Executors.newSingleThreadExecutor();
  JSONArray history=new JSONArray();
  volatile boolean busy=false;
  @Override public void onCreate(Bundle b){
    super.onCreate(b);
    getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);
    LinearLayout root=new LinearLayout(this);
    root.setOrientation(LinearLayout.VERTICAL);root.setPadding(24,24,24,24);root.setBackgroundColor(DARK);
    ScrollView scroll=new ScrollView(this);scroll.addView(root);setContentView(scroll);
    add(root,"BINANCE × GPT",26,YELLOW);
    add(root,"مساعد مستقل يستخدم OpenAI API وBinance Exchange API. العمليات المالية تحتاج موافقة منك.",15,Color.WHITE);
    add(root,"مفاتيح الاتصال (لا تُحفظ على الهاتف)",17,YELLOW);
    ai=entry(root,"OpenAI API key",true);
    key=entry(root,"Binance HMAC API key",true);
    secret=entry(root,"Binance HMAC API secret",true);
    Button test=new Button(this);test.setText("اختبار قراءة الحساب");root.addView(test);
    test.setOnClickListener(v->testAccount());
    add(root,"أمر الذكاء الاصطناعي",17,YELLOW);
    command=entry(root,"مثال: اعرض أرصدتي أو اشتري 0.001 BTC بسعر السوق",false);
    go=new Button(this);go.setText("إرسال الأمر");root.addView(go);go.setOnClickListener(v->send());
    Button clear=new Button(this);clear.setText("جلسة جديدة ومسح المفاتيح");root.addView(clear);
    clear.setOnClickListener(v->{if(!busy){ai.setText("");key.setText("");secret.setText("");history=new JSONArray();output.setText("");}});
    output=add(root,"الحالة: جاهز. لا تضع كلمة مرور حسابك أو عبارة استرداد المحفظة.",15,Color.WHITE);
  }
  TextView add(LinearLayout p,String s,int size,int color){
    TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setTextColor(color);
    t.setPadding(4,12,4,12);t.setTextDirection(View.TEXT_DIRECTION_RTL);p.addView(t);return t;
  }
  EditText entry(LinearLayout p,String hint,boolean hidden){
    EditText e=new EditText(this);e.setHint(hint);e.setTextColor(Color.WHITE);
    e.setHintTextColor(Color.LTGRAY);e.setSingleLine(hidden);
    if(hidden)e.setInputType(129);
    else e.setMinLines(3);
    p.addView(e);return e;
  }
  void result(String s){ui.post(()->output.setText(s));}
  void testAccount(){
    if(busy)return;
    String k=key.getText().toString().trim(),s=secret.getText().toString().trim();
    if(k.isEmpty()||s.isEmpty()){result("أدخل مفتاح Binance ومفتاحه السري");return;}
    busy=true;go.setEnabled(false);result("جار اختبار الحساب...");
    worker.execute(()->{try{
      JSONObject acc=new JSONObject(binance("GET","/api/v3/account",new JSONObject(),true,k,s));
      result("تم الاتصال. التداول: "+acc.optBoolean("canTrade")+"؛ نوع الحساب: "+acc.optString("accountType"));
    }catch(Exception e){result("فشل الاتصال: "+error(e));}finally{busy=false;ui.post(()->go.setEnabled(true));}});
  }
  void send(){
    if(busy)return;
    String a=ai.getText().toString().trim(),k=key.getText().toString().trim(),s=secret.getText().toString().trim(),prompt=command.getText().toString().trim();
    if(a.isEmpty()||k.isEmpty()||s.isEmpty()||prompt.isEmpty()){result("أدخل المفاتيح والأمر");return;}
    busy=true;go.setEnabled(false);result("جار تنفيذ طلب المساعد...");
    worker.execute(()->{try{
      JSONArray messages=new JSONArray();
      for(int i=Math.max(0,history.length()-12);i<history.length();i++)messages.put(history.getJSONObject(i));
      messages.put(new JSONObject().put("role","user").put("content",prompt));
      String reply=agent(messages,a,k,s);
      history=messages;result(reply);
    }catch(Exception e){result("خطأ: "+error(e));}
    finally{busy=false;ui.post(()->go.setEnabled(true));}});
  }
  String agent(JSONArray messages,String oa,String bk,String bs)throws Exception{
    for(int pass=0;pass<6;pass++){
      JSONArray all=new JSONArray().put(new JSONObject().put("role","system").put("content",
        "You are an Arabic Binance exchange assistant. Use official documented REST APIs only. "+
        "Never invent trade sizes, addresses, or transfers. Only perform money-moving requests if the user explicitly asked. "+
        "Never put credentials or secrets in tool calls. Approval is required on the phone for every write. "+
        "Use signed=true for private endpoints; signed=false for public market data. Never claim execution without API success. "+
        "Binance Web3 Agentic Wallet is different from the Binance Exchange API."));
      for(int i=0;i<messages.length();i++)all.put(messages.getJSONObject(i));
      JSONObject p=new JSONObject().put("model","gpt-4.1-mini").put("messages",all).put("tools",tools()).put("tool_choice","auto");
      JSONObject resp=new JSONObject(request("https://api.openai.com/v1/chat/completions","POST",p.toString(),"Bearer "+oa,null));
      JSONObject m=resp.getJSONArray("choices").getJSONObject(0).getJSONObject("message");
      messages.put(m);
      JSONArray calls=m.optJSONArray("tool_calls");
      if(calls==null||calls.length()==0)return m.optString("content","اكتمل الطلب.");
      for(int i=0;i<calls.length();i++){
        JSONObject c=calls.getJSONObject(i), args=new JSONObject(c.getJSONObject("function").getString("arguments"));
        String answer;
        try{
          String method=args.getString("method").toUpperCase(Locale.ROOT),path=args.getString("path");
          JSONObject params=args.optJSONObject("params");if(params==null)params=new JSONObject();
          validate(method,path,params);
          if(!method.equals("GET")&&!approve(method,path,params))answer="NOT_EXECUTED: user declined authorization";
          else answer=binance(method,path,params,args.optBoolean("signed",true),bk,bs);
        }catch(Exception ex){answer="ERROR: "+error(ex);}
        if(answer.length()>10000)answer=answer.substring(0,10000)+"...[truncated]";
        messages.put(new JSONObject().put("role","tool").put("tool_call_id",c.getString("id")).put("content",answer));
      }
    }
    return "تم بلوغ الحد الأقصى للخطوات. أرسل أمر متابعة.";
  }
  JSONArray tools()throws Exception{
    JSONObject props=new JSONObject();
    props.put("method",new JSONObject().put("type","string").put("enum",new JSONArray().put("GET").put("POST").put("PUT").put("DELETE")));
    props.put("path",new JSONObject().put("type","string"));
    props.put("signed",new JSONObject().put("type","boolean"));
    props.put("params",new JSONObject().put("type","object").put("additionalProperties",new JSONObject().put("type","string")));
    JSONObject fn=new JSONObject().put("name","binance_call").put("description","Call Binance official REST APIs; writes require phone approval.")
      .put("parameters",new JSONObject().put("type","object").put("properties",props)
      .put("required",new JSONArray().put("method").put("path").put("params").put("signed")));
    return new JSONArray().put(new JSONObject().put("type","function").put("function",fn));
  }
  void validate(String method,String path,JSONObject params)throws Exception{
    if(!method.matches("GET|POST|PUT|DELETE")||path.length()>160||
       !path.matches("^/(api/v3|sapi/v1|sapi/v2|fapi/v1|fapi/v2|dapi/v1|dapi/v2)/[A-Za-z0-9_/-]+$"))
      throw new Exception("طريقة الطلب أو المسار غير مسموح");
    Iterator<String> it=params.keys();
    while(it.hasNext()){
      String k=it.next();
      if(!k.matches("[a-zA-Z][a-zA-Z0-9_]{0,60}")||k.matches("(?i)timestamp|signature|apiKey|secretKey|password|access_token|authorization"))
        throw new Exception("معامل غير مسموح: "+k);
      if(params.isNull(k)||params.getString(k).length()>2048)throw new Exception("قيمة غير صالحة");
    }
  }
  boolean approve(String method,String path,JSONObject params)throws Exception{
    CountDownLatch latch=new CountDownLatch(1);java.util.concurrent.atomic.AtomicBoolean ok=new java.util.concurrent.atomic.AtomicBoolean(false);
    String detail=method+" "+path+"\n"+params.toString(2)+"\n\nعملية مالية حقيقية قد لا يمكن التراجع عنها.";
    ui.post(()->new AlertDialog.Builder(this).setTitle("موافقة على تنفيذ Binance").setMessage(detail).setCancelable(false)
      .setNegativeButton("إلغاء",(d,w)->latch.countDown())
      .setPositiveButton("أوافق على التنفيذ",(d,w)->{ok.set(true);latch.countDown();}).show());
    return latch.await(180,TimeUnit.SECONDS)&&ok.get();
  }
  String binance(String method,String path,JSONObject params,boolean signed,String api,String sec)throws Exception{
    validate(method,path,params);
    String host=path.startsWith("/fapi/")?"https://fapi.binance.com":path.startsWith("/dapi/")?"https://dapi.binance.com":"https://api.binance.com";
    StringBuilder q=new StringBuilder();
    for(Iterator<String>it=params.keys();it.hasNext();){
      String k=it.next();if(q.length()>0)q.append("&");q.append(enc(k)).append("=").append(enc(params.getString(k)));
    }
    if(signed){
      String timePath=path.startsWith("/fapi/")?"/fapi/v1/time":path.startsWith("/dapi/")?"/dapi/v1/time":"/api/v3/time";
      long t=new JSONObject(request(host+timePath,"GET",null,null,null)).getLong("serverTime");
      if(q.length()>0)q.append("&");q.append("recvWindow=5000&timestamp=").append(t);
      Mac mac=Mac.getInstance("HmacSHA256");mac.init(new SecretKeySpec(sec.getBytes(StandardCharsets.UTF_8),"HmacSHA256"));
      StringBuilder hash=new StringBuilder();for(byte b:mac.doFinal(q.toString().getBytes(StandardCharsets.UTF_8)))hash.append(String.format(Locale.US,"%02x",b&255));
      q.append("&signature=").append(hash);
    }
    return request(host+path+(q.length()==0?"":"?"+q),method,null,null,signed?api:null);
  }
  static String enc(String s)throws Exception{return URLEncoder.encode(s,"UTF-8").replace("+","%20");}
  static String error(Exception e){String s=e.getMessage();return s==null?"غير معروف":s.substring(0,Math.min(600,s.length()));}
  static String request(String link,String method,String data,String auth,String key)throws Exception{
    HttpURLConnection c=(HttpURLConnection)new URL(link).openConnection();
    try{
      c.setRequestMethod(method);c.setConnectTimeout(15000);c.setReadTimeout(30000);
      c.setInstanceFollowRedirects(false);c.setRequestProperty("Accept","application/json");
      if(auth!=null)c.setRequestProperty("Authorization",auth);
      if(key!=null)c.setRequestProperty("X-MBX-APIKEY",key);
      if(data!=null){c.setDoOutput(true);c.setRequestProperty("Content-Type","application/json");
        try(OutputStream out=c.getOutputStream()){out.write(data.getBytes(StandardCharsets.UTF_8));}}
      int status=c.getResponseCode();
      InputStream in=status<400?c.getInputStream():c.getErrorStream();
      if(in==null)throw new Exception("HTTP "+status);
      StringBuilder result=new StringBuilder();
      try(BufferedReader r=new BufferedReader(new InputStreamReader(in,StandardCharsets.UTF_8))){
        String line;while((line=r.readLine())!=null && result.length()<200000)result.append(line);
      }
      if(status<200||status>=300)throw new Exception("HTTP "+status+": "+result.substring(0,Math.min(500,result.length())));
      return result.toString();
    }finally{c.disconnect();}
  }
  @Override protected void onDestroy(){
    ai.setText("");key.setText("");secret.setText("");history=new JSONArray();worker.shutdownNow();super.onDestroy();
  }
}
