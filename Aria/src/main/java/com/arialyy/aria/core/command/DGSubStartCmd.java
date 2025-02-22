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
package com.arialyy.aria.core.command;

import com.arialyy.aria.core.inf.AbsGroupTaskWrapper;

/**
 * Created by AriaL on 2017/6/29.
 * 任务组开始命令，该命令负责处理任务组子任务的开始\恢复等工作
 */
final class DGSubStartCmd<T extends AbsGroupTaskWrapper> extends AbsGroupCmd<T> {
  DGSubStartCmd(T wrapper) {
    super(wrapper);
  }

  @Override public void executeCmd() {
    if (checkTask()) {
      tempTask.startSubTask(childUrl);
    }
  }
}
