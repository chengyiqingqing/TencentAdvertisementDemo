package com.qq.e.union.demo;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.androidquery.AQuery;
import com.androidquery.callback.AjaxStatus;
import com.androidquery.callback.BitmapAjaxCallback;
import com.qq.e.ads.cfg.VideoOption;
import com.qq.e.ads.nativ.MediaView;
import com.qq.e.ads.nativ.NativeADEventListener;
import com.qq.e.ads.nativ.NativeADMediaListener;
import com.qq.e.ads.nativ.NativeADUnifiedListener;
import com.qq.e.ads.nativ.NativeUnifiedAD;
import com.qq.e.ads.nativ.NativeUnifiedADData;
import com.qq.e.ads.nativ.widget.NativeAdContainer;
import com.qq.e.comm.constants.AdPatternType;
import com.qq.e.comm.util.AdError;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;
import java.util.WeakHashMap;

public class NativeADUnifiedListViewActivity extends Activity implements NativeADUnifiedListener, AbsListView.OnScrollListener {

  private static final String TAG = NativeADUnifiedListViewActivity.class.getSimpleName();
  private CustomAdapter mAdapter;
  private AQuery mAQuery;

  private NativeUnifiedAD mAdManager;
  private List<NativeUnifiedADData> mAds = new ArrayList<>();

  private H mHandler = new H();

  private static final int AD_COUNT = 3;
  private static final int ITEM_COUNT = 30;
  private static final int FIRST_AD_POSITION = 5;
  private static final int AD_DISTANCE = 10;
  private static final int MSG_REFRESH_LIST = 1;

  private boolean mPlayMute = true;
  private int mLastVisibleItem = 0;
  private int mTotalItemCount = 0;
  private boolean mIsLoading = true;

  private WeakHashMap<NativeUnifiedADData, Boolean> mMuteMap = new WeakHashMap<>();

