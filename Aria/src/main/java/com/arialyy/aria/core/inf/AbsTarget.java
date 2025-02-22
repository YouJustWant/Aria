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
package com.arialyy.aria.core.inf;

import android.os.Handler;
import android.os.Looper;
import android.support.annotation.CheckResult;
import android.text.TextUtils;
import com.arialyy.aria.core.command.CancelCmd;
import com.arialyy.aria.core.command.NormalCmdFactory;
import com.arialyy.aria.core.download.DGTaskWrapper;
import com.arialyy.aria.core.download.DTaskWrapper;
import com.arialyy.aria.core.download.DownloadGroupEntity;
import com.arialyy.aria.core.event.EventMsgUtil;
import com.arialyy.aria.core.manager.TaskWrapperManager;
import com.arialyy.aria.core.scheduler.DownloadGroupSchedulers;
import com.arialyy.aria.core.scheduler.DownloadSchedulers;
import com.arialyy.aria.core.scheduler.ISchedulers;
import com.arialyy.aria.core.scheduler.UploadSchedulers;
import com.arialyy.aria.core.upload.UTaskWrapper;
import com.arialyy.aria.util.ALog;
import com.arialyy.aria.util.CommonUtil;
import com.arialyy.aria.util.RecordUtil;

/**
 * Created by AriaL on 2017/7/3.
 */
public abstract class AbsTarget<TARGET extends AbsTarget> implements ITargetHandler {

  protected String TAG;
  private AbsEntity mEntity;
  private AbsTaskWrapper mTaskWrapper;
  private String mTargetName;

  protected AbsTarget() {
    TAG = CommonUtil.getClassName(this);
  }

  public void setTaskWrapper(AbsTaskWrapper wrapper) {
    mTaskWrapper = wrapper;
    mEntity = wrapper.getEntity();
  }

  /**
   * 重置状态，将任务状态设置为未开始状态
   * 注意：如果在后续方法调用链中没有调用 {@link #start()}、{@link #stop()}、{@link #cancel()}、{@link #resume()}
   * 等操作任务的方法，那么你需要调用{@link #save()}才能将修改保存到数据库
   */
  @CheckResult(suggest = "after use #start()、#stop()、#cancel()、#resume()、#save()?")
  public TARGET resetState() {
    mTaskWrapper.getEntity().setState(IEntity.STATE_WAIT);
    mTaskWrapper.setRefreshInfo(true);
    return (TARGET) this;
  }

  /**
   * 删除记录，如果任务正在执行，则会删除正在下载的任务
   */
  public void removeRecord() {
    if (isRunning()) {
      ALog.d("AbsTarget", "任务正在下载，即将删除任务");
      cancel();
    } else {
      if (mEntity instanceof AbsNormalEntity) {
        RecordUtil.delTaskRecord((AbsNormalEntity) mEntity, mTaskWrapper.isRemoveFile());
      } else if (mEntity instanceof DownloadGroupEntity) {
        RecordUtil.delGroupTaskRecord(((DownloadGroupEntity) mEntity), mTaskWrapper.isRemoveFile(),
            true);
      }
      TaskWrapperManager.getInstance().removeTaskWrapper(mEntity.getKey());
    }
  }

  public AbsEntity getEntity() {
    return mEntity;
  }

  public String getTargetName() {
    return mTargetName;
  }

  public void setTargetName(String mTargetName) {
    this.mTargetName = mTargetName;
  }

  /**
   * 获取任务实体
   */
  public AbsTaskWrapper getTaskWrapper() {
    return mTaskWrapper;
  }

  /**
   * 获取任务进度，如果任务存在，则返回当前进度
   *
   * @return 该任务进度
   */
  public long getCurrentProgress() {
    return mEntity == null ? -1 : mEntity.getCurrentProgress();
  }

  /**
   * 获取任务文件大小
   *
   * @return 文件大小
   */
  public long getFileSize() {
    return mEntity == null ? 0 : mEntity.getFileSize();
  }

  /**
   * 获取单位转换后的文件大小
   *
   * @return 文件大小{@code xxx mb}
   */
  public String getConvertFileSize() {
    return mEntity == null ? "0b" : CommonUtil.formatFileSize(mEntity.getFileSize());
  }

  /**
   * 设置扩展字段，用来保存你的其它数据，如果你的数据比较多，你可以把你的数据转换为JSON字符串，然后再存到Aria中
   * 注意：如果在后续方法调用链中没有调用 {@link #start()}、{@link #stop()}、{@link #cancel()}、{@link #resume()}
   * 等操作任务的方法，那么你需要调用{@link #save()}才能将修改保存到数据库
   *
   * @param str 扩展数据
   */
  @CheckResult(suggest = "after use #start()、#stop()、#cancel()、#resume()、#save()?")
  public TARGET setExtendField(String str) {
    if (TextUtils.isEmpty(str)) return (TARGET) this;
    if (TextUtils.isEmpty(mEntity.getStr()) || !mEntity.getStr().equals(str)) {
      mEntity.setStr(str);
    } else {
      ALog.e(TAG, "设置扩展字段失败，扩展字段为一致");
    }

    return (TARGET) this;
  }

  /**
   * 获取存放的扩展字段
   * 设置扩展字段{@link #setExtendField(String)}
   */
  public String getExtendField() {
    return mEntity.getStr();
  }

  /**
   * 获取任务状态
   *
   * @return {@link IEntity}
   */
  public int getTaskState() {
    return mEntity.getState();
  }

