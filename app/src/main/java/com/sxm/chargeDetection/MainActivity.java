package com.sxm.chargeDetection;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        start();
    }

    public static final int BMS = 0;
    public static final int USB = 1;

    StringBuilder sb = new StringBuilder();
    public static Thread thread = new Thread();

    //程序开始
    public void start() {
        TextView usbText = findViewById(R.id.text);
        thread.interrupt();
        thread = new Thread() {
            @Override
            public void run() {
                try {
                    while (true) {
                        String bms = fileToString("/sys/class/power_supply/bms/uevent");
                        String usb = fileToString("/sys/class/power_supply/usb/uevent");
                        String s = translate(bms, BMS) + translate(usb, USB);
                        SawService.s = sb.toString();
                        runOnUiThread(() -> usbText.setText(s));
                        Thread.sleep(2000);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    Button btn = findViewById(R.id.btn);
                    btn.setVisibility(Button.GONE);
                    runOnUiThread(() -> usbText.setText("此软件需要在“SELinux宽容模式”下运行！"));
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        };
        thread.start();
        if (SawService.saw) {
            ((Button) findViewById(R.id.btn)).setText("隐藏悬浮窗");
            listenSaw();
        }
    }

    //读取文件转换成字符串
    public String fileToString(String pathname) throws IOException {
        File file = new File(pathname);
        FileReader fileReader = new FileReader(file);
        char[] chars = new char[(int) file.length()];
        fileReader.read(chars);
        return new String(chars, 0, binSearch(chars));
    }

    //二分查找
    public int binSearch(char[] chars) {
        int start = 0, end = chars.length;
        while (start < end) {
            int mid = start + (end - start) / 2;
            if (chars[mid] == 0) {
                end = mid;
            } else {
                start = mid + 1;
            }
        }
        return start;
    }

    //将字符串转换成列表
    public String translate(String s, int type) {
        String[] temp = s.split("\n");
        StringBuilder sb = new StringBuilder();
        float A = 0, V = 0;
        if (type == BMS) {
            this.sb = new StringBuilder();
            int cfd = 0, cf = 0;
            for (String t1 : temp) {
                String[] t2 = t1.split("=");
                switch (t2[0]) {
                    case "POWER_SUPPLY_CURRENT_NOW":
                        A = Float.parseFloat(t2[1]) / 1000000;
                        sb.append("电池电流：").append(Integer.parseInt(t2[1]) / -1000).append("mA").append('\n');
                        this.sb.append("电池电流：").append(Integer.parseInt(t2[1]) / -1000).append("mA").append('\n');
                        break;
                    case "POWER_SUPPLY_CAPACITY":
                        sb.append("电池电量：").append(t2[1]).append('%').append('\n');
                        break;
                    case "POWER_SUPPLY_TEMP":
                        sb.append("电池温度：").append(Float.parseFloat(t2[1]) / 10).append('℃').append('\n');
                        this.sb.append("电池温度：").append(Float.parseFloat(t2[1]) / 10).append('℃').append('\n');
                        break;
                    case "POWER_SUPPLY_VOLTAGE_NOW":
                        V = Float.parseFloat(t2[1]) / 1000000;
                        sb.append("电池电压：").append(String.format("%.2f", V)).append('V').append('\n');
                        break;
                    case "POWER_SUPPLY_CHARGE_FULL_DESIGN":
                        cfd = Integer.parseInt(t2[1]);
                        sb.append("设计容量：").append(cfd / 1000).append("mAh").append('\n');
                        break;
                    case "POWER_SUPPLY_VOLTAGE_MAX_DESIGN":
                        sb.append("设计电压：").append(Float.parseFloat(t2[1]) / 1000000).append('V').append('\n');
                        break;
                    case "POWER_SUPPLY_CHARGE_FULL":
                        cf = Integer.parseInt(t2[1]);
                        sb.append("充满容量：").append(cf / 1000).append("mAh").append('\n');
                        break;
                    case "POWER_SUPPLY_CYCLE_COUNT":
                        sb.append("循环次数：").append(t2[1]).append('\n');
                        break;
                    case "POWER_SUPPLY_TIME_TO_FULL_NOW":
                        int time = Integer.parseInt(t2[1]);
                        if (time < 1) break;
                        sb.append("预计充满时间：").append(timeFormat(time)).append('\n');
                        break;
                    case "POWER_SUPPLY_TIME_TO_EMPTY_AVG":
                        time = Integer.parseInt(t2[1]);
                        if (time < 0) break;
                        sb.append("预计使用时间：").append(timeFormat(time)).append('\n');
                        break;
                }
            }
            if (cfd != 0) {
                sb.append("电池寿命：").append((int) (cf * 100 / cfd)).append('%').append('\n');
            }
            sb.append("电池功率：").append(String.format("%.2f", -A * V)).append('W');
            this.sb.append("电池功率：").append(String.format("%.2f", -A * V)).append('W');
        } else if (type == USB) {
            sb.append("\n\n");
            for (String t1 : temp) {
                String[] t2 = t1.split("=");
                switch (t2[0]) {
                    case "POWER_SUPPLY_PRESENT":
                        if (t2[1].equals("0")) return "";
                        break;
                    case "POWER_SUPPLY_VOLTAGE_NOW":
                        V = Float.parseFloat(t2[1]) / 1000000;
                        sb.append("充电电压：").append(String.format("%.2f", V)).append('V').append('\n');
                        this.sb.append('\n').append("充电电压：").append(String.format("%.2f", V))
                                .append('V').append('\n');
                        break;
                    case "POWER_SUPPLY_QUICK_CHARGE_TYPE":
                        String chargeType;
                        switch (t2[1]) {
                            case "0":
                                chargeType = "普速充电";
                                break;
                            case "1":
                                chargeType = "快速充电";
                                break;
                            case "2":
                                chargeType = "极速充电";
                                break;
                            default:
                                chargeType = "未知";
                                break;
                        }
                        sb.append("充电类型：").append(chargeType).append('(').append(t2[1]).append(')').append('\n');
                        break;
                    case "POWER_SUPPLY_CURRENT_MAX":
                        sb.append("最大电流：").append(Float.parseFloat(t2[1]) / 1000000).append('A').append('\n');
                        break;
                    case "POWER_SUPPLY_TYPE":
                        sb.append("充电协议：").append(t2[1]).append('\n');
                        break;
                    case "POWER_SUPPLY_INPUT_CURRENT_NOW":
                        A = Float.parseFloat(t2[1]) / 1000000;
                        sb.append("充电电流：").append(String.format("%.3f", A)).append('A').append('\n');
                        this.sb.append("充电电流：").append(String.format("%.3f", A)).append('A').append('\n');
                        break;
                }
            }
            sb.append("充电功率：").append(String.format("%.2f", A * V)).append('W');
            this.sb.append("充电功率：").append(String.format("%.2f", A * V)).append('W');
        }
        return sb.toString();
    }

    //时间格式化，单位为“秒”
    public String timeFormat(int time) {
        if (time <= 0) return "0分钟";
        int day = time / 86400;
        time %= 86400;
        int hour = time / 3600;
        time %= 3600;
        int minute = time / 60;
        return (day > 0 ? day + "天" : "")
                + (hour > 0 ? hour + "小时" : "")
                + (minute > 0 ? minute + "分钟" : "");
    }

    public static final int CODE_WINDOW = 0;

    public void saw(View view) {
        //申请悬浮窗权限
        if (Build.VERSION.SDK_INT >= 23) {
            if (!Settings.canDrawOverlays(this)) {
                Toast.makeText(this, "请打开此应用悬浮窗权限", Toast.LENGTH_SHORT).show();
                startActivityForResult(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + getPackageName())), CODE_WINDOW);
            }
        }

        Intent intent = new Intent(this, SawService.class);
        Button btn = findViewById(R.id.btn);
        if (!SawService.saw) {
            SawService.saw = true;
            startService(intent);
            btn.setText("隐藏悬浮窗");
            listenSaw();
        } else {
            stopService(intent);
            btn.setText("显示悬浮窗");
        }
    }

    //监听悬浮窗是否被关闭
    void listenSaw() {
        new Thread() {
            @Override
            public void run() {
                try {
                    Button btn = findViewById(R.id.btn);
                    while (true) {
                        if (!SawService.saw) {
                            runOnUiThread(() -> btn.setText("显示悬浮窗"));
                            break;
                        }
                        sleep(33);
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }.start();
    }
}