  @Override
  protected void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_native_unified_ad_listview);
    initView();

    boolean nonOption = getIntent().getBooleanExtra(NativeADUnifiedActivity.NONE_OPTION, false);
    if(!nonOption){
      mPlayMute = getIntent().getBooleanExtra(NativeADUnifiedActivity.PLAY_MUTE,true);
    }

    mAdManager = new NativeUnifiedAD(this, Constants.APPID, getPosId(), this);
    mAdManager.setMaxVideoDuration(getMaxVideoDuration());
    mAdManager.loadData(AD_COUNT);
  }

  private String getPosId() {
    return getIntent().getStringExtra(Constants.POS_ID);
  }

  private int getMaxVideoDuration() {
    return getIntent().getIntExtra(Constants.MAX_VIDEO_DURATION, 0);
  }

  @Override
  protected void onResume() {
    super.onResume();
    if (mAds != null) {
      for (NativeUnifiedADData ad : mAds) {
        ad.resume();
      }
    }
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
    if (mAds != null) {
      for (NativeUnifiedADData ad : mAds) {
        ad.destroy();
      }
    }
    mAds = null;
  }

  private void initView() {
    ListView listView = findViewById(R.id.listview);
    List<NormalItem> list = new ArrayList<>();
    for (int i = 0; i < 10; ++i) {
      list.add(new NormalItem("No." + i + " Init Data"));
    }
    mAdapter = new CustomAdapter(this, list);
    listView.setAdapter(mAdapter);
    listView.setOnScrollListener(this);

    mAQuery = new AQuery(this);
  }


  @Override
  public void onADLoaded(List<NativeUnifiedADData> ads) {
    mIsLoading = false;
    if(mAds != null){
      mAds.addAll(ads);
      Message msg = mHandler.obtainMessage(MSG_REFRESH_LIST, ads);
      mHandler.sendMessage(msg);
    }
  }

  @Override
  public void onNoAD(AdError error) {
    mIsLoading = false;
    Log.d(TAG, "onNoAd error code: " + error.getErrorCode()
        + ", error msg: " + error.getErrorMsg());
  }

  @Override
  public void onScrollStateChanged(AbsListView view, int scrollState) {
    if(!mIsLoading && scrollState == SCROLL_STATE_IDLE && mLastVisibleItem == mTotalItemCount){
      mAdManager.loadData(AD_COUNT);
      mIsLoading = true;
    }
  }

  @Override
  public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
    mLastVisibleItem = firstVisibleItem + visibleItemCount;
    mTotalItemCount = totalItemCount;
  }

  private class CustomAdapter extends BaseAdapter {

    private List<Object> mData;
    private Context mContext;
    private TreeSet mADSet = new TreeSet();

    private static final int TYPE_DATA = 0;
    private static final int TYPE_AD = 1;

    public CustomAdapter(Context context, List list) {
      mContext = context;
      mData = list;
    }

    public void addNormalItem(NormalItem item){
      mData.add(item);
    }

    public void addAdToPosition(NativeUnifiedADData nativeUnifiedADData, int position) {
      if (position >= 0 && position < mData.size()) {
        mData.add(position, nativeUnifiedADData);
        mADSet.add(position);
      }
    }

    @Override
    public int getCount() {
      return mData.size();
    }

    @Override
    public Object getItem(int position) {
      return mData.get(position);
    }

    @Override
    public long getItemId(int position) {
      return position;
    }

    @Override
    public int getViewTypeCount() {
      return 2;
    }

    @Override
    public int getItemViewType(int position) {
      return mADSet.contains(position) ? TYPE_AD : TYPE_DATA;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
      ViewHolder holder;
      int type = getItemViewType(position);
      if (convertView == null || convertView.getTag() == null) {
        holder = new ViewHolder();
        switch (type) {
          case TYPE_DATA:
            convertView = View.inflate(mContext, R.layout.item_data, null);
            holder.title = convertView.findViewById(R.id.title);
            break;
          case TYPE_AD:
            convertView = View.inflate(mContext, R.layout.item_ad_unified, null);
            holder.btnsContainer = convertView.findViewById(R.id.video_btns_container);
            holder.btnPlay = convertView.findViewById(R.id.btn_play);
            holder.btnPause = convertView.findViewById(R.id.btn_pause);
            holder.btnStop = convertView.findViewById(R.id.btn_stop);
            holder.btnMute = convertView.findViewById(R.id.btn_mute);
            holder.mediaView = convertView.findViewById(R.id.gdt_media_view);
            holder.adInfoContainer = convertView.findViewById(R.id.ad_info);
            holder.logo = convertView.findViewById(R.id.img_logo);
            holder.poster = convertView.findViewById(R.id.img_poster);
            holder.name = convertView.findViewById(R.id.text_title);
            holder.desc = convertView.findViewById(R.id.text_desc);
            holder.download = convertView.findViewById(R.id.btn_download);
            holder.container = convertView.findViewById(R.id.native_ad_container);
            break;
        }
        convertView.setTag(holder);
      } else {
        holder = (ViewHolder) convertView.getTag();
      }

      initItemView(position, convertView, holder, type);
      return convertView;
    }

    private void initItemView(int position, View convertView, final ViewHolder holder, int type) {
      if (type == TYPE_DATA) {
        holder.title.setText(((NormalItem) mData.get(position)).getTitle());
      } else if (type == TYPE_AD) {
        final NativeUnifiedADData ad = (NativeUnifiedADData) mData.get(position);
        AQuery logoAQ = mAQuery.recycle(convertView);
        logoAQ.id(R.id.img_logo).image(
            TextUtils.isEmpty(ad.getIconUrl())? ad.getImgUrl() : ad.getIconUrl(), false, true);
        holder.name.setText(ad.getTitle());
        holder.desc.setText(ad.getDesc());
        List<View> clickableViews = new ArrayList<>();
        clickableViews.add(holder.download);
        // 视频广告
        if (ad.getAdPatternType() == 2) {
          holder.poster.setVisibility(View.INVISIBLE);
          holder.mediaView.setVisibility(View.VISIBLE);
          holder.btnsContainer.setVisibility(View.VISIBLE);
        }
        ad.bindAdToView(NativeADUnifiedListViewActivity.this, holder.container, null,
            clickableViews);

        logoAQ.id(R.id.img_poster).image(ad.getImgUrl(), false, true, 0, 0,
            new BitmapAjaxCallback() {
              @Override
              protected void callback(String url, ImageView iv, Bitmap bm, AjaxStatus status) {
                if (iv.getVisibility() == View.VISIBLE) {
                  iv.setImageBitmap(bm);
                }
              }
            });

        setAdListener(holder, ad);

        NativeADUnifiedSampleActivity.updateAdAction(holder.download, ad);
      }
    }

    private void setAdListener(final ViewHolder holder, final NativeUnifiedADData ad) {
      ad.setNativeAdEventListener(new NativeADEventListener() {
        @Override
        public void onADExposed() {
          Log.d(TAG, "onADExposed: " + ad.getTitle());
        }

        @Override
        public void onADClicked() {
          Log.d(TAG, "onADClicked: " + ad.getTitle());
        }

        @Override
        public void onADError(AdError error) {
          Log.d(TAG, "onADError error code :" + error.getErrorCode()
              + "  error msg: " + error.getErrorMsg());
        }

        @Override
        public void onADStatusChanged() {
          NativeADUnifiedSampleActivity.updateAdAction(holder.download, ad);
        }
      });
        // 视频广告
      if (ad.getAdPatternType() == AdPatternType.NATIVE_VIDEO) {
        VideoOption videoOption =
            NativeADUnifiedSampleActivity.getVideoOption(getIntent());
        ad.bindMediaView(holder.mediaView, videoOption, new NativeADMediaListener() {
              @Override
              public void onVideoInit() {
                Log.d(TAG, "onVideoInit: ");
              }

              @Override
              public void onVideoLoading() {
                Log.d(TAG, "onVideoLoading: ");
              }

              @Override
              public void onVideoReady() {
                Log.d(TAG, "onVideoReady: duration:" + ad.getVideoDuration());
              }

              @Override
              public void onVideoLoaded(int videoDuration) {
                Log.d(TAG, "onVideoLoaded: ");
              }

              @Override
              public void onVideoStart() {
                Log.d(TAG, "onVideoStart: duration:" + ad.getVideoDuration());
              }

              @Override
              public void onVideoPause() {
                Log.d(TAG, "onVideoPause: ");
              }

              @Override
              public void onVideoResume() {
                Log.d(TAG, "onVideoResume: ");
              }

              @Override
              public void onVideoCompleted() {
                Log.d(TAG, "onVideoCompleted: ");
              }

              @Override
              public void onVideoError(AdError error) {
                Log.d(TAG, "onVideoError: ");
              }

              @Override
              public void onVideoStop() {
                Log.d(TAG, "onVideoStop");
              }

              @Override
              public void onVideoClicked() {
                Log.d(TAG, "onVideoClicked");
              }
            });

        View.OnClickListener listener = new View.OnClickListener(){
          @Override
          public void onClick(View v) {
            if(v == holder.btnPlay){
              ad.startVideo();
            }else if(v == holder.btnPause){
              ad.pauseVideo();
            }else if(v == holder.btnStop){
              ad.stopVideo();
            }
          }
        };
        holder.btnPlay.setOnClickListener(listener);
        holder.btnPause.setOnClickListener(listener);
        holder.btnStop.setOnClickListener(listener);

        // 静音复选框状态恢复
        boolean mute = mMuteMap.containsKey(ad) ? mMuteMap.get(ad) : mPlayMute;
        holder.btnMute.setChecked(mute);
        mMuteMap.put(ad, mute);

        holder.btnMute.setOnClickListener(new View.OnClickListener() {
          @Override
          public void onClick(View v) {
            boolean check = holder.btnMute.isChecked();
            ad.setVideoMute(check);
            mMuteMap.put(ad, check);
          }
        });


        // 不要监听此回调，恢复状态时调用setChecked，自动触发onCheckedChanged回调
        // 如果holder是很快复用的，listenr和ad可能是之前的，会把之前ad的状态搞坏掉
//        holder.btnMute.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
//          @Override
//          public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
//            ad.setVideoMute(isChecked);
//            mMuteMap.put(ad, isChecked);
//          }
//        });
      }
    }

  }

  static class ViewHolder {
    public TextView title;
    public MediaView mediaView;
    private View btnsContainer;
    private Button btnPlay;
    private Button btnPause;
    private Button btnStop;
    private CheckBox btnMute;
    public RelativeLayout adInfoContainer;
    public TextView name;
    public TextView desc;
    public ImageView logo;
    public ImageView poster;
    public Button download;
    public NativeAdContainer container;
  }

  class NormalItem {
    private String mTitle;

    public NormalItem(int index){
      this("No." + index + " Normal Data");
    }

    public NormalItem(String title) {
      this.mTitle = title;
    }

    public String getTitle() {
      return mTitle;
    }

  }
  private class H extends Handler {
    @Override
    public void handleMessage(Message msg) {
      switch (msg.what) {
        case MSG_REFRESH_LIST:
          int count = mAdapter.getCount();
          for(int i = 0; i < ITEM_COUNT; i++){
            mAdapter.addNormalItem(new NormalItem(count + i));
          }

          List<NativeUnifiedADData> ads = (List<NativeUnifiedADData>) msg.obj;
          if (ads != null && ads.size() > 0 && mAdapter != null) {
            for (int i = 0; i < ads.size(); i++) {
              mAdapter.addAdToPosition(ads.get(i), count + i * AD_DISTANCE + FIRST_AD_POSITION);
              Log.d(TAG, i + ": eCPM = " + ads.get(i).getECPM());
            }
          }
          mAdapter.notifyDataSetChanged();
          break;

        default:
      }
    }
  }
}
