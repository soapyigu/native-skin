package com.ooyala.android.ooyalaskinsdk;

import android.content.Intent;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.WritableArray;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.modules.core.DeviceEventManagerModule;
import com.ooyala.android.OoyalaException;
import com.ooyala.android.OoyalaPlayer;
import com.ooyala.android.OoyalaPlayerLayout;
import com.ooyala.android.ClientId;
import com.ooyala.android.item.Caption;
import com.ooyala.android.item.Video;
import com.ooyala.android.discovery.DiscoveryManager;
import com.ooyala.android.discovery.DiscoveryOptions;
import com.ooyala.android.player.FCCTVRatingUI;
import com.ooyala.android.ui.LayoutController;
import com.ooyala.android.util.DebugMode;
import com.ooyala.android.OoyalaException;
import com.ooyala.android.captions.ClosedCaptionsView;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.Iterator;
import java.util.Observable;
import java.util.Observer;
import java.util.Set;

/**
 * Created by zchen on 9/21/15.
 */

public class OoyalaSkinLayoutController extends ReactContextBaseJavaModule implements LayoutController, Observer,OoyalaSkinLayout.FrameChangeListener,DiscoveryManager.Callback {
  final String TAG = this.getClass().toString();
  private OoyalaSkinLayout _layout;
  private OoyalaPlayer _player;
  private FCCTVRatingUI _tvRatingUI;
  private boolean _isFullscreen = false;
  private static final String BUTTON_PLAYPAUSE = "PlayPause";
  private static final String BUTTON_PLAY = "Play";
  private static final String BUTTON_SHARE = "Share";
  private static final String BUTTON_SOCIALSHARE = "SocialShare";
  private static final String BUTTON_FULLSCREEN = "Fullscreen";
  private static final String BUTTON_LEARNMORE = "LearnMore";
  private static final String BUTTON_MORE_OPTION = "More";
  private static final String BUTTON_UPNEXT_DISMISS = "upNextDismiss";
  private static final String BUTTON_UPNEXT_CLICK = "upNextClick";

  private static final String KEY_NAME = "name";
  private static final String KEY_EMBEDCODE = "embedCode";
  private static final String KEY_PERCENTAG = "percentage";
  private static final String KEY_LANGUAGE = "language";
  private static final String KEY_BUCKETINFO = "bucketInfo";
  private static final String KEY_ACTION = "action";
  private static final String KEY_STATE = "state";
  private ClosedCaptionsView _closedCaptionsView;
  private int width,height;
  private String shareTitle;
  private float dpi,cal;

  @Override
  public void callback(Object results, OoyalaException error) {
  JSONArray discoveryResult = (JSONArray) results;
    if(discoveryResult!=null) {
        WritableArray dresults = Arguments.createArray();
          for (int i = 0; i < discoveryResult.length(); i++) {
          JSONObject jsonObject = null;
          try {
            jsonObject = discoveryResult.getJSONObject(i);
            } catch (JSONException e) {
                e.printStackTrace();
            }
           WritableMap argument = Arguments.createMap();
           int duration1 = Integer.parseInt(jsonObject.optString("duration").toString());
           String embedCode = jsonObject.optString("embed_code").toString();
           String imageUrl = jsonObject.optString("preview_image_url").toString();
           String name = jsonObject.optString("name").toString();
           argument.putString("name", name);
           argument.putString("imageUrl", imageUrl);
           argument.putInt("duration", duration1);
           argument.putString("embedCode", embedCode);
           dresults.pushMap(argument);
          }
            WritableMap discoveryresults1 = Arguments.createMap();
            discoveryresults1.putArray("results", dresults);
            this.getReactApplicationContext()
                    .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
                    .emit("discoveryResultsReceived", discoveryresults1);
    }
  }
  @Override
  public String getName() {
    return "OoyalaSkinLayoutController";
  }

