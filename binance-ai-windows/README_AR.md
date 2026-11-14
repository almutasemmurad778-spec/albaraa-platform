# Binance AI Desktop — Windows 10/11

نسخة أولية مستقلة من تطبيق المحادثة. **هذا ليس تطبيق ChatGPT الرسمي ولا يربط المحادثة الحالية بتطبيق Binance.**
يبقى اشتراك ChatGPT Plus كما هو، ويحتاج التطبيق إلى OpenAI API Key مع رصيد وفوترة منفصلين.

## وظائف الإصدار الأولي
- محادثة عربية عبر OpenAI Responses API بنموذج gpt-5.4-mini.
- أسعار Binance الفورية، إحصائيات 24 ساعة وشموع آخر 40 فترة.
- قراءة أرصدة Spot بعد إدخال مفتاح Binance HMAC وصلاحيات القراءة.
- معاينة أمر Spot والتحقق منه عبر POST /api/v3/order/test.
- إرسال أوامر Spot حقيقية لأزواج USDT فقط عندما تضع علامة السماح وتوافق على كل عملية.
- عرض سعر Binance Convert والقبول بعد الموافقة الصريحة.
- نقل داخلي بين Spot وFunding لنفس الحساب (إذا أتاح Binance ذلك للمفتاح).
- حد أعلى تحدده أنت بقيمة USDT لكل عملية.
- المفاتيح في الذاكرة أثناء التشغيل فقط؛ لا يتم حفظها داخل ملف أو GitHub.

## التثبيت
1. نزّل Binance_AI_Desktop.exe من GitHub Actions Artifacts ثم افتحه على Windows 10/11.
2. افتح https://platform.openai.com/api-keys وأنشئ مفتاح OpenAI API. اضبط سقف الإنفاق؛ الاستخدام ليس ضمن Plus.
3. أنشئ مفتاح Binance API حسب https://www.binance.com/en/support/faq/detail/360002502072.
4. ابدأ بصلاحية القراءة فقط واختبر الاتصال. لا تضع عبارة الاسترداد أو كلمة مرور Binance هنا.
5. بعد التأكد، تستطيع تفعيل صلاحية Spot Trade في Binance؛ وتفعيل خيار التداول داخل البرنامج؛ كل صفقة أو تحويل يحتاج تأكيدًا محليًا.
6. لا تمنح withdrawal أو صلاحيات السحب الخارجي لهذا التطبيق. وإذا كان Binance يتطلب IP whitelist، اضبطها بما يناسب شبكة جهازك.

## محاذير
- التطبيق برنامج **أولي للاختبار** وليس نظام تداول معتمدًا؛ لم تُختبر معاملات مالية حقيقية عليه.
- لا يشمل هذا الإصدار Futures أو P2P أو Simple Earn أو السحب الخارجي أو الربط داخل ChatGPT نفسه.
- لا يضمن أي أرباح. قد تتغير الأسعار أو تنتهي عروض Convert.
- في حالة انقطاع الاتصال بعد إرسال أمر مالي، **تحقق من سجل عمليات Binance قبل إعادة المحاولة** لتجنب الازدواج.
- لا يُرسل مفاتيح Binance إلى OpenAI؛ يتلقى النموذج فقط نتائج الأدوات/البيانات المطلوبة.

## من الشفرة المصدرية
شغل python app.py بعد تثبيت Python 3.11 أو أحدث. يستخدم التطبيق مكتبات Python القياسية.
بناء EXE يتم على Windows بواسطة PyInstaller في GitHub Actions.

## الروابط المرجعية
- https://developers.openai.com/api/docs/guides/function-calling
- https://developers.binance.com/
- https://help.openai.com/en/articles/6950777-what-is-chatgpt-plus
