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
package com.arialyy.aria.core.download.m3u8;

import android.text.TextUtils;
import com.arialyy.aria.core.common.CompleteInfo;
import com.arialyy.aria.core.common.IUtil;
import com.arialyy.aria.core.common.OnFileInfoCallback;
import com.arialyy.aria.core.download.DTaskWrapper;
import com.arialyy.aria.core.download.M3U8Listener;
import com.arialyy.aria.core.inf.AbsEntity;
import com.arialyy.aria.exception.BaseException;
import com.arialyy.aria.exception.M3U8Exception;
import com.arialyy.aria.util.ALog;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * M3U8点播文件下载工具
 * 工作流程：
 * 1、创建一个和文件同父路径并且同名隐藏文件夹
 * 2、将所有m3u8的ts文件下载到该文件夹中
 * 3、完成所有分片下载后，合并ts文件
 * 4、删除该隐藏文件夹
 */
public class M3U8VodUtil implements IUtil {
  private final String TAG = "M3U8DownloadUtil";

  private DTaskWrapper mWrapper;
  private M3U8Listener mListener;
  private boolean isStop = false, isCancel = false;
  private List<String> mUrls = new ArrayList<>();
  private M3U8VodLoader mLoader;

  public M3U8VodUtil(DTaskWrapper wrapper, M3U8Listener listener) {
    mWrapper = wrapper;
    mListener = listener;
    mLoader = new M3U8VodLoader(mListener, mWrapper);
  }

  @Override public String getKey() {
    return mWrapper.getKey();
  }

  @Override public long getFileSize() {
    return 0;
  }

  @Override public long getCurrentLocation() {
    return 0;
  }

  @Override public boolean isRunning() {
    return mLoader.isRunning();
  }

  @Override public void cancel() {
    isCancel = true;
    mLoader.cancel();
  }

  @Override public void stop() {
    isStop = true;
    mLoader.stop();
  }

  @Override public void start() {
    if (isStop || isCancel) {
      return;
    }
    mListener.onPre();
    getVodInfo();
  }

  /**
   * 获取点播文件信息
   */
  private void getVodInfo() {
    M3U8InfoThread thread = new M3U8InfoThread(mWrapper, new OnFileInfoCallback() {
      @Override public void onComplete(String key, CompleteInfo info) {
        IVodTsUrlConverter converter = mWrapper.asM3U8().getVodUrlConverter();
        if (converter != null) {
          if (TextUtils.isEmpty(mWrapper.asM3U8().getBandWidthUrl())) {
            mUrls.addAll(converter.convert(mWrapper.getEntity().getUrl(), (List<String>) info.obj));
          } else {
            mUrls.addAll(
                converter.convert(mWrapper.asM3U8().getBandWidthUrl(), (List<String>) info.obj));
          }
        } else {
          mUrls.addAll((Collection<? extends String>) info.obj);
        }
        if (mUrls.isEmpty()) {
          failDownload(new M3U8Exception(TAG, "获取地址失败"), false);
          return;
        } else if (!mUrls.get(0).startsWith("http")) {
          failDownload(new M3U8Exception(TAG, "地址错误，请使用IM3U8UrlExtInfHandler处理你的url信息"), false);
          return;
        }
        mWrapper.asM3U8().setUrls(mUrls);
        if (isStop) {
          mListener.onStop(mWrapper.getEntity().getCurrentProgress());
        } else if (isCancel) {
          mListener.onCancel();
        } else {
          mLoader.start();
        }
      }

      @Override public void onFail(AbsEntity entity, BaseException e, boolean needRetry) {
        failDownload(e, needRetry);
      }
    });
    new Thread(thread).start();
  }

  private void failDownload(BaseException e, boolean needRetry) {
    if (isStop || isCancel) {
      return;
    }
    mListener.onFail(needRetry, e);
    mLoader.onDestroy();
  }
}
