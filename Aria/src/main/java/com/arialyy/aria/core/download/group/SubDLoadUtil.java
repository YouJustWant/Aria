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
package com.arialyy.aria.core.download.group;

import android.os.Handler;
import com.arialyy.aria.core.common.CompleteInfo;
import com.arialyy.aria.core.common.IUtil;
import com.arialyy.aria.core.common.OnFileInfoCallback;
import com.arialyy.aria.core.download.DTaskWrapper;
import com.arialyy.aria.core.download.DownloadEntity;
import com.arialyy.aria.core.download.downloader.Downloader;
import com.arialyy.aria.core.download.downloader.HttpFileInfoThread;
import com.arialyy.aria.core.inf.AbsEntity;
import com.arialyy.aria.core.inf.AbsTaskWrapper;
import com.arialyy.aria.core.scheduler.ISchedulers;
import com.arialyy.aria.exception.BaseException;
import com.arialyy.aria.util.ALog;

/**
 * 子任务下载器，负责创建{@link Downloader}
 */
class SubDLoadUtil implements IUtil {
  private final String TAG = "SubDownloadLoader";

  private Downloader mDownloader;
  private DTaskWrapper mWrapper;
  private Handler mSchedulers;
  private ChildDLoadListener mListener;
  private boolean needGetInfo;

  /**
   * @param schedulers 调度器
   * @param needGetInfo {@code true} 需要获取文件信息。{@code false} 不需要获取文件信息
   */
  SubDLoadUtil(Handler schedulers, DTaskWrapper taskWrapper, boolean needGetInfo) {
    mWrapper = taskWrapper;
    mSchedulers = schedulers;
    this.needGetInfo = needGetInfo;
    mListener = new ChildDLoadListener(mSchedulers, SubDLoadUtil.this);
  }

  @Override public String getKey() {
    return mWrapper.getKey();
  }

  public DTaskWrapper getWrapper() {
    return mWrapper;
  }

  public DownloadEntity getEntity() {
    return mWrapper.getEntity();
  }

  /**
   * 重新开始任务
   */
  void reStart() {
    if (mDownloader != null) {
      mDownloader.retryTask();
    }
  }

  public Downloader getDownloader() {
    return mDownloader;
  }

  @Override public long getFileSize() {
    return mDownloader == null ? -1 : mDownloader.getFileSize();
  }

  @Override public long getCurrentLocation() {
    return mDownloader == null ? -1 : mDownloader.getCurrentLocation();
  }

  @Override public boolean isRunning() {
    return mDownloader != null && mDownloader.isRunning();
  }

  @Override public void cancel() {
    if (mDownloader != null && isRunning()) {
      mDownloader.cancel();
    } else {
      mSchedulers.obtainMessage(ISchedulers.CANCEL, this).sendToTarget();
    }
  }

  @Override public void stop() {
    if (mDownloader != null && isRunning()) {
      mDownloader.stop();
    } else {
      mSchedulers.obtainMessage(ISchedulers.STOP, this).sendToTarget();
    }
  }

  @Override public void start() {
    if (mWrapper.getRequestType() == AbsTaskWrapper.D_HTTP) {
      if (needGetInfo) {
        new Thread(new HttpFileInfoThread(mWrapper, new OnFileInfoCallback() {

          @Override public void onComplete(String url, CompleteInfo info) {
            mDownloader = new Downloader(mListener, mWrapper);
            mDownloader.start();
          }

          @Override public void onFail(AbsEntity entity, BaseException e, boolean needRetry) {
            mSchedulers.obtainMessage(ISchedulers.FAIL, SubDLoadUtil.this).sendToTarget();
          }
        })).start();
      } else {
        mDownloader = new Downloader(mListener, mWrapper);
        mDownloader.start();
      }
    } else if (mWrapper.getRequestType() == AbsTaskWrapper.D_FTP) {
      mDownloader = new Downloader(mListener, mWrapper);
      mDownloader.start();
    } else {
      ALog.w(TAG, String.format("不识别的类型，requestType：%s", mWrapper.getRequestType()));
    }
  }
}