  public OoyalaSkinLayoutController(
    ReactApplicationContext c, OoyalaSkinLayout l, OoyalaPlayer p) {
    super(c);
    _layout = l;
    _layout.setFrameChangeListener(this);
    _player = p;
    _player.setLayoutController(this);
    _player.addObserver(this);
    DisplayMetrics metrics = c.getResources().getDisplayMetrics();
    dpi = metrics.densityDpi;
    cal = 160/dpi;
    width = Math.round(_layout.getViewWidth()*cal);
    height = Math.round(_layout.getViewHeight() * cal);
  }

  public FrameLayout getLayout() {
    return _layout.getPlayerLayout();
  }

  public void setFullscreen(boolean fullscreen) {
    if(fullscreen) {
      _layout.setSystemUiVisibility(
              View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                      | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                      | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                      | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION // hide nav bar
                      | View.SYSTEM_UI_FLAG_FULLSCREEN // hide status bar
                      | View.SYSTEM_UI_FLAG_IMMERSIVE);

    }
    else
    {
      _layout.setSystemUiVisibility(
              View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                      | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                      | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
    }

  }

  public boolean isFullscreen() {

    return _isFullscreen;

  }

  public void showClosedCaptionsMenu() {

  }

  public boolean onTouchEvent(MotionEvent event, OoyalaPlayerLayout source) {
    return false;
  }


  public boolean onKeyUp(int keyCode, KeyEvent event) {
    return false;
  }

  public void addVideoView(View videoView) {
    removeVideoView();
    if (videoView != null) {
      _tvRatingUI = new FCCTVRatingUI(_player, videoView, getLayout(), _player.getOptions().getTVRatingConfiguration());
    }
  }

  public void removeVideoView() {
    if (_tvRatingUI != null) {
      _tvRatingUI.destroy();
      _tvRatingUI = null;
    }
  }

  public void reshowTVRating() {
    if (_tvRatingUI != null) {
      _tvRatingUI.reshow();
    }
  }

  public void setFullscreenButtonShowing(boolean showing) {

  }

  @ReactMethod
  public void onClosedCaptionUpdateRequested(ReadableMap parameters) {
    final String languageName;
    if (parameters.hasKey("language")) {
      languageName = parameters.getString("language");
    }
    else
    {
      languageName = null;
    }
    Video currentItem = _player.getCurrentItem();
      double currT = _player.getPlayheadTime() / 1000d;
      Caption caption = currentItem.getClosedCaptions().getCaption(languageName, currT);
      if (caption != null) {
        WritableMap body = Arguments.createMap();
        body.putString("text", caption.getText());
        body.putDouble("end", caption.getEnd());
        body.putDouble("begin", caption.getBegin());
        this.getReactApplicationContext()
                .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
                .emit("onClosedCaptionUpdate", body);
    }
  }
  @ReactMethod
  public void onPress(ReadableMap parameters) {
    final String buttonName;
    if (parameters.hasKey("name")) {

      buttonName = parameters.getString("name");
    }
    else
    {
      buttonName = null;
    }
    if (buttonName != null) {
      DebugMode.logD(TAG, "onPress with buttonName:" + buttonName);
      this.getReactApplicationContext().runOnUiQueueThread(new Runnable() {
        @Override
        public void run() {
          if (buttonName.equals(BUTTON_PLAY)) {
            handlePlay();
          } else if (buttonName.equals(BUTTON_PLAYPAUSE)) {
            handlePlayPause();
          } else if (buttonName.equals(BUTTON_FULLSCREEN)) {
            _isFullscreen = !isFullscreen();
              setFullscreen(_isFullscreen);
          } else if (buttonName.equals(BUTTON_SHARE)) {
            handleShare();
          }
        }
      });
    }
  }
    @ReactMethod
    public void shareTitle(ReadableMap parameters) {
        shareTitle = parameters.getString("shareTitle");
    }

    private void handleShare() {
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, shareTitle);
        shareIntent.putExtra(Intent.EXTRA_TEXT,     "http://www.ooyala.com");
        Intent chooserIntent = Intent.createChooser(shareIntent, "share to");
        chooserIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        getReactApplicationContext().startActivity(chooserIntent);
    }

