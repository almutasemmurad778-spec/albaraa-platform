package ye.binance.aibridge;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.os.Build;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.Locale;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.crypto.Mac;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import java.security.KeyStore;
import javax.crypto.spec.SecretKeySpec;

/**
 * Binance × AI Android prototype.
 * - OpenAI API is used for natural-language planning and tool calls.
 * - ALL Binance API signatures are performed on device; no Binance secret is put into the AI messages.
 * - Every non-read action is confirmed on-device; the Binance API key determines actual permissions.
 * - No API keys saved to disk. Exchange REST APIs are NOT Binance Agentic Wallet (MPC/Web3).
 *
 * Not audited for production use. Carefully verify ALL actions.
 */
public final class MainActivity extends Activity {
    private static final String PREFS = "encrypted_link";
    private static final String ALIAS = "binance_bridge_keystore_v1";
    private static final String[] FIELDS = {"openai", "binance_key", "binance_secret"};
    private static final int BG = Color.rgb(11, 16, 22), PANEL = Color.rgb(26, 32, 40);
    private static final int YELLOW = Color.rgb(243, 186, 47), WHITE = Color.rgb(240, 242, 245);
    private static final int SECONDARY = Color.rgb(164, 173, 183);
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler ui = new Handler(Looper.getMainLooper());
    private EditText openAiKey, binanceKey, binanceSecret, messageBox;
    private TextView statusView, resultView;
    private Button sendButton;
    private JSONArray conversation = new JSONArray();
    private volatile boolean running = false;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);
        getWindow().setStatusBarColor(BG); getWindow().setNavigationBarColor(BG);
        draw();
        restoreLink();
    }
    private int dp(int x) {return (int)(x*getResources().getDisplayMetrics().density+0.5f);}
    private GradientDrawable background(int color) {
        GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(14));return d;
    }
    private TextView txt(String s,int size,int color,boolean bold) {
        TextView t=new TextView(this); t.setText(s);t.setTextSize(size);t.setTextColor(color);
        t.setGravity(Gravity.RIGHT);t.setTextDirection(View.TEXT_DIRECTION_RTL);
        if(bold)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
        t.setPadding(dp(5),dp(5),dp(5),dp(6));return t;
    }
    private void append(LinearLayout l,View v) {
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.bottomMargin=dp(12);l.addView(v,p);
    }
    private LinearLayout section(LinearLayout root,String name) {
        LinearLayout s=new LinearLayout(this);s.setOrientation(LinearLayout.VERTICAL);
        s.setPadding(dp(15),dp(16),dp(15),dp(8));s.setBackground(background(PANEL));
        append(root,s);append(s,txt(name,18,WHITE,true));return s;
    }
    private EditText field(String hint,boolean secret,int lines) {
        EditText t=new EditText(this);t.setTextSize(15);t.setHint(hint);
        t.setTextColor(WHITE);t.setHintTextColor(SECONDARY);t.setTextDirection(View.TEXT_DIRECTION_LTR);
        t.setGravity(Gravity.TOP|Gravity.LEFT);t.setPadding(dp(12),dp(12),dp(12),dp(12));
        if(secret)t.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_PASSWORD);
        else t.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        t.setSingleLine(lines==1);t.setMinLines(lines);t.setBackground(background(BG));return t;
    }
    private Button button(String s,boolean positive,View.OnClickListener click) {
        Button b=new Button(this);b.setText(s);b.setAllCaps(false);b.setTextSize(14);
        b.setBackground(background(positive?YELLOW:BG));b.setTextColor(positive?BG:YELLOW);
        b.setOnClickListener(click);return b;
    }
    private void draw() {
        ScrollView scroll=new ScrollView(this);scroll.setBackgroundColor(BG);scroll.setFillViewport(true);
        if(Build.VERSION.SDK_INT>=35){
            scroll.setOnApplyWindowInsetsListener((v,insets)->{
                android.graphics.Insets bars=insets.getInsets(android.view.WindowInsets.Type.systemBars());
                scroll.setPadding(0,bars.top,0,bars.bottom);
                return insets;
            });
        }
        LinearLayout root=new LinearLayout(this);root.setPadding(dp(16),dp(23),dp(16),dp(23));
        root.setOrientation(LinearLayout.VERTICAL);scroll.addView(root);setContentView(scroll);
        append(root,txt("BINANCE  ×  GPT",26,YELLOW,true));
        append(root,txt("تطبيق مساعد Binance بصلاحيات الحساب التي تسمح بها Binance",17,WHITE,true));
        append(root,txt("أوامر ذكاء اصطناعي حقيقية: عرض الأرصدة، أوامر Spot، تحويلات الحساب، والعمليات المدعومة في Binance API. كل عملية تغيير تستلزم تأكيدًا منك.",14,SECONDARY,false));

        LinearLayout c=section(root,"① مفاتيح الاتصال (لا تُحفظ بعد إغلاق التطبيق)");
        append(c,txt("مفتاح OpenAI API منفصل عن اشتراك ChatGPT. مفتاح Binance يكون HMAC System-generated؛ فعّل صلاحياته في Binance بنفسك.",13,SECONDARY,false));
        openAiKey=field("OpenAI API key  (sk-...) ",true,1);
        binanceKey=field("Binance API Key",true,1);
        binanceSecret=field("Binance API Secret",true,1);
        append(c,openAiKey);append(c,binanceKey);append(c,binanceSecret);
        append(c,txt("يمكن حفظ الربط مشفّرًا بمفتاح Android Keystore داخل الهاتف. لا تُحفظ كلمة مرور Binance أو رمز التحقق الثنائي أبدًا.",12,SECONDARY,false));
        append(c,button("حفظ الربط مشفّرًا على هذا الهاتف",true,v->confirmSaveLink()));
        append(c,button("مسح مفاتيح الربط المحفوظة",false,v->confirmEraseLink()));
        append(c,button("اختبار الاتصال بحساب Binance",false,v->testLink()));
        append(c,button("إدارة مفاتيح Binance الرسمية",false,v->browse("https://www.binance.com/en/my/settings/api-management")));

        LinearLayout r=section(root,"② تحدث مع المساعد");
        append(r,txt("أمثلة: اعرض أرصدة Spot / ما سعر BTCUSDT؟ / اعرض الطلبات المفتوحة / اشترِ كمية محددة بعد أن أراجع التفاصيل.",13,SECONDARY,false));
        messageBox=field("اكتب الأمر هنا...",false,3);messageBox.setTextDirection(View.TEXT_DIRECTION_RTL);
        append(r,messageBox);
        sendButton=button("تنفيذ الأمر باستخدام GPT",true,v->send());append(r,sendButton);
        append(r,button("جلسة جديدة",false,v->{if(!running){conversation=new JSONArray();resultView.setText("لم تبدأ جلسة بعد.");status("تم بدء جلسة جديدة.");}}));

        LinearLayout result=section(root,"③ نتيجة الأمر وسجل العملية");
        statusView=txt("جاهز — أدخل مفاتيحك ثم الأمر",13,YELLOW,false);append(result,statusView);
        resultView=txt("لن تُرسل مفاتيح Binance إلى OpenAI. تُرسل نتائج الاستعلام فقط إلى نموذج الذكاء الاصطناعي كي يجيبك.",14,WHITE,false);
        resultView.setTextIsSelectable(true);append(result,resultView);

        LinearLayout official=section(root,"④ ربط Agentic Wallet الرسمي (محفظة Web3 منفصلة)");
        append(official,txt("للربط المباشر بواسطة MCP مع برنامج يدعمه، استخدم خدمة Binance الرسمية. هذا يختلف عن واجهات الحساب الفوري التي يستخدمها التطبيق.",13,SECONDARY,false));
        append(official,button("فتح Binance MCP الرسمي",false,v->browse("https://agent.binance.com/mcp/agentic")));
        append(official,button("تعليمات Agentic Wallet",false,v->browse("https://developers.binance.com/en/docs/products/agentic-wallet/welcome")));
        append(root,txt("الإصدار 1.1 | مصمم لأندرويد 15 | الربط عبر Binance Exchange API مستقل عن محادثة ChatGPT هذه. غير مختبر بحساب حقيقي. كل عملية مالية تتطلب تأكيدًا. لا تشارك Seed Phrase أو كلمة المرور.",12,SECONDARY,false));
    }
    private void confirmSaveLink(){
        if(openAiKey.getText().length()==0||binanceKey.getText().length()==0||binanceSecret.getText().length()==0){
            status("أدخل مفاتيح الاتصال أولًا.");return;
        }
        new AlertDialog.Builder(this).setTitle("حفظ ربط الحساب على الهاتف")
            .setMessage("سيتم تشفير مفاتيح Binance وOpenAI باستخدام Android Keystore وحفظها في مساحة التطبيق الخاصة. من يصل إلى هاتفك المفتوح قد يستطيع تشغيل التطبيق. لا تستخدم هاتفًا مشتركًا.")
            .setNegativeButton("إلغاء",(d,w)->{})
            .setPositiveButton("أوافق على الحفظ",(d,w)->{
                try{
                    String[] values={openAiKey.getText().toString().trim(),binanceKey.getText().toString().trim(),binanceSecret.getText().toString().trim()};
                    SharedPreferences.Editor e=getSharedPreferences(PREFS,MODE_PRIVATE).edit();
                    for(int i=0;i<FIELDS.length;i++)e.putString(FIELDS[i],encrypt(values[i]));
                    if(!e.commit())throw new Exception("تعذر التخزين المشفر");
                    status("تم حفظ الربط مشفرًا على هاتفك.");
                }catch(Exception ex){status("تعذر حفظ الربط: "+safeError(ex.getMessage()));}
            }).show();
    }
    private void confirmEraseLink(){
        new AlertDialog.Builder(this).setTitle("مسح الربط")
            .setMessage("هل تريد حذف مفاتيح الاتصال المخزنة وإفراغ الحقول؟ لن يُلغى مفتاح Binance من حسابك؛ يمكنك إلغاؤه من موقع Binance.")
            .setNegativeButton("إلغاء",(d,w)->{})
            .setPositiveButton("احذف الربط",(d,w)->{
                getSharedPreferences(PREFS,MODE_PRIVATE).edit().clear().commit();
                openAiKey.setText("");binanceKey.setText("");binanceSecret.setText("");
                conversation=new JSONArray();resultView.setText("");status("تم مسح بيانات الربط المحلية.");
            }).show();
    }
    private SecretKey key() throws Exception {
        KeyStore ks=KeyStore.getInstance("AndroidKeyStore");ks.load(null);
        if(ks.containsAlias(ALIAS))return ((KeyStore.SecretKeyEntry)ks.getEntry(ALIAS,null)).getSecretKey();
        KeyGenerator kg=KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES,"AndroidKeyStore");
        kg.init(new KeyGenParameterSpec.Builder(ALIAS,KeyProperties.PURPOSE_ENCRYPT|KeyProperties.PURPOSE_DECRYPT)
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256).build());
        return kg.generateKey();
    }
    private String encrypt(String plain) throws Exception {
        Cipher c=Cipher.getInstance("AES/GCM/NoPadding");c.init(Cipher.ENCRYPT_MODE,key());
        byte[] cipher=c.doFinal(plain.getBytes(StandardCharsets.UTF_8));
        return Base64.encodeToString(c.getIV(),Base64.NO_WRAP)+":"+Base64.encodeToString(cipher,Base64.NO_WRAP);
    }
    private String decrypt(String encrypted) throws Exception {
        String[] v=encrypted.split(":",2);if(v.length!=2)throw new Exception("قيمة تشفير تالفة");
        Cipher c=Cipher.getInstance("AES/GCM/NoPadding");
        c.init(Cipher.DECRYPT_MODE,key(),new GCMParameterSpec(128,Base64.decode(v[0],Base64.NO_WRAP)));
        return new String(c.doFinal(Base64.decode(v[1],Base64.NO_WRAP)),StandardCharsets.UTF_8);
    }
    private void restoreLink(){
        try{
            SharedPreferences p=getSharedPreferences(PREFS,MODE_PRIVATE);
            if(!p.contains(FIELDS[0]))return;
            EditText[] boxes={openAiKey,binanceKey,binanceSecret};
            String[] values=new String[FIELDS.length];
            for(int i=0;i<FIELDS.length;i++)values[i]=decrypt(p.getString(FIELDS[i],""));
            for(int i=0;i<FIELDS.length;i++)boxes[i].setText(values[i]);
            status("تم استعادة ربط الحساب المشفر. اكتب أمرًا للمساعد.");
        }catch(Exception e){
            getSharedPreferences(PREFS,MODE_PRIVATE).edit().clear().apply();
            status("تعذر استعادة الربط المشفر. أدخل المفاتيح من جديد.");
        }
    }
    private void testLink(){
        if(running)return;
        String bk=binanceKey.getText().toString().trim();
        String bs=binanceSecret.getText().toString().trim();
        if(bk.isEmpty()||bs.isEmpty()){status("أدخل مفاتيح Binance أولًا.");return;}
        running=true;sendButton.setEnabled(false);status("اختبار الاتصال الآمن مع Binance...");
        executor.execute(()->{
            try{
                JSONObject acc=new JSONObject(binanceCall("GET","/api/v3/account",new JSONObject(),true,bk,bs));
                status("نجح الاتصال وقراءة الحساب.");
                show("تم الاتصال بنجاح بحساب Binance.
نوع الحساب: "+acc.optString("accountType","غير محدد")+
                    "
إمكانية التداول: "+acc.optBoolean("canTrade",false)+
                    "
إمكانية السحب: "+acc.optBoolean("canWithdraw",false)+
                    "
لا تعني هذه النتيجة أن كل أمر سيتاح؛ تخضع العمليات لصلاحيات Binance وشروطها.");
            }catch(Exception e){status("فشل اختبار الربط.");show("خطأ Binance: "+safeError(e.getMessage()));}
            finally{running=false;ui.post(()->sendButton.setEnabled(true));}
        });
    }
    private void browse(String url){try{startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse(url)));}catch(Exception ex){status("تعذّر فتح الرابط");}}
    private void status(String s){ui.post(()->statusView.setText(s));}
    private void show(String s){ui.post(()->resultView.setText(s));}
    private void send(){
        if(running)return;
        String prompt=messageBox.getText().toString().trim();
        String oa=openAiKey.getText().toString().trim();
        String bk=binanceKey.getText().toString().trim();
        String bs=binanceSecret.getText().toString().trim();
        if(prompt.isEmpty()||oa.isEmpty()||bk.isEmpty()||bs.isEmpty()){
            status("أكمل مفاتيح OpenAI وBinance واكتب الأمر.");return;
        }
        running=true;sendButton.setEnabled(false);status("المساعد يعالج طلبك...");show("جاري التحليل والاتصال بالخدمات الرسمية...");
        // Secrets only live in this call's local RAM variables; no disk persistence.
        executor.execute(()->{
            try{
                JSONArray msgs=copyMessages();
                msgs.put(new JSONObject().put("role","user").put("content",prompt));
                String answer=processAgent(msgs,oa,bk,bs);
                conversation=msgs;
                show(answer);status("اكتمل الطلب. راجع نتيجة العمليات المسجلة.");
                ui.post(()->messageBox.setText(""));
            }catch(Exception e){show("حدث خطأ: "+safeError(e.getMessage()));status("تعذّر إكمال العملية");}
            finally{running=false;ui.post(()->sendButton.setEnabled(true));}
        });
    }
    private JSONArray copyMessages() throws Exception {
        JSONArray n=new JSONArray();
        for(int i=Math.max(0,conversation.length()-16);i<conversation.length();i++)n.put(conversation.getJSONObject(i));
        return n;
    }
    private String processAgent(JSONArray messages,String oa,String bk,String bs) throws Exception {
        // Each command is sent explicitly as a chat completion. No autonomous background activity.
        for(int round=0;round<7;round++){
            JSONObject req=new JSONObject();req.put("model","gpt-4.1-mini");
            JSONArray all=new JSONArray();
            all.put(new JSONObject().put("role","system").put("content",systemPrompt()));
            for(int i=0;i<messages.length();i++)all.put(messages.getJSONObject(i));
            req.put("messages",all);req.put("tools",toolSchema());req.put("tool_choice","auto");
            JSONObject response=new JSONObject(http("https://api.openai.com/v1/chat/completions","POST",req.toString(),
                "Bearer "+oa,null));
            JSONObject choice=response.getJSONArray("choices").getJSONObject(0).getJSONObject("message");
            JSONArray calls=choice.optJSONArray("tool_calls");
            messages.put(choice);
            if(calls==null||calls.length()==0){String a=choice.optString("content","تم التنفيذ.");return a;}
            for(int i=0;i<calls.length();i++){
                JSONObject call=calls.getJSONObject(i);
                JSONObject args=new JSONObject(call.getJSONObject("function").getString("arguments"));
                String result;
                try{
                    String method=args.getString("method").toUpperCase(Locale.ROOT);
                    String path=args.getString("path");
                    JSONObject params=args.optJSONObject("params");if(params==null)params=new JSONObject();
                    boolean signed=args.optBoolean("signed",true);
                    validate(method,path,params);
                    if(!method.equals("GET")){
                        // Nothing that changes money/positions can proceed without approval.
                        if(!approve(method,path,params))result="NOT_EXECUTED: المستخدم رفض هذه العملية.";
                        else result=binanceCall(method,path,params,signed,bk,bs);
                    }else result=binanceCall(method,path,params,signed,bk,bs);
                }catch(Exception ex){result="ERROR: "+safeError(ex.getMessage());}
                if(result.length()>16000)result=result.substring(0,16000)+"... [truncated]";
                messages.put(new JSONObject().put("role","tool").put("tool_call_id",call.getString("id"))
                    .put("content",result));
            }
        }
        return "توقفت الجلسة بعد بلوغ حد خطوات التنفيذ؛ يمكنك إرسال أمر متابعة.";
    }
    private String systemPrompt(){return "You are a Binance API assistant. Respond to the user in Arabic. " +
       "You can query and act on Binance Exchange API via binance_call. Actual permission comes from the user's key and Binance. " +
       "Use documented endpoints only, with accurate parameters. Use signed=true for private endpoints; public market queries signed=false. " +
       "Never request, display, or transmit API keys, 2FA or seed phrases in messages or parameters. " +
       "Never make money-moving requests without explicit user intent in this conversation. " +
       "Do not guess order size, withdrawal destination, network or recipient. Ask if missing. " +
       "The phone itself requires approval for each write. Separate Binance Web3 MPC Agentic Wallet cannot be accessed using this API. " +
       "Explain order amounts, fees, leverage and risk when relevant. Do not claim success without tool confirmation. " +
       "You must not call any non-Binance host.";}
    private JSONArray toolSchema() throws Exception {
        JSONObject props=new JSONObject();
        props.put("method",new JSONObject().put("type","string").put("enum",new JSONArray().put("GET").put("POST").put("PUT").put("DELETE")));
        props.put("path",new JSONObject().put("type","string").put("description","Documented Binance REST path e.g. /api/v3/account, /api/v3/order, /sapi/v1/capital/withdraw/apply"));
        props.put("params",new JSONObject().put("type","object").put("description","API parameters as exact strings; do not include timestamp/signature/API keys")
            .put("additionalProperties",new JSONObject().put("type","string")));
        props.put("signed",new JSONObject().put("type","boolean").put("description","Whether endpoint requires HMAC signature and Binance key"));
        JSONObject fn=new JSONObject().put("name","binance_call").put("description",
                "Call an official Binance Exchange REST endpoint with the account's permissions. Writes require local user confirmation.")
            .put("parameters",new JSONObject().put("type","object").put("properties",props)
                .put("required",new JSONArray().put("method").put("path").put("params").put("signed")));
        return new JSONArray().put(new JSONObject().put("type","function").put("function",fn));
    }
    private void validate(String method,String path,JSONObject params) throws Exception {
        if(!method.matches("GET|POST|PUT|DELETE"))throw new Exception("طريقة HTTP غير مدعومة");
        if(path.length()>150||!path.matches("^/(api/v3|sapi/v1|sapi/v2|fapi/v1|fapi/v2|dapi/v1|dapi/v2)/[A-Za-z0-9_/-]+$"))
            throw new Exception("المسار ليس من واجهات Binance المعتمدة في التطبيق.");
        for(Iterator<String> it=params.keys();it.hasNext();){
            String k=it.next();
            if(!k.matches("[A-Za-z][A-Za-z0-9_]{0,60}")||k.equalsIgnoreCase("signature")||k.equalsIgnoreCase("timestamp")
              ||k.equalsIgnoreCase("apiKey")||k.equalsIgnoreCase("secretKey"))throw new Exception("معامل غير مسموح: "+k);
            if(params.isNull(k)||params.getString(k).length()>2048)throw new Exception("قيمة طويلة أو فارغة للمعامل: "+k);
        }
    }
    private boolean approve(String method,String path,JSONObject params) throws Exception {
        CountDownLatch latch=new CountDownLatch(1);AtomicBoolean yes=new AtomicBoolean(false);
        String details="الطريقة: "+method+"\nالمسار: "+path+"\n\nالمعاملات:\n"+params.toString(2)+
            "\n\nقد تكون هذه عملية مالية حقيقية وغير قابلة للاسترجاع. وافق فقط بعد قراءة التفاصيل.";
        ui.post(()->new AlertDialog.Builder(this).setTitle("تأكيد تنفيذ العملية على حساب Binance")
            .setMessage(details).setCancelable(false)
            .setNegativeButton("إلغاء",(d,w)->{yes.set(false);latch.countDown();})
            .setPositiveButton("أوافق على التنفيذ",(d,w)->{yes.set(true);latch.countDown();})
            .show());
        if(!latch.await(180,TimeUnit.SECONDS))return false;
        return yes.get();
    }
    private String binanceCall(String method,String path,JSONObject params,boolean signed,String bk,String bs) throws Exception {
        String host=path.startsWith("/fapi/")?"https://fapi.binance.com":
                    path.startsWith("/dapi/")?"https://dapi.binance.com":"https://api.binance.com";
        StringBuilder query=new StringBuilder();
        for(Iterator<String> it=params.keys();it.hasNext();){
            String k=it.next();if(query.length()>0)query.append('&');
            query.append(encode(k)).append('=').append(encode(params.getString(k)));
        }
        if(signed){
            // Binance signed REST uses server-adjusted milliseconds to tolerate handset clock offset.
            JSONObject t=new JSONObject(http(host+(path.startsWith("/fapi/")?"/fapi/v1/time":
                    path.startsWith("/dapi/")?"/dapi/v1/time":"/api/v3/time"),"GET",null,null,null));
            if(query.length()>0)query.append('&');
            query.append("recvWindow=5000&timestamp=").append(t.getLong("serverTime"));
            Mac mac=Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(bs.getBytes(StandardCharsets.UTF_8),"HmacSHA256"));
            byte[] sig=mac.doFinal(query.toString().getBytes(StandardCharsets.UTF_8));
            StringBuilder hex=new StringBuilder();for(byte b:sig)hex.append(String.format(Locale.US,"%02x",b&255));
            query.append("&signature=").append(hex);
        }
        String apiUrl=host+path+(query.length()>0?"?"+query:"");
        String key=signed?bk:null;
        return http(apiUrl,method,null,null,key);
    }
    private String encode(String s) throws Exception {
        return URLEncoder.encode(s,"UTF-8").replace("+","%20");
    }
    private static String safeError(String s){
        if(s==null)return "خطأ غير معروف";
        return s.length()>700?s.substring(0,700):s;
    }
    private static String http(String url,String method,String json,String bearer,String apiKey) throws Exception {
        HttpURLConnection c=(HttpURLConnection)new URL(url).openConnection();
        c.setRequestMethod(method);c.setConnectTimeout(12000);c.setReadTimeout(30000);
        c.setInstanceFollowRedirects(false);c.setRequestProperty("Accept","application/json");
        if(bearer!=null)c.setRequestProperty("Authorization",bearer);
        if(apiKey!=null)c.setRequestProperty("X-MBX-APIKEY",apiKey);
        if(json!=null){c.setDoOutput(true);c.setRequestProperty("Content-Type","application/json; charset=UTF-8");
            try(OutputStream out=c.getOutputStream()){out.write(json.getBytes(StandardCharsets.UTF_8));}}
        try{
            int status=c.getResponseCode();
            InputStream in=status>=200&&status<300?c.getInputStream():c.getErrorStream();
            if(in==null)throw new Exception("HTTP "+status);
            StringBuilder data=new StringBuilder();
            try(BufferedReader r=new BufferedReader(new InputStreamReader(in,StandardCharsets.UTF_8))){
                String line;while((line=r.readLine())!=null&&data.length()<350000)data.append(line);
            }
            if(status<200||status>=300)throw new Exception("HTTP "+status+": "+data.substring(0,Math.min(data.length(),600)));
            return data.toString();
        }finally{c.disconnect();}
    }
    @Override protected void onDestroy(){
        if(openAiKey!=null)openAiKey.setText("");
        if(binanceKey!=null)binanceKey.setText("");
        if(binanceSecret!=null)binanceSecret.setText("");
        conversation=new JSONArray();executor.shutdownNow();super.onDestroy();
    }
}
