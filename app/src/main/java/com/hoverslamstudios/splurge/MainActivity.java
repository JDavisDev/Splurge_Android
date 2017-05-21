package com.hoverslamstudios.splurge;
import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TextInputEditText;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.EditText;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResult;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

public class MainActivity
		extends AppCompatActivity
		implements com.google.android.gms.location.LocationListener,
		GoogleApiClient.ConnectionCallbacks {

	private TextInputEditText locationEditText;
	private ArrayList<Place> nearbyPlaceList;
	private GoogleApiClient mGoogleApiClient;
	private Location mLastLocation;
	private String latitude;
	private String longitude;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
		setSupportActionBar(toolbar);
		bindViews();
		checkPermissions();

		// Create an instance of GoogleAPIClient.
		if (mGoogleApiClient == null) {
			mGoogleApiClient = new GoogleApiClient.Builder(this)
					.addConnectionCallbacks(this)
					.addApi(LocationServices.API)
					.build();
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.menu_main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();

		//noinspection SimplifiableIfStatement
		if (id == R.id.action_settings) {
			return true;
		}

		return super.onOptionsItemSelected(item);
	}

	@Override
	public void onStart() {
		mGoogleApiClient.connect();
		super.onStart();
	}

	@Override
	public void onStop() {
		super.onStop();
	}

	@Override
	public void onResume() {
		super.onResume();
	}

	private void bindViews() {
		Button submitButton = (Button) findViewById(R.id.searchButton);
		submitButton.setOnClickListener(submitButtonClickListener);

		Button getLocationButton = (Button) findViewById(R.id.getLocationButton);
		getLocationButton.setOnClickListener(getLocationClickListener);

		locationEditText = (TextInputEditText) findViewById(R.id.locationEditText);
	}

	View.OnClickListener submitButtonClickListener = new View.OnClickListener() {
		@Override
		public void onClick(View v) {
			performPlacesRequest();
		}
	};

	View.OnClickListener getLocationClickListener = new View.OnClickListener() {
		@Override
		public void onClick(View v) {
			performPlacesRequest();
		}
	};

	private void startLocationUpdates() {
		checkPermissions();

		LocationRequest request = createLocationRequest();

		LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
				.addLocationRequest(request);

		PendingResult<LocationSettingsResult> result =
				LocationServices.SettingsApi.checkLocationSettings(mGoogleApiClient,
						builder.build());

		try {
			LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, request, this);
		} catch(SecurityException ex) {

		}
	}

	/**
	 * LISTENER METHODS
	 */
	@Override
	public void onConnected(@Nullable Bundle bundle) {
		startLocationUpdates();
	}

	@Override
	public void onConnectionSuspended(int i) {

	}

	@Override
	public void onLocationChanged(Location location) {
		String.valueOf(location);
	}

	protected LocationRequest createLocationRequest() {
		LocationRequest mLocationRequest = new LocationRequest();
		mLocationRequest.setInterval(10000);
		mLocationRequest.setFastestInterval(5000);
		mLocationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);
		return mLocationRequest;
	}

	private void checkPermissions() {
		if (ContextCompat.checkSelfPermission(this,
				Manifest.permission.ACCESS_COARSE_LOCATION)
				== PackageManager.PERMISSION_GRANTED) {
			// we have permissions...
		} else {
			ActivityCompat.requestPermissions(this,
					new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, PermissionsHelper.LOCATION);
		}
	}

	@Override
	public void onRequestPermissionsResult(int requestCode,
	                                       @NonNull String permissions[], @NonNull int[] grantResults) {
		switch (requestCode) {
			case PermissionsHelper.LOCATION: {
				// If request is cancelled, the result arrays are empty.
				if (grantResults.length > 0
						&& grantResults[0] == PackageManager.PERMISSION_GRANTED) {
					performPlacesRequest();
				} else if (grantResults.length > 0 &&
						grantResults[0] == PackageManager.PERMISSION_DENIED) {
					Snackbar.make(findViewById(R.id.mainContentView), "Please enable location permissions in app settings", Snackbar.LENGTH_LONG)
							.setAction("Settings", new View.OnClickListener() {
								@Override
								public void onClick(View v) {
									// settings intent
									startActivityForResult(new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS), 0);
								}
							}).show();
				}
			}
		}
	}

	private void getLastLocation() {
		checkPermissions();
		try {
			mLastLocation = LocationServices.FusedLocationApi.getLastLocation(
					mGoogleApiClient);
			if (mLastLocation != null) {
				latitude = String.valueOf(mLastLocation.getLatitude());
				longitude = String.valueOf(mLastLocation.getLongitude());
				locationEditText.setText("Current Location");
			}
		} catch (SecurityException ex) {

		}
	}

	private void performPlacesRequest() {
		getLastLocation();
		// example
		// https://maps.googleapis.com/maps/api/place/nearbysearch/json?location=-33.8670522,151.1957362&radius=500&type=restaurant&keyword=cruise&key=YOUR_API_KEY
		final String GOOGLE_API_KEY = "AIzaSyCmQ5BBi-AJ7sY2w8JsicR00FjZHFB8nCo";
		final String priceLevel;
		final String radius;
		final String foodURL = "https://maps.googleapis.com/maps/api/place/nearbysearch/json?" +
				"location=" + getUserLocation() + "&" +
				"maxPriceLevel=4&" +
				"radius=50000&" +
				"type=restaurant&" +
				"key=" + GOOGLE_API_KEY;


		// Instantiate the RequestQueue.
		RequestQueue queue = Volley.newRequestQueue(this);

		// Request a string response from the provided URL.
		JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, foodURL,
				null,
				new Response.Listener<JSONObject>() {
					@Override
					public void onResponse(JSONObject response) {
						try {
							JSONArray object = response.getJSONArray("results");
							parseObjectArray(object);
						} catch (JSONException ex) {
							//?
						}

					}
				}, new Response.ErrorListener() {
			@Override
			public void onErrorResponse(VolleyError error) {

			}
		});
		// Add the request to the RequestQueue.
		queue.add(request);
	}

	private String getUserLocation() {
		if(!locationEditText.getText().toString().isEmpty()) {
			if (locationEditText.getText().toString().equals("Current Location")) {
				return latitude + "," + longitude;
			} else {
				return locationEditText.getText().toString();
			}
		}

		return null;
	}

	private void parseObjectArray(JSONArray jsonArray) {
		nearbyPlaceList = new ArrayList<Place>();


		try {
			for (int i = 0; i < jsonArray.length(); i++) {
				boolean isLodging = false;
				JSONObject placeJSON = jsonArray.getJSONObject(i);
				Place place = new Place();
				place.latitude = placeJSON.getJSONObject("geometry").getJSONObject("location").getString("lat");
				place.longitude = placeJSON.getJSONObject("geometry").getJSONObject("location").getString("lng");
				place.name = placeJSON.has("name") ? placeJSON.getString("name") : "";
				place.placeId = placeJSON.has("place_id") ? placeJSON.getString("place_id") : "";
				place.rating = placeJSON.has("rating") ? placeJSON.getInt("rating") : 0;
				place.priceLevel = placeJSON.has("price_level") ? placeJSON.getInt("price_level") : 0;
				place.address = placeJSON.has("vicinity") ? placeJSON.getString("vicinity") : "";
				JSONArray tempArray = placeJSON.has("types") ? placeJSON.getJSONArray("types") : null;

				if (tempArray != null) {
					for (int x = 0; x < tempArray.length(); x++) {
						place.types.add(tempArray.getString(x));
					}
				}

				for (int j = 0; j < place.types.size(); j++) {
					if (place.types.get(j).equals("lodging")) {
						isLodging = true;
					}
				}

				if (!isLodging) {
					nearbyPlaceList.add(place);
				}
			}
		} catch (JSONException ex) {

		}
	}
}
