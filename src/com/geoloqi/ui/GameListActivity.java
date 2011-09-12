package com.geoloqi.ui;

import java.util.List;

import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.geoloqi.ADB;
import com.geoloqi.R;
import com.geoloqi.data.Game;
import com.geoloqi.interfaces.RPCException;
import com.geoloqi.rpc.MapAttackClient;
import com.geoloqi.services.GeoloqiPositioning;

public class GameListActivity extends ListActivity {

	private Game selection = null;
	Intent positioningIntent;

	OnTouchListener refreshListener = new OnTouchListener() {
		@Override
		public boolean onTouch(View view, MotionEvent evt) {
			if (evt.getAction() == MotionEvent.ACTION_UP) {
				new AsyncRequestGames().execute();
			}
			return view.onTouchEvent(evt);
		}
	};

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.game_list_activity);
		((Button) findViewById(R.id.refresh_button)).setOnTouchListener(refreshListener);

		positioningIntent = new Intent(this, GeoloqiPositioning.class);
	}

	@Override
	public void onStart() {
		super.onStart();
		stopService(positioningIntent);
		startService(positioningIntent);
		new AsyncRequestGames().execute();
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
	public void onDestroy() {
		super.onDestroy();
		stopService(positioningIntent);
	}

	@Override
	public void onListItemClick(ListView l, View v, int position, long id) {
		selection = (Game) l.getItemAtPosition(position);
		startActivity(new Intent(this, MapAttackActivity.class).putExtra("id", selection.id));
	}

	class AsyncRequestGames extends AsyncTask<Void, Void, List<Game>> {

		ProgressDialog progressDialog;

		@Override
		protected void onPreExecute() {
			progressDialog = ProgressDialog.show(GameListActivity.this, "Map Attack!", "Getting nearby games.");
		}

		@Override
		protected List<Game> doInBackground(Void... params) {
			try {
				Location location = getLastKnownLocation();
				if (location == null) {
					return null;
				}
				List<Game> games = MapAttackClient.getApplicationClient(GameListActivity.this).getGames(location.getLatitude(), location.getLongitude());
				ADB.log("Found " + games.size() + (games.size() == 1 ? " game." : " games."));
				for (Game game : games) {
					ADB.log(game.name + ": " + game.description);
				}
				return games;
			} catch (RPCException e) {
				ADB.makeToast(GameListActivity.this, "Game Server Unavailable", Toast.LENGTH_LONG);
				return null;
			}
		}

		@Override
		protected void onPostExecute(List<Game> games) {
			if (games != null) {
				setListAdapter(new GameArrayAdapter(GameListActivity.this, R.layout.game_list_element, games.toArray(new Game[games.size()])));
			}
			progressDialog.dismiss();
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
		public View getView(int position, View convertView, ViewGroup group) {
			LinearLayout element = (LinearLayout) LinearLayout.inflate(GameListActivity.this, R.layout.game_list_element, null);
			Game game = this.getItem(position);
			((TextView) element.findViewById(R.id.name)).setText(game.name);
			((TextView) element.findViewById(R.id.description)).setText(game.description);
			return element;
		}
	}

}
