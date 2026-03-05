import android.app.*;
import android.widget.*;
import android.view.*;
import android.graphics.*;
import android.graphics.drawable.*;
import android.util.*;
import android.content.*;
import android.os.*;
import android.text.*;
import java.net.URL;
import java.util.*;

String CFG="automm_cfg", 
K_DEL="delay_seconds", 
K_WHT="whitelist", 
K_ENA="is_enabled", 
K_CNT="msg_count";
Dialog G_DLG; 
boolean isRun = false;

int dp(Context c, float v){ return (int)TypedValue.applyDimension(1,v,c.getResources().getDisplayMetrics()); }
LinearLayout.LayoutParams lp(int w, int h) { return new LinearLayout.LayoutParams(w, h); }

Drawable shape(Context c, int col, float r, int s, boolean str) {
    GradientDrawable g = new GradientDrawable(); 
    g.setShape(s>0?1:0); g.setColor(str?0:col);
    if(s>0)g.setSize(dp(c,s),dp(c,s)); 
    if(r>0)g.setCornerRadius(dp(c,r));
    if(str)g.setStroke(dp(c,2),col); 
    return g;
}

StateListDrawable sel(Context c) {
    StateListDrawable s = new StateListDrawable();
    s.addState(new int[]{android.R.attr.state_checked}, shape(c, Color.parseColor("#66CCFF"),0,24,false));
    s.addState(new int[]{}, shape(c, Color.parseColor("#8E918F"),0,24,true)); 
    return s;
}

Drawable track(Context c) {
    GradientDrawable bg = (GradientDrawable)shape(c, Color.parseColor("#DEE3EB"),8,0,false); bg.setSize(-1,dp(c,24));
    GradientDrawable fg = (GradientDrawable)shape(c, Color.parseColor("#295D88"),8,0,false); fg.setSize(-1,dp(c,24));
    LayerDrawable l = new LayerDrawable(new Drawable[]{new LayerDrawable(new Drawable[]{bg}), new ClipDrawable(new LayerDrawable(new Drawable[]{fg}),3,1)});
    l.setId(0,android.R.id.background); l.setId(1,android.R.id.progress); 
    return l;
}

TextView lab(ViewGroup p, Context c, String t, float s, int col, boolean b){
    TextView v = new TextView(c); v.setText(t); v.setTextSize(s); v.setTextColor(col);
    if(b) v.setTypeface(Typeface.DEFAULT_BOLD); p.addView(v); 
    return v;
}

void stop() { isRun=false; toast("Stopped"); }
void start() {
    if(isRun) return; isRun=true; toast("Started");
    new Thread(new Runnable(){
        public void run(){
            while(isRun) {
                try {
                    int del = Math.max(1, getInt(CFG, K_DEL, 60));
                    for(int i=0;i<del;i++) { if(!isRun)break; Thread.sleep(1000); }
                    if(!isRun) break;
                    
                    String[] grps = getString(CFG, K_WHT, "").split(",");
                    for(String g : grps) {
                        if(!isRun || g.length()==0) continue;
                        List m = getGroupMemberList(g);
                        if(m==null) continue;
                        
                        List v = new ArrayList();
                        for(Object o : m) {
                            String u = String.valueOf(o.uin).trim();
                            if(!u.equals("0") && !u.equals(myUin) && u.length()>0 && !u.equals("10000")) v.add(u);
                        }
                        
                        if(v.size()>0) {
                            sendMsg(g, "[atUin=" + v.get(new Random().nextInt(v.size())) + "] 妹妹吗？", 2);
                            putLong(CFG, K_CNT, getLong(CFG, K_CNT, 0)+1);
                            Thread.sleep(2000);
                        }
                    }
                } catch(Exception e){}
            }
        }
    }).start();
}