  @Override
  public void update(Observable arg0, Object arg1) {
    if (arg1 == OoyalaPlayer.STATE_CHANGED_NOTIFICATION) {
      bridgeStateChangedNotification();
    } else if (arg1 == OoyalaPlayer.CURRENT_ITEM_CHANGED_NOTIFICATION) {
      bridgeCurrentItemChangedNotification();
    } else if (arg1 == OoyalaPlayer.TIME_CHANGED_NOTIFICATION) {
      bridgeTimeChangedNotification();
    } else if (arg1 == OoyalaPlayer.PLAY_COMPLETED_NOTIFICATION) {
      bridgePlayCompletedNotification();
    } else if (arg1 == OoyalaPlayer.AD_STARTED_NOTIFICATION) {
      bridgeAdStartNotification();
    } else if (arg1 == OoyalaPlayer.AD_COMPLETED_NOTIFICATION) {
      bridgeAdPodCompleteNotification();
    } else if (arg1 == OoyalaPlayer.PLAY_STARTED_NOTIFICATION) {
      bridgePlayStartedNotification();
      requestDiscovery();
    } else if (arg1 == OoyalaPlayer.ERROR_NOTIFICATION) {
      bridgeErrorNotification();
    } else if (arg1 == OoyalaPlayer.CLOSED_CAPTIONS_LANGUAGE_CHANGED) {
      onClosedCaptionChangeNotification();
    }
  }

  // private methods
  private void handlePlay() {
    _player.play();
  }

  private void handlePlayPause() {
    if (_player.isPlaying()) {
      _player.pause();
    } else {
      _player.play();
    }
  }

  @ReactMethod
  public void onScrub(ReadableMap percentage) {
    double percentValue = percentage.getDouble("percentage");
    percentValue = percentValue * 100;
    int percent = ((int) percentValue);
    _player.seekToPercent(percent);
  }
  @ReactMethod
  public void onDiscoveryRow(ReadableMap parameters) {
  }

  private void requestDiscovery() {
      DiscoveryManager.getResults(new DiscoveryOptions.Builder().build(),
         _player.getEmbedCode(),
         _player.getPcode(),
         ClientId.getId(_layout.getContext()), null, this);
  }
  private void onClosedCaptionChangeNotification() {
  }

  // notification bridges
  private void bridgeCurrentItemChangedNotification() {
    WritableMap params = Arguments.createMap();
    Video currentItem = _player.getCurrentItem();
    if (currentItem != null) {
      String title = currentItem.getTitle();
      params.putString("title", title != null ? title : "");
      String description = currentItem.getDescription();
      params.putString("description", description != null ? description : "");

      String promoUrl = currentItem.getPromoImageURL(2000, 2000);
      params.putString("promoUrl", promoUrl != null ? promoUrl : "");

//      String hostedAtUrl = _player.currentItem.hostedAtURL ? _player.currentItem.hostedAtURL : @"";
      Double duration = currentItem.getDuration() / 1000.0;
      params.putDouble("duration", duration);
      params.putBoolean("live", currentItem.isLive());
      params.putInt("width", width);
      params.putInt("height", height);
      if (currentItem.hasClosedCaptions()) {
          WritableArray languages = Arguments.createArray();
          for (String s : currentItem.getClosedCaptions().getLanguages()) {
              languages.pushString(s);
          }
          params.putArray("languages", languages);
      }
    }
    this.getReactApplicationContext()
            .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
            .emit(OoyalaPlayer.CURRENT_ITEM_CHANGED_NOTIFICATION, params);

//    if (_player.currentItem.embedCode && self.skinOptions.discoveryOptions) {
//      [self loadDiscovery:_player.currentItem.embedCode];
//    }
  }

