package com.zander.digitalwatermark;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.provider.Settings.Secure;
import android.support.v4.app.ActivityCompat;
import android.telephony.TelephonyManager;
import android.widget.Toast;

import com.baidu.location.BDLocation;
import com.github.yoojia.qrcode.qrcode.QRCodeDecoder;
import com.github.yoojia.qrcode.qrcode.QRCodeEncoder;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import static android.content.Context.TELEPHONY_SERVICE;

/**
 * Created by Zander on 7/10/2017.
 */

class Watermark {
    private static Boolean TEST = false;

    static Bitmap generateSimpleWatermarkPlain() {
        //1、创建一个bitmap，并放入画布
        Bitmap bitmap = Bitmap.createBitmap(160, 160, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);

        //2、设置画笔
        Paint paint = new Paint();
        paint.setColor(Color.BLACK);//设置画笔颜色
        paint.setStrokeWidth(15.0f);// 设置画笔粗细
        paint.setTextSize(25);//设置文字大小
        paint.setAntiAlias(true);

        //3.在画布上绘制内容
        canvas.drawColor(Color.WHITE);//默认背景是黑色的
        canvas.drawText("水印", 2, 25, paint);
        canvas.drawText("在此", 2, 65, paint);
        canvas.drawText("this is", 2, 105, paint);
        canvas.drawText("waterMark", 2, 145, paint);

        return bitmap;
    }

    static Bitmap generateWatermarkPlain() {
        //1、创建一个bitmap，并放入画布
        Bitmap bitmap = Bitmap.createBitmap(120, 120, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);

        //2、设置画笔
        Paint paint = new Paint();
        paint.setColor(Color.BLACK);//设置画笔颜色
        paint.setStrokeWidth(12.0f);// 设置画笔粗细
        paint.setTextSize(23f);//设置文字大小

        //3.在画布上绘制内容
        canvas.drawColor(Color.WHITE);//默认背景是黑色的

        canvas.drawText("我是", 2, 30, paint);
        canvas.drawText("Android", 2, 80, paint);

        return bitmap;
//        return RotateBitmap(bitmap, -45);
    }

    /**
     * 变换
     */
    public static Bitmap arnold(Bitmap bitmap, int count) {
        int SIZE = bitmap.getWidth();
        int[] origin = new int[SIZE * SIZE];
        bitmap.getPixels(origin, 0, SIZE, 0, 0, SIZE, SIZE);
        int[] dest = new int[SIZE * SIZE];
        int oldY, oldX, newY, newX;
        while (count > 0) {
            for (int index = 0; index < origin.length; index++) {
                oldX = index % SIZE;
                oldY = index / SIZE;
                newX = (oldX + oldY) % SIZE;
                newY = (oldX + 2 * oldY) % SIZE;
                dest[newY * SIZE + newX] = origin[index];
            }
            count--;
            origin = Arrays.copyOf(dest, dest.length);
        }
        bitmap.setPixels(dest, 0, SIZE, 0, 0, SIZE, SIZE);
        return bitmap;
    }

    /**
     * 逆变换
     */
    public static Bitmap inverseArnold(Bitmap bitmap, int count) {
        int SIZE = bitmap.getWidth();
        int[] origin = new int[SIZE * SIZE];
        bitmap.getPixels(origin, 0, SIZE, 0, 0, SIZE, SIZE);
        int[] dest = new int[SIZE * SIZE];
        int oldY, oldX, newY, newX;
        while (count > 0) {
            for (int index = 0; index < origin.length; index++) {
                oldX = index % SIZE;
                oldY = index / SIZE;
                newY = mod((oldY - oldX), SIZE);
                newX = mod((2 * oldX - oldY), SIZE);
                dest[newY * SIZE + newX] = origin[index];
            }
            count--;
            origin = Arrays.copyOf(dest, dest.length);
        }
        bitmap.setPixels(dest, 0, SIZE, 0, 0, SIZE, SIZE);
        return bitmap;
    }

    /**
     * 求模运算
     */
    private static int mod(int number, int mod) {
        return (number % mod + mod) % mod;
    }