void openSel(final Context c, final EditText edt) {
    final Dialog d = new Dialog(c, android.R.style.Theme_DeviceDefault_Light_Dialog_NoActionBar);
    LinearLayout root = new LinearLayout(c); root.setOrientation(1);
    root.setBackground(shape(c, Color.WHITE,16,0,false)); root.setPadding(dp(c,20),dp(c,20),dp(c,20),dp(c,20));
    lab(root,c,"Select Groups",20,Color.parseColor("#1F1F1F"),true).setPadding(0,0,0,dp(c,16));
    
    ScrollView sv = new ScrollView(c); final LinearLayout ctr = new LinearLayout(c); ctr.setOrientation(1); sv.addView(ctr);
    root.addView(sv, lp(-1, dp(c,400)));
    
    final Set cur = new HashSet(Arrays.asList(edt.getText().toString().split(",")));
    final Map cbs = new HashMap();
    
    new Thread(new Runnable(){
        public void run(){
            final List gs = getGroupList();
            new Handler(Looper.getMainLooper()).post(new Runnable(){
                public void run(){
                    if(gs!=null) for(Object g:gs) {
                        LinearLayout il = new LinearLayout(c); il.setGravity(16); il.setPadding(0,dp(c,8),0,dp(c,8));
                        il.setBackground(shape(c, Color.WHITE,12,0,false));
                        lab(il,c,"[Gp] "+g.groupName+"\n"+g.group,14,Color.parseColor("#1F1F1F"),false).setLayoutParams(new LinearLayout.LayoutParams(0,-2,1));
                        
                        final CheckBox cb = new CheckBox(c); cb.setButtonDrawable(sel(c)); cb.setChecked(cur.contains(g.group));
                        il.addView(cb); ctr.addView(il); cbs.put(cb, g.group);
                        
                        View.OnClickListener l = new View.OnClickListener(){
                            public void onClick(View v){
                                if(v instanceof LinearLayout) cb.setChecked(!cb.isChecked());
                                StringBuilder sb = new StringBuilder(); boolean f=true;
                                for(Object k : cbs.keySet()) if(((CheckBox)k).isChecked()){ if(!f)sb.append(","); sb.append(cbs.get(k)); f=false; }
                                String s = sb.toString(); edt.setText(s); putString(CFG, K_WHT, s);
                            }
                        };
                        cb.setOnClickListener(l); il.setOnClickListener(l);
                    }
                }
            });
        }
    }).start();
    
    Button cls = new Button(c); cls.setText("Close"); cls.setTextColor(-1);
    cls.setBackground(shape(c, Color.parseColor("#0B57D0"),20,0,false));
    cls.setOnClickListener(new View.OnClickListener(){ public void onClick(View v){d.dismiss();} });
    LinearLayout.LayoutParams bp = lp(-1,dp(c,48)); bp.topMargin=dp(c,16); root.addView(cls, bp);
    d.setContentView(root); d.show();
}

