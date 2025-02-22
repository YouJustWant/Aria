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

package com.arialyy.aria.util;

import android.text.TextUtils;
import com.arialyy.aria.core.download.DTaskWrapper;
import com.arialyy.aria.core.download.DownloadEntity;
import com.arialyy.aria.core.inf.AbsTaskWrapper;
import com.arialyy.aria.core.upload.UTaskWrapper;
import com.arialyy.aria.core.upload.UploadEntity;
import com.arialyy.aria.exception.ParamException;
import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by Lyy on 2016/9/23.
 * 检查帮助类
 */
public class CheckUtil {
  private static final String TAG = "CheckUtil";

  /**
   * 检查ftp上传路径，如果ftp上传路径为空，抛出空指针异常
   * 如果ftp上传路径不是以"ftp"或"sftp"，抛出参数异常
   *
   * @param ftpUrl ftp上传路径
   */
  public static void checkFtpUploadUrl(String ftpUrl) {
    if (TextUtils.isEmpty(ftpUrl)) {
      throw new ParamException("ftp上传路径为空");
    } else if (!ftpUrl.startsWith("ftp") || !ftpUrl.startsWith("sftp")) {
      throw new ParamException("ftp上传路径无效");
    }
  }

  /**
   * 判空
   */
  public static void checkNull(Object obj) {
    if (obj == null) throw new IllegalArgumentException("不能传入空对象");
  }

  /**
   * 检查分页数据，需要查询的页数，从1开始，如果page小于1 或 num 小于1，则抛出{@link NullPointerException}
   *
   * @param page 从1 开始
   * @param num 每页数量
   */
  public static void checkPageParams(int page, int num) {
    if (page < 1 || num < 1) throw new NullPointerException("page和num不能小于1");
  }

  /**
   * 检查sql的expression是否合法
   *
   * @return false 不合法
   */
  public static boolean checkSqlExpression(String... expression) {
    if (expression.length == 0) {
      ALog.e(TAG, "sql语句表达式不能为null");
      return false;
    }
    if (expression.length == 1) {
      ALog.e(TAG, String.format("表达式需要写入参数，参数信息：%s", Arrays.toString(expression)));
      return false;
    }
    String where = expression[0];
    if (!where.contains("?")) {
      ALog.e(TAG, String.format("请在where语句的'='后编写?，参数信息：%s", Arrays.toString(expression)));
      return false;
    }
    Pattern pattern = Pattern.compile("\\?");
    Matcher matcher = pattern.matcher(where);
    int count = 0;
    while (matcher.find()) {
      count++;
    }
    if (count < expression.length - 1) {
      ALog.e(TAG, String.format("条件语句的?个数不能小于参数个数，参数信息：%s", Arrays.toString(expression)));
      return false;
    }
    if (count > expression.length - 1) {
      ALog.e(TAG, String.format("条件语句的?个数不能大于参数个数， 参数信息：%s", Arrays.toString(expression)));
      return false;
    }
    return true;
  }

  /**
   * 检查下载实体
   */
  public static void checkDownloadEntity(DownloadEntity entity) {
    checkUrlInvalidThrow(entity.getUrl());
    entity.setUrl(entity.getUrl());
    checkPath(entity.getDownloadPath());
  }

  /**
   * 检测下载链接是否为null
   */
  public static void checkPath(String path) {
    if (TextUtils.isEmpty(path)) {
      throw new IllegalArgumentException("保存路径不能为null");
    }
  }

  /**
   * 检测url是否合法，如果url不合法，将抛异常
   */
  public static void checkUrlInvalidThrow(String url) {
    if (TextUtils.isEmpty(url)) {
      throw new IllegalArgumentException("url不能为null");
    } else if (!url.startsWith("http") && !url.startsWith("ftp") && !url.startsWith("sftp")) {
      throw new IllegalArgumentException("url错误");
    }
    int index = url.indexOf("://");
    if (index == -1) {
      throw new IllegalArgumentException("url不合法");
    }
  }

  /**
   * 检测url是否合法，如果url不合法，将抛出{@link IllegalArgumentException}异常
   */
  public static void checkUrl(String url) {
    if (TextUtils.isEmpty(url)) {
      throw new NullPointerException("url为空");
    } else if (!url.startsWith("http") && !url.startsWith("ftp") && !url.startsWith("sftp")) {
      throw new IllegalArgumentException(String.format("url【%s】错误", url));
    }
    int index = url.indexOf("://");
    if (index == -1) {
      throw new IllegalArgumentException(String.format("url【%s】不合法", url));
    }
  }

