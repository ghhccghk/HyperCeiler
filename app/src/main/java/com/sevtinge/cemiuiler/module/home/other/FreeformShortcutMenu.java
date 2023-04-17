package com.sevtinge.cemiuiler.module.home.other;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.core.content.ContextCompat;

import com.sevtinge.cemiuiler.R;
import com.sevtinge.cemiuiler.module.base.BaseHook;
import com.sevtinge.cemiuiler.utils.LogUtils;
import com.sevtinge.cemiuiler.utils.Settings;

import java.util.ArrayList;
import java.util.List;

import de.robv.android.xposed.XposedHelpers;

public class FreeformShortcutMenu extends BaseHook {

    Class<?> mActivity;
    Class<?> mViewDarkModeHelper;
    Class<?> mSystemShortcutMenu;
    Class<?> mSystemShortcutMenuItem;
    Class<?> mAppShortcutMenu;
    Class<?> mShortcutMenuItem;
    Class<?> mAppDetailsShortcutMenuItem;
    Class<?> mActivityUtilsCompat;
    Class<?> mRecentsAndFSGestureUtils;

    Context context;

    @Override
    public void init() {

        mActivity = Activity.class;
        mViewDarkModeHelper = findClassIfExists("com.miui.home.launcher.util.ViewDarkModeHelper");
        mSystemShortcutMenu = findClassIfExists("com.miui.home.launcher.shortcuts.SystemShortcutMenu");
        mSystemShortcutMenuItem = findClassIfExists("com.miui.home.launcher.shortcuts.SystemShortcutMenuItem");
        mAppShortcutMenu = findClassIfExists("com.miui.home.launcher.shortcuts.AppShortcutMenu");
        mShortcutMenuItem = findClassIfExists("com.miui.home.launcher.shortcuts.ShortcutMenuItem");
        mAppDetailsShortcutMenuItem = findClassIfExists("com.miui.home.launcher.shortcuts.SystemShortcutMenuItem$AppDetailsShortcutMenuItem");
        mActivityUtilsCompat = findClassIfExists("com.miui.launcher.utils.ActivityUtilsCompat");
        mRecentsAndFSGestureUtils = findClassIfExists("com.miui.home.launcher.RecentsAndFSGestureUtils");

        try {

            hookAllMethods(mViewDarkModeHelper, "onConfigurationChanged", new MethodHook() {
                @Override
                protected void after(MethodHookParam param) throws Throwable {
                    XposedHelpers.callStaticMethod(mSystemShortcutMenuItem, "createAllSystemShortcutMenuItems");
                }
            });

            hookAllMethods(mShortcutMenuItem, "getShortTitle", new MethodHook() {
                @Override
                protected void after(MethodHookParam param) throws Throwable {
                    if (param.getResult().equals(R.string.app_info)) {
                        param.setResult(R.string.info);
                    }
                }
            });


            findAndHookMethod(mAppDetailsShortcutMenuItem, "lambda$getOnClickListener$0", mAppDetailsShortcutMenuItem, View.class, new MethodHook() {

                @Override
                protected void before(MethodHookParam param) throws Throwable {

                    Object obj = param.args[0];
                    View view = (View) param.args[1];
                    CharSequence mShortTitle = (CharSequence) XposedHelpers.callMethod(obj, "getShortTitle", new Object[0]);

                    if (mShortTitle.equals("妙享中心")) {

                        param.setResult(null);
                        XposedHelpers.callStaticMethod(mRecentsAndFSGestureUtils, "startWorld", context);

                    } else if (mShortTitle.equals("小窗")) {

                        param.setResult(null);
                        Intent intent = new Intent();
                        ComponentName mComponentName = (ComponentName) XposedHelpers.callMethod(obj, "getComponentName", new Object[0]);
                        intent.setAction("android.intent.action.MAIN");
                        intent.addCategory("android.intent.category.DEFAULT");
                        intent.setComponent(mComponentName);
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        Object callStaticMethod = XposedHelpers.callStaticMethod(mActivityUtilsCompat, "makeFreeformActivityOptions", view.getContext(), mComponentName.getPackageName());

                        if (callStaticMethod != null) {
                            view.getContext().startActivity(intent, (Bundle) XposedHelpers.callMethod(callStaticMethod, "toBundle", new Object[0]));
                        }
                    }
                }
            });


            hookAllMethods(mActivity, "onCreate", new MethodHook() {
                @Override
                protected void after(MethodHookParam param) throws Throwable {
                    context = (Context) param.thisObject;
                }
            });


            hookAllMethods(mSystemShortcutMenu, "getMaxShortcutItemCount", new MethodHook() {
                @Override
                protected void after(MethodHookParam param) throws Throwable {
                    param.setResult(5);
                }
            });

            hookAllMethods(mAppShortcutMenu, "getMaxShortcutItemCount", new MethodHook() {
                @Override
                protected void after(MethodHookParam param) throws Throwable {
                    param.setResult(5);
                }
            });

            hookAllMethods(mSystemShortcutMenuItem, "createAllSystemShortcutMenuItems", new MethodHook() {
                @SuppressLint("DiscouragedApi")
                @Override
                protected void after(MethodHookParam param) throws Throwable {

                    List mAllSystemShortcutMenuItems = (List) XposedHelpers.getStaticObjectField(mSystemShortcutMenuItem, "sAllSystemShortcutMenuItems");

                    Object mSmallWindowInstance = XposedHelpers.newInstance(mAppDetailsShortcutMenuItem);
                    XposedHelpers.callMethod(mSmallWindowInstance, "setShortTitle", "小窗");
                    XposedHelpers.callMethod(mSmallWindowInstance, "setIconDrawable", ContextCompat.getDrawable(context, context.getResources().getIdentifier("ic_task_small_window", "drawable", context.getPackageName())));

                    ArrayList sAllSystemShortcutMenuItems = new ArrayList();
                    sAllSystemShortcutMenuItems.add(mSmallWindowInstance);
                    sAllSystemShortcutMenuItems.addAll(mAllSystemShortcutMenuItems);
                    XposedHelpers.setStaticObjectField(mSystemShortcutMenuItem, "sAllSystemShortcutMenuItems", sAllSystemShortcutMenuItems);
                }
            });

        } catch (Throwable th) {
            LogUtils.log("FreeformShortcutMenu" + th);
        }
    }
}
