package com.geoloqi.ui;

import java.util.List;

import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.geoloqi.ADB;
import com.geoloqi.mapattack.R;
import com.geoloqi.data.Game;
import com.geoloqi.interfaces.RPCException;
import com.geoloqi.rpc.MapAttackClient;
import com.geoloqi.services.GeoloqiPositioning;

public class GameListActivity extends ListActivity implements OnClickListener {
	public static final String TAG = "GameListActivity";

	public static final String PARAM_SYNC_ON_START = "sync_on_start";

	/** Stub */
	private Intent mPositioningIntent;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.game_list_activity);
		
		boolean syncOnStart = true;
		if (savedInstanceState != null) {
			// Restore our saved instance state
			syncOnStart = savedInstanceState.getBoolean(PARAM_SYNC_ON_START, true);
		}
		
		// Find our views
		final Button refreshButton = (Button) findViewById(R.id.refresh_button);
		final ImageButton geoloqiButton = (ImageButton) findViewById(R.id.geoloqi);
		
		// Set our on click listeners
		refreshButton.setOnClickListener(this);
		geoloqiButton.setOnClickListener(this);
		
		// Reference our positioning service Intent
		mPositioningIntent = new Intent(this, GeoloqiPositioning.class);
		
		if (syncOnStart) {
			// Start our positioning service
			stopService(mPositioningIntent);
			startService(mPositioningIntent);
			
			// Search for nearby games
			new RequestGamesListTask(this).execute();
		}
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		stopService(mPositioningIntent);
	}
	
	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		
		// Don't sync just because the device orientation changed
		outState.putBoolean(PARAM_SYNC_ON_START, false);
	}

	private Location getLastKnownLocation() {
		LocationManager lm = ((LocationManager) getSystemService(LOCATION_SERVICE));
		List<String> providers = lm.getAllProviders();
		for (String provider : providers) {
			Location last = lm.getLastKnownLocation(provider);
			if (last != null) {
				return last;
			}
		}
		return null;
	}

	@Override
	public void onListItemClick(ListView l, View v, int position, long id) {
		final Game selection = (Game) l.getItemAtPosition(position);
		startActivity(new Intent(this, MapAttackActivity.class).putExtra("id", selection.id));
	}

	/** Stub */
	class RequestGamesListTask extends AsyncTask<Void, Void, List<Game>> {
		final ProgressDialog mProgressDialog;

		public RequestGamesListTask(final Context context) {
			mProgressDialog = new ProgressDialog(context);
			mProgressDialog.setTitle(null);
			mProgressDialog.setMessage("Searching for nearby games...");
		}

		@Override
		protected void onPreExecute() {
			mProgressDialog.show();
		}

		@Override
		protected List<Game> doInBackground(Void... params) {
			try {
				Location location = getLastKnownLocation();
				if (location == null) {
					return null;
				}
				List<Game> games = MapAttackClient.getApplicationClient(
						GameListActivity.this).getGames(location.getLatitude(), location.getLongitude());
				ADB.log("Found " + games.size() + (games.size() == 1 ? " game." : " games."));
				for (Game game : games) {
					ADB.log(game.name + ": " + game.description);
				}
				return games;
			} catch (RPCException e) {
				ADB.makeToast(GameListActivity.this, "The game server is unavailable!", Toast.LENGTH_LONG);
				return null;
			}
		}

		@Override
		protected void onPostExecute(List<Game> games) {
			if (games != null) {
				setListAdapter(new GameArrayAdapter(GameListActivity.this,
						R.layout.game_list_element, games.toArray(new Game[games.size()])));
			}
			mProgressDialog.dismiss();
		}
	}

	class GameArrayAdapter extends ArrayAdapter<Game> {
		public GameArrayAdapter(Context context, int textViewResourceId, Game[] objects) {
			super(context, textViewResourceId, objects);
		}

		List<Game> games = null;

		public void setGameList(List<Game> games) {
			this.games = games;
		}
		
		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			// TODO: This seems overly complex. Refactor!
			LinearLayout element = (LinearLayout) LinearLayout.inflate(GameListActivity.this,
					R.layout.game_list_element, null);
			Game game = this.getItem(position);
			((TextView) element.findViewById(R.id.name)).setText(game.name);
			((TextView) element.findViewById(R.id.description)).setText(game.description);
			return element;
		}
	}

	@Override
	public void onClick(View view) {
		switch(view.getId()) {
		case R.id.refresh_button:
			new RequestGamesListTask(this).execute();
			return;
		case R.id.geoloqi:
			final Intent geoloqiIntent = new Intent(Intent.ACTION_VIEW,
					Uri.parse("https://geoloqi.com/"));
			geoloqiIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			geoloqiIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			startActivity(geoloqiIntent);
			return;
		}
	}
}