  private void bridgeStateChangedNotification() {
    WritableMap params = Arguments.createMap();
    params.putString(KEY_STATE, _player.getState().toString().toLowerCase());
    DebugMode.logD(TAG, "state change event params are" + params.toString());
    this.getReactApplicationContext()
            .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
            .emit(OoyalaPlayer.STATE_CHANGED_NOTIFICATION, params);
  }

  private void bridgeTimeChangedNotification() {
    Double duration = _player.getDuration() / 1000.0;
    Double playhead = _player.getPlayheadTime() / 1000.0;
    WritableArray cuePoints = Arguments.createArray();
    Set<Integer> cuePointsPercentValues = _player.getCuePointsInPercentage();
    for (Iterator<Integer> i = cuePointsPercentValues.iterator(); i.hasNext(); ) {
      int cuePointLocation =(int) Math.round ((i.next()/100.0)*duration);
      cuePoints.pushInt(cuePointLocation);
    }

    WritableArray languages = Arguments.createArray();
    Set<String> cclanguage = _player.getAvailableClosedCaptionsLanguages();
    for (Iterator<String> j = cclanguage.iterator(); j.hasNext(); ) {
      String languageItem=j.next();
      languages.pushString(languageItem);
    }
    WritableMap params = Arguments.createMap();
    params.putDouble("duration", duration);
    params.putDouble("playhead", playhead);
    params.putArray("availableClosedCaptionsLanguages", languages);
    params.putArray("cuePoints", cuePoints);

    onClosedCaptionChangeNotification();
    this.getReactApplicationContext()
            .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
            .emit(OoyalaPlayer.TIME_CHANGED_NOTIFICATION, params);
  }

  private void bridgePlayCompletedNotification() {
    WritableMap params = Arguments.createMap();
    Video currentItem = _player.getCurrentItem();
    if (currentItem != null) {
      String title = currentItem.getTitle();
      params.putString("title", title != null ? title : "");

      String description = currentItem.getDescription();
      params.putString("description", description != null ? description : "");

      String promoUrl = currentItem.getPromoImageURL(2000, 2000);
      params.putString("promoUrl", promoUrl != null ? promoUrl : "");
      //String hostedAtUrl = _player.currentItem.hostedAtURL ? _player.currentItem.hostedAtURL : "";
      Double duration = currentItem.getDuration() / 1000.0;
      params.putDouble("duration", duration);
    }
    this.getReactApplicationContext()
            .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
            .emit(OoyalaPlayer.PLAY_COMPLETED_NOTIFICATION, params);
  }

  private void bridgePlayStartedNotification() {
    this.getReactApplicationContext()
            .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
            .emit(OoyalaPlayer.PLAY_STARTED_NOTIFICATION, null);
  }

  private void bridgeErrorNotification() {
    OoyalaException ex = _player.getError();
    WritableMap params = Arguments.createMap();
    if (ex != null) {
      int errorCode = ex.getCode().ordinal();
      params.putInt("code", errorCode);

      String descrptions = ex.getLocalizedMessage();
      params.putString("description", descrptions != null ? descrptions : "");
    }

    this.getReactApplicationContext()
            .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
            .emit(OoyalaPlayer.ERROR_NOTIFICATION, params);

  }

  private void bridgeAdStartNotification() {
    WritableMap params = Arguments.createMap();
    this.getReactApplicationContext()
            .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
            .emit(OoyalaPlayer.AD_STARTED_NOTIFICATION, params);
  }

  private void bridgeAdPodCompleteNotification() {
    WritableMap params = Arguments.createMap();
    this.getReactApplicationContext()
            .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
            .emit(OoyalaPlayer.AD_COMPLETED_NOTIFICATION, params);
  }

  @Override
  public void onFrameChange(int width, int height, int prevWdith,int prevHeight) {
    height = Math.round(height * cal);
    width = Math.round(width * cal);
    this.width=width;
    this.height=height;
    WritableMap params = Arguments.createMap();
    params.putInt("width", width);
    params.putInt("height", height);
    params.putBoolean("fullscreen",_isFullscreen);

    this.getReactApplicationContext()
            .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
            .emit("frameChanged", params);

  }
}