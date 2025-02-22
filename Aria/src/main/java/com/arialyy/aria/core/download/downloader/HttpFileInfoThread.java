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
package com.arialyy.aria.core.download.downloader;

import android.net.TrafficStats;
import android.net.Uri;
import android.os.Process;
import android.text.TextUtils;
import com.arialyy.aria.core.AriaManager;
import com.arialyy.aria.core.common.CompleteInfo;
import com.arialyy.aria.core.common.OnFileInfoCallback;
import com.arialyy.aria.core.common.RequestEnum;
import com.arialyy.aria.core.common.http.HttpTaskConfig;
import com.arialyy.aria.core.download.DTaskWrapper;
import com.arialyy.aria.core.download.DownloadEntity;
import com.arialyy.aria.core.inf.IHttpFileLenAdapter;
import com.arialyy.aria.exception.AriaIOException;
import com.arialyy.aria.exception.BaseException;
import com.arialyy.aria.exception.TaskException;
import com.arialyy.aria.util.ALog;
import com.arialyy.aria.util.CheckUtil;
import com.arialyy.aria.util.CommonUtil;
import com.arialyy.aria.util.RecordUtil;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.CookieManager;
import java.net.HttpCookie;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * 下载文件信息获取
 */
public class HttpFileInfoThread implements Runnable {
  private static final String TAG = "HttpFileInfoThread";
  private DownloadEntity mEntity;
  private DTaskWrapper mTaskWrapper;
  private int mConnectTimeOut;
  private OnFileInfoCallback onFileInfoCallback;
  private HttpTaskConfig mTaskDelegate;

  public HttpFileInfoThread(DTaskWrapper taskWrapper, OnFileInfoCallback callback) {
    this.mTaskWrapper = taskWrapper;
    mEntity = taskWrapper.getEntity();
    mConnectTimeOut =
        AriaManager.getInstance(AriaManager.APP).getDownloadConfig().getConnectTimeOut();
    onFileInfoCallback = callback;
    mTaskDelegate = taskWrapper.asHttp();
  }

  @Override public void run() {
    Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
    TrafficStats.setThreadStatsTag(UUID.randomUUID().toString().hashCode());
    HttpURLConnection conn = null;
    try {
      URL url = ConnectionHelp.handleUrl(mEntity.getUrl(), mTaskDelegate);
      conn = ConnectionHelp.handleConnection(url, mTaskDelegate);
      ConnectionHelp.setConnectParam(mTaskDelegate, conn);
      conn.setRequestProperty("Range", "bytes=" + 0 + "-");
      conn.setConnectTimeout(mConnectTimeOut);
      conn.connect();
      handleConnect(conn);
    } catch (IOException e) {
      failDownload(new AriaIOException(TAG,
              String.format("下载失败，filePath: %s, url: %s", mEntity.getDownloadPath(), mEntity.getUrl())),
          true);
    } finally {
      if (conn != null) {
        conn.disconnect();
      }
    }
  }