View ui(final Context c) {
    ScrollView sv = new ScrollView(c); sv.setBackgroundColor(Color.parseColor("#F2F6FC")); sv.setFillViewport(true);
    LinearLayout l = new LinearLayout(c); l.setOrientation(1); l.setPadding(dp(c,24),dp(c,60),dp(c,24),dp(c,24)); sv.addView(l);
    
    lab(l,c,"AutoMM",34,Color.parseColor("#1F1F1F"),true).setPadding(0,0,0,dp(c,30));
    
    lab(l,c,"You'll appear as",14,Color.parseColor("#444746"),true).setPadding(0,0,0,dp(c,16));
    LinearLayout uc = new LinearLayout(c); uc.setBackground(shape(c,-1,24,0,false));
    uc.setPadding(dp(c,16),dp(c,16),dp(c,16),dp(c,16)); uc.setGravity(16); l.addView(uc);
    final ImageView iv = new ImageView(c); uc.addView(iv, lp(dp(c,48),dp(c,48)));
    
    new Thread(new Runnable(){
        public void run(){ try {
            Bitmap b = BitmapFactory.decodeStream(new URL("https://q.qlogo.cn/headimg_dl?dst_uin="+(myUin!=null?myUin:"1000")+"&spec=640").openStream());
            if(b!=null) {
                final Bitmap rb = Bitmap.createBitmap(b.getWidth(),b.getHeight(),android.graphics.Bitmap.Config.ARGB_8888);
                Canvas cv=new Canvas(rb); Paint p=new Paint(); p.setAntiAlias(true);
                cv.drawCircle(b.getWidth()/2,b.getHeight()/2,b.getWidth()/2,p);
                p.setXfermode(new PorterDuffXfermode(android.graphics.PorterDuff.Mode.SRC_IN)); cv.drawBitmap(b,0,0,p);
                new Handler(Looper.getMainLooper()).post(new Runnable(){ public void run(){ iv.setImageBitmap(rb); } });
            }
        }catch(Exception e){}}
    }).start();
    
    LinearLayout ti = new LinearLayout(c); ti.setOrientation(1); ti.setPadding(dp(c,16),0,0,0);
    lab(ti,c,myUin!=null?myUin:"Unknown",16,Color.parseColor("#1F1F1F"),true);
    lab(ti,c,android.os.Build.MODEL,12,Color.parseColor("#444746"),false); uc.addView(ti);
    
    lab(l,c,"Number of inquiries",14,Color.parseColor("#444746"),true).setPadding(0,dp(c,30),0,dp(c,10));
    LinearLayout sc = new LinearLayout(c); sc.setBackground(shape(c,-1,24,0,false));
    sc.setPadding(dp(c,20),dp(c,16),dp(c,20),dp(c,16)); sc.setOrientation(1); l.addView(sc);
    lab(sc,c,""+getLong(CFG, K_CNT, 0),32,Color.parseColor("#0B57D0"),true);
    
    lab(l,c,"Settings",14,Color.parseColor("#444746"),true).setPadding(0,dp(c,30),0,dp(c,10));
    LinearLayout st = new LinearLayout(c); st.setOrientation(1); st.setBackground(shape(c,-1,24,0,false));
    st.setPadding(dp(c,20),dp(c,16),dp(c,20),dp(c,16)); l.addView(st);
    
    LinearLayout sr = new LinearLayout(c); sr.setGravity(16);
    lab(sr,c,"Enable AutoMM",16,Color.parseColor("#1F1F1F"),false);
    sr.addView(new View(c), new LinearLayout.LayoutParams(0,1,1));
    final CheckBox en = new CheckBox(c); en.setButtonDrawable(sel(c)); en.setChecked(getBoolean(CFG, K_ENA, false));
    sr.addView(en); st.addView(sr);
    
    View d1 = new View(c); d1.setBackgroundColor(Color.parseColor("#E0E2E0"));
    LinearLayout.LayoutParams dp = lp(-1,dp(c,1)); dp.setMargins(0,dp(c,16),0,dp(c,16)); st.addView(d1,dp);
    
    lab(st,c,"Delay(Seconds)",12,Color.parseColor("#444746"),false).setPadding(0,0,0,dp(c,8));
    LinearLayout tr = new LinearLayout(c); tr.setGravity(16);
    final SeekBar sk = new SeekBar(c); sk.setMax(299); sk.setProgressDrawable(track(c));
    
    GradientDrawable th = (GradientDrawable)shape(c, Color.parseColor("#295D88"), 4, 0, false);
    th.setSize(dp(c,4), dp(c,40));
    GradientDrawable gp = new GradientDrawable(); gp.setSize(dp(c,14), dp(c,48));
    LayerDrawable thl = new LayerDrawable(new Drawable[]{gp, th});
    thl.setLayerGravity(1,17); thl.setBounds(0,0,dp(c,24),dp(c,48));
    
    sk.setThumb(thl); sk.setSplitTrack(true); sk.setPadding(dp(c,12),0,dp(c,12),0);
    tr.addView(sk, new LinearLayout.LayoutParams(0,dp(c,48),1));
    
    tr.addView(new View(c), lp(dp(c,12),-1));
    final EditText et = new EditText(c); et.setBackground(shape(c, Color.parseColor("#295D88"),0,24,false));
    et.setTextColor(-1); et.setTextSize(14); et.setGravity(17); et.setInputType(2);
    
    android.text.InputFilter[] filts = new android.text.InputFilter[1];
    filts[0] = new android.text.InputFilter.LengthFilter(5);
    et.setFilters(filts);
    
    tr.addView(et, lp(dp(c,48),dp(c,48))); st.addView(tr);
    
    View d2 = new View(c); d2.setBackgroundColor(Color.parseColor("#E0E2E0"));
    st.addView(d2, dp);
    
    lab(st,c,"Target Groups",12,Color.parseColor("#444746"),false).setPadding(0,0,0,dp(c,8));
    final EditText wl = new EditText(c); wl.setText(getString(CFG, K_WHT, ""));
    wl.setBackground(shape(c, Color.parseColor("#F0F4F9"),12,0,false));
    wl.setPadding(dp(c,12),dp(c,12),dp(c,12),dp(c,12)); wl.setFocusable(false);
    wl.setOnClickListener(new View.OnClickListener(){ public void onClick(View v){openSel(c,wl);} }); st.addView(wl);
    
    int cur = Math.max(1, getInt(CFG, K_DEL, 60));
    sk.setProgress(Math.min(299, cur-1)); et.setText(""+cur);
    
    sk.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){
        public void onProgressChanged(SeekBar s,int p,boolean b){ if(b)et.setText(""+(p+1)); }
        public void onStartTrackingTouch(SeekBar s){} public void onStopTrackingTouch(SeekBar s){}
    });
    et.addTextChangedListener(new TextWatcher(){
        public void beforeTextChanged(CharSequence s,int x,int y,int z){}
        public void onTextChanged(CharSequence s,int x,int y,int z){}
        public void afterTextChanged(Editable s){ try{
            String str=s.toString(); if(str.length()==0)return;
            int v=Integer.parseInt(str); sk.setProgress(v>300?299:(v<1?0:v-1));
        }catch(Exception e){}}
    });
    
    Button svB = new Button(c); svB.setText("Save"); svB.setTextColor(-1); svB.setAllCaps(false);
    svB.setBackground(shape(c, Color.parseColor("#0B57D0"),28,0,false));
    svB.setOnClickListener(new View.OnClickListener(){
        public void onClick(View v){
            try{ int val=Integer.parseInt(et.getText().toString()); putInt(CFG,K_DEL,Math.max(1,val)); }catch(Exception e){}
            putString(CFG, K_WHT, wl.getText().toString());
            boolean on = en.isChecked(); putBoolean(CFG, K_ENA, on);
            if(on) start(); else stop();
            toast("Saved"); if(G_DLG!=null)G_DLG.dismiss();
        }
    });
    LinearLayout.LayoutParams sp = lp(dp(c,120),dp(c,56)); sp.gravity=17; sp.topMargin=dp(c,40); l.addView(svB,sp);
    return sv;
}

void show(int t, String u, String n){
    final Activity a=getNowActivity(); if(a==null)return;
    new Handler(Looper.getMainLooper()).post(new Runnable(){
        public void run(){
            try{ if(G_DLG!=null&&G_DLG.isShowing())G_DLG.dismiss();
            G_DLG=new Dialog(a, android.R.style.Theme_DeviceDefault_Light_NoActionBar_Fullscreen);
            G_DLG.setContentView(ui(a)); G_DLG.show(); }catch(Exception e){toast("Err:"+e);}
        }
    });
}

void onMsg(Object m){}
void unLoadPlugin(){ stop(); }
if(getBoolean(CFG, K_ENA, false)) start();
addItem("AutoMM Config", "show");
