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
package com.arialyy.aria.core.config;

import com.arialyy.aria.core.event.EventMsgUtil;
import com.arialyy.aria.core.event.SpeedEvent;
import com.arialyy.aria.core.queue.UploadTaskQueue;
import java.io.Serializable;

/**
 * 上传配置
 */
public class UploadConfig extends BaseTaskConfig implements Serializable {

  UploadConfig() {
  }

  @Override public UploadConfig setMaxSpeed(int maxSpeed) {
    super.setMaxSpeed(maxSpeed);
    EventMsgUtil.getDefault().post(new SpeedEvent(maxSpeed));
    return this;
  }

  public UploadConfig setMaxTaskNum(int maxTaskNum) {
    super.setMaxTaskNum(maxTaskNum);
    UploadTaskQueue.getInstance().setMaxTaskNum(maxTaskNum);
    return this;
  }

  @Override int getType() {
    return TYPE_UPLOAD;
  }
}