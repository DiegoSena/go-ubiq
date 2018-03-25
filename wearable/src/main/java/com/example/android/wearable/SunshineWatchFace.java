package com.example.android.wearable;

import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.wearable.watchface.CanvasWatchFaceService;
import android.support.wearable.watchface.WatchFaceStyle;
import android.view.SurfaceHolder;
import android.view.WindowInsets;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.Wearable;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Analog watch face with a ticking second hand. In ambient mode, the second hand isn't
 * shown. On devices with low-bit ambient mode, the hands are drawn without anti-aliasing in ambient
 * mode. The watch face is drawn with less contrast in mute mode.
 * <p>
 * Important Note: Because watch face apps do not have a default Activity in
 * their project, you will need to set your Configurations to
 * "Do not launch Activity" for both the Wear and/or Application modules. If you
 * are unsure how to do this, please review the "Run Starter project" section
 * in the Google Watch Face Code Lab:
 * https://codelabs.developers.google.com/codelabs/watchface/index.html#0
 */
public class SunshineWatchFace extends CanvasWatchFaceService {

    private static final String LOG_TAG = SunshineWatchFace.class.getSimpleName();

    @Override
    public Engine onCreateEngine() {
        return new Engine();
    }

    private class Engine extends CanvasWatchFaceService.Engine implements GoogleApiClient.ConnectionCallbacks, DataApi.DataListener {

