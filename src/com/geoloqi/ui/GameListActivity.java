package com.geoloqi.ui;

import java.util.List;

import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
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
import com.geoloqi.interfaces.GeoloqiConstants;
import com.geoloqi.interfaces.RPCException;
import com.geoloqi.rpc.MapAttackClient;

public class GameListActivity extends ListActivity {

	private Game selection = null;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.game_list_activity);
		((Button) findViewById(R.id.refresh_button)).setOnTouchListener(new OnTouchListener() {
			@Override
			public boolean onTouch(View view, MotionEvent evt) {
				if (evt.getAction() == MotionEvent.ACTION_UP) {
					new AsyncRequestGames().execute(45.5246, -122.6834);
				}
				return view.onTouchEvent(evt);
			}
		});
		new AsyncRequestGames().execute(45.5246, -122.6834);
	}

	@Override
	public void onStart() {
		super.onStart();
	}

	@Override
	public void onListItemClick(ListView l, View v, int position, long id) {
		selection = (Game) l.getItemAtPosition(position);
		if (!MapAttackClient.getApplicationClient(this).hasToken()) {
			startActivityForResult(new Intent(this, SignInActivity.class), 77);
		} else {
			startGame();
		}
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == 77 && resultCode == RESULT_OK) {
			startGame();
		}
	}

	public void startGame() {
		SharedPreferences prefs = getSharedPreferences(GeoloqiConstants.PREFERENCES_FILE, Context.MODE_PRIVATE);
		String id = selection.id, email = prefs.getString("email", null), initials = prefs.getString("initials", null);
		try {
			MapAttackClient.getApplicationClient(this).joinGame(id, email, initials);
		} catch (RPCException e) {
			ADB.makeToast(this, "Could not start game", Toast.LENGTH_LONG);
			return;
		}

		Intent gameIntent = new Intent(this, MapAttackActivity.class);
		gameIntent.putExtra("id", id);

		startActivity(gameIntent);
	}

	class AsyncRequestGames extends AsyncTask<Double, Void, List<Game>> {

		ProgressDialog progressDialog;

		@Override
		protected void onPreExecute() {
			progressDialog = ProgressDialog.show(GameListActivity.this, "Map Attack!", "Getting nearby games.");
		}

		@Override
		protected List<Game> doInBackground(Double... params) {
			try {
				List<Game> games = MapAttackClient.getApplicationClient(GameListActivity.this).getGames(params[0], params[1]);
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
