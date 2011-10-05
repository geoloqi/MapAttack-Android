package com.geoloqi.widget;

import com.geoloqi.data.Game;
import com.geoloqi.mapattack.R;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;


/** Stub */
public class GameListArrayAdapter extends ArrayAdapter<Game> {
	private final int mLayoutResourceId;
	private final LayoutInflater mInflater;
	private final Game[] mGames;

	/** Stub */
	private static class ViewHolder {
		public TextView name;
		public TextView description;
	}

	/**
	 * Stub
	 * 
	 * @param context
	 * @param textViewResourceId
	 * @param games
	 */
	public GameListArrayAdapter(Context context, int textViewResourceId, Game[] games) {
		super(context, textViewResourceId, games);

		// Store our arguments as object members
		mLayoutResourceId = textViewResourceId;
		mGames = games;

		// Get a layout inflater
		mInflater = LayoutInflater.from(context);
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		final ViewHolder holder;

		if (convertView == null) {
			// Inflate our row layout
			convertView = mInflater.inflate(mLayoutResourceId, parent, false);

			// Cache the row elements for efficient retrieval
			holder = new ViewHolder();
			holder.name = (TextView) convertView.findViewById(R.id.name);
			holder.description = (TextView) convertView.findViewById(R.id.description);

			// Store the holder object on the row
			convertView.setTag(holder);
		} else {
			holder = (ViewHolder) convertView.getTag();
		}

		// Populate our game data
		final Game game = mGames[position];
		holder.name.setText(game.name);
		holder.description.setText(game.description);

		return convertView;
	}
}