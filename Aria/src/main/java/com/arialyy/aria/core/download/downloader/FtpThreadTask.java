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

import aria.apache.commons.net.ftp.FTPClient;
import aria.apache.commons.net.ftp.FTPReply;
import com.arialyy.aria.core.common.SubThreadConfig;
import com.arialyy.aria.core.common.ftp.AbsFtpThreadTask;
import com.arialyy.aria.core.config.DownloadConfig;
import com.arialyy.aria.core.download.DTaskWrapper;
import com.arialyy.aria.core.download.DownloadEntity;
import com.arialyy.aria.exception.AriaIOException;
import com.arialyy.aria.exception.TaskException;
import com.arialyy.aria.util.ALog;
import com.arialyy.aria.util.BufferedRandomAccessFile;
import com.arialyy.aria.util.CommonUtil;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;

/**
 * Created by Aria.Lao on 2017/7/24. Ftp下载任务
 */
class FtpThreadTask extends AbsFtpThreadTask<DownloadEntity, DTaskWrapper> {
  private final String TAG = "FtpThreadTask";

  FtpThreadTask(SubThreadConfig<DTaskWrapper> config) {
    super(config);
  }

  @Override public FtpThreadTask call() throws Exception {
    super.call();
    if (mRecord.isComplete) {
      handleComplete();
      return this;
    }
    FTPClient client = null;
    InputStream is = null;

    try {
      ALog.d(TAG,
          String.format("任务【%s】线程__%s__开始下载【开始位置 : %s，结束位置：%s】", getFileName(),
              mRecord.threadId, mRecord.startLocation, mRecord.endLocation));
      client = createClient();
      if (client == null) {
        fail(mChildCurrentLocation, new TaskException(TAG, "ftp client 创建失败"));
        return this;
      }
      if (mRecord.startLocation > 0) {
        client.setRestartOffset(mRecord.startLocation);
      }
      //发送第二次指令时，还需要再做一次判断
      int reply = client.getReplyCode();
      if (!FTPReply.isPositivePreliminary(reply) && reply != FTPReply.COMMAND_OK) {
        fail(mChildCurrentLocation,
            new AriaIOException(TAG,
                String.format("获取文件信息错误，错误码为：%s，msg：%s", reply, client.getReplyString())));
        client.disconnect();
        return this;
      }
      String remotePath =
          CommonUtil.convertFtpChar(charSet, getTaskWrapper().asFtp().getUrlEntity().remotePath);
      ALog.i(TAG, String.format("remotePath【%s】", remotePath));
      is = client.retrieveFileStream(remotePath);
      reply = client.getReplyCode();
      if (!FTPReply.isPositivePreliminary(reply)) {
        fail(mChildCurrentLocation,
            new AriaIOException(TAG,
                String.format("获取流失败，错误码为：%s，msg：%s", reply, client.getReplyString())));
        client.disconnect();
        return this;
      }

      if (getConfig().isOpenDynamicFile) {
        readDynamicFile(is);
      } else {
        readNormal(is);
        handleComplete();
      }
    } catch (IOException e) {
      fail(mChildCurrentLocation,
          new AriaIOException(TAG, String.format("下载失败【%s】", getConfig().url), e));
    } catch (Exception e) {
      fail(mChildCurrentLocation,
          new AriaIOException(TAG, String.format("下载失败【%s】", getConfig().url), e));
    } finally {
      try {
        if (is != null) {
          is.close();
        }
        if (client != null && client.isConnected()) {
          client.disconnect();
        }
      } catch (IOException e) {
        e.printStackTrace();
      }

      onThreadComplete();
    }
    return this;
  }

  /**
   * 处理线程完成的情况
   */
  private void handleComplete() {
    if (isBreak()) {
      return;
    }
    if (!checkBlock()) {
      return;
    }
    ALog.i(TAG, String.format("任务【%s】线程__%s__下载完毕", getFileName(), mRecord.threadId));
    writeConfig(true, mRecord.endLocation);
    sendCompleteMsg();
  }

  /**
   * 动态长度文件读取方式
   */
  private void readDynamicFile(InputStream is) {
    FileOutputStream fos = null;
    FileChannel foc = null;
    ReadableByteChannel fic = null;
    try {
      int len;
      fos = new FileOutputStream(getConfig().tempFile, true);
      foc = fos.getChannel();
      fic = Channels.newChannel(is);
      ByteBuffer bf = ByteBuffer.allocate(getTaskConfig().getBuffSize());
      while (isLive() && (len = fic.read(bf)) != -1) {
        if (isBreak()) {
          break;
        }
        if (mSpeedBandUtil != null) {
          mSpeedBandUtil.limitNextBytes(len);
        }
        if (mChildCurrentLocation + len >= mRecord.endLocation) {
          len = (int) (mRecord.endLocation - mChildCurrentLocation);
          bf.flip();
          fos.write(bf.array(), 0, len);
          bf.compact();
          progress(len);
          break;
        } else {
          bf.flip();
          foc.write(bf);
          bf.compact();
          progress(len);
        }
      }
      handleComplete();
    } catch (IOException e) {
      fail(mChildCurrentLocation,
          new AriaIOException(TAG, String.format("下载失败【%s】", getConfig().url), e));
    } finally {
      try {
        if (fos != null) {
          fos.close();
        }
        if (foc != null) {
          foc.close();
        }
        if (fic != null) {
          fic.close();
        }
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
  }

  /**
   * 多线程写文件方式
   */
  private void readNormal(InputStream is) {
    BufferedRandomAccessFile file = null;
    try {
      file =
          new BufferedRandomAccessFile(getConfig().tempFile, "rwd", getTaskConfig().getBuffSize());
      file.seek(mRecord.startLocation);
      byte[] buffer = new byte[getTaskConfig().getBuffSize()];
      int len;
      while (isLive() && (len = is.read(buffer)) != -1) {
        if (isBreak()) {
          break;
        }
        if (mSpeedBandUtil != null) {
          mSpeedBandUtil.limitNextBytes(len);
        }
        if (mChildCurrentLocation + len >= mRecord.endLocation) {
          len = (int) (mRecord.endLocation - mChildCurrentLocation);
          file.write(buffer, 0, len);
          progress(len);
          break;
        } else {
          file.write(buffer, 0, len);
          progress(len);
        }
      }
    } catch (IOException e) {
      fail(mChildCurrentLocation,
          new AriaIOException(TAG, String.format("下载失败【%s】", getConfig().url), e));
    } finally {
      try {
        if (file != null) {
          file.close();
        }
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
  }

  @Override public int getMaxSpeed() {
    return getTaskConfig().getMaxSpeed();
  }

  @Override protected DownloadConfig getTaskConfig() {
    return getTaskWrapper().getConfig();
  }
}