  /**
   * 检测url是否合法
   *
   * @return {@code true} 合法，{@code false} 非法
   */
  public static boolean checkUrlNotThrow(String url) {
    if (TextUtils.isEmpty(url)) {
      ALog.e(TAG, "url不能为null");
      return false;
    } else if (!url.startsWith("http") && !url.startsWith("ftp") && !url.startsWith("sftp")) {
      ALog.e(TAG, "url【" + url + "】错误");
      return false;
    }
    int index = url.indexOf("://");
    if (index == -1) {
      ALog.e(TAG, "url【" + url + "】不合法");
    }
    return true;
  }

  /**
   * 检测下载链接组是否为null
   */
  public static void checkDownloadUrls(List<String> urls) {
    if (urls == null || urls.isEmpty()) {
      throw new IllegalArgumentException("链接组不能为null");
    }
  }

  /**
   * 检查下载任务组保存路径
   */
  public static void checkDownloadPaths(List<String> paths) {
    if (paths == null || paths.isEmpty()) {
      throw new IllegalArgumentException("链接保存路径不能为null");
    }
  }

  /**
   * 检测上传地址是否为null
   */
  public static void checkUploadPath(String uploadPath) {
    if (TextUtils.isEmpty(uploadPath)) {
      throw new IllegalArgumentException("上传地址不能为null");
    }
    File file = new File(uploadPath);
    if (!file.exists()) {
      throw new IllegalArgumentException("上传文件不存在");
    }
  }

  /**
   * 检查任务实体
   */
  public static void checkTaskEntity(AbsTaskWrapper entity) {
    if (entity instanceof DTaskWrapper) {
      checkDownloadTaskEntity(((DTaskWrapper) entity).getEntity());
    } else if (entity instanceof UTaskWrapper) {
      checkUploadTaskEntity(((UTaskWrapper) entity).getEntity());
    }
  }

  /**
   * 检查命令实体
   *
   * @param checkType 删除命令和停止命令不需要检查下载链接和保存路径
   * @return {@code false}实体无效
   */
  public static boolean checkCmdEntity(AbsTaskWrapper entity, boolean checkType) {
    boolean b = false;
    if (entity instanceof DTaskWrapper) {
      DownloadEntity entity1 = ((DTaskWrapper) entity).getEntity();
      if (entity1 == null) {
        ALog.e(TAG, "下载实体不能为空");
      } else if (checkType && TextUtils.isEmpty(entity1.getUrl())) {
        ALog.e(TAG, "下载链接不能为空");
      } else if (checkType && TextUtils.isEmpty(entity1.getDownloadPath())) {
        ALog.e(TAG, "保存路径不能为空");
      } else {
        b = true;
      }
    } else if (entity instanceof UTaskWrapper) {
      UploadEntity entity1 = ((UTaskWrapper) entity).getEntity();
      if (entity1 == null) {
        ALog.e(TAG, "上传实体不能为空");
      } else if (TextUtils.isEmpty(entity1.getFilePath())) {
        ALog.e(TAG, "上传文件路径不能为空");
      } else {
        b = true;
      }
    }
    return b;
  }

  /**
   * 检查上传实体是否合法
   */
  private static void checkUploadTaskEntity(UploadEntity entity) {
    if (entity == null) {
      throw new NullPointerException("上传实体不能为空");
    } else if (TextUtils.isEmpty(entity.getFilePath())) {
      throw new IllegalArgumentException("上传文件路径不能为空");
    } else if (TextUtils.isEmpty(entity.getFileName())) {
      throw new IllegalArgumentException("上传文件名不能为空");
    }
  }

  /**
   * 检测下载实体是否合法
   * 合法(true)
   *
   * @param entity 下载实体
   */
  private static void checkDownloadTaskEntity(DownloadEntity entity) {
    if (entity == null) {
      throw new NullPointerException("下载实体不能为空");
    } else if (TextUtils.isEmpty(entity.getUrl())) {
      throw new IllegalArgumentException("下载链接不能为空");
    } else if (TextUtils.isEmpty(entity.getFileName())) {
      throw new NullPointerException("文件名不能为null");
    } else if (TextUtils.isEmpty(entity.getDownloadPath())) {
      throw new NullPointerException("文件保存路径不能为null");
    }
  }
}