        private static final String WEATHER_DATA_URI = "/WEATHER_DATA_URI";
        private static final java.lang.String WEATHER_DATA = "WEATHER_DATA";
        private static final java.lang.String WEATHER_DATA_ID = "WEATHER_DATA_ID";
        private static final String KEY_WEATHER = "WEATHER_KEY";
        private static final String KEY_WEATHER_ID = "WEATHER_KEY_ID";
        private static final String PREFERENCES = "PREFERENCES";
        private GoogleApiClient mGoogleApiClient;
        private Paint mBackgroundPaint;
        private Date mDate;
        private SimpleDateFormat timeOfTheDayFormat = new SimpleDateFormat("hh:mm");
        private SimpleDateFormat dateFormat = new SimpleDateFormat("EEE, MMM dd yyyy");
        private float mXOffset;
        private float mTimeYOffset;
        private Paint mTimeOfTheDayPaint;
        private final Typeface BOLD_TYPEFACE = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD);
        private final Typeface NORMAL_TYPEFACE = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL);
        int mInteractiveBackgroundColor = Color.parseColor("White");
        private Paint mTemperaturePaint;
        private Paint mDatePaint;
        private Paint mRectPaint;
        private Bitmap mBitmap;
        private Paint mIconPaint;
        private String mWeather;
        private int mWeatherId;
        private boolean mAmbient;

        @Override
        public void onCreate(SurfaceHolder holder) {
            super.onCreate(holder);


            setWatchFaceStyle(new WatchFaceStyle.Builder(SunshineWatchFace.this)
                    .setCardPeekMode(WatchFaceStyle.PEEK_MODE_VARIABLE)
                    .setBackgroundVisibility(WatchFaceStyle.BACKGROUND_VISIBILITY_INTERRUPTIVE)
                    .setShowSystemUiTime(false)
                    .build());

            mGoogleApiClient = new GoogleApiClient.Builder(SunshineWatchFace.this)
                    .addApi(Wearable.API)
                    .addConnectionCallbacks(this)
                    .build();

            getWeatherFromPreferences();
            getBitMapForWeatherId();

            Resources resources = SunshineWatchFace.this.getResources();

            mBackgroundPaint = new Paint();
            mBackgroundPaint.setColor(resources.getColor(R.color.background));
            mRectPaint = new Paint();
            mRectPaint.setColor(resources.getColor(R.color.rect_background));
            mTimeYOffset = resources.getDimension(R.dimen.digital_y_offset);
            mTimeOfTheDayPaint = createTextPaint(mInteractiveBackgroundColor, BOLD_TYPEFACE);
            mDatePaint = createTextPaint(mInteractiveBackgroundColor, NORMAL_TYPEFACE);
            mTemperaturePaint = createTextPaint(mInteractiveBackgroundColor, BOLD_TYPEFACE);
            mDate = new Date();
            mIconPaint = new Paint();
        }

        private void getWeatherFromPreferences() {
            SharedPreferences preferences = getSharedPreferences(PREFERENCES, MODE_PRIVATE);
            mWeather = preferences.getString(KEY_WEATHER, "");
            mWeatherId = preferences.getInt(KEY_WEATHER_ID, 0);
        }

        private void getBitMapForWeatherId() {

            int iconId = 0;
            if (mWeatherId >= 200 && mWeatherId <= 232) {
                iconId = R.drawable.ic_storm;
            } else if (mWeatherId >= 300 && mWeatherId <= 321) {
                iconId = R.drawable.ic_light_rain;
            } else if (mWeatherId >= 500 && mWeatherId <= 504) {
                iconId = R.drawable.ic_rain;
            } else if (mWeatherId == 511) {
                iconId = R.drawable.ic_snow;
            } else if (mWeatherId >= 520 && mWeatherId <= 531) {
                iconId = R.drawable.ic_rain;
            } else if (mWeatherId >= 600 && mWeatherId <= 622) {
                iconId = R.drawable.ic_snow;
            } else if (mWeatherId >= 701 && mWeatherId <= 761) {
                iconId = R.drawable.ic_fog;
            } else if (mWeatherId == 761 || mWeatherId == 781) {
                iconId = R.drawable.ic_storm;
            } else if (mWeatherId == 800) {
                iconId = R.drawable.ic_clear;
            } else if (mWeatherId == 801) {
                iconId = R.drawable.ic_light_clouds;
            } else if (mWeatherId >= 802 && mWeatherId <= 804) {
                iconId = R.drawable.ic_cloudy;
            }

            if (iconId != 0) {
                float scale = 1.2f;
                mBitmap = BitmapFactory.decodeResource(getResources(), iconId);
                float sizeY = (float) mBitmap.getHeight() * scale;
                float sizeX = (float) mBitmap.getWidth() * scale;
                mBitmap = Bitmap.createScaledBitmap(mBitmap, (int) sizeX, (int) sizeY, false);
            }
        }

        private Paint createTextPaint(int defaultInteractiveColor, Typeface typeface) {
            Paint paint = new Paint();
            paint.setColor(defaultInteractiveColor);
            paint.setTypeface(typeface);
            paint.setAntiAlias(true);
            return paint;
        }

        @Override
        public void onPropertiesChanged(Bundle properties) {
            super.onPropertiesChanged(properties);
        }

        @Override
        public void onTimeTick() {
            invalidate();
        }

        @Override
        public void onAmbientModeChanged(boolean inAmbientMode) {
            super.onAmbientModeChanged(inAmbientMode);
            if (mAmbient != inAmbientMode) {
                mAmbient = inAmbientMode;
                invalidate();
            }
        }

        @Override
        public void onDraw(Canvas canvas, Rect bounds) {
            if(isInAmbientMode()){
                canvas.drawColor(Color.BLACK);
            }else{
                canvas.drawRect(0, 0, bounds.width(), bounds.height(), mBackgroundPaint);
            }
            mDate.setTime(System.currentTimeMillis());

            // Draw time
            String time = timeOfTheDayFormat.format(mDate);
            float xPosTime = canvas.getWidth() / 2 - mTimeOfTheDayPaint.measureText(time, 0, time.length()) / 2;
            canvas.drawText(time, xPosTime, mTimeYOffset, mTimeOfTheDayPaint);

            //Draw date
            String date = dateFormat.format(mDate);
            int padding = 16;
            float yPosDate = mTimeYOffset + mDatePaint.getTextSize() + padding;
            float xPosDate = canvas.getWidth() / 2 - mDatePaint.measureText(date, 0, date.length()) / 2;
            canvas.drawText(date, xPosDate, yPosDate, mDatePaint);

            if(!isInAmbientMode()){
                //Icon
                float yPosIcon = yPosDate + padding;
                float xPosIcon = canvas.getWidth() / 2 - mBitmap.getWidth();
                canvas.drawBitmap(mBitmap, xPosIcon, yPosIcon, mIconPaint);

                // Temperatures
                float yPosWeather = yPosDate + mTemperaturePaint.getTextSize() + mBitmap.getHeight() / 2;
                float xPosWeather = canvas.getWidth() / 2;
                canvas.drawText(mWeather != null ? mWeather : "", xPosWeather, yPosWeather, mTemperaturePaint);
            }
        }

        @Override
        public void onApplyWindowInsets(WindowInsets insets) {
            Resources resources = SunshineWatchFace.this.getResources();
            boolean isRound = insets.isRound();
            mXOffset = resources.getDimension(isRound
                    ? R.dimen.digital_x_offset_round : R.dimen.digital_x_offset);
            float timeTextSize = resources.getDimension(isRound
                    ? R.dimen.digital_time_text_size_round : R.dimen.digital_time_text_size);
            mTimeOfTheDayPaint.setTextSize(timeTextSize);
            float dateTextSize = resources.getDimension(isRound
                    ? R.dimen.date_text_size_round : R.dimen.date_text_size);
            mDatePaint.setTextSize(dateTextSize);
            float temperatureTextSize = resources.getDimension(isRound
                    ? R.dimen.temperature_text_size_round : R.dimen.temperature_text_size);
            mTemperaturePaint.setTextSize(temperatureTextSize);
        }

        @Override
        public void onVisibilityChanged(boolean visible) {
            super.onVisibilityChanged(visible);
        }

        @Override
        public void onConnected(@Nullable Bundle bundle) {
            Wearable.DataApi.addListener(mGoogleApiClient, this);
        }

        @Override
        public void onConnectionSuspended(int i) {

        }

        @Override
        public void onDataChanged(DataEventBuffer dataEventBuffer) {
            for (DataEvent event : dataEventBuffer) {
                DataItem item = event.getDataItem();
                if (WEATHER_DATA_URI.equals(item.getUri().getPath())) {
                    DataMap dataMap = DataMapItem.fromDataItem(item).getDataMap();
                    mWeather = dataMap.getString(WEATHER_DATA);
                    mWeatherId = dataMap.getInt(WEATHER_DATA_ID);

                    getBitMapForWeatherId();

                    SharedPreferences preferences = getSharedPreferences(PREFERENCES, MODE_PRIVATE);
                    SharedPreferences.Editor editor = preferences.edit();
                    editor.putString(KEY_WEATHER, mWeather);
                    editor.putInt(KEY_WEATHER_ID, mWeatherId);
                    editor.apply();
                }
            }
        }
    }
}
