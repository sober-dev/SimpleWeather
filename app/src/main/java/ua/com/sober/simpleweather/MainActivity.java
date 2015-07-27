package ua.com.sober.simpleweather;

import android.app.Activity;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;


public class MainActivity extends Activity {

    private static final String jsonUrl = "http://api.openweathermap.org/data/2.5/weather?q=Emmen&units=metric";
    private static String basicImgUrl = "http://api.openweathermap.org/img/w/";
    private TextView weatherInfo;
    private ImageView weatherIcon;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        weatherIcon = (ImageView) findViewById(R.id.weatherIconImageView);
        weatherInfo = (TextView) findViewById(R.id.weatherInfoTextView);

        new getWeatherInfoTask().execute(jsonUrl);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private class getWeatherInfoTask extends AsyncTask<String, Void, String> {

        private HttpURLConnection urlConnection = null;
        private BufferedReader bufferedReader = null;
        private String resultJson = "";
        private static final String DEBUG_TAG = "getWeatherInfoTask";

        @Override
        protected String doInBackground(String... params) {

            String url = "";
            if (params.length > 0) {
                url = params[0];
            }

            InputStream inputStream = null;

            try {
                URL urlConn = new URL(url);
                urlConnection = (HttpURLConnection) urlConn.openConnection();
                urlConnection.setRequestMethod("GET");
                urlConnection.connect();
                inputStream = urlConnection.getInputStream();
                bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
                resultJson = bufferedReader.readLine();
                bufferedReader.close();
            } catch (MalformedURLException e) {
                Log.d(DEBUG_TAG, "Something wrong with URL...");
                e.printStackTrace();
            } catch (IOException e) {
                Log.d(DEBUG_TAG, "Something wrong with connection...");
                e.printStackTrace();
            }

            return resultJson;
        }

        @Override
        protected void onPostExecute(String strJson) {
            super.onPostExecute(strJson);
            Log.i(DEBUG_TAG, strJson);

            parseJson(strJson);

            new getIconTask().execute(basicImgUrl);
        }

        private void parseJson(String strJson) {
            JSONObject mainJsonObject = null;

            try {
                mainJsonObject = new JSONObject(strJson);

                if (mainJsonObject.has("coord") && !mainJsonObject.isNull("coord")) {
                    JSONObject coord = mainJsonObject.getJSONObject("coord");
//                    weatherInfo.append("\tCity geo location");
                    if (coord.has("lon") && !coord.isNull("lon")) {
                        weatherInfo.append("\nlongitude: " + coord.getString("lon"));
                    }
                    if (coord.has("lat") && !coord.isNull("lat")) {
                        weatherInfo.append("\nlatitude: " + coord.getString("lat"));
                    }
                }

                if (mainJsonObject.has("weather") && !mainJsonObject.isNull("weather")) {
                    JSONArray weatherArr = mainJsonObject.getJSONArray("weather");
                    if (weatherArr.length() > 0) {
                        JSONObject weatherObj = weatherArr.getJSONObject(0);
                        if (weatherObj.has("id") && !weatherObj.isNull("id")) {
                            weatherInfo.append("\nWeather condition id: " + weatherObj.getString("id"));
                        }
                        if (weatherObj.has("main") && !weatherObj.isNull("main")) {
                            weatherInfo.append("\nGroup of weather parameters: " + weatherObj.getString("main"));
                        }
                        if (weatherObj.has("description") && !weatherObj.isNull("description")) {
                            weatherInfo.append("\nWeather condition within the group: " + weatherObj.getString("description"));
                        }
                        if (weatherObj.has("icon") && !weatherObj.isNull("icon")) {
                            basicImgUrl = basicImgUrl + weatherObj.getString("icon") + ".png";
                        }
                    }
                }

                if (mainJsonObject.has("base") && !mainJsonObject.isNull("base")) {
                    weatherInfo.append("\nInternal parameter: " + mainJsonObject.getString("base"));
                }

                if (mainJsonObject.has("main") && !mainJsonObject.isNull("main")) {
                    JSONObject main = mainJsonObject.getJSONObject("main");
                    if (main.has("temp") && !main.isNull("temp")) {
                        weatherInfo.append("\nTemperature: " + main.getString("temp"));
                    }
                    if (main.has("pressure") && !main.isNull("pressure")) {
                        weatherInfo.append("\nAtmospheric pressure, hPa: " + main.getString("pressure"));
                    }
                    if (main.has("humidity") && !main.isNull("humidity")) {
                        weatherInfo.append("\nHumidity, %: " + main.getString("humidity"));
                    }
                    if (main.has("temp_min") && !main.isNull("temp_min")) {
                        weatherInfo.append("\nMinimum temperature at the moment: " + main.getString("temp_min"));
                    }
                    if (main.has("temp_max") && !main.isNull("temp_max")) {
                        weatherInfo.append("\nMaximum temperature at the moment: " + main.getString("temp_max"));
                    }
                    if (main.has("sea_level") && !main.isNull("sea_level")) {
                        weatherInfo.append("\nAtmospheric pressure on the sea level, hPa: " + main.getString("sea_level"));
                    }
                    if (main.has("grnd_level") && !main.isNull("grnd_level")) {
                        weatherInfo.append("\nAtmospheric pressure on the ground level, hPa: " + main.getString("grnd_level"));
                    }
                }

                if (mainJsonObject.has("visibility") && !mainJsonObject.isNull("visibility")) {
                    weatherInfo.append("\nVisibility, meter: " + mainJsonObject.getString("visibility"));
                }

                if (mainJsonObject.has("wind") && !mainJsonObject.isNull("wind")) {
                    JSONObject wind = mainJsonObject.getJSONObject("wind");
                    if (wind.has("speed") && !wind.isNull("speed")) {
                        weatherInfo.append("\nWind speed: " + wind.getString("speed"));
                    }
                    if (wind.has("deg") && !wind.isNull("deg")) {
                        weatherInfo.append("\nWind direction, degrees (meteorological): " + wind.getString("deg"));
                    }
                }

                if (mainJsonObject.has("clouds") && !mainJsonObject.isNull("clouds")) {
                    JSONObject clouds = mainJsonObject.getJSONObject("clouds");
                    weatherInfo.append("\nCloudiness, %: " + clouds.getString("all"));
                }

                if (mainJsonObject.has("rain") && !mainJsonObject.isNull("rain")) {
                    JSONObject rain = mainJsonObject.getJSONObject("rain");
                    String hoursValue = rain.keys().next();
                    weatherInfo.append("\nRain volume(" + hoursValue + "): " + rain.getString(hoursValue));
                }

                if (mainJsonObject.has("snow") && !mainJsonObject.isNull("snow")) {
                    JSONObject snow = mainJsonObject.getJSONObject("snow");
                    String hoursValue = snow.keys().next();
                    weatherInfo.append("\nSnow volume(" + hoursValue + "): " + snow.getString(hoursValue));
                }

                if (mainJsonObject.has("dt") && !mainJsonObject.isNull("dt")) {
                    java.util.Date time = new java.util.Date(mainJsonObject.getLong("dt") * 1000);
                    weatherInfo.append("\nTime of data calculation: " + time);
                }

                if (mainJsonObject.has("sys") && !mainJsonObject.isNull("sys")) {
                    JSONObject sys = mainJsonObject.getJSONObject("sys");
                    if (sys.has("type") && !sys.isNull("type")) {
                        weatherInfo.append("\nInternal parameter(type): " + sys.getString("type"));
                    }
                    if (sys.has("id") && !sys.isNull("id")) {
                        weatherInfo.append("\nInternal parameter(id): " + sys.getString("id"));
                    }
                    if (sys.has("message") && !sys.isNull("message")) {
                        weatherInfo.append("\nInternal parameter(message): " + sys.getString("message"));
                    }
                    if (sys.has("country") && !sys.isNull("country")) {
                        weatherInfo.append("\nCountry code: " + sys.getString("country"));
                    }
                    if (sys.has("sunrise") && !sys.isNull("sunrise")) {
                        java.util.Date time = new java.util.Date(sys.getLong("sunrise") * 1000);
                        weatherInfo.append("\nSunrise time: " + time);
                    }
                    if (sys.has("sunset") && !sys.isNull("sunset")) {
                        java.util.Date time = new java.util.Date(sys.getLong("sunset") * 1000);
                        weatherInfo.append("\nSunset time: " + time);
                    }
                }

                if (mainJsonObject.has("id") && !mainJsonObject.isNull("id")) {
                    weatherInfo.append("\nCity ID: " + mainJsonObject.getString("id"));
                }

                if (mainJsonObject.has("name") && !mainJsonObject.isNull("name")) {
                    weatherInfo.append("\nCity name: " + mainJsonObject.getString("name"));
                }

                if (mainJsonObject.has("cod") && !mainJsonObject.isNull("cod")) {
                    weatherInfo.append("\nInternal parameter(cod): " + mainJsonObject.getString("cod"));
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }

        }
    }

    private class getIconTask extends AsyncTask<String, Void, Drawable> {

        private Drawable drawable = null;
        private static final String DEBUG_TAG = "getIconTask";

        @Override
        protected Drawable doInBackground(String... params) {

            String url = "";
            if (params.length > 0) {
                url = params[0];
            }

            drawable = getImage(url);
            return drawable;
        }

        @Override
        protected void onPostExecute(Drawable drawable) {
            super.onPostExecute(drawable);
            if (drawable != null) {
                weatherIcon.setImageDrawable(drawable);
            } else {
                weatherIcon.setImageResource(R.mipmap.ic_launcher);
            }
        }

        private Drawable getImage(String url) {
            try {
                InputStream inputStream = (InputStream) new URL(url).getContent();
                drawable = Drawable.createFromStream(inputStream, "src name");
            } catch (MalformedURLException e) {
                Log.d(DEBUG_TAG, "Something wrong with URL...");
                e.printStackTrace();
            } catch (IOException e) {
                Log.d(DEBUG_TAG, "Something wrong with connection...");
                e.printStackTrace();
            }
            return drawable;
        }
    }
}
