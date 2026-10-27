package com.dolbin.animatronics;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.View;

public class SkullView extends View {
    private final Paint p = new Paint(3);
    private boolean speaking = false;
    private long start = 0;
    public SkullView(Context c) { super(c); setBackgroundColor(Color.rgb(12,13,22)); }
    public void setSpeaking(boolean value) { speaking=value; if(value) start=System.currentTimeMillis(); invalidate(); }
    private void oval(Canvas c,int color,float l,float t,float r,float b){p.setColor(color);p.setStyle(Paint.Style.FILL);c.drawOval(l,t,r,b,p);}
    @Override protected void onDraw(Canvas actual){
        super.onDraw(actual);
        Canvas c=actual; c.save(); float s=Math.min(getWidth()/320f,getHeight()/350f);c.translate((getWidth()-320*s)/2,(getHeight()-350*s)/2);c.scale(s,s);
        int bone=Color.rgb(211,199,166),shade=Color.rgb(105,95,81);
        oval(c,bone,58,35,262,260);oval(c,shade,65,156,255,278);
        oval(c,bone,85,195,235,260);
        oval(c,Color.rgb(22,16,23),91,139,146,199);oval(c,Color.rgb(22,16,23),174,139,229,199);
        int eye=speaking?Color.rgb(255,38,27):Color.rgb(74,19,18);
        oval(c,eye,105,154,134,184);oval(c,eye,186,154,215,184);
        p.setColor(Color.rgb(26,19,20));p.setStyle(Paint.Style.FILL);
        android.graphics.Path nose=new android.graphics.Path();nose.moveTo(160,183);nose.lineTo(144,214);nose.lineTo(177,214);nose.close();c.drawPath(nose,p);
        float open=speaking?(13f+(float)Math.abs(Math.sin((System.currentTimeMillis()-start)/95.0))*38f):3f;
        oval(c,Color.rgb(15,9,13),98,252,222,269+open);oval(c,bone,91,267+open,229,292+open);
        p.setColor(shade);p.setStrokeWidth(3);for(int x=110;x<=210;x+=20)c.drawLine(x,270+open,x,287+open,p);
        c.restore();if(speaking)postInvalidateDelayed(45);
    }
}
