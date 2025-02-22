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

package com.arialyy.simple;

import android.Manifest;
import android.arch.lifecycle.Observer;
import android.arch.lifecycle.ViewModelProviders;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.AppCompatImageView;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.TextView;
import com.arialyy.frame.permission.OnPermissionCallback;
import com.arialyy.frame.permission.PermissionManager;
import com.arialyy.frame.util.show.T;
import com.arialyy.simple.base.BaseActivity;
import com.arialyy.simple.base.adapter.AbsHolder;
import com.arialyy.simple.base.adapter.AbsRVAdapter;
import com.arialyy.simple.base.adapter.RvItemClickSupport;
import com.arialyy.simple.core.download.m3u8.M3U8LiveDLoadActivity;
import com.arialyy.simple.core.download.m3u8.M3U8VodDLoadActivity;
import com.arialyy.simple.databinding.ActivityMainBinding;
import com.arialyy.simple.core.download.DownloadActivity;
import com.arialyy.simple.core.download.FtpDownloadActivity;
import com.arialyy.simple.core.download.group.DownloadGroupActivity;
import com.arialyy.simple.core.download.group.FTPDirDownloadActivity;
import com.arialyy.simple.core.upload.FtpUploadActivity;
import com.arialyy.simple.core.upload.HttpUploadActivity;
import com.arialyy.simple.modlue.CommonModule;
import com.arialyy.simple.to.NormalTo;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by lyy on 2017/3/1.
 * 首页
 */
public class MainActivity extends BaseActivity<ActivityMainBinding> {
  public static final String KEY_MAIN_DATA = "KEY_MAIN_DATA";

  @Override protected void init(Bundle savedInstanceState) {
    super.init(savedInstanceState);
    setSupportActionBar(mBar);
    mBar.setTitle("Aria  Demo");
    getSupportActionBar().setDisplayHomeAsUpEnabled(false);

    getBinding().list.setLayoutManager(new LinearLayoutManager(this));
    final List<NormalTo> data = new ArrayList<>();
    final Adapter adapter = new Adapter(this, data);
    getBinding().list.setAdapter(adapter);
    getBinding().list.addItemDecoration(
        new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));

    final CommonModule module = ViewModelProviders.of(this).get(CommonModule.class);
    module.getMainData(this).observe(this, new Observer<List<NormalTo>>() {
      @Override public void onChanged(@Nullable List<NormalTo> normalTos) {
        if (normalTos != null) {
          data.addAll(normalTos);
          adapter.notifyDataSetChanged();
        }
      }
    });

    RvItemClickSupport.addTo(getBinding().list).setOnItemClickListener(
        new RvItemClickSupport.OnItemClickListener() {
          @Override public void onItemClicked(RecyclerView recyclerView, int position, View v) {
            switch (position) {
              case 0:
                module.startNextActivity(MainActivity.this, data.get(position),
                    DownloadActivity.class);
                break;
              case 1:
                module.startNextActivity(MainActivity.this, data.get(position),
                    HttpUploadActivity.class);
                break;
              case 2:
                module.startNextActivity(MainActivity.this, data.get(position),
                    DownloadGroupActivity.class);
                break;
              case 3:
                module.startNextActivity(MainActivity.this, data.get(position),
                    FtpDownloadActivity.class);
                break;
              case 4:
                module.startNextActivity(MainActivity.this, data.get(position),
                    FTPDirDownloadActivity.class);
                break;
              case 5:
                module.startNextActivity(MainActivity.this, data.get(position),
                    FtpUploadActivity.class);
                break;
              case 6:
                module.startNextActivity(MainActivity.this, data.get(position),
                    M3U8VodDLoadActivity.class);
                break;
              case 7:
                module.startNextActivity(MainActivity.this, data.get(position),
                    M3U8LiveDLoadActivity.class);
                break;
            }
          }
        });

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
      boolean hasPermission = PermissionManager.getInstance()
          .checkPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
      if (!hasPermission) {
        PermissionManager.getInstance().requestPermission(this, new OnPermissionCallback() {
          @Override public void onSuccess(String... permissions) {
          }

          @Override public void onFail(String... permissions) {
            T.showShort(MainActivity.this, "没有文件读写权限");
            finish();
          }
        }, Manifest.permission.WRITE_EXTERNAL_STORAGE);
      }
    }
  }

  @Override protected int setLayoutId() {
    return R.layout.activity_main;
  }

  private static class Adapter extends AbsRVAdapter<NormalTo, Adapter.Holder> {

    Adapter(Context context, List<NormalTo> data) {
      super(context, data);
    }

    @Override protected Holder getViewHolder(View convertView, int viewType) {
      return new Holder(convertView);
    }

    @Override protected int setLayoutId(int type) {
      return R.layout.item_main;
    }

    @Override protected void bindData(Holder holder, int position, NormalTo item) {
      holder.title.setText(item.title);
      holder.desc.setText(item.desc);
      holder.image.setImageResource(item.icon);
    }

    private static class Holder extends AbsHolder {
      TextView title, desc;
      AppCompatImageView image;

      Holder(View itemView) {
        super(itemView);
        title = findViewById(R.id.title);
        desc = findViewById(R.id.desc);
        image = findViewById(R.id.image);
      }
    }
  }
}
