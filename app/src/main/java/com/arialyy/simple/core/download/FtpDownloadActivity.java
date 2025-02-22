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
package com.arialyy.simple.core.download;

import android.arch.lifecycle.Observer;
import android.arch.lifecycle.ViewModelProviders;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.View;
import com.arialyy.annotations.Download;
import com.arialyy.aria.core.Aria;
import com.arialyy.aria.core.download.DownloadEntity;
import com.arialyy.aria.core.download.DownloadTask;
import com.arialyy.aria.core.download.FtpDownloadTarget;
import com.arialyy.aria.core.inf.IEntity;
import com.arialyy.aria.util.ALog;
import com.arialyy.aria.util.CommonUtil;
import com.arialyy.frame.util.show.L;
import com.arialyy.frame.util.show.T;
import com.arialyy.simple.R;
import com.arialyy.simple.base.BaseActivity;
import com.arialyy.simple.common.DirChooseDialog;
import com.arialyy.simple.common.ModifyUrlDialog;
import com.arialyy.simple.databinding.ActivityFtpDownloadBinding;
import com.arialyy.simple.util.AppUtil;
import java.io.File;
import java.io.IOException;

/**
 * Created by lyy on 2017/7/25.
 * Ftp下载
 */
public class FtpDownloadActivity extends BaseActivity<ActivityFtpDownloadBinding> {
  private String mUrl, mFilePath;
  private FtpDownloadModule mModule;
  private FtpDownloadTarget mTarget;

  @Override protected void init(Bundle savedInstanceState) {
    super.init(savedInstanceState);
    setTitle("FTP文件下载");
    Aria.download(this).register();
    mModule = ViewModelProviders.of(this).get(FtpDownloadModule.class);

    mModule.getFtpDownloadInfo(this).observe(this, new Observer<DownloadEntity>() {

      @Override public void onChanged(@Nullable DownloadEntity entity) {
        if (entity == null) {
          return;
        }
        mTarget = Aria.download(FtpDownloadActivity.this).loadFtp(entity.getUrl());
        if (mTarget.getTaskState() == IEntity.STATE_STOP) {
          getBinding().setStateStr(getString(R.string.resume));
        } else if (mTarget.isRunning()) {
          getBinding().setStateStr(getString(R.string.stop));
        }

        if (entity.getFileSize() != 0) {
          getBinding().setFileSize(CommonUtil.formatFileSize(entity.getFileSize()));
          getBinding().setProgress(entity.isComplete() ? 100
              : (int) (entity.getCurrentProgress() * 100 / entity.getFileSize()));
        }
        getBinding().setUrl(entity.getUrl());
        getBinding().setFilePath(entity.getFilePath());
        mUrl = entity.getUrl();
        mFilePath = entity.getFilePath();
      }
    });
    getBinding().setViewModel(this);
    try {
      getBinding().codeView.setSource(AppUtil.getHelpCode(this, "FtpDownload.java"));
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public void onClick(View view) {
    switch (view.getId()) {
      case R.id.start:
        if (mTarget.isRunning()) {
          getBinding().setStateStr(getString(R.string.resume));
          Aria.download(this).loadFtp(mUrl).stop();
        } else {
          getBinding().setStateStr(getString(R.string.stop));
          Aria.download(this).loadFtp(mUrl).login("N0rI", "0qcK")
              .setFilePath(mFilePath, true)
              .start();
        }
        break;
      case R.id.cancel:
        Aria.download(this).loadFtp(mUrl).cancel();
        break;
    }
  }

  public void chooseUrl() {
    ModifyUrlDialog dialog =
        new ModifyUrlDialog(this, getString(R.string.modify_url_dialog_title), mUrl);
    dialog.show(getSupportFragmentManager(), "ModifyUrlDialog");
  }

  public void chooseFilePath() {
    DirChooseDialog dirChooseDialog = new DirChooseDialog(this);
    dirChooseDialog.show(getSupportFragmentManager(), "DirChooseDialog");
  }

  @Download.onPre() protected void onPre(DownloadTask task) {
    L.d(TAG, "ftp pre");
  }

  @Download.onTaskPre() protected void onTaskPre(DownloadTask task) {
    L.d(TAG, "ftp task pre");
    getBinding().setFileSize(task.getConvertFileSize());
  }

  @Download.onTaskStart() void taskStart(DownloadTask task) {
    L.d(TAG, "ftp task start");
  }

  @Download.onTaskRunning() protected void running(DownloadTask task) {
    getBinding().setProgress(task.getPercent());
    getBinding().setSpeed(task.getConvertSpeed());
  }

  @Download.onTaskResume() void taskResume(DownloadTask task) {
    L.d(TAG, "ftp task resume");
  }

  @Download.onTaskStop() void taskStop(DownloadTask task) {
    L.d(TAG, "ftp task stop");
    getBinding().setSpeed("");
  }

  @Download.onTaskCancel() void taskCancel(DownloadTask task) {
    getBinding().setSpeed("");
    getBinding().setProgress(0);
  }

  @Download.onTaskFail() void taskFail(DownloadTask task) {
    L.d(TAG, "ftp task fail");
  }

  @Download.onTaskComplete() void taskComplete(DownloadTask task) {
    getBinding().setSpeed("");
    getBinding().setProgress(100);
    Log.d(TAG, "md5 ==> " + CommonUtil.getFileMD5(new File(task.getDownloadPath())));
    T.showShort(this, "文件：" + task.getEntity().getFileName() + "，下载完成");
  }

  @Override protected int setLayoutId() {
    return R.layout.activity_ftp_download;
  }

  @Override protected void dataCallback(int result, Object data) {
    super.dataCallback(result, data);
    if (result == ModifyUrlDialog.MODIFY_URL_DIALOG_RESULT) {
      mModule.uploadUrl(this, String.valueOf(data));
    }else if (result == DirChooseDialog.DIR_CHOOSE_DIALOG_RESULT) {
      mModule.updateFilePath(this, String.valueOf(data));
    }
  }

}
