package com.example.location;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback {

    ProgressDialog pd;
    GoogleMap mMap;
    private static final String MAPVIEW_BUNDLE_KEY = "MapViewBundleKey";
    Location currLocat;
    FusedLocationProviderClient fusedLocationProviderClient;
    private static final int REQUEST_CODE = 101;
    TextView Lat, Long, DistanceBetween, From2;
    EditText City;
    Double Lat1,Long1;
    Button fetch;
    private MapView maps;
    LatLng latLng;
    Marker marker = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        fetchLastLocation();
        Lat = (TextView) findViewById(R.id.textView);
        Long = (TextView) findViewById(R.id.textView2);
        fetch = (Button) findViewById(R.id.button);
        City = (EditText) findViewById(R.id.textInputEditText);
        DistanceBetween = (TextView) findViewById(R.id.textView3);
        From2 = (TextView) findViewById(R.id.textView4);
        Bundle mapViewBundle = null;
        if (savedInstanceState != null) { mapViewBundle = savedInstanceState.getBundle(MAPVIEW_BUNDLE_KEY); }
        maps = (MapView) findViewById(R.id.mapView);
        maps.onCreate(mapViewBundle);
        maps.getMapAsync(this);

        fetch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                GetCity();
            }});
    }

    private void fetchLastLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,new String[]
                    {Manifest.permission.ACCESS_FINE_LOCATION},REQUEST_CODE);
            return;
        }
        Task<Location> task = fusedLocationProviderClient.getLastLocation();
        task.addOnSuccessListener(new OnSuccessListener<Location>() {
            @Override
            public void onSuccess(Location location) {
                if(location != null){
                    currLocat = location;
                    Lat1 = currLocat.getLatitude();
                    Long1 = currLocat.getLongitude();
                    Lat.setText("Latitute: "+Lat1);
                    Long.setText("Longitude: "+Long1);
                    latLng = new LatLng(currLocat.getLatitude(),currLocat.getLongitude());
                    MarkerOptions markerOptions = new MarkerOptions().position(latLng).title("Yo");
                    mMap.animateCamera(CameraUpdateFactory.newLatLng(latLng));
                    mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng,17));
                    mMap.addMarker(markerOptions);
                }
            }
    });
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(this,R.raw.style));
    }

    @Override
    protected void onResume() { maps.onResume(); super.onResume(); }

    public void GetCity(){
        Geocoder geocoder = new Geocoder(this);
        List<Address> addressList;
        try{
            addressList = geocoder.getFromLocationName(City.getText().toString(),1);
            if(addressList != null){
                double Lat2 = addressList.get(0).getLatitude();
                double Long2 = addressList.get(0).getLongitude();
                LatLng latLng2 = new LatLng(Lat2,Long2);
                if(marker != null){ marker.remove(); }
                marker = mMap.addMarker(new MarkerOptions()
                        .position(latLng2).title("here")
                        .icon(BitmapDescriptorFactory.defaultMarker(200)));
                new JsonTask().execute("https://maps.googleapis.com/maps/api/distancematrix/json?destinations="+Lat1+","+Long1+
                        "&origins="+Lat2+","+Long2+
                        "&units=metric&key="+getString(R.string.map_key));
            }

        } catch (Exception e) { e.printStackTrace(); }
    }
    private class JsonTask extends AsyncTask<String, String, String> {

        protected void onPreExecute() {
            super.onPreExecute();

            pd = new ProgressDialog(MainActivity.this);
            pd.setMessage("Please wait");
            pd.setCancelable(false);
            pd.show();
        }

        protected String doInBackground(String... params) {

            HttpURLConnection connection = null;
            BufferedReader reader = null;

            try {
                URL url = new URL(params[0]);
                connection = (HttpURLConnection) url.openConnection();
                connection.connect();

                InputStream stream = connection.getInputStream();

                reader = new BufferedReader(new InputStreamReader(stream));

                StringBuffer buffer = new StringBuffer();
                String line = "";

                while ((line = reader.readLine()) != null) {
                    buffer.append(line+"\n");
                    Log.d("Response: ", "> " + line);
                }

                return buffer.toString();

            } catch (MalformedURLException e) { e.printStackTrace();
            } catch (IOException e) { e.printStackTrace(); }
            finally { if (connection != null) { connection.disconnect(); }
                try { if (reader != null) { reader.close(); }
                } catch (IOException e) { e.printStackTrace(); }
            }
            return null;
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            if (pd.isShowing()){ pd.dismiss(); }
            JSONObject data = null;
            try {
                data = new JSONObject(result);
                if(data!=null){
                    String Dist = ""+data.getJSONArray("rows").
                            getJSONObject(0).
                            getJSONArray("elements").
                            getJSONObject(0).
                            getJSONObject("distance").
                            getString("text");
                    String F2 = data.getJSONArray("destination_addresses").getString(0)
                            +"\n ➜ \n"+
                            data.getJSONArray("origin_addresses").getString(0);
                    DistanceBetween.setText(Dist);
                    From2.setText(F2);
                }
            } catch (JSONException e) { e.printStackTrace(); }
        }
    }
}