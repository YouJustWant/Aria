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

import com.arialyy.aria.core.config.Configuration;
import com.arialyy.aria.util.ALog;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 组合任务队列，该队列生命周期和{@link AbsGroupUtil}生命周期一致
 */
class SimpleSubQueue implements ISubQueue<SubDLoadUtil> {
  private static final String TAG = "SimpleSubQueue";
  /**
   * 缓存下载器
   */
  private Map<String, SubDLoadUtil> mCache = new LinkedHashMap<>();
  /**
   * 执行中的下载器
   */
  private Map<String, SubDLoadUtil> mExec = new LinkedHashMap<>();

  /**
   * 最大执行任务数
   */
  private int mExecSize;

  /**
   * 是否停止任务任务
   */
  private boolean isStopAll = false;

  private SimpleSubQueue() {
    mExecSize = Configuration.getInstance().dGroupCfg.getSubMaxTaskNum();
  }

  static SimpleSubQueue newInstance() {
    return new SimpleSubQueue();
  }

  Map<String, SubDLoadUtil> getExec() {
    return mExec;
  }

  /**
   * 获取缓存队列大小
   */
  int getCacheSize() {
    return mCache.size();
  }

  boolean isStopAll() {
    return isStopAll;
  }

  @Override public void addTask(SubDLoadUtil fileer) {
    mCache.put(fileer.getKey(), fileer);
  }

  @Override public void startTask(SubDLoadUtil fileer) {
    if (mExec.size() < mExecSize) {
      mCache.remove(fileer.getKey());
      mExec.put(fileer.getKey(), fileer);
      ALog.d(TAG, String.format("开始执行子任务：%s", fileer.getEntity().getFileName()));
      fileer.start();
    } else {
      ALog.d(TAG, String.format("执行队列已满，任务进入缓存器中，key: %s", fileer.getKey()));
      addTask(fileer);
    }
  }

  @Override public void stopTask(SubDLoadUtil fileer) {
    fileer.stop();
    mExec.remove(fileer.getKey());
  }

  @Override public void stopAllTask() {
    isStopAll = true;
    ALog.d(TAG, "停止组合任务");
    Set<String> keys = mExec.keySet();
    for (String key : keys) {
      SubDLoadUtil loader = mExec.get(key);
      if (loader != null) {
        ALog.d(TAG, String.format("停止子任务：%s", loader.getEntity().getFileName()));
        loader.stop();
      }
    }
  }

  @Override public void modifyMaxExecNum(int num) {
    if (num < 1) {
      ALog.e(TAG, String.format("修改组合任务子任务队列数失败，num: %s", num));
      return;
    }
    if (num == mExecSize) {
      ALog.i(TAG, String.format("忽略此次修改，oldSize: %s, num: %s", mExecSize, num));
      return;
    }
    int oldSize = mExecSize;
    mExecSize = num;
    int diff = Math.abs(oldSize - num);

    if (oldSize < num) { // 处理队列变小的情况，该情况下将停止队尾任务，并将这些任务添加到缓存队列中
      if (mExec.size() > num) {
        Set<String> keys = mExec.keySet();
        List<SubDLoadUtil> caches = new ArrayList<>();
        int i = 0;
        for (String key : keys) {
          if (i > num) {
            caches.add(mExec.get(key));
          }
          i++;
        }
        Collection<SubDLoadUtil> temp = mCache.values();
        mCache.clear();
        for (SubDLoadUtil cache : caches) {
          addTask(cache);
        }
        for (SubDLoadUtil t : temp) {
          addTask(t);
        }
      }
    } else { // 处理队列变大的情况，该情况下将增加任务
      if (mExec.size() < num) {
        for (int i = 0; i < diff; i++) {
          SubDLoadUtil next = getNextTask();
          if (next != null) {
            startTask(next);
          } else {
            ALog.d(TAG, "子任务中没有缓存任务");
          }
        }
      }
    }
  }

  @Override public void removeTaskFromExecQ(SubDLoadUtil fileer) {
    if (mExec.containsKey(fileer.getKey())) {
      if (fileer.isRunning()) {
        fileer.stop();
      }
      mExec.remove(fileer.getKey());
    }
  }

  @Override public void removeTask(SubDLoadUtil fileer) {
    removeTaskFromExecQ(fileer);
    mCache.remove(fileer.getKey());
  }

  @Override public void removeAllTask() {
    ALog.d(TAG, "删除组合任务");
    Set<String> keys = mExec.keySet();
    for (String key : keys) {
      SubDLoadUtil loader = mExec.get(key);
      if (loader != null) {
        ALog.d(TAG, String.format("停止子任务：%s", loader.getEntity().getFileName()));
        loader.cancel();
      }
    }
  }

  @Override public SubDLoadUtil getNextTask() {
    Iterator<String> keys = mCache.keySet().iterator();
    if (keys.hasNext()) {
      return mCache.get(keys.next());
    }
    return null;
  }

  @Override public void clear() {
    mCache.clear();
    mExec.clear();
  }
}