  /**
   * 获取任务进度百分比
   *
   * @return 返回任务进度
   */
  public int getPercent() {
    if (mEntity == null) {
      ALog.e("AbsTarget", "下载管理器中没有该任务");
      return 0;
    }
    if (mEntity.getFileSize() != 0) {
      return (int) (mEntity.getCurrentProgress() * 100 / mEntity.getFileSize());
    }
    return 0;
  }

  /**
   * 检查实体是否合法，如果实体合法，将保存实体到数据库，或更新数据库中的实体对象
   *
   * @return {@code true} 合法
   */
  protected abstract boolean checkEntity();

  protected int checkTaskType() {
    int taskType = 0;
    if (mTaskWrapper instanceof DTaskWrapper) {
      taskType = ITask.DOWNLOAD;
    } else if (mTaskWrapper instanceof DGTaskWrapper) {
      taskType = ITask.DOWNLOAD_GROUP;
    } else if (mTaskWrapper instanceof UTaskWrapper) {
      taskType = ITask.UPLOAD;
    }
    return taskType;
  }

  /**
   * 如果检查实体失败，将错误回调
   */
  protected boolean checkConfig() {
    boolean b = checkEntity();
    ISchedulers schedulers = getScheduler();
    if (!b && schedulers != null) {

      new Handler(Looper.getMainLooper(), schedulers).obtainMessage(ISchedulers.CHECK_FAIL,
          checkTaskType(), -1, null).sendToTarget();
    }

    return b;
  }

  private ISchedulers getScheduler() {
    int type = getTargetType();
    switch (type) {
      case D_FTP:
      case D_HTTP:
        return DownloadSchedulers.getInstance();
      case U_FTP:
      case U_HTTP:
        return UploadSchedulers.getInstance();
      case GROUP_HTTP:
      case GROUP_FTP_DIR:
        return DownloadGroupSchedulers.getInstance();
    }
    return null;
  }

  /**
   * 保存修改
   */
  @Override public void save() {
    if (!checkConfig()) {
      ALog.e(TAG, "保存修改失败");
    } else {
      ALog.i(TAG, "保存成功");
    }
  }

  /**
   * 任务是否在执行
   *
   * @return {@code true} 任务正在执行
   */
  public abstract boolean isRunning();

  /**
   * 任务是否存在
   *
   * @return {@code true} 任务存在
   */
  public abstract boolean taskExists();

  /**
   * 设置target类型
   *
   * @return {@link #D_HTTP}、{@link #U_HTTP}、{@link #D_FTP}、{@link #U_FTP}、{@link
   * #GROUP_HTTP}、{@link #GROUP_FTP_DIR}
   */
  public abstract int getTargetType();

  /**
   * 添加任务
   */
  @Override public void add() {
    if (checkConfig()) {
      EventMsgUtil.getDefault()
          .post(CommonUtil.createNormalCmd(mTaskWrapper, NormalCmdFactory.TASK_CREATE,
              checkTaskType()));
    }
  }

  /**
   * 开始任务
   */
  @Override public void start() {
    if (checkConfig()) {
      EventMsgUtil.getDefault()
          .post(CommonUtil.createNormalCmd(mTaskWrapper, NormalCmdFactory.TASK_START,
              checkTaskType()));
    }
  }

  /**
   * 停止任务
   *
   * @see #stop()
   */
  @Deprecated public void pause() {
    stop();
  }

  @Override public void stop() {
    if (checkConfig()) {
      EventMsgUtil.getDefault()
          .post(CommonUtil.createNormalCmd(mTaskWrapper, NormalCmdFactory.TASK_STOP,
              checkTaskType()));
    }
  }

  /**
   * 恢复任务
   */
  @Override public void resume() {
    if (checkConfig()) {
      EventMsgUtil.getDefault()
          .post(CommonUtil.createNormalCmd(mTaskWrapper, NormalCmdFactory.TASK_START,
              checkTaskType()));
    }
  }

  /**
   * 删除任务
   */
  @Override public void cancel() {
    if (checkConfig()) {
      EventMsgUtil.getDefault()
          .post(CommonUtil.createNormalCmd(mTaskWrapper, NormalCmdFactory.TASK_CANCEL,
              checkTaskType()));
    }
  }

  /**
   * 任务重试
   */
  @Override public void reTry() {
    if (checkConfig()) {
      int taskType = checkTaskType();
      EventMsgUtil.getDefault()
          .post(CommonUtil.createNormalCmd(mTaskWrapper, NormalCmdFactory.TASK_STOP, taskType));
      EventMsgUtil.getDefault()
          .post(CommonUtil.createNormalCmd(mTaskWrapper, NormalCmdFactory.TASK_START, taskType));
    }
  }

  /**
   * 删除任务
   *
   * @param removeFile {@code true} 不仅删除任务数据库记录，还会删除已经删除完成的文件
   * {@code false}如果任务已经完成，只删除任务数据库记录，
   */
  @Override public void cancel(boolean removeFile) {
    if (checkConfig()) {
      CancelCmd cancelCmd =
          (CancelCmd) CommonUtil.createNormalCmd(mTaskWrapper, NormalCmdFactory.TASK_CANCEL,
              checkTaskType());
      cancelCmd.removeFile = removeFile;
      EventMsgUtil.getDefault().post(cancelCmd);
    }
  }

  /**
   * 重新下载
   */
  @Override public void reStart() {
    if (checkConfig()) {
      EventMsgUtil.getDefault()
          .post(CommonUtil.createNormalCmd(mTaskWrapper, NormalCmdFactory.TASK_RESTART,
              checkTaskType()));
    }
  }
}
