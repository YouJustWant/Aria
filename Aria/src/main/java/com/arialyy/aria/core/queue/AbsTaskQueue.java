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

package com.arialyy.aria.core.queue;

import com.arialyy.aria.core.download.DownloadGroupTask;
import com.arialyy.aria.core.download.DownloadTask;
import com.arialyy.aria.core.inf.AbsTask;
import com.arialyy.aria.core.inf.AbsTaskWrapper;
import com.arialyy.aria.core.inf.IEntity;
import com.arialyy.aria.core.inf.TaskSchedulerType;
import com.arialyy.aria.core.manager.TaskWrapperManager;
import com.arialyy.aria.core.manager.ThreadTaskManager;
import com.arialyy.aria.core.queue.pool.BaseCachePool;
import com.arialyy.aria.core.queue.pool.BaseExecutePool;
import com.arialyy.aria.core.queue.pool.DGLoadSharePool;
import com.arialyy.aria.core.queue.pool.DLoadSharePool;
import com.arialyy.aria.core.queue.pool.UploadSharePool;
import com.arialyy.aria.core.upload.UploadTask;
import com.arialyy.aria.util.ALog;

/**
 * Created by lyy on 2017/2/23. 任务队列
 */
public abstract class AbsTaskQueue<TASK extends AbsTask, TASK_WRAPPER extends AbsTaskWrapper>
    implements ITaskQueue<TASK, TASK_WRAPPER> {
  final int TYPE_D_QUEUE = 1;
  final int TYPE_DG_QUEUE = 2;
  final int TYPE_U_QUEUE = 3;

  private final String TAG = "AbsTaskQueue";
  BaseCachePool<TASK> mCachePool;
  BaseExecutePool<TASK> mExecutePool;

  AbsTaskQueue() {
    switch (getQueueType()) {
      case TYPE_D_QUEUE:
        mCachePool = DLoadSharePool.getInstance().cachePool;
        mExecutePool = DLoadSharePool.getInstance().executePool;
        break;
      case TYPE_DG_QUEUE:
        mCachePool = DGLoadSharePool.getInstance().cachePool;
        mExecutePool = DGLoadSharePool.getInstance().executePool;
        break;
      case TYPE_U_QUEUE:
        mCachePool = UploadSharePool.getInstance().cachePool;
        mExecutePool = UploadSharePool.getInstance().executePool;
        break;
    }
  }

  abstract int getQueueType();

  @Override public boolean taskIsRunning(String key) {
    TASK task = mExecutePool.getTask(key);
    if (task == null && ThreadTaskManager.getInstance().taskIsRunning(key)) {
      ThreadTaskManager.getInstance().removeTaskThread(key);
    }
    return task != null && task.isRunning() && taskExists(key);
  }

  /**
   * 恢复任务 如果执行队列任务未满，则直接启动任务。 如果执行队列已经满了，则暂停执行队列队首任务，并恢复指定任务
   *
   * @param task 需要恢复的任务
   */
  @Override public void resumeTask(TASK task) {
    if (task == null) {
      ALog.w(TAG, "resume task fail, task is null");
      return;
    }
    if (mExecutePool.size() >= getMaxTaskNum()) {
      task.getTaskWrapper().getEntity().setState(IEntity.STATE_WAIT);
      mCachePool.putTaskToFirst(task);
      stopTask(mExecutePool.pollTask());
    } else {
      startTask(task);
    }
  }

  /**
   * 停止所有任务
   */
  @Override public void stopAllTask() {
    for (String key : mExecutePool.getAllTask().keySet()) {
      TASK task = mExecutePool.getAllTask().get(key);
      if (task != null) {
        int state = task.getState();
        if (task.isRunning() || (state != IEntity.STATE_COMPLETE
            && state != IEntity.STATE_CANCEL)) {
          task.stop(TaskSchedulerType.TYPE_STOP_NOT_NEXT);
        }
      }
    }

    for (String key : mCachePool.getAllTask().keySet()) {
      TASK task = mCachePool.getAllTask().get(key);
      if (task != null) {
        task.stop(TaskSchedulerType.TYPE_STOP_NOT_NEXT);
      }
    }
    ThreadTaskManager.getInstance().removeAllThreadTask();
    mCachePool.clear();
  }

  /**
   * 获取配置文件旧的最大任务数
   */
  public abstract int getOldMaxNum();

  /**
   * 获取任务执行池
   */
  public BaseExecutePool getExecutePool() {
    return mExecutePool;
  }

  /**
   * 获取缓存池
   */
  public BaseCachePool getCachePool() {
    return mCachePool;
  }

  /**
   * 获取缓存任务数
   *
   * @return 获取缓存的任务数
   */
  @Override public int getCurrentCachePoolNum() {
    return mCachePool.size();
  }

  /**
   * 获取执行池中的任务数量
   *
   * @return 当前正在执行的任务数
   */
  @Override public int getCurrentExePoolNum() {
    return mExecutePool.size();
  }

  @Override public void setMaxTaskNum(int downloadNum) {
    int oldMaxSize = getOldMaxNum();
    int diff = downloadNum - oldMaxSize;
    if (oldMaxSize == downloadNum) {
      ALog.w(TAG, "设置的下载任务数和配置文件的下载任务数一直，跳过");
      return;
    }
    //设置的任务数小于配置任务数
    if (diff <= -1 && mExecutePool.size() >= oldMaxSize) {
      for (int i = 0, len = Math.abs(diff); i < len; i++) {
        TASK eTask = mExecutePool.pollTask();
        if (eTask != null) {
          stopTask(eTask);
        }
      }
    }
    mExecutePool.setMaxNum(downloadNum);
    if (diff >= 1) {
      for (int i = 0; i < diff; i++) {
        TASK nextTask = getNextTask();
        if (nextTask != null && nextTask.getState() == IEntity.STATE_WAIT) {
          startTask(nextTask);
        }
      }
    }
  }

  @Override public TASK createTask(TASK_WRAPPER wrapper) {
    TaskWrapperManager.getInstance().addTaskWrapper(wrapper);
    return null;
  }

  @Override public boolean taskExists(String key) {
    return getTask(key) != null;
  }

  @Override public TASK getTask(String key) {
    TASK task = mExecutePool.getTask(key);
    if (task == null) {
      task = mCachePool.getTask(key);
    }
    return task;
  }

  /**
   * 添加等待中的任务
   *
   * @param task {@link DownloadTask}、{@link UploadTask}、{@link DownloadGroupTask}
   */
  void addTask(TASK task) {
    if (task == null) {
      ALog.w(TAG, "add task fail, task is null");
      return;
    }
    if (!mCachePool.taskExits(task.getKey())) {
      mCachePool.putTask(task);
    }
  }

  @Override public void startTask(TASK task) {
    if (task == null) {
      ALog.w(TAG, "start fail, task is null");
    }
    if (mExecutePool.taskExits(task.getKey())) {
      ALog.w(TAG, String.format("任务【%s】执行中", task.getKey()));
      return;
    }
    mCachePool.removeTask(task);
    mExecutePool.putTask(task);
    task.getTaskWrapper().getEntity().setFailNum(0);
    task.start();
  }

  @Override public void stopTask(TASK task) {
    if (task == null) {
      ALog.w(TAG, "stop fail, task is null");
      return;
    }
    int state = task.getState();
    boolean canStop = false;
    switch (state) {
      case IEntity.STATE_WAIT:
        mCachePool.removeTask(task);
        canStop = true;
        break;
      case IEntity.STATE_POST_PRE:
      case IEntity.STATE_PRE:
      case IEntity.STATE_RUNNING:
        mExecutePool.removeTask(task);
        canStop = true;
        break;
      case IEntity.STATE_STOP:
      case IEntity.STATE_OTHER:
      case IEntity.STATE_FAIL:
        ALog.w(TAG, String.format("停止任务【%s】失败，原因：已停止", task.getTaskName()));
        if (taskIsRunning(task.getKey())) {
          removeTaskFormQueue(task.getKey());
          if (ThreadTaskManager.getInstance().taskIsRunning(task.getKey())) {
            ThreadTaskManager.getInstance().removeTaskThread(task.getKey());
          }
        }
        break;
      case IEntity.STATE_CANCEL:
        ALog.w(TAG, String.format("停止任务【%s】失败，原因：任务已删除", task.getTaskName()));
        break;
      case IEntity.STATE_COMPLETE:
        ALog.w(TAG, String.format("停止任务【%s】失败，原因：已完成", task.getTaskName()));
        break;
    }

    if (canStop) {
      task.stop();
    }
  }

  @Override public void removeTaskFormQueue(String key) {
    TASK task = mExecutePool.getTask(key);
    if (task != null) {
      ALog.d(TAG, String.format("从执行池删除任务【%s】%s", task.getTaskName(),
          (mExecutePool.removeTask(task) ? "成功" : "失败")));
    }
    task = mCachePool.getTask(key);
    if (task != null) {
      ALog.d(TAG, String.format("从缓存池删除任务【%s】%s", task.getTaskName(),
          (mCachePool.removeTask(task) ? "成功" : "失败")));
    }
  }

  @Override public void reTryStart(TASK task) {
    if (task == null) {
      ALog.e(TAG, "reTry fail, task is null");
      return;
    }

    int state = task.getState();
    switch (state) {
      case IEntity.STATE_POST_PRE:
      case IEntity.STATE_PRE:
      case IEntity.STATE_RUNNING:
        ALog.w(TAG, String.format("任务【%s】没有停止，即将重新下载", task.getTaskName()));
        task.stop(TaskSchedulerType.TYPE_STOP_NOT_NEXT);
        task.start();
        break;
      case IEntity.STATE_WAIT:
      case IEntity.STATE_STOP:
      case IEntity.STATE_OTHER:
      case IEntity.STATE_FAIL:
        task.start();
        break;
      case IEntity.STATE_CANCEL:
        ALog.e(TAG, String.format("任务【%s】重试失败，原因：任务没已删除", task.getTaskName()));
        break;
      case IEntity.STATE_COMPLETE:
        ALog.e(TAG, String.format("任务【%s】重试失败，原因：已完成", task.getTaskName()));
        break;
    }
  }

  @Override public void cancelTask(TASK task) {
    task.cancel();
  }

  @Override public TASK getNextTask() {
    return mCachePool.pollTask();
  }
}
