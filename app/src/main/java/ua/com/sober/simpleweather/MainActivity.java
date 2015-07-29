package ua.com.sober.simpleweather;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewGroup.LayoutParams;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;

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

public class MainActivity extends Activity implements OnMapReadyCallback {

    private static final String jsonUrl = "http://api.openweathermap.org/data/2.5/weather?q=";
    private static String units = "";
    public static final String APP_PREFERENCES = "mysettings";
    public static final String APP_PREFERENCES_UNITS = "units";
    private SharedPreferences mSettings;
    private final CharSequence[] settingsItems = {"Standard", "metric", "imperial"};
    private TextView weatherInfo;
    private EditText searchCity;
    private LinearLayout iconsLayout;
    private static final String DEBUG_TAG = "debug";
    private static final String ICONS_KEY = "iconsKey";
    private static final String WEATHER_INFO_KEY = "weatherInfo";
    private GoogleMap map;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        iconsLayout = (LinearLayout) findViewById(R.id.iconsLayout);
        weatherInfo = (TextView) findViewById(R.id.weatherInfoTextView);
        mSettings = getSharedPreferences(APP_PREFERENCES, Context.MODE_PRIVATE);

//        App settings
        if (mSettings.contains(APP_PREFERENCES_UNITS)) {
            units = mSettings.getString(APP_PREFERENCES_UNITS, "");
        }

//        Restore View state
        if (savedInstanceState != null) {
            weatherInfo.setText(savedInstanceState.getCharSequence("weatherInfo"));

            if (savedInstanceState.getParcelable(ICONS_KEY) != null) {
                Bitmap tmp = savedInstanceState.getParcelable(ICONS_KEY);
                ImageView iconImages = new ImageView(this);
                iconImages.setImageDrawable(new BitmapDrawable(getResources(), tmp));
                iconsLayout.addView(iconImages);
            }

        }

//        Custom ActionBar
        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            actionBar.setCustomView(R.layout.actionbar_view);
            searchCity = (EditText) actionBar.getCustomView().findViewById(R.id.searchCityEditText);
            searchCity.setOnEditorActionListener(new TextView.OnEditorActionListener() {
                @Override
                public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {

                    new getWeatherInfoTask().execute(jsonUrl + v.getText() + units);

                    return false;
                }
            });
            actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);
        }

//        Google Map
        MapFragment mapFragment = (MapFragment) getFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        map = mapFragment.getMap();

    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putCharSequence(WEATHER_INFO_KEY, weatherInfo.getText());

        iconsLayout.setDrawingCacheEnabled(true);
        iconsLayout.buildDrawingCache(true);
        Bitmap iconsBitmap = Bitmap.createBitmap(iconsLayout.getDrawingCache());
        iconsLayout.setDrawingCacheEnabled(false);
        outState.putParcelable(ICONS_KEY, iconsBitmap);

    }

    @Override
    protected void onStop() {
        super.onStop();

//        Save settings
        SharedPreferences.Editor editor = mSettings.edit();
        editor.putString(APP_PREFERENCES_UNITS, units);
        editor.apply();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_settings: {

//                Choice units format
                int checkedItem;
                switch (units) {
                    case "":
                        checkedItem = 0;
                        break;
                    case "&units=metric":
                        checkedItem = 1;
                        break;
                    case "&units=imperial":
                        checkedItem = 2;
                        break;
                    default:
                        checkedItem = -1;
                        break;
                }
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle("Units format");
                builder.setCancelable(true);
                builder.setSingleChoiceItems(settingsItems, checkedItem, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        switch (which) {
                            case 0:
                                units = "";
                                break;
                            case 1:
                                units = "&units=metric";
                                break;
                            case 2:
                                units = "&units=imperial";
                                break;
                            default:
                                break;
                        }
                        dialog.dismiss();
                    }
                });
                builder.show();

                return true;
            }
            case R.id.getWeatherBtn: {

                new getWeatherInfoTask().execute(jsonUrl + searchCity.getText() + units);

//                Hide keyboard
                InputMethodManager inputManager = (InputMethodManager) this.getSystemService(Context.INPUT_METHOD_SERVICE);
                inputManager.hideSoftInputFromWindow(this.getCurrentFocus().getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);

                return true;
            }
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
    }

    private void showCityOnMap(LatLng latLng) {
        if (map != null) {
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15));
            map.animateCamera(CameraUpdateFactory.zoomTo(10), 2000, null);
        }
    }

    private void setWeatherIcon(Drawable drawable) {
        ImageView iconImage = new ImageView(this);
        iconImage.setLayoutParams(new LayoutParams(100, 100));
        iconImage.setImageDrawable(drawable);
        iconsLayout.addView(iconImage);
    }

    private class getWeatherInfoTask extends AsyncTask<String, Void, String> {

        private HttpURLConnection urlConnection = null;
        private BufferedReader bufferedReader = null;
        private String resultJson = "";

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
//            Clear old info
            if (iconsLayout.getChildCount() > 0) {
                iconsLayout.removeAllViews();
            }
            weatherInfo.setText("");
        }

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
                inputStream.close();
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

