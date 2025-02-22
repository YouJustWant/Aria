/*
 * Copyright (C) 2016 AriaLyy(https://github.com/AriaLyy/Aria)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.arialyy.aria.core;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Application;
import android.app.Dialog;
import android.app.Service;
import android.content.Context;
import android.os.Build;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.widget.PopupWindow;
import com.arialyy.aria.core.download.DownloadReceiver;
import com.arialyy.aria.core.upload.UploadReceiver;
import com.arialyy.aria.util.ALog;

/**
 * Created by lyy on 2016/12/1.
 *
 * @see <a href="https://github.com/AriaLyy/Aria">Aria</a>
 * @see <a href="https://aria.laoyuyu.me/aria_doc/">Aria doc</a>
 * Aria启动，管理全局任务
 * <pre>
 *   <code>
 *   //下载
 *   Aria.download(this)
 *       .load(URL)     //下载地址，必填
 *       //文件保存路径，必填
 *       .setDownloadPath(Environment.getExternalStorageDirectory().getPath() + "/test.apk")
 *       .start();
 *   </code>
 *   <code>
 *    //上传
 *    Aria.upload(this)
 *        .load(filePath)     //文件路径，必填
 *        .setTempUrl(uploadUrl)  //上传路径，必填
 *        .setAttachment(fileKey)   //服务器读取文件的key，必填
 *        .start();
 *   </code>
 * </pre>
 *
 * 如果你需要在【Activity、Service、Application、DialogFragment、Fragment、PopupWindow、Dialog】
 * 之外的java中使用Aria，那么你应该在Application或Activity初始化的时候调用{@link #init(Context)}对Aria进行初始化
 * 然后才能使用{@link #download(Object)}、{@link #upload(Object)}
 *
 * <pre>
 *   <code>
 *       Aria.init(this);
 *
 *      Aria.download(this)
 *       .load(URL)     //下载地址，必填
 *       //文件保存路径，必填
 *       .setDownloadPath(Environment.getExternalStorageDirectory().getPath() + "/test.apk")
 *       .start();
 *
 *   </code>
 *
 * </pre>
 */
@TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1) public class Aria {

  private Aria() {
  }

  /**
   * 下载，在当前类中调用Aria方法，参数需要使用this，否则将
   * 如果不是【Activity、Service、Application、DialogFragment、Fragment、PopupWindow、Dialog】对象，那么你
   * 需要在对象中初始化下载前，在Application或Activity初始化的时候调用{@link #init(Context)}对Aria进行初始化
   *
   * @param obj 观察者对象，为本类对象，使用{@code this}
   */
  public static DownloadReceiver download(Object obj) {
    if (AriaManager.getInstance() != null){
     return AriaManager.getInstance().download(obj);
    }
    return get(convertContext(obj)).download(obj);
  }

  /**
   * 上传
   * 如果不是【Activity、Service、Application、DialogFragment、Fragment、PopupWindow、Dialog】对象，那么你
   * 需要在对象中初始化下载前，在Application或Activity初始化的时候调用{@link #init(Context)}对Aria进行初始化
   *
   * @param obj 观察者对象，为本类对象，使用{@code this}
   */
  public static UploadReceiver upload(Object obj) {
    if (AriaManager.getInstance() != null){
      return AriaManager.getInstance().upload(obj);
    }
    return get(convertContext(obj)).upload(obj);
  }

  /**
   * 处理通用事件
   */
  public static AriaManager get(Context context) {
    if (context == null) {
      throw new NullPointerException("context 无效，在非【Activity、Service、Application、DialogFragment、Fragment、PopupWindow、Dialog】，"
          + "请参考【https://aria.laoyuyu.me/aria_doc/start/any_java.html】，参数请使用 download(this) 或 upload(this);"
          + "不要使用 download(getContext()) 或 upload(getContext())");
    }
    return AriaManager.getInstance(context);
  }

  /**
   * 初始化Aria，如果你需要在【Activity、Service、Application、DialogFragment、Fragment、PopupWindow、Dialog】
   * 之外的java中使用Aria，那么你应该在Application或Activity初始化的时候调用本方法对Aria进行初始化
   * 只需要初始化一次就可以
   * {@link #download(Object)}、{@link #upload(Object)}
   */
  public static AriaManager init(Context context) {
    return AriaManager.getInstance(context);
  }

  private static Context convertContext(Object obj) {
    if (obj instanceof Application) {
      return (Application) obj;
    } else if (obj instanceof Service) {
      return (Service) obj;
    } else if (obj instanceof Activity) {
      return (Activity) obj;
    } else if (obj instanceof DialogFragment) {
      return ((DialogFragment) obj).getContext();
    } else if (obj instanceof android.app.DialogFragment) {
      return ((android.app.DialogFragment) obj).getActivity();
    } else if (obj instanceof android.support.v4.app.Fragment) {
      return ((Fragment) obj).getContext();
    } else if (obj instanceof android.app.Fragment) {
      return ((android.app.Fragment) obj).getActivity();
    } else if (obj instanceof Dialog) {
      return ((Dialog) obj).getContext();
    } else if (obj instanceof PopupWindow) {
      return ((PopupWindow) obj).getContentView().getContext();
    }
    ALog.e("Aria", "请使用download(this)或upload(this)");
    return null;
  }
}
