import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Rect;
import android.os.Build;
import android.support.annotation.ColorInt;
import android.support.annotation.IntRange;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.LinearLayout;

import com.eebbk.bfc.common.devices.DisplayUtils;
import com.eebbk.bfc.common.inner.StatusBarView;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * 状态栏工具类
 * 用于快速调整状态栏颜色, 透明度等
 * Created by Simon on 2016/9/28.
 */
public final class StatusBarUtils {


    /***
     * 默认的透明度,  可以修改这个值,从而修改调用时的默认透明度
     */
    public static int DEFAULT_STATUS_BAR_ALPHA = 0;
    private static final int MIN_STATUS_BAR_ALPHA = 0;
    private static final int MAX_STATUS_BAR_ALPHA = 255;

    /**
     * 设置状态栏颜色
     *
     * @param activity 需要设置的 activity
     * @param color    状态栏颜色值
     */
    public static void setColor(Activity activity, @ColorInt int color) {
        setColor(activity, color, DEFAULT_STATUS_BAR_ALPHA);
    }


    /**
     * 设置状态栏颜色
     *
     * @param activity       需要设置的activity
     * @param color          状态栏颜色值
     * @param statusBarAlpha 状态栏透明度  0~255
     */

    public static void setColor(Activity activity, @ColorInt int color, @IntRange(from = MIN_STATUS_BAR_ALPHA, to = MAX_STATUS_BAR_ALPHA) int statusBarAlpha) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            activity.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            activity.getWindow().setStatusBarColor(calculateStatusColor(color, statusBarAlpha));
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            ViewGroup decorView = (ViewGroup) activity.getWindow().getDecorView();
            int count = decorView.getChildCount();
            if (count > 0 && decorView.getChildAt(count - 1) instanceof StatusBarView) {
                decorView.getChildAt(count - 1).setBackgroundColor(calculateStatusColor(color, statusBarAlpha));
            } else {
                StatusBarView statusView = createStatusBarView(activity, color, statusBarAlpha);
                decorView.addView(statusView);
            }
            setRootView(activity);
        }
    }


    /**
     * 使状态栏透明
     * <p>
     * 4.4之后的系统才有效<br>
     * 开启后, 状态栏会覆盖在布局之上;  要在布局里面, 对需要调整的view, 添加fitsSystemWindows标志, 或者在布局里面, 空出状态栏高度
     * </p>
     */
    public static void setTransparent(Activity activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            activity.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
            activity.getWindow().setStatusBarColor(Color.TRANSPARENT);
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        }
    }

    /**
     * 使状态栏半透明
     * <p>
     * 设置后状态栏会覆盖在布局之上;<br>
     * 对布局里面. 不需要在statusBar下面显示的view, 需要添加fitsSystemWindows标志, 或者在布局里面, 空出状态栏高度;
     * <p>
     * 另:
     * fitsSystemWindows在5.0之前, 和之后系统有改动; 对应调用系统的{@link View#fitSystemWindows(Rect)}方法, 加在不同的view上面效果不一样; 如果添加之后有问题, 直接来问我吧 - -
     *
     * @param activity       需要设置的activity
     * @param statusBarAlpha 状态栏透明度 0~255
     */
    public static void setTranslucent(Activity activity, @IntRange(from = MIN_STATUS_BAR_ALPHA, to = MAX_STATUS_BAR_ALPHA) int statusBarAlpha) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
            return;
        }
        setTransparent(activity);

        addTranslucentView(activity, statusBarAlpha);
    }

    /**
     * 添加半透明矩形条
     *
     * @param activity       需要设置的 activity
     * @param statusBarAlpha 透明值
     */
    private static void addTranslucentView(Activity activity, int statusBarAlpha) {
        // TODO: 2016/10/5 这里到底是应该加在DecorView 还是在R.id.content对应的view里面, 还要去看下源代码, 目前反正可以用, 等有时间了要去查一下加在哪个里面好一点
        ViewGroup contentView = (ViewGroup) activity.findViewById(android.R.id.content);
        if (contentView.getChildCount() > 1 && contentView.getChildAt(1) instanceof StatusBarView) {
            contentView.getChildAt(1).setBackgroundColor(Color.argb(statusBarAlpha, 0, 0, 0));
        } else {
            contentView.addView(createTranslucentStatusBarView(activity, statusBarAlpha));
        }
    }

    /**
     * 创建半透明矩形 View
     *
     * @param alpha 透明值
     * @return 半透明 View
     */
    private static StatusBarView createTranslucentStatusBarView(Activity activity, int alpha) {
        // 绘制一个和状态栏一样高的矩形
        StatusBarView statusBarView = new StatusBarView(activity);
        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, getStatusBarHeight(activity));
        statusBarView.setLayoutParams(params);
        statusBarView.setBackgroundColor(Color.argb(alpha, 0, 0, 0));
        return statusBarView;
    }


    /**
     * 生成一个和状态栏大小相同的半透明矩形条
     *
     * @param activity 需要设置的activity
     * @param color    状态栏颜色值
     * @param alpha    透明值
     * @return 状态栏矩形条
     */
    private static StatusBarView createStatusBarView(Activity activity, @ColorInt int color, int alpha) {
        // 绘制一个和状态栏一样高的矩形
        StatusBarView statusBarView = new StatusBarView(activity);
        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, getStatusBarHeight(activity));
        statusBarView.setLayoutParams(params);
        statusBarView.setBackgroundColor(calculateStatusColor(color, alpha));
        return statusBarView;
    }

    /**
     * 设置根布局参数
     */
    private static void setRootView(Activity activity) {
        ViewGroup rootView = (ViewGroup) ((ViewGroup) activity.findViewById(android.R.id.content)).getChildAt(0);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            // FLAG_TRANSLUCENT_STATUS 会让布局到从状态栏开始, 加上fitSystemWindow标志, 让view自动加上padding把系统的位置空出来;
            rootView.setFitsSystemWindows(true);
        }
        // padding中不可绘制, 系统view默认的应该就是true; 主动设置一下, 确保正确
        rootView.setClipToPadding(true);
    }


    /**
     * 计算状态栏颜色
     *
     * @param color color值
     * @param alpha alpha值
     * @return 最终的状态栏颜色
     */
    private static int calculateStatusColor(@ColorInt int color, int alpha) {
        float a = 1 - alpha / 255f;
        int red = color >> 16 & 0xff;
        int green = color >> 8 & 0xff;
        int blue = color & 0xff;
        red = (int) (red * a + 0.5);
        green = (int) (green * a + 0.5);
        blue = (int) (blue * a + 0.5);
        return 0xff << 24 | red << 16 | green << 8 | blue;
    }


    private static int getStatusBarHeight(Context context) {
        return DisplayUtils.getStatusBarHeight(context);
    }

