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
package com.arialyy.aria.core.download.m3u8;

/**
 * M3U8 bandWidth 码率url转换器，对于某些服务器，返回的ts地址可以是相对地址，也可能是处理过的，
 * 对于这种情况，你需要使用url转换器将地址转换为可正常访问的http地址
 */
public interface IBandWidthUrlConverter {

  /**
   * 转换码率地址为可用的http地址，对于某些服务器，返回的切片信息有可能是相对地址，也可能是处理过的，
   * 对于这种情况，你需要使用url转换器将地址转换为可正常访问的http地址
   *
   * @param bandWidthUrl 原始码率地址
   * @return 可正常访问的http地址
   */
  String convert(String bandWidthUrl);
}
