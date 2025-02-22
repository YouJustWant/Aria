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
package com.arialyy.aria.core.scheduler;

import com.arialyy.aria.core.queue.UploadTaskQueue;
import com.arialyy.aria.core.upload.UTaskWrapper;
import com.arialyy.aria.core.upload.UploadTask;

/**
 * Created by lyy on 2017/2/27.
 * 上传任务调度器
 */
public class UploadSchedulers extends AbsSchedulers<UTaskWrapper, UploadTask, UploadTaskQueue> {
  private static final String TAG = "UploadSchedulers";
  private static volatile UploadSchedulers INSTANCE = null;

  private UploadSchedulers() {
    super();
    mQueue = UploadTaskQueue.getInstance();
  }

  public static UploadSchedulers getInstance() {
    if (INSTANCE == null) {
      synchronized (UploadSchedulers.class) {
        INSTANCE = new UploadSchedulers();
      }
    }

    return INSTANCE;
  }
}
