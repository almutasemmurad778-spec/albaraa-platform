# Windows desktop UI for an independently billed AI+Binance application.
# Keys are never written to a file, environment, logs, or GitHub.
import threading
import tkinter as tk
from tkinter import ttk, messagebox, scrolledtext
import webbrowser
from bridge import Binance, Agent

DARK="#101923"
PANEL="#192838"
WHITE="#EFF6FD"
MUTED="#AEBEC9"
YELLOW="#F0B90B"
GREEN="#36BC92"

class Desktop:
    def __init__(self,root):
        self.root=root
        self.root.title("Binance AI Desktop — OpenAI + Binance")
        self.root.geometry("1000x785")
        self.root.minsize(750,620)
        self.root.configure(bg=DARK)
        self.agent=Agent()
        self.busy=False
        self.api=tk.StringVar()
        self.binance_key=tk.StringVar()
        self.binance_secret=tk.StringVar()
        self.live=tk.BooleanVar(value=False)
        self.limit=tk.StringVar(value="25")
        frame=tk.Frame(root,bg=DARK,padx=22,pady=14)
        frame.pack(fill="both",expand=True)
        tk.Label(frame,text="BINANCE  ×  OpenAI",font=("Segoe UI",21,"bold"),fg=YELLOW,bg=DARK).pack(anchor="e")
        tk.Label(frame,text="مساعد مستقل للكمبيوتر  |  يعمل عبر OpenAI API برسوم منفصلة عن ChatGPT Plus",
                 font=("Segoe UI",11),fg=MUTED,bg=DARK).pack(anchor="e",pady=(2,11))
        creds=tk.LabelFrame(frame,text="  بيانات الاتصال — تُحفظ في ذاكرة البرنامج فقط  ",bg=PANEL,fg=WHITE,
                             font=("Segoe UI",11,"bold"),padx=12,pady=9,relief="flat")
        creds.pack(fill="x",pady=4)
        grid=tk.Frame(creds,bg=PANEL)
        grid.pack(fill="x")
        self.entry(grid,"OpenAI API key",self.api,0)
        self.entry(grid,"Binance API key",self.binance_key,1)
        self.entry(grid,"Binance HMAC secret",self.binance_secret,2)
        row=tk.Frame(creds,bg=PANEL)
        row.pack(fill="x",pady=(5,1))
        self.live_box=tk.Checkbutton(row,text="السماح بالتداول والتحويل الحقيقي (تأكيد لكل عملية)",
                              variable=self.live,bg=PANEL,fg=YELLOW,selectcolor=DARK,
                              activebackground=PANEL,activeforeground=YELLOW,
                              font=("Segoe UI",10,"bold"))
        self.live_box.pack(side="right")
        tk.Label(row,text="الحد الأعلى للعملية (USDT):",fg=WHITE,bg=PANEL).pack(side="left",padx=8)
        tk.Entry(row,textvariable=self.limit,width=10,fg=WHITE,bg=DARK,insertbackground=YELLOW).pack(side="left")
        links=tk.Frame(creds,bg=PANEL)
        links.pack(fill="x",pady=(7,0))
        self.button(links,"اختبار حساب Binance",self.test_binance).pack(side="right",padx=3)
        self.button(links,"OpenAI API",lambda:webbrowser.open("https://platform.openai.com/api-keys")).pack(side="left",padx=3)
        self.button(links,"إرشادات Binance API",lambda:webbrowser.open(
            "https://www.binance.com/en/support/faq/detail/360002502072")).pack(side="left",padx=3)
        tk.Label(frame,text="المحادثة والأوامر",fg=YELLOW,bg=DARK,
                 font=("Segoe UI",14,"bold")).pack(anchor="e",pady=(12,3))
        self.chat=scrolledtext.ScrolledText(frame,wrap="word",font=("Segoe UI",11),
                     bg=PANEL,fg=WHITE,insertbackground=YELLOW,relief="flat",padx=13,pady=13)
        self.chat.pack(fill="both",expand=True)
        self.chat.insert("end","المساعد: أهلاً بك. يمكنك طلب تحليل BTCUSDT أو معرفة أرصدتك.\n"
                   "التداول الحقيقي متوقف افتراضيًا، وتحتاج كل عملية مالية إلى موافقتك.\n\n")
        self.chat.configure(state="disabled")
        tk.Label(frame,text="اكتب طلبك هنا باللغة العربية:",fg=MUTED,bg=DARK,
                 font=("Segoe UI",10)).pack(anchor="e",pady=(12,3))
        self.input=tk.Text(frame,height=3,font=("Segoe UI",12),bg=PANEL,fg=WHITE,
                   insertbackground=YELLOW,wrap="word",relief="flat",padx=12,pady=9)
        self.input.pack(fill="x")
        btns=tk.Frame(frame,bg=DARK)
        btns.pack(fill="x",pady=9)
        self.send_btn=self.button(btns,"إرسال إلى المساعد",self.send)
        self.send_btn.pack(side="right")
        self.button(btns,"جلسة جديدة",self.new_session).pack(side="right",padx=8)
        self.button(btns,"مسح المفاتيح",self.clear_keys).pack(side="left")
        self.status=tk.StringVar(value="جاهز. لا تشارك مفاتيح API أو عبارة استرداد المحفظة في المحادثة.")
        tk.Label(frame,textvariable=self.status,bg=DARK,fg=GREEN,
                 font=("Segoe UI",9),anchor="e").pack(fill="x",pady=(5,0))
    def entry(self,parent,label,var,row):
        tk.Label(parent,text=label,bg=PANEL,fg=MUTED,
                 font=("Segoe UI",10)).grid(row=row,column=1,sticky="e",padx=12,pady=4)
        e=tk.Entry(parent,textvariable=var,show="●",width=76,bg=DARK,fg=WHITE,
                   insertbackground=YELLOW,relief="flat")
        e.grid(row=row,column=0,sticky="ew",padx=(0,12),ipady=6)
        parent.grid_columnconfigure(0,weight=1)
    def button(self,parent,label,command):
        return tk.Button(parent,text=label,command=command,bg=YELLOW,fg="#12191E",
                         font=("Segoe UI",10,"bold"),activebackground="#DDA300",
                         borderwidth=0,padx=13,pady=7,cursor="hand2")
    def append(self,who,text):
        def act():
            if not self.root.winfo_exists():return
            self.chat.configure(state="normal")
            self.chat.insert("end",who+": "+str(text)+"\n\n")
            self.chat.see("end")
            self.chat.configure(state="disabled")
        self.root.after(0,act)
    def set_status(self,value):
        self.root.after(0,lambda:self.status.set(value) if self.root.winfo_exists() else None)
    def check_busy(self):
        if self.busy:
            messagebox.showinfo("عملية جارية","أكمل الطلب الحالي أولًا",parent=self.root)
            return True
        return False
    def start(self,action):
        if self.check_busy():return
        self.busy=True
        self.send_btn.configure(state="disabled")
        def wrapper():
            try:action()
            except Exception as exc:
                self.append("خطأ",str(exc)[:850])
                self.set_status("فشل الطلب؛ راجع البيانات والصلاحيات")
            finally:
                self.busy=False
                self.root.after(0,lambda:self.send_btn.configure(state="normal") if self.root.winfo_exists() else None)
        threading.Thread(target=wrapper,daemon=True).start()
    def confirm(self,title,details):
        answer=[]
        complete=threading.Event()
        def ui():
            try:answer.append(messagebox.askyesno(title,details+"\n\nهل توافق على التنفيذ الحقيقي الآن؟",
                                                   icon="warning",parent=self.root))
            finally:complete.set()
        self.root.after(0,ui)
        return complete.wait(timeout=150) and bool(answer and answer[0])
    def test_binance(self):
        key=self.binance_key.get().strip()
        secret=self.binance_secret.get().strip()
        if not key or not secret:
            messagebox.showwarning("مفاتيح Binance","أدخل مفاتيح Binance API أولًا",parent=self.root);return
        def task():
            self.set_status("جارٍ اختبار قراءة بيانات الحساب...")
            info=Binance(key,secret).balances()
            self.append("اختبار Binance","الاتصال نجح.\n"+str(info)[:2300])
            self.set_status("تم الاتصال بحساب Binance")
        self.start(task)
    def send(self):
        prompt=self.input.get("1.0","end").strip()
        if not prompt:return
        api=self.api.get().strip()
        if not api:
            messagebox.showwarning("OpenAI API","ضع مفتاح OpenAI API أولًا (خدمة برسوم منفصلة)",parent=self.root);return
        if self.check_busy():return
        key=self.binance_key.get().strip()
        secret=self.binance_secret.get().strip()
        live=self.live.get()
        cap=self.limit.get().strip()
        self.input.delete("1.0","end")
        self.append("أنت",prompt)
        def task():
            answer=self.agent.run(prompt,api,Binance(key,secret),live,cap,self.confirm,self.set_status)
            self.append("المساعد",answer)
            self.set_status("اكتملت الاستجابة")
        self.start(task)
    def new_session(self):
        if self.check_busy():return
        self.agent.new_session()
        self.append("النظام","بدأت جلسة جديدة؛ لم تُحذف المفاتيح.")
    def clear_keys(self):
        if self.check_busy():return
        self.api.set("");self.binance_key.set("");self.binance_secret.set("")
        self.append("النظام","تم مسح المفاتيح من حقول البرنامج.")
    def on_close(self):
        if self.busy and not messagebox.askyesno("إنهاء البرنامج",
             "يوجد طلب جارٍ، وقد تكون عملية مالية قيد الإرسال. هل تريد الإغلاق؟",parent=self.root):
            return
        self.api.set("");self.binance_key.set("");self.binance_secret.set("")
        self.root.destroy()

if __name__=="__main__":
    root=tk.Tk()
    app=Desktop(root)
    root.protocol("WM_DELETE_WINDOW",app.on_close)
    root.mainloop()
