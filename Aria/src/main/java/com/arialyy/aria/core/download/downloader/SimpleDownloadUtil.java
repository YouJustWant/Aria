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

package com.arialyy.aria.core.download.downloader;

import com.arialyy.aria.core.common.CompleteInfo;
import com.arialyy.aria.core.common.IUtil;
import com.arialyy.aria.core.common.OnFileInfoCallback;
import com.arialyy.aria.core.download.DTaskWrapper;
import com.arialyy.aria.core.inf.AbsEntity;
import com.arialyy.aria.core.inf.AbsTaskWrapper;
import com.arialyy.aria.core.inf.IDownloadListener;
import com.arialyy.aria.exception.BaseException;

/**
 * Created by lyy on 2015/8/25.
 * D_HTTP\FTP单任务下载工具
 */
public class SimpleDownloadUtil implements IUtil {
  private String TAG = "SimpleDownloadUtil";
  private IDownloadListener mListener;
  private Downloader mDownloader;
  private DTaskWrapper mTaskWrapper;
  private boolean isStop = false, isCancel = false;

  public SimpleDownloadUtil(DTaskWrapper wrapper, IDownloadListener downloadListener) {
    mTaskWrapper = wrapper;
    mListener = downloadListener;
    mDownloader = new Downloader(downloadListener, wrapper);
  }

  @Override public String getKey() {
    return mTaskWrapper.getKey();
  }

  @Override public long getFileSize() {
    return mDownloader.getFileSize();
  }

  /**
   * 获取当前下载位置
   */
  @Override public long getCurrentLocation() {
    return mDownloader.getCurrentLocation();
  }

  @Override public boolean isRunning() {
    return mDownloader.isRunning();
  }

  /**
   * 取消下载
   */
  @Override public void cancel() {
    isCancel = true;
    mDownloader.cancel();
  }

  /**
   * 停止下载
   */
  @Override public void stop() {
    isStop = true;
    mDownloader.stop();
  }

  /**
   * 多线程断点续传下载文件，开始下载
   */
  @Override public void start() {
    if (isStop || isCancel) {
      return;
    }
    mListener.onPre();
    // 如果网址没有变，而服务器端端文件改变，以下代码就没有用了
    //if (mTaskWrapper.getEntity().getFileSize() <= 1
    //    || mTaskWrapper.isRefreshInfo()
    //    || mTaskWrapper.getRequestType() == AbsTaskWrapper.D_FTP
    //    || mTaskWrapper.getState() == IEntity.STATE_FAIL) {
    //  new Thread(createInfoThread()).start();
    //} else {
    //  mDownloader.start();
    //}
    new Thread(createInfoThread()).start();
  }

  private void failDownload(BaseException e, boolean needRetry) {
    if (isStop || isCancel) {
      return;
    }
    mListener.onFail(needRetry, e);
    mDownloader.onDestroy();
  }

  /**
   * 通过链接类型创建不同的获取文件信息的线程
   */
  private Runnable createInfoThread() {
    switch (mTaskWrapper.getRequestType()) {
      case AbsTaskWrapper.D_FTP:
        return new FtpFileInfoThread(mTaskWrapper, new OnFileInfoCallback() {
          @Override public void onComplete(String url, CompleteInfo info) {
            mDownloader.updateTempFile();
            mDownloader.start();
          }

          @Override public void onFail(AbsEntity entity, BaseException e, boolean needRetry) {
            failDownload(e, needRetry);
            mDownloader.closeTimer();
          }
        });
      case AbsTaskWrapper.D_HTTP:
        return new HttpFileInfoThread(mTaskWrapper, new OnFileInfoCallback() {
          @Override public void onComplete(String url, CompleteInfo info) {
            mDownloader.updateTempFile();
            mDownloader.start();
          }

          @Override public void onFail(AbsEntity entity, BaseException e, boolean needRetry) {
            failDownload(e, needRetry);
            mDownloader.closeTimer();
          }
        });
    }
    return null;
  }
}