package com.example.coolweather;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.example.coolweather.gson.Forecast;
import com.example.coolweather.gson.Weather;
import com.example.coolweather.util.HttpUtil;
import com.example.coolweather.util.Utility;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class WeatherActivity extends AppCompatActivity {

    private final static String TAG = "WeatherActivity";

    private ScrollView sc_weather;
    private TextView tv_city;
    private LinearLayout ll_forecast;
    private TextView tv_update_time;
    private TextView tv_degree;
    private TextView tv_weather_info;
    private TextView tv_aqi;
    private TextView tv_pm25;
    private TextView tv_comfort;
    private TextView tv_car_wash;
    private TextView tv_sport;
    private ImageView iv_bing;
    public SwipeRefreshLayout swipe_refresh;

    private String mWeatherId;
    public DrawerLayout drawer_layout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_weather);

        if (Build.VERSION.SDK_INT >= 21) {
            //拿到当前活动的DecorView
            View decorView = getWindow().getDecorView();
            //改变系统UI的显示
            decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
            //将状态栏设置成透明色
            getWindow().setStatusBarColor(Color.TRANSPARENT);
        }

        iv_bing = findViewById(R.id.iv_bing);

        sc_weather = findViewById(R.id.sc_weather);
        tv_city = findViewById(R.id.tv_city);
        ll_forecast = findViewById(R.id.ll_forecast);
        tv_update_time = findViewById(R.id.tv_update_time);
        tv_degree = findViewById(R.id.tv_degree);
        tv_weather_info = findViewById(R.id.tv_weather_info);
        tv_aqi = findViewById(R.id.tv_aqi);
        tv_pm25 = findViewById(R.id.tv_pm25);
        tv_comfort = findViewById(R.id.tv_comfort);
        tv_car_wash = findViewById(R.id.tv_car_wash);
        tv_sport = findViewById(R.id.tv_sport);
        swipe_refresh = findViewById(R.id.swipe_refresh);
        drawer_layout = findViewById(R.id.drawer_layout);
        Button bt_nav = findViewById(R.id.bt_nav);


        //设置下拉刷新进度条的颜色
        swipe_refresh.setColorSchemeResources(R.color.colorPrimary);

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);

        //背景图
        String bingPic = prefs.getString("bing_pic", null);
        if(bingPic != null) {
            Glide.with(this).load(bingPic).into(iv_bing);
        } else {
            loadBingPic();
        }

        String weatherString = prefs.getString("weather",null);
        if (weatherString != null) {
            //有缓存时直接解析天气数据
            Weather weather = Utility.handleWeatherResponse(weatherString);
            mWeatherId = weather.basic.weatherId;
            showWeatherInfo(weather);
        } else {
            //无缓存时去服务器查询天气
            String weatherId = getIntent().getStringExtra("weather_id");
            sc_weather.setVisibility(View.INVISIBLE);
            requestWeather(weatherId);
        }

        //下拉刷新监听器
        swipe_refresh.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                requestWeather(mWeatherId);
            }
        });

        bt_nav.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                drawer_layout.openDrawer(GravityCompat.START); //打开滑动菜单
            }
        });

    }

    /**
     * 根据天气id请求城市天气信息
     * @param weatherId
     */
    public void requestWeather(final String weatherId) {
        String weatherUrl = "http://guolin.tech/api/weather?cityid=" + weatherId +"&key=5a6e3123570c43759e57638c30d159f8";

        HttpUtil.sendOkHttpRequest(weatherUrl, new Callback() {
            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                final String responseText = response.body().string();
                final Weather weather = Utility.handleWeatherResponse(responseText);

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (weather != null && "ok".equals(weather.status)) {
                            SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(WeatherActivity.this).edit();
                            editor.putString("weather", responseText);
                            editor.apply();
                            showWeatherInfo(weather);
                        } else {
                            Toast.makeText(WeatherActivity.this, "获取天气信息失败", Toast.LENGTH_SHORT).show();
                        }
                        swipe_refresh.setRefreshing(false); //刷新事件结束，隐藏刷新进度条
                    }
                });
            }

            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                e.printStackTrace();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(WeatherActivity.this, "获取天气信息失败", Toast.LENGTH_SHORT).show();
                        swipe_refresh.setRefreshing(false);//刷新事件结束，隐藏刷新进度条
                    }
                });
            }
        });

        loadBingPic(); //每次请求天气信息的同时会刷新背景图片
    }

    /**
     * 处理并展示Weather实体类中的数据
     * @param weather
     */
    private void showWeatherInfo(Weather weather) {
        Log.d(TAG, "showWeatherInfo()------weather："+weather);
        String cityName = weather.basic.cityName;
        String updateTime = weather.basic.update.updateTime.split(" ")[1];
        String degree = weather.now.temperature + "°C";
        String weatherInfo = weather.now.more.info;

        tv_city.setText(cityName);
        tv_update_time.setText(updateTime);
        tv_degree.setText(degree);
        tv_weather_info.setText(weatherInfo);

        ll_forecast.removeAllViews();

        for (Forecast forecast : weather.forecastList) {
            View view = LayoutInflater.from(this).inflate(R.layout.forecast_item, ll_forecast, false);
            TextView tv_date = view.findViewById(R.id.tv_date);
            TextView tv_info = view.findViewById(R.id.tv_info);
            TextView tv_max = view.findViewById(R.id.tv_max);
            TextView tv_min = view.findViewById(R.id.tv_min);

            tv_date.setText(forecast.date);
            tv_info.setText(forecast.more.info);
            tv_max.setText(forecast.temperature.max);
            tv_min.setText(forecast.temperature.min);

            ll_forecast.addView(view);
        }

        if (weather.aqi != null) {
            tv_aqi.setText(weather.aqi.city.aqi);
            tv_pm25.setText(weather.aqi.city.pm25);
        }

        String comfort = "舒适度："+weather.suggestion.comfort.info;
        String carWash = "洗车指数："+weather.suggestion.carWash.info;
        String sport = "运动建议："+weather.suggestion.sport.info;

        tv_comfort.setText(comfort);
        tv_car_wash.setText(carWash);
        tv_sport.setText(sport);

        sc_weather.setVisibility(View.VISIBLE);
    }

    /**
     * 加载必应每日一图
     */
    public void loadBingPic() {
        String requestBingPic = "https://cn.bing.com/HPImageArchive.aspx?format=js&idx=0&n=1";
        HttpUtil.sendOkHttpRequest(requestBingPic, new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                e.printStackTrace();
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                final String responseText = response.body().string();
                final String url = Utility.handlePicurlResponse(responseText);
                final String bingPic = "http://cn.bing.com"+url;
                SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(WeatherActivity.this).edit();
                editor.putString("bing_pic",bingPic);
                editor.apply();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Glide.with(WeatherActivity.this).load(bingPic).into(iv_bing);
                    }
                });

            }
        });
    }
}