package com.example.weather.Utils;

import android.content.Context;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.weather.Api.ApiService;
import com.example.weather.Models.ForecastItem;
import com.example.weather.Models.ForecastListItem;
import com.example.weather.Models.ForecastResponse;
import com.example.weather.Models.WeatherData;
import com.example.weather.R;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.TreeMap;
import java.util.function.Consumer;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class WeatherUtils {
    public interface WeatherCallback {
        void onResult(String condition, String descriptionWeather);
    }
    public static void callApiUI(Context context, String place, WeatherUIComponents ui) {
        final boolean[] forecastLoaded = {false};
        final boolean[] weatherLoaded = {false};
        final double[] minTemp = {Double.MAX_VALUE};
        final double[] maxTemp = {Double.MIN_VALUE};
        ApiService.apiService.convertForecast(place, "f247b776bb63be4537ae6466fdcb2d1d", "metric")
                .enqueue(new Callback<ForecastResponse>() {

                    @Override
                    public void onResponse(Call<ForecastResponse> call, Response<ForecastResponse> response) {
                        if (response.isSuccessful() && response.body() != null && response.body().list != null) {
                            List<ForecastListItem> forecastList = response.body().list;
                            if (forecastList.isEmpty()) return;

                            for (ForecastListItem item : forecastList) {
                                if (item.main.temp_min < minTemp[0]) minTemp[0] = item.main.temp_min;
                                if (item.main.temp_max > maxTemp[0]) maxTemp[0] = item.main.temp_max;
                            }
                            forecastLoaded[0] = true;
                            if (weatherLoaded[0]) {
                                ui.maxTemp.setText(String.format("Max: %.1f°C", maxTemp[0]));
                                ui.minTemp.setText(String.format("Min: %.1f°C", minTemp[0]));
                            }
                        }
                    }
                    @Override
                    public void onFailure(Call<ForecastResponse> call, Throwable t) {
                        Log.e("WeatherAPI", "Forecast API failed", t);
                    }
                });

        ApiService.apiService.convertWeather(place, "f247b776bb63be4537ae6466fdcb2d1d", "metric")
                .enqueue(new Callback<WeatherData>() {

                    @Override
                    public void onResponse(Call<WeatherData> call, Response<WeatherData> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            WeatherData responseBody = response.body();
                            ui.temp.setText(String.format("%.1f°C", responseBody.main.temp));
                            ui.humidity.setText(responseBody.main.humidity + "%");
                            ui.windSpeed.setText(responseBody.wind.speed + " m/s");
                            ui.sunRise.setText(timeFormat(responseBody.sys.sunrise));
                            ui.sunSet.setText(timeFormat(responseBody.sys.sunset));
                            ui.seaLevel.setText(responseBody.main.pressure + " hPa");
                            ui.condition.setText(responseBody.weather.get(0).main);
                            ui.weather.setText(responseBody.weather.get(0).main);
                            ui.day.setText(dayName(responseBody.dt));
                            ui.date.setText(dateName(responseBody.dt));
                            ui.location.setText(place);
                            changeImageFromWeatherCondition(ui, ui.condition.getText().toString());

                            weatherLoaded[0] = true;
                            if (forecastLoaded[0]) {
                                ui.maxTemp.setText(String.format("Max: %.1f°C", maxTemp[0]));
                                ui.minTemp.setText(String.format("Min: %.1f°C", minTemp[0]));
                            }
                        }
                    }

                    @Override
                    public void onFailure(Call<WeatherData> call, Throwable t) {
                        Log.e("WeatherAPI", "Weather API failed", t);
                    }
                });

    }

    public static void callForecastList(String place, Consumer<List<ForecastItem>> callback) {
        ApiService.apiService.convertForecast(place, "f247b776bb63be4537ae6466fdcb2d1d", "metric")
                .enqueue(new Callback<ForecastResponse>() {
                    @Override
                    public void onResponse(Call<ForecastResponse> call, Response<ForecastResponse> response) {
                        if (response.isSuccessful() && response.body() != null && response.body().list != null) {
                            List<ForecastListItem> forecastList = response.body().list;
                            Map<String, List<ForecastListItem>> dayGroups = new TreeMap<>();

                            SimpleDateFormat dateTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                            SimpleDateFormat dateOnly = new SimpleDateFormat("yyyy-MM-dd");
                            SimpleDateFormat dayFormat = new SimpleDateFormat("EEE");

                            dateTime.setTimeZone(TimeZone.getDefault());
                            dateOnly.setTimeZone(TimeZone.getDefault());
                            dayFormat.setTimeZone(TimeZone.getDefault());

                            for (ForecastListItem item : forecastList) {
                                try {
                                    Date date = dateTime.parse(item.dt_txt);
                                    String day = dateOnly.format(date);

                                    if (!dayGroups.containsKey(day)) {
                                        dayGroups.put(day, new ArrayList<>());
                                    }
                                    dayGroups.get(day).add(item);
                                } catch (ParseException e) {
                                    e.printStackTrace();
                                }
                            }

                            String today = dateOnly.format(new Date());
                            List<ForecastItem> nextDays = new ArrayList<>();

                            for(String day : dayGroups.keySet()){
                                if(day.equals(today)) continue;

                                List<ForecastListItem> items = dayGroups.get(day);
                                double min = Double.MAX_VALUE;
                                double max = Double.MIN_VALUE;
                                String condition ="";

                                for(ForecastListItem itemWeather: items){
                                    if(itemWeather.main.temp_min < min) min = itemWeather.main.temp_min;
                                    if(itemWeather.main.temp_max > max) max = itemWeather.main.temp_max;
                                    condition = itemWeather.weather.get(0).main;
                                }

                                try {
                                    String next_day = dayFormat.format(dateOnly.parse(day));
                                    nextDays.add(new ForecastItem(next_day, min, max, condition));
                                } catch (ParseException e) {
                                    e.printStackTrace();
                                }
                            }
                            callback.accept(nextDays);
                        }
                    }
                    @Override
                    public void onFailure(Call<ForecastResponse> call, Throwable t) {
                        Log.e("ForecastList", "Failed", t);
                    }
                });
    }

    public static void callApiReceiver(Context context, String place, WeatherCallback callback) {
        ApiService.apiService.convertWeather(place, "f247b776bb63be4537ae6466fdcb2d1d", "metric")
                .enqueue(new Callback<WeatherData>() {
                    @Override
                    public void onResponse(Call<WeatherData> call, Response<WeatherData> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            WeatherData responseBody= response.body();

                            String condition = responseBody.weather.get(0).main;
                            String descriptionWeather = responseBody.weather.get(0).description;

                            callback.onResult(condition, descriptionWeather);
                        }
                    }

                    @Override
                    public void onFailure(Call<WeatherData> call, Throwable t) {
                        callback.onResult("Unclear", "Connection error");
                    }
                });
    }

    public static void callApiManager(Context context, String place, TextView tempResult, ImageView weatherIcon) {
        ApiService.apiService.convertWeather(place, "f247b776bb63be4537ae6466fdcb2d1d", "metric")
                .enqueue(new Callback<WeatherData>() {
                    @Override
                    public void onResponse(Call<WeatherData> call, Response<WeatherData> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            WeatherData responseBody = response.body();
                            double temperature = responseBody.main.temp;
                            tempResult.setText(String.format(Locale.US, "%.1f°C", temperature));

                            String weatherMain = responseBody.weather.get(0).main;
                            int iconResId = getWeatherIconResource(weatherMain);
                            weatherIcon.setImageResource(iconResId);

                        } else {
                            Log.e("WeatherAPI", "Unsuccessful response or empty body");
                        }
                    }

                    @Override
                    public void onFailure(Call<WeatherData> call, Throwable t) {
                        Log.e("WeatherAPI", "Error fetching weather", t);
                    }
                });
    }

    private static int getWeatherIconResource(String weatherMain) {
        switch (weatherMain) {
            case "Clear":
                return R.drawable.clear_sky;
            case "Rain":
                return R.drawable.heavy_rain;
            case "Clouds":
                return R.drawable.cloud;
            case "Snow":
                return R.drawable.snowy;
            case "Thunderstorm":
                return R.drawable.thunderstorm;
            default:
                return R.drawable.sunny;
        }
    }
    private static void changeImageFromWeatherCondition(WeatherUIComponents ui, String condition) {
        condition = condition.toLowerCase();

        if (condition.contains("clear") || condition.contains("sunny")) {
            ui.constraintLayout.setBackgroundResource(R.drawable.sunny_background);
            ui.lottieAnimationView.setAnimation(R.raw.sun);
        } else if (condition.contains("cloud") || condition.contains("mist") || condition.contains("fog")) {
            ui.constraintLayout.setBackgroundResource(R.drawable.colud_background);
            ui.lottieAnimationView.setAnimation(R.raw.cloud);
        } else if (condition.contains("rain") || condition.contains("drizzle")) {
            ui.constraintLayout.setBackgroundResource(R.drawable.rain_background);
            ui.lottieAnimationView.setAnimation(R.raw.rain);
        } else if (condition.contains("snow")) {
            ui.constraintLayout.setBackgroundResource(R.drawable.snow_background);
            ui.lottieAnimationView.setAnimation(R.raw.snow);
        }else {
            ui.constraintLayout.setBackgroundResource(R.drawable.sunny_background);
            ui.lottieAnimationView.setAnimation(R.raw.sun);
        }
        ui.lottieAnimationView.playAnimation();
    }

    public static void changeIconFromWeatherCondition(ImageView imgForecast, String condition) {
        condition = condition.toLowerCase();

        if (condition.contains("clear") || condition.contains("sunny")) {
            imgForecast.setImageResource(R.drawable.clear_sky);
        } else if (condition.contains("cloud") || condition.contains("mist") || condition.contains("fog")) {
            imgForecast.setImageResource(R.drawable.cloud);
        } else if (condition.contains("rain") || condition.contains("drizzle")) {
            imgForecast.setImageResource(R.drawable.heavy_rain);
        } else if (condition.contains("snow")) {
            imgForecast.setImageResource(R.drawable.snowy);
        }else {
            imgForecast.setImageResource(R.drawable.clear_sky);
        }
    }

    private static String dayName(long timeStamp) {
        SimpleDateFormat sdf = new SimpleDateFormat("EEEE", Locale.getDefault());
        return sdf.format(new Date(timeStamp * 1000));
    }
    private static String dateName(long timeStamp) {
        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
        return sdf.format(new Date(timeStamp * 1000));
    }
    private static String timeFormat(long timestamp) {
        SimpleDateFormat sdf = new SimpleDateFormat("hh:mm a", Locale.getDefault());
        return sdf.format(new Date(timestamp * 1000));
    }


}
