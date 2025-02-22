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
package com.arialyy.aria.core.common;

import com.arialyy.aria.core.inf.AbsTaskWrapper;

/**
 * Created by AriaL on 2018/3/3.
 * 获取文件信息完成后 回调给下载线程的信息
 */
public class CompleteInfo {
  /**
   * 自定义的状态码
   */
  public int code;

  public AbsTaskWrapper wrapper;

  public Object obj;

  public CompleteInfo() {

  }

  public CompleteInfo(int code, AbsTaskWrapper wrapper) {
    this.code = code;
    this.wrapper = wrapper;
  }
}
