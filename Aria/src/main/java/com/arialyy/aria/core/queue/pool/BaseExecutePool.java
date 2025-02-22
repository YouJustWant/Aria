/*
 * Copyright (C) 2016 AriaLyy(DownloadUtil)
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

package com.arialyy.aria.core.queue.pool;

import android.text.TextUtils;
import com.arialyy.aria.core.AriaManager;
import com.arialyy.aria.core.inf.AbsTask;
import com.arialyy.aria.util.ALog;
import com.arialyy.aria.util.CommonUtil;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Created by lyy on 2016/8/15. 任务执行池，所有当前下载任务都该任务池中，默认下载大小为2
 */
public class BaseExecutePool<TASK extends AbsTask> implements IPool<TASK> {
  private final String TAG = "BaseExecutePool";
  private static final Object LOCK = new Object();
  final long TIME_OUT = 1000;
  ArrayBlockingQueue<TASK> mExecuteQueue;
  Map<String, TASK> mExecuteMap;
  int mSize;

  BaseExecutePool() {
    mSize = getMaxSize();
    mExecuteQueue = new ArrayBlockingQueue<>(mSize);
    mExecuteMap = new ConcurrentHashMap<>();
  }

  /**
   * 获取最大任务数配置
   *
   * @return {@link AriaManager#getDownloadConfig()} {@link AriaManager#getUploadConfig()}，如果不设置，默认返回2
   */
  protected int getMaxSize() {
    return 2;
  }

  /**
   * 获取所有正在执行的任务
   */
  public Map<String, TASK> getAllTask() {
    return mExecuteMap;
  }

  @Override public boolean putTask(TASK task) {
    synchronized (LOCK) {
      if (task == null) {
        ALog.e(TAG, "任务不能为空！！");
        return false;
      }
      if (mExecuteQueue.contains(task)) {
        ALog.e(TAG, "任务【" + task.getTaskName() + "】进入执行队列失败，原因：已经在执行队列中");
        return false;
      } else {
        if (mExecuteQueue.size() >= mSize) {
          if (pollFirstTask()) {
            return putNewTask(task);
          }
        } else {
          return putNewTask(task);
        }
      }
    }
    return false;
  }

  /**
   * 设置执行队列最大任务数
   *
   * @param maxNum 下载数
   */
  public void setMaxNum(int maxNum) {
    synchronized (LOCK) {
      try {
        ArrayBlockingQueue<TASK> temp = new ArrayBlockingQueue<>(maxNum);
        TASK task;
        while ((task = mExecuteQueue.poll(TIME_OUT, TimeUnit.MICROSECONDS)) != null) {
          temp.offer(task);
        }
        mExecuteQueue = temp;
        mSize = maxNum;
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }
  }

  /**
   * 添加新任务
   *
   * @param newTask 新任务
   */
  boolean putNewTask(TASK newTask) {
    synchronized (LOCK) {
      String url = newTask.getKey();
      boolean s = mExecuteQueue.offer(newTask);
      ALog.d(TAG, "任务【" + newTask.getTaskName() + "】进入执行队列" + (s ? "成功" : "失败"));
      if (s) {
        mExecuteMap.put(CommonUtil.keyToHashKey(url), newTask);
      }
      return s;
    }
  }

  /**
   * 队列满时，将移除下载队列中的第一个任务
   */
  boolean pollFirstTask() {
    synchronized (LOCK) {
      try {
        TASK oldTask = mExecuteQueue.poll(TIME_OUT, TimeUnit.MICROSECONDS);
        if (oldTask == null) {
          ALog.w(TAG, "移除任务失败，原因：任务为null");
          return false;
        }
        oldTask.stop();
        String key = CommonUtil.keyToHashKey(oldTask.getKey());
        mExecuteMap.remove(key);
      } catch (InterruptedException e) {
        e.printStackTrace();
        return false;
      }
      return true;
    }
  }

  @Override public TASK pollTask() {
    synchronized (LOCK) {
      try {
        TASK task;
        task = mExecuteQueue.poll(TIME_OUT, TimeUnit.MICROSECONDS);
        if (task != null) {
          String url = task.getKey();
          mExecuteMap.remove(CommonUtil.keyToHashKey(url));
        }
        return task;
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
      return null;
    }
  }

  @Override public TASK getTask(String key) {
    synchronized (LOCK) {
      if (TextUtils.isEmpty(key)) {
        ALog.e(TAG, "key 为null");
        return null;
      }
      return mExecuteMap.get(CommonUtil.keyToHashKey(key));
    }
  }

  @Override public boolean taskExits(String key) {
    return mExecuteMap.containsKey(CommonUtil.keyToHashKey(key));
  }

  @Override public boolean removeTask(TASK task) {
    synchronized (LOCK) {
      if (task == null) {
        ALog.e(TAG, "任务不能为空");
        return false;
      } else {
        return removeTask(task.getKey());
      }
    }
  }

  @Override public boolean removeTask(String key) {
    synchronized (LOCK) {
      if (TextUtils.isEmpty(key)) {
        ALog.e(TAG, "key 为null");
        return false;
      }
      String convertKey = CommonUtil.keyToHashKey(key);
      TASK task = mExecuteMap.get(convertKey);
      final int oldQueueSize = mExecuteQueue.size();
      boolean isSuccess = mExecuteQueue.remove(task);
      final int newQueueSize = mExecuteQueue.size();
      if (isSuccess && newQueueSize != oldQueueSize) {
        mExecuteMap.remove(convertKey);
        return true;
      }
      return false;
    }
  }

  @Override public int size() {
    return mExecuteQueue.size();
  }
}