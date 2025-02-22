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
import com.arialyy.aria.core.inf.AbsTask;
import com.arialyy.aria.util.ALog;
import com.arialyy.aria.util.CommonUtil;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * Created by lyy on 2016/8/14. 任务缓存池，所有下载任务最先缓存在这个池中
 */
public class BaseCachePool<TASK extends AbsTask> implements IPool<TASK> {
  private static final String TAG = "BaseCachePool";
  private static final int MAX_NUM = Integer.MAX_VALUE;  //最大下载任务数
  private static final long TIME_OUT = 1000;
  private static final Object LOCK = new Object();
  private Map<String, TASK> mCacheMap;
  private LinkedBlockingQueue<TASK> mCacheQueue;

  BaseCachePool() {
    mCacheQueue = new LinkedBlockingQueue<>(MAX_NUM);
    mCacheMap = new ConcurrentHashMap<>();
  }

  /**
   * 获取被缓存的任务
   */
  public Map<String, TASK> getAllTask() {
    return mCacheMap;
  }

  /**
   * 清除所有缓存的任务
   */
  public void clear() {
    for (String key : mCacheMap.keySet()) {
      TASK task = mCacheMap.get(key);
      mCacheQueue.remove(task);
      mCacheMap.remove(key);
    }
  }

  /**
   * 将任务放在队首
   */
  public boolean putTaskToFirst(TASK task) {
    if (mCacheQueue.isEmpty()) {
      return putTask(task);
    } else {
      Set<TASK> temps = new LinkedHashSet<>();
      temps.add(task);
      for (int i = 0, len = size(); i < len; i++) {
        TASK temp = pollTask();
        temps.add(temp);
      }
      for (TASK t : temps) {
        putTask(t);
      }
      return true;
    }
  }

  @Override public boolean putTask(TASK task) {
    synchronized (LOCK) {
      if (task == null) {
        ALog.e(TAG, "任务不能为空！！");
        return false;
      }
      String key = task.getKey();
      if (mCacheQueue.contains(task)) {
        ALog.w(TAG, "任务【" + task.getTaskName() + "】进入缓存队列失败，原因：已经在缓存队列中");
        return false;
      } else {
        boolean s = mCacheQueue.offer(task);
        ALog.d(TAG, "任务【" + task.getTaskName() + "】进入缓存队列" + (s ? "成功" : "失败"));
        if (s) {
          mCacheMap.put(CommonUtil.keyToHashKey(key), task);
        }
        return s;
      }
    }
  }

  @Override public TASK pollTask() {
    synchronized (LOCK) {
      try {
        TASK task;
        task = mCacheQueue.poll(TIME_OUT, TimeUnit.MICROSECONDS);
        if (task != null) {
          String url = task.getKey();
          mCacheMap.remove(CommonUtil.keyToHashKey(url));
        }
        return task;
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }
    return null;
  }

  @Override public TASK getTask(String key) {
    synchronized (LOCK) {
      if (TextUtils.isEmpty(key)) {
        ALog.e(TAG, "key 为null");
        return null;
      }
      return mCacheMap.get(CommonUtil.keyToHashKey(key));
    }
  }

  @Override public boolean taskExits(String key) {
    return mCacheMap.containsKey(CommonUtil.keyToHashKey(key));
  }

  @Override public boolean removeTask(TASK task) {
    synchronized (LOCK) {
      if (task == null) {
        ALog.e(TAG, "任务不能为空");
        return false;
      } else {
        String key = CommonUtil.keyToHashKey(task.getKey());
        mCacheMap.remove(key);
        return mCacheQueue.remove(task);
      }
    }
  }

  @Override public boolean removeTask(String key) {
    synchronized (LOCK) {
      if (TextUtils.isEmpty(key)) {
        ALog.e(TAG, "请传入有效的下载链接");
        return false;
      }
      String temp = CommonUtil.keyToHashKey(key);
      TASK task = mCacheMap.get(temp);
      mCacheMap.remove(temp);
      return mCacheQueue.remove(task);
    }
  }

  @Override public int size() {
    return mCacheQueue.size();
  }
}