//    ########################  设置状态栏字体黑色  #############################

    /**
     * 设置状态栏的LightMode,  即设置状态栏文字颜色,  <b>只有白色和深色2种</b>
     * <p>
     * <p>
     * 这个在6.0上才有效果, 小米和魅族的, 需要单独的适配
     * 只对api>23; miui>6, flyme>4
     * </p>
     *
     * @param enable ture 深色; false 白色
     */
    public static void enableDarkMode(Activity activity, boolean enable) {
        String manufacturer = Build.MANUFACTURER;
        if ("xiaomi".equalsIgnoreCase(manufacturer)) {
            setStatusBarDarkModeMiui(activity, enable);
            return;
        }

        if ("MeiZu".equalsIgnoreCase(manufacturer)) {
            setStatusBarLightModeFlyme(activity, enable);
            return;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            View decorView = activity.getWindow().getDecorView();
            int systemUiVisibilityFlag = decorView.getSystemUiVisibility();

            if (enable) {
                decorView.setSystemUiVisibility(systemUiVisibilityFlag | View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
//                decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
            } else {
                decorView.setSystemUiVisibility(systemUiVisibilityFlag & ~View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
            }
        }
    }

    /**
     * 参考 http://dev.xiaomi.com/doc/p=4769/index.html
     */
    private static void setStatusBarDarkModeMiui(Activity activity, boolean darkmode) {
        Class<? extends Window> clazz = activity.getWindow().getClass();
        try {
            int darkModeFlag;
            Class<?> layoutParams = Class.forName("android.view.MiuiWindowManager$LayoutParams");
            Field field = layoutParams.getField("EXTRA_FLAG_STATUS_BAR_DARK_MODE");
            darkModeFlag = field.getInt(layoutParams);
            Method extraFlagField = clazz.getMethod("setExtraFlags", int.class, int.class);
            extraFlagField.invoke(activity.getWindow(), darkmode ? darkModeFlag : 0, darkModeFlag);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 设置状态栏图标为深色和魅族特定的文字风格
     * 可以用来判断是否为Flyme用户
     *
     * @param darkmode 是否把状态栏字体及图标颜色设置为深色
     * @return boolean 成功执行返回true
     */
    private static boolean setStatusBarLightModeFlyme(Activity activity, boolean darkmode) {
        Window window = activity.getWindow();
        boolean result = false;
        if (window != null) {
            try {
                WindowManager.LayoutParams lp = window.getAttributes();
                Field darkFlag = WindowManager.LayoutParams.class
                        .getDeclaredField("MEIZU_FLAG_DARK_STATUS_BAR_ICON");
                Field meizuFlags = WindowManager.LayoutParams.class
                        .getDeclaredField("meizuFlags");
                darkFlag.setAccessible(true);
                meizuFlags.setAccessible(true);
                int bit = darkFlag.getInt(null);
                int value = meizuFlags.getInt(lp);
                if (darkmode) {
                    value |= bit;
                } else {
                    value &= ~bit;
                }
                meizuFlags.setInt(lp, value);
                window.setAttributes(lp);
                result = true;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return result;
    }


}