  private void handleConnect(HttpURLConnection conn) throws IOException {
    if (mTaskDelegate.getRequestEnum() == RequestEnum.POST) {
      Map<String, String> params = mTaskDelegate.getParams();
      if (params != null) {
        OutputStreamWriter dos = new OutputStreamWriter(conn.getOutputStream());
        Set<String> keys = params.keySet();
        StringBuilder sb = new StringBuilder();
        for (String key : keys) {
          sb.append(key).append("=").append(URLEncoder.encode(params.get(key))).append("&");
        }
        String url = sb.toString();
        url = url.substring(0, url.length() - 1);
        dos.write(url);
        dos.flush();
        dos.close();
      }
    }

    IHttpFileLenAdapter lenAdapter = mTaskWrapper.asHttp().getFileLenAdapter();
    if (lenAdapter == null) {
      lenAdapter = new FileLenAdapter();
    } else {
      ALog.d(TAG, "使用自定义adapter");
    }
    long len = lenAdapter.handleFileLen(conn.getHeaderFields());

    if (!CommonUtil.checkSDMemorySpace(mEntity.getFilePath(), len)) {
      failDownload(new TaskException(TAG,
          String.format("下载失败，内存空间不足；filePath: %s, url: %s", mEntity.getDownloadPath(),
              mEntity.getUrl())), false);
      return;
    }

    int code = conn.getResponseCode();
    boolean end = false;
    if (TextUtils.isEmpty(mEntity.getMd5Code())) {
      String md5Code = conn.getHeaderField("Content-MD5");
      mEntity.setMd5Code(md5Code);
    }

    boolean isChunked = false;
    final String str = conn.getHeaderField("Transfer-Encoding");
    if (!TextUtils.isEmpty(str) && str.equals("chunked")) {
      isChunked = true;
    }
    Map<String, List<String>> headers = conn.getHeaderFields();
    String disposition = conn.getHeaderField("Content-Disposition");

    if (mTaskDelegate.isUseServerFileName()) {
      if (!TextUtils.isEmpty(disposition)) {
        mEntity.setDisposition(CommonUtil.encryptBASE64(disposition));
        handleContentDisposition(disposition);
      } else {
        ALog.w(TAG, "Content-Disposition对于端字段为空，使用服务器端文件名失败");
      }
    }
    CookieManager msCookieManager = new CookieManager();
    List<String> cookiesHeader = headers.get("Set-Cookie");

    if (cookiesHeader != null) {
      for (String cookie : cookiesHeader) {
        msCookieManager.getCookieStore().add(null, HttpCookie.parse(cookie).get(0));
      }
      mTaskDelegate.setCookieManager(msCookieManager);
    }

    mTaskWrapper.setCode(code);
    if (code == HttpURLConnection.HTTP_PARTIAL) {
      if (!checkLen(len) && !isChunked) {
        if (len < 0) {
          failDownload(
              new AriaIOException(TAG, String.format("任务下载失败，文件长度小于0， url: %s", mEntity.getUrl())),
              false);
        }
        return;
      }
      mEntity.setFileSize(len);
      mTaskWrapper.setSupportBP(true);
      end = true;
    } else if (code == HttpURLConnection.HTTP_OK) {
      String contentType = conn.getHeaderField("Content-Type");
      if (TextUtils.isEmpty(contentType)) {
        return;
      }
      if (contentType.equals("text/html")) {
        BufferedReader reader =
            new BufferedReader(new InputStreamReader(ConnectionHelp.convertInputStream(conn)));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
          sb.append(line);
        }
        reader.close();
        handleUrlReTurn(conn, CommonUtil.getWindowReplaceUrl(sb.toString()));
        return;
      } else if (!checkLen(len) && !isChunked) {
        if (len < 0) {
          failDownload(
              new AriaIOException(TAG, String.format("任务下载失败，文件长度小于0， url: %s", mEntity.getUrl())),
              false);
        }
        ALog.d(TAG, "len < 0");
        return;
      }
      mEntity.setFileSize(len);
      mTaskWrapper.setNewTask(true);
      mTaskWrapper.setSupportBP(false);
      end = true;
    } else if (code == HttpURLConnection.HTTP_NOT_FOUND) {
      failDownload(new AriaIOException(TAG,
          String.format("任务下载失败，errorCode：404, url: %s", mEntity.getUrl())), true);
    } else if (code == HttpURLConnection.HTTP_MOVED_TEMP
        || code == HttpURLConnection.HTTP_MOVED_PERM
        || code == HttpURLConnection.HTTP_SEE_OTHER
        || code == HttpURLConnection.HTTP_CREATED // 201 跳转
        || code == 307) {
      handleUrlReTurn(conn, conn.getHeaderField("Location"));
    } else {
      failDownload(new AriaIOException(TAG,
          String.format("任务下载失败，errorCode：%s, errorMsg: %s, url: %s", code,
              conn.getResponseMessage(), mEntity.getUrl())), true);
    }
    if (end) {
      mTaskDelegate.setChunked(isChunked);
      if (onFileInfoCallback != null) {
        CompleteInfo info = new CompleteInfo(code, mTaskWrapper);
        onFileInfoCallback.onComplete(mEntity.getUrl(), info);
      }
      mEntity.update();
    }
  }

  /**
   * 处理"Content-Disposition"参数
   * <a href=https://cloud.tencent.com/developer/section/1189916>Content-Disposition</a></>
   *
   * @throws UnsupportedEncodingException
   */
  private void handleContentDisposition(String disposition) throws UnsupportedEncodingException {
    if (disposition.contains(";")) {
      String[] infos = disposition.split(";");
      if (infos[0].equals("attachment")) {
        for (String info : infos) {
          if (info.startsWith("filename") && info.contains("=")) {
            String[] temp = info.split("=");
            if (temp.length > 1) {
              String newName = URLDecoder.decode(temp[1], "utf-8").replaceAll("\"", "");
              mEntity.setServerFileName(newName);
              renameFile(newName);
              break;
            }
          }
        }
      } else if (infos[0].equals("form-data") && infos.length > 2) {
        String[] temp = infos[2].split("=");
        if (temp.length > 1) {
          String newName = URLDecoder.decode(temp[1], "utf-8").replaceAll("\"", "");
          mEntity.setServerFileName(newName);
          renameFile(newName);
        }
      } else {
        ALog.w(TAG, "不识别的Content-Disposition参数");
      }
    }
  }

  /**
   * 重命名文件
   */
  private void renameFile(String newName) {
    if (TextUtils.isEmpty(newName)) {
      ALog.w(TAG, "重命名失败【服务器返回的文件名为空】");
      return;
    }
    ALog.d(TAG, String.format("文件重命名为：%s", newName));
    File oldFile = new File(mEntity.getFilePath());
    String newPath = oldFile.getParent() + "/" + newName;
    if (oldFile.exists()) {
      boolean b = oldFile.renameTo(new File(newPath));
      ALog.d(TAG, String.format("文件重命名%s", b ? "成功" : "失败"));
    }
    mEntity.setFileName(newName);
    mEntity.setFilePath(newPath);
    RecordUtil.modifyTaskRecord(oldFile.getPath(), newPath);
  }

  /**
   * 处理30x跳转
   */
  private void handleUrlReTurn(HttpURLConnection conn, String newUrl) throws IOException {
    ALog.d(TAG, "30x跳转，新url为【" + newUrl + "】");
    if (TextUtils.isEmpty(newUrl) || newUrl.equalsIgnoreCase("null")) {
      if (onFileInfoCallback != null) {
        onFileInfoCallback.onFail(mEntity, new TaskException(TAG, "获取重定向链接失败"), false);
      }
      return;
    }
    if (newUrl.startsWith("/")) {
      Uri uri = Uri.parse(mEntity.getUrl());
      newUrl = uri.getHost() + newUrl;
    }

    if (!CheckUtil.checkUrlNotThrow(newUrl)) {
      failDownload(new TaskException(TAG, "下载失败，重定向url错误"), false);
      return;
    }
    mTaskDelegate.setRedirectUrl(newUrl);
    mEntity.setRedirect(true);
    mEntity.setRedirectUrl(newUrl);
    String cookies = conn.getHeaderField("Set-Cookie");
    conn.disconnect();
    URL url = ConnectionHelp.handleUrl(newUrl, mTaskDelegate);
    conn = ConnectionHelp.handleConnection(url, mTaskDelegate);
    ConnectionHelp.setConnectParam(mTaskDelegate, conn);
    conn.setRequestProperty("Cookie", cookies);
    conn.setRequestProperty("Range", "bytes=" + 0 + "-");
    conn.setConnectTimeout(mConnectTimeOut);
    conn.connect();
    handleConnect(conn);
    conn.disconnect();
  }

  /**
   * 检查长度是否合法，并且检查新获取的文件长度是否和数据库的文件长度一直，如果不一致，则表示该任务为新任务
   *
   * @param len 从服务器获取的文件长度
   * @return {@code true}合法
   */
  private boolean checkLen(long len) {
    if (len != mEntity.getFileSize()) {
      ALog.d(TAG, "长度不一致，任务为新任务");
      mTaskWrapper.setNewTask(true);
    }
    return true;
  }

  private void failDownload(BaseException e, boolean needRetry) {
    if (onFileInfoCallback != null) {
      onFileInfoCallback.onFail(mEntity, e, needRetry);
    }
  }

  private static class FileLenAdapter implements IHttpFileLenAdapter {

    @Override public long handleFileLen(Map<String, List<String>> headers) {
      if (headers == null || headers.isEmpty()) {
        ALog.e(TAG, "header为空，获取文件长度失败");
        return -1;
      }
      List<String> sLength = headers.get("Content-Length");
      if (sLength == null || sLength.isEmpty()) {
        return -1;
      }
      String temp = sLength.get(0);
      long len = TextUtils.isEmpty(temp) ? -1 : Long.parseLong(temp);
      // 某些服务，如果设置了conn.setRequestProperty("Range", "bytes=" + 0 + "-");
      // 会返回 Content-Range: bytes 0-225427911/225427913
      if (len < 0) {
        List<String> sRange = headers.get("Content-Range");
        if (sRange == null || sRange.isEmpty()) {
          len = -1;
        } else {
          int start = temp.indexOf("/");
          len = Long.parseLong(temp.substring(start + 1));
        }
      }

      return len;
    }
  }
}