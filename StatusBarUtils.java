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
 * ״̬��������
 * ���ڿ��ٵ���״̬����ɫ, ͸���ȵ�
 * Created by Simon on 2016/9/28.
 */
public final class StatusBarUtils {


    /***
     * Ĭ�ϵ�͸����,  �����޸����ֵ,�Ӷ��޸ĵ���ʱ��Ĭ��͸����
     */
    public static int DEFAULT_STATUS_BAR_ALPHA = 0;
    private static final int MIN_STATUS_BAR_ALPHA = 0;
    private static final int MAX_STATUS_BAR_ALPHA = 255;

    /**
     * ����״̬����ɫ
     *
     * @param activity ��Ҫ���õ� activity
     * @param color    ״̬����ɫֵ
     */
    public static void setColor(Activity activity, @ColorInt int color) {
        setColor(activity, color, DEFAULT_STATUS_BAR_ALPHA);
    }


    /**
     * ����״̬����ɫ
     *
     * @param activity       ��Ҫ���õ�activity
     * @param color          ״̬����ɫֵ
     * @param statusBarAlpha ״̬��͸����  0~255
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
     * ʹ״̬��͸��
     * <p>
     * 4.4֮���ϵͳ����Ч<br>
     * ������, ״̬���Ḳ���ڲ���֮��;  Ҫ�ڲ�������, ����Ҫ������view, ���fitsSystemWindows��־, �����ڲ�������, �ճ�״̬���߶�
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
     * ʹ״̬����͸��
     * <p>
     * ���ú�״̬���Ḳ���ڲ���֮��;<br>
     * �Բ�������. ����Ҫ��statusBar������ʾ��view, ��Ҫ���fitsSystemWindows��־, �����ڲ�������, �ճ�״̬���߶�;
     * <p>
     * ��:
     * fitsSystemWindows��5.0֮ǰ, ��֮��ϵͳ�иĶ�; ��Ӧ����ϵͳ��{@link View#fitSystemWindows(Rect)}����, ���ڲ�ͬ��view����Ч����һ��; ������֮��������, ֱ�������Ұ� - -
     *
     * @param activity       ��Ҫ���õ�activity
     * @param statusBarAlpha ״̬��͸���� 0~255
     */
    public static void setTranslucent(Activity activity, @IntRange(from = MIN_STATUS_BAR_ALPHA, to = MAX_STATUS_BAR_ALPHA) int statusBarAlpha) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
            return;
        }
        setTransparent(activity);

        addTranslucentView(activity, statusBarAlpha);
    }

    /**
     * ��Ӱ�͸��������
     *
     * @param activity       ��Ҫ���õ� activity
     * @param statusBarAlpha ͸��ֵ
     */
    private static void addTranslucentView(Activity activity, int statusBarAlpha) {
        // TODO: 2016/10/5 ���ﵽ����Ӧ�ü���DecorView ������R.id.content��Ӧ��view����, ��Ҫȥ����Դ����, Ŀǰ����������, ����ʱ����Ҫȥ��һ�¼����ĸ������һ��
        ViewGroup contentView = (ViewGroup) activity.findViewById(android.R.id.content);
        if (contentView.getChildCount() > 1 && contentView.getChildAt(1) instanceof StatusBarView) {
            contentView.getChildAt(1).setBackgroundColor(Color.argb(statusBarAlpha, 0, 0, 0));
        } else {
            contentView.addView(createTranslucentStatusBarView(activity, statusBarAlpha));
        }
    }

    /**
     * ������͸������ View
     *
     * @param alpha ͸��ֵ
     * @return ��͸�� View
     */
    private static StatusBarView createTranslucentStatusBarView(Activity activity, int alpha) {
        // ����һ����״̬��һ���ߵľ���
        StatusBarView statusBarView = new StatusBarView(activity);
        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, getStatusBarHeight(activity));
        statusBarView.setLayoutParams(params);
        statusBarView.setBackgroundColor(Color.argb(alpha, 0, 0, 0));
        return statusBarView;
    }


    /**
     * ����һ����״̬����С��ͬ�İ�͸��������
     *
     * @param activity ��Ҫ���õ�activity
     * @param color    ״̬����ɫֵ
     * @param alpha    ͸��ֵ
     * @return ״̬��������
     */
    private static StatusBarView createStatusBarView(Activity activity, @ColorInt int color, int alpha) {
        // ����һ����״̬��һ���ߵľ���
        StatusBarView statusBarView = new StatusBarView(activity);
        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, getStatusBarHeight(activity));
        statusBarView.setLayoutParams(params);
        statusBarView.setBackgroundColor(calculateStatusColor(color, alpha));
        return statusBarView;
    }

    /**
     * ���ø����ֲ���
     */
    private static void setRootView(Activity activity) {
        ViewGroup rootView = (ViewGroup) ((ViewGroup) activity.findViewById(android.R.id.content)).getChildAt(0);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            // FLAG_TRANSLUCENT_STATUS ���ò��ֵ���״̬����ʼ, ����fitSystemWindow��־, ��view�Զ�����padding��ϵͳ��λ�ÿճ���;
            rootView.setFitsSystemWindows(true);
        }
        // padding�в��ɻ���, ϵͳviewĬ�ϵ�Ӧ�þ���true; ��������һ��, ȷ����ȷ
        rootView.setClipToPadding(true);
    }


    /**
     * ����״̬����ɫ
     *
     * @param color colorֵ
     * @param alpha alphaֵ
     * @return ���յ�״̬����ɫ
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

//    ########################  ����״̬�������ɫ  #############################

    /**
     * ����״̬����LightMode,  ������״̬��������ɫ,  <b>ֻ�а�ɫ����ɫ2��</b>
     * <p>
     * <p>
     * �����6.0�ϲ���Ч��, С�׺������, ��Ҫ����������
     * ֻ��api>23; miui>6, flyme>4
     * </p>
     *
     * @param enable ture ��ɫ; false ��ɫ
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
     * �ο� http://dev.xiaomi.com/doc/p=4769/index.html
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
     * ����״̬��ͼ��Ϊ��ɫ�������ض������ַ��
     * ���������ж��Ƿ�ΪFlyme�û�
     *
     * @param darkmode �Ƿ��״̬�����弰ͼ����ɫ����Ϊ��ɫ
     * @return boolean �ɹ�ִ�з���true
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