//            Test JSON string
//            parseJson("{\"coord\":{\"lon\":30.5,\"lat\":50.45},\"weather\":[{\"id\":520,\"main\":\"Rain\",\"description\":\"light intensity shower rain\",\"icon\":\"09d\"},{\"id\":701,\"main\":\"Mist\",\"description\":\"mist\",\"icon\":\"50d\"}],\"base\":\"stations\",\"main\":{\"temp\":13.2,\"pressure\":1006,\"humidity\":100,\"temp_max\":18},\"visibility\":3300,\"wind\":{\"speed\":3,\"deg\":300},\"clouds\":{\"all\":90},\"dt\":1438067252,\"sys\":{\"type\":1,\"id\":7358,\"message\":0.0081,\"country\":\"UA\",\"sunrise\":1438049986,\"sunset\":1438105700},\"id\":696050,\"name\":\"Pushcha-Voditsa\",\"cod\":200}");

        }

        private void parseJson(String strJson) {
            JSONObject mainJsonObject = null;
            double longitude = 0;
            double latitude = 0;

            try {
                mainJsonObject = new JSONObject(strJson);

                if (mainJsonObject.has("coord") && !mainJsonObject.isNull("coord")) {
                    JSONObject coord = mainJsonObject.getJSONObject("coord");
                    if (coord.has("lon") && !coord.isNull("lon")) {
                        weatherInfo.append("City geo location(longitude): " + coord.getString("lon"));
                        longitude = coord.getDouble("lon");
                    }
                    if (coord.has("lat") && !coord.isNull("lat")) {
                        weatherInfo.append("\nCity geo location(latitude): " + coord.getString("lat"));
                        latitude = coord.getDouble("lat");
                    }
                    showCityOnMap(new LatLng(latitude, longitude));
                }

                if (mainJsonObject.has("weather") && !mainJsonObject.isNull("weather")) {
                    JSONArray weatherArr = mainJsonObject.getJSONArray("weather");
                    for (int i = 0; i < weatherArr.length(); i++) {
                        weatherInfo.append("\nWeather(" + (i + 1) + "): ");
                        JSONObject weatherObj = weatherArr.getJSONObject(i);
                        if (weatherObj.has("id") && !weatherObj.isNull("id")) {
                            weatherInfo.append("\n\tWeather condition id: " + weatherObj.getString("id"));
                        }
                        if (weatherObj.has("main") && !weatherObj.isNull("main")) {
                            weatherInfo.append("\n\tGroup of weather parameters: " + weatherObj.getString("main"));
                        }
                        if (weatherObj.has("description") && !weatherObj.isNull("description")) {
                            weatherInfo.append("\n\tWeather condition within the group: " + weatherObj.getString("description"));
                        }
                        if (weatherObj.has("icon") && !weatherObj.isNull("icon")) {
                            new getIconTask().execute("http://api.openweathermap.org/img/w/" + weatherObj.getString("icon") + ".png");
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
                setWeatherIcon(drawable);
            }
        }

        private Drawable getImage(String url) {
            try {
                InputStream inputStream = (InputStream) new URL(url).getContent();
                drawable = Drawable.createFromStream(inputStream, "src name");
                inputStream.close();
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
