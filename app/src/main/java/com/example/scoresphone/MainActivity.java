package com.example.scoresphone;

import android.app.*;
import android.os.Bundle;
import android.graphics.Typeface;
import android.graphics.Color;
import android.view.*;
import android.widget.*;
import android.content.*;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import java.util.*;
import java.text.SimpleDateFormat;

public class MainActivity extends Activity {
    DB db;
    LinearLayout page;
    long gameStart;
    int g25Id=-1, pos=1;
    int[] shots=new int[5];
    int g33Id=-1;
    ArrayList<String> names=new ArrayList<>();
    ArrayList<Integer> ids=new ArrayList<>();
    boolean finished=false;
    int bg=Color.rgb(245,247,250), ink=Color.rgb(25,35,50), blue=Color.rgb(35,100,210);

    @Override public void onCreate(Bundle b){super.onCreate(b); db=new DB(this); home();}
    TextView text(String s,int size,boolean bold){
        TextView t=new TextView(this); t.setText(s); t.setTextSize(size); t.setTextColor(ink);
        if(bold)t.setTypeface(null,Typeface.BOLD); t.setPadding(4,8,4,8); return t;
    }
    void base(String title){
        page=new LinearLayout(this); page.setOrientation(1); page.setPadding(22,24,22,22); page.setBackgroundColor(bg);
        ScrollView sc=new ScrollView(this); sc.addView(page); setContentView(sc);
        TextView h=text(title,26,true); h.setPadding(2,12,2,22); page.addView(h);
    }
    Button btn(String label,Runnable r){
        Button b=new Button(this); b.setText(label); b.setAllCaps(false); b.setTextSize(16);
        b.setOnClickListener(v->r.run()); LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);
        lp.setMargins(0,5,0,5); page.addView(b,lp); return b;
    }
    Button small(String label,Runnable r,LinearLayout row){
        Button b=new Button(this); b.setText(label); b.setAllCaps(false); b.setOnClickListener(v->r.run());
        row.addView(b,new LinearLayout.LayoutParams(0,-2,1)); return b;
    }
    void note(String s){page.addView(text(s,16,false));}
    String dur(long ms){long sec=Math.max(0,ms/1000),m=sec/60,s=sec%60,h=m/60;m%=60; return h>0?h+"ч "+m+"м "+s+"с":m>0?m+"м "+s+"с":s+"с";}
    String date(long ms){return new SimpleDateFormat("dd.MM.yyyy HH:mm",Locale.getDefault()).format(new Date(ms));}
    void home(){
        base("🏀 Счёт баскетбола"); note("Локальное приложение · данные хранятся на этом телефоне");
        btn("🏀 Игра 25",()->start25()); btn("🎯 Игра 33",()->new33());
        btn("📁 Сохранённые игры",()->saved()); btn("🏆 Таблица лидеров",()->leaders());
    }
    void start25(){
        Cursor c=db.getReadableDatabase().rawQuery("SELECT id FROM g25 WHERE status='active' LIMIT 1",null);
        if(c.moveToFirst()){int id=c.getInt(0);c.close();base("Есть незавершённая игра");note("Сначала заверши или сбрось текущую игру 25.");btn("Удалить старую и начать заново",()->{db.del25(id);start25Fresh();});btn("В меню",()->home());return;} c.close();start25Fresh();
    }
    void start25Fresh(){shots=new int[5];pos=1;gameStart=System.currentTimeMillis();g25Id=(int)db.getWritableDatabase().insert("g25",null,values25("active",0,gameStart,0,shots));show25();}
    ContentValues values25(String status,int total,long start,long duration,int[] a){
        ContentValues v=new ContentValues();v.put("status",status);v.put("total",total);v.put("started",start);v.put("ended",0);v.put("duration",duration);
        for(int i=0;i<5;i++)v.put("p"+(i+1),a[i]); return v;
    }
    void show25(){
        base("🏀 Игра 25"); note("Позиция "+pos+" из 5. Сколько попаданий из 5?");
        LinearLayout row=new LinearLayout(this);page.addView(row);
        for(int i=0;i<=5;i++){final int val=i; Button b=new Button(this);b.setText(""+i);b.setOnClickListener(v->{shots[pos-1]=val;if(pos<5){pos++;show25();}else finish25();});row.addView(b,new LinearLayout.LayoutParams(0,-2,1));}
        note("Уже записано: "+(pos-1)+" из 5 позиций");
        btn("Отменить игру",()->{db.del25(g25Id);home();});
    }
    void finish25(){
        long end=System.currentTimeMillis(),elapsed=end-gameStart;int total=0;for(int x:shots)total+=x;
        ContentValues v=values25("finished",total,gameStart,elapsed,shots);v.put("ended",end);db.getWritableDatabase().update("g25",v,"id=?",new String[]{""+g25Id});
        base("Игра 25 завершена");StringBuilder s=new StringBuilder();for(int i=0;i<5;i++)s.append("Позиция ").append(i+1).append(": ").append(shots[i]).append("/5\n");
        note(s+"Итого: "+total+"/25\nВремя: "+dur(elapsed));
        btn("💾 Сохранить",()->{db.status25(g25Id,"saved");home();});
        btn("🗑 Сбросить",()->{db.del25(g25Id);home();});
    }
    void new33(){
        Cursor c=db.getReadableDatabase().rawQuery("SELECT id FROM g33 WHERE status IN ('active','finished') LIMIT 1",null);
        if(c.moveToFirst()){int id=c.getInt(0);c.close();base("Есть незавершённая игра 33");btn("Удалить старую и начать новую",()->{db.del33(id);askNames();});btn("В меню",()->home());return;}c.close();askNames();
    }
    void askNames(){
        base("🎯 Новая игра 33");note("Введи имена игроков через запятую или каждое с новой строки (до 10).");
        EditText input=new EditText(this);input.setHint("Например: Вася, Петя, Коля");input.setMinLines(2);page.addView(input);
        btn("Начать игру",()->{
            String raw=input.getText().toString().replace("\n",",");LinkedHashSet<String> set=new LinkedHashSet<>();
            for(String n:raw.split(",")){n=n.trim();if(!n.isEmpty())set.add(n.length()>40?n.substring(0,40):n);}
            if(set.isEmpty()||set.size()>10){new AlertDialog.Builder(this).setMessage("Нужно указать от 1 до 10 игроков.").setPositiveButton("ОК",null).show();return;}
            names=new ArrayList<>(set);ids.clear();gameStart=System.currentTimeMillis();g33Id=db.create33(gameStart,names);finished=false;show33();
        });btn("В меню",()->home());
    }
    void show33(){
        base("🎯 Игра 33");note("Нажимай + или − для изменения очков. Побеждает первый, кто набрал 33.");
        for(int i=0;i<names.size();i++){
            final int ix=i; int score=db.score(ids.get(i));
            TextView label=text(names.get(i)+" — "+score+" очков",19,true);page.addView(label);
            LinearLayout row=new LinearLayout(this);page.addView(row);
            int step=score>=30?1:3;
            small("➖ "+step,()->change(ix,-step),row);small("➕ "+step,()->change(ix,step),row);
        }
        btn("Отменить игру",()->{db.del33(g33Id);home();});
    }
    void change(int i,int delta){
        if(finished)return;int id=ids.get(i),score=Math.max(0,db.score(id)+delta);
        if(score>=33){score=33;finished=true;long elapsed=System.currentTimeMillis()-gameStart;db.finish33(g33Id,names.get(i),elapsed,System.currentTimeMillis(),id,score);
            base("🏆 Победитель: "+names.get(i));note("Набрано 33 очка!\nВремя игры: "+dur(elapsed));btn("💾 Сохранить результат",()->{db.status33(g33Id,"saved");home();});btn("🗑 Сбросить",()->{db.del33(g33Id);home();});
        }else{db.setScore(id,score);show33();}
    }
    void saved(){
        base("📁 Сохранённые игры");SQLiteDatabase d=db.getReadableDatabase();
        Cursor a=d.rawQuery("SELECT total,p1,p2,p3,p4,p5,ended,duration FROM g25 WHERE status='saved' ORDER BY ended DESC LIMIT 20",null);
        note("🏀 Игра 25");if(a.getCount()==0)note("Пока нет сохранённых игр.");
        while(a.moveToNext())note(a.getInt(0)+"/25 · "+a.getInt(1)+"+"+a.getInt(2)+"+"+a.getInt(3)+"+"+a.getInt(4)+"+"+a.getInt(5)+" · "+date(a.getLong(6))+" · "+dur(a.getLong(7)));
        a.close();Cursor b=d.rawQuery("SELECT winner,ended,duration FROM g33 WHERE status='saved' ORDER BY ended DESC LIMIT 20",null);
        note("🎯 Игра 33");if(b.getCount()==0)note("Пока нет сохранённых игр.");
        while(b.moveToNext())note("🏆 "+b.getString(0)+" · "+date(b.getLong(1))+" · "+dur(b.getLong(2)));b.close();btn("◀️ В меню",()->home());
    }
    void leaders(){
        base("🏆 Таблица лидеров");Cursor a=db.getReadableDatabase().rawQuery("SELECT total,duration,ended FROM g25 WHERE status='saved' ORDER BY total DESC,duration ASC LIMIT 10",null);
        note("🏀 Игра 25 — лучшие результаты");if(a.getCount()==0)note("Пока нет результатов.");
        int i=1;while(a.moveToNext())note((i++)+". "+a.getInt(0)+"/25 · "+dur(a.getLong(1))+" · "+date(a.getLong(2)));a.close();
        Cursor b=db.getReadableDatabase().rawQuery("SELECT winner,duration FROM g33 WHERE status='saved' ORDER BY duration ASC LIMIT 10",null);
        note("🎯 Игра 33 — быстрейшие победы");if(b.getCount()==0)note("Пока нет результатов.");
        i=1;while(b.moveToNext())note((i++)+". 🏆 "+b.getString(0)+" · "+dur(b.getLong(1)));b.close();btn("◀️ В меню",()->home());
    }
    class DB extends SQLiteOpenHelper{
        DB(Context c){super(c,"scores_phone.db",null,1);}
        public void onCreate(SQLiteDatabase d){
            d.execSQL("CREATE TABLE g25(id INTEGER PRIMARY KEY AUTOINCREMENT,status TEXT,total INTEGER,started INTEGER,ended INTEGER,duration INTEGER,p1 INTEGER,p2 INTEGER,p3 INTEGER,p4 INTEGER,p5 INTEGER)");
            d.execSQL("CREATE TABLE g33(id INTEGER PRIMARY KEY AUTOINCREMENT,status TEXT,started INTEGER,ended INTEGER,duration INTEGER,winner TEXT)");
            d.execSQL("CREATE TABLE players(id INTEGER PRIMARY KEY AUTOINCREMENT,game_id INTEGER,name TEXT,score INTEGER,ord INTEGER)");
        }
        public void onUpgrade(SQLiteDatabase d,int a,int b){}
        void del25(int id){getWritableDatabase().delete("g25","id=?",new String[]{""+id});}
        void status25(int id,String s){ContentValues v=new ContentValues();v.put("status",s);getWritableDatabase().update("g25",v,"id=?",new String[]{""+id});}
        int create33(long start,ArrayList<String> ns){
            SQLiteDatabase d=getWritableDatabase();ContentValues v=new ContentValues();v.put("status","active");v.put("started",start);long gid=d.insert("g33",null,v);
            for(int i=0;i<ns.size();i++){ContentValues p=new ContentValues();p.put("game_id",gid);p.put("name",ns.get(i));p.put("score",0);p.put("ord",i);d.insert("players",null,p);}
            Cursor c=d.rawQuery("SELECT id FROM players WHERE game_id=? ORDER BY ord",new String[]{""+gid});ids.clear();while(c.moveToNext())ids.add(c.getInt(0));c.close();return (int)gid;
        }
        int score(int id){Cursor c=getReadableDatabase().rawQuery("SELECT score FROM players WHERE id=?",new String[]{""+id});int s=0;if(c.moveToFirst())s=c.getInt(0);c.close();return s;}
        void setScore(int id,int s){ContentValues v=new ContentValues();v.put("score",s);getWritableDatabase().update("players",v,"id=?",new String[]{""+id});}
        void finish33(int gid,String winner,long duration,long end,int pid,int score){
            setScore(pid,score);ContentValues v=new ContentValues();v.put("status","finished");v.put("winner",winner);v.put("duration",duration);v.put("ended",end);getWritableDatabase().update("g33",v,"id=?",new String[]{""+gid});
        }
        void status33(int id,String s){ContentValues v=new ContentValues();v.put("status",s);getWritableDatabase().update("g33",v,"id=?",new String[]{""+id});}
        void del33(int id){SQLiteDatabase d=getWritableDatabase();d.delete("players","game_id=?",new String[]{""+id});d.delete("g33","id=?",new String[]{""+id});}
    }
}