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
package com.arialyy.aria.core.download;

import android.os.Handler;
import com.arialyy.aria.core.common.BaseListener;
import com.arialyy.aria.core.common.RecordHandler;
import com.arialyy.aria.core.download.group.IDGroupListener;
import com.arialyy.aria.core.inf.GroupSendParams;
import com.arialyy.aria.core.inf.IEntity;
import com.arialyy.aria.core.inf.TaskSchedulerType;
import com.arialyy.aria.core.scheduler.ISchedulers;
import com.arialyy.aria.exception.BaseException;
import com.arialyy.aria.util.ALog;
import com.arialyy.aria.util.CommonUtil;
import com.arialyy.aria.util.ErrorHelp;
import com.arialyy.aria.util.RecordUtil;

/**
 * Created by Aria.Lao on 2017/7/20. 任务组下载事件
 */
class DownloadGroupListener
    extends BaseListener<DownloadGroupEntity, DGTaskWrapper, DownloadGroupTask>
    implements IDGroupListener {
  private GroupSendParams<DownloadGroupTask, DownloadEntity> mSeedEntity;

  DownloadGroupListener(DownloadGroupTask task, Handler outHandler) {
    super(task, outHandler);
    mSeedEntity = new GroupSendParams<>();
    mSeedEntity.groupTask = task;
  }

  @Override
  public void onSubPre(DownloadEntity subEntity) {
    saveSubState(IEntity.STATE_PRE, subEntity);
    sendInState2Target(ISchedulers.SUB_PRE, subEntity);
  }

  @Override
  public void supportBreakpoint(boolean support, DownloadEntity subEntity) {

  }

  @Override
  public void onSubStart(DownloadEntity subEntity) {
    saveSubState(IEntity.STATE_RUNNING, subEntity);
    sendInState2Target(ISchedulers.SUB_START, subEntity);
  }

  @Override
  public void onSubStop(DownloadEntity subEntity) {
    saveSubState(IEntity.STATE_STOP, subEntity);
    saveCurrentLocation();
    sendInState2Target(ISchedulers.SUB_STOP, subEntity);
  }

  @Override
  public void onSubComplete(DownloadEntity subEntity) {
    saveSubState(IEntity.STATE_COMPLETE, subEntity);
    saveCurrentLocation();
    sendInState2Target(ISchedulers.SUB_COMPLETE, subEntity);
  }

  @Override
  public void onSubFail(DownloadEntity subEntity, BaseException e) {
    saveSubState(IEntity.STATE_FAIL, subEntity);
    saveCurrentLocation();
    sendInState2Target(ISchedulers.SUB_FAIL, subEntity);
    if (e != null) {
      e.printStackTrace();
      ErrorHelp.saveError(e.getTag(), "", ALog.getExceptionString(e));
    }
  }

  @Override
  public void onSubCancel(DownloadEntity subEntity) {
    saveSubState(IEntity.STATE_CANCEL, subEntity);
    saveCurrentLocation();
    sendInState2Target(ISchedulers.SUB_CANCEL, subEntity);
  }

  @Override
  public void onSubRunning(DownloadEntity subEntity) {
    if (System.currentTimeMillis() - mLastSaveTime >= RUN_SAVE_INTERVAL) {
      saveSubState(IEntity.STATE_RUNNING, subEntity);
      mLastSaveTime = System.currentTimeMillis();
    }
    sendInState2Target(ISchedulers.SUB_RUNNING, subEntity);
  }

  /**
   * 将任务状态发送给下载器
   *
   * @param state {@link ISchedulers#START}
   */
  private void sendInState2Target(int state, DownloadEntity subEntity) {
    if (outHandler.get() != null) {
      mSeedEntity.entity = subEntity;
      outHandler.get().obtainMessage(state, ISchedulers.IS_SUB_TASK, 0, mSeedEntity).sendToTarget();
    }
  }

  private void saveSubState(int state, DownloadEntity subEntity) {
    subEntity.setState(state);
    if (state == IEntity.STATE_STOP) {
      subEntity.setStopTime(System.currentTimeMillis());
    } else if (state == IEntity.STATE_COMPLETE) {
      subEntity.setComplete(true);
      subEntity.setCompleteTime(System.currentTimeMillis());
      subEntity.setCurrentProgress(subEntity.getFileSize());
      subEntity.setPercent(100);
      subEntity.setConvertSpeed("0kb/s");
      subEntity.setSpeed(0);
      ALog.i(TAG, String.format("任务【%s】完成，将删除线程任务记录", mEntity.getKey()));
      RecordUtil.delTaskRecord(subEntity.getFilePath(), RecordHandler.TYPE_DOWNLOAD, false, false);
    }
    subEntity.update();
  }

  private void saveCurrentLocation() {
    if (mEntity.getSubEntities() == null || mEntity.getSubEntities().isEmpty()) {
      ALog.w(TAG, "保存进度失败，子任务为null");
      return;
    }
    long location = 0;
    for (DownloadEntity e : mEntity.getSubEntities()) {
      location += e.getCurrentProgress();
    }
    if (location > mEntity.getFileSize()) {
      location = mEntity.getFileSize();
    }
    mEntity.setCurrentProgress(location);
    mEntity.update();
  }

  @Override
  public void onPostPre(long fileSize) {
    mEntity.setFileSize(fileSize);
    mEntity.setConvertFileSize(CommonUtil.formatFileSize(fileSize));
    saveData(IEntity.STATE_POST_PRE, -1);
    sendInState2Target(ISchedulers.POST_PRE);
  }

  @Override
  public void supportBreakpoint(boolean support) {

  }

  @Override protected void handleCancel() {
    int sType = getTask().getSchedulerType();
    if (sType == TaskSchedulerType.TYPE_CANCEL_AND_NOT_NOTIFY) {
      mEntity.setComplete(false);
      mEntity.setState(IEntity.STATE_WAIT);
      RecordUtil.delGroupTaskRecord(mEntity, mTaskWrapper.isRemoveFile(), false);
    } else {
      RecordUtil.delGroupTaskRecord(mEntity, mTaskWrapper.isRemoveFile(), true);
    }
  }
}