    // 置乱过程
    public static Bitmap scramble(Bitmap origin, int time) {
        // 缩放图片至目标大小
        origin = Attack.scaleBitmap(origin, true);
        for (int i = 0; i < time; i++) {
            doScramble(origin, origin.getWidth());
        }
        return origin;
    }

    // 进行置换
    private static Bitmap doScramble(Bitmap bitmap, int size) {
        int[] inPixels = new int[size * size];
        bitmap.getPixels(inPixels, 0, size, 0, 0, size, size);
        int[] outPixels = shuffle(inPixels);
        bitmap.setPixels(outPixels, 0, size, 0, 0, size, size);
        return bitmap;
    }

    private static int[] shuffle(int[] x) {
        List<Integer> list = new ArrayList<>();
        for (int value : x) {
            list.add(value);
        }

        Collections.shuffle(list);

        int[] out = new int[x.length];
        for (int i = 0; i < out.length; i++) {
            out[i] = list.get(i);
        }
        return out;
    }

    static Bitmap generateSimpleWatermarkQRCode(Context context) {
        String imei = getIMEI(context);
        int size = 1;
        return new QRCodeEncoder.Builder()
                .width(size) // 二维码图案的宽度
                .height(size)
                .paddingPx(0) // 二维码的内边距
                .marginPt(0) // 二维码的外边距
                .build()
                .encode(imei);
    }

    static Bitmap generateWatermarkQRCode(Context context, BDLocation bdLocation) {
        String[] location = getLocation(bdLocation);
        String date = getDate();
        String imei = getIMEI(context);
        String androidID = getAndroidID(context);
        String content = "";
        for (String i : location) {
            if (i == null) {
                break;
            }
            content += (i + "\n");
        }
        content += ("时间：" + date + "\nIMEI：" + imei + "\nAndroid ID： " + androidID);
        int size = 70;
        Bitmap qrCodeImage = new QRCodeEncoder.Builder()
                .width(size) // 二维码图案的宽度
                .height(size)
                .paddingPx(0) // 二维码的内边距
                .marginPt(0) // 二维码的外边距
                .build()
                .encode(content);
        if (TEST) {
            final QRCodeDecoder mDecoder = new QRCodeDecoder.Builder().build();
            String decode = mDecoder.decode(qrCodeImage);
            Toast.makeText(context, decode, Toast.LENGTH_LONG).show();
        }
        return qrCodeImage;
    }

    private static String[] getLocation(BDLocation bdLocation) {
        String[] res = new String[5];
        if (bdLocation != null) {
            res[0] = "位置：";
            res[1] = "  经度：" + String.valueOf(bdLocation.getLongitude());
            res[2] = "  纬度：" + String.valueOf(bdLocation.getLatitude());
            res[3] = bdLocation.getCountry() + bdLocation.getCity() + bdLocation.getDistrict();
            res[4] = bdLocation.getStreet();
        }
        return res;
    }

    private static String getDate() {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd-HH:mm:ss", Locale.CHINA);
        return format.format(new Date());
    }

    private static String getIMEI(Context context) {
        String res = "";
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED) {
            TelephonyManager tm = (TelephonyManager) context.getSystemService(TELEPHONY_SERVICE);
            if (tm.getDeviceId() != null) {
                res += ((TelephonyManager) context.getSystemService(TELEPHONY_SERVICE)).getDeviceId();
            }
        }
        return res;
    }

    private static String getAndroidID(Context context) {
        return Secure.getString(context.getContentResolver(), Secure.ANDROID_ID);
    }

    private static Bitmap RotateBitmap(Bitmap bitmap, int degree) {
//        Canvas canvas = new Canvas();
//        canvas.drawColor(Color.BLACK);
//        canvas.drawBitmap(bitmap, 0, 0, null);
        Matrix matrix = new Matrix();
//        // matrix.postScale(1f, 1f);
        matrix.setRotate(degree, (float) bitmap.getWidth() / 2, (float) bitmap.getHeight() / 2);
//        Bitmap dstbmp = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
    }
}
