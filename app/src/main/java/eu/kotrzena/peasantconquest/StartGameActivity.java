package eu.kotrzena.peasantconquest;

import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.TabLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

public class StartGameActivity extends AppCompatActivity {
	public static class MapInfo {
		public int resId;
		public int mapName;
		public int numberOfPlayers;
		public int thumbId;

		public MapInfo(int resId, int mapName, int numberOfPlayers, int thumbId) {
			this.resId = resId;
			this.mapName = mapName;
			this.numberOfPlayers = numberOfPlayers;
			this.thumbId = thumbId;
		}
	}
	public static MapInfo[] mapInfoList = new MapInfo[]{
		new MapInfo(R.xml.mapa, R.string.map_mapa_name, 2, R.drawable.mapa_thumb),
		new MapInfo(R.xml.around_farm, R.string.map_around_farm_name, 2, R.drawable.around_farm_thumb),
		new MapInfo(R.xml.forrest, R.string.map_forrest_name, 3, R.drawable.forrest_thumb)
	};

	/**
	 * The {@link android.support.v4.view.PagerAdapter} that will provide
	 * fragments for each of the sections. We use a
	 * {@link FragmentPagerAdapter} derivative, which will keep every
	 * loaded fragment in memory. If this becomes too memory intensive, it
	 * may be best to switch to a
	 * {@link android.support.v4.app.FragmentStatePagerAdapter}.
	 */
	private SectionsPagerAdapter mSectionsPagerAdapter;

	/**
	 * The {@link ViewPager} that will host the section contents.
	 */
	private ViewPager mViewPager;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_start_game);

		Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
		setSupportActionBar(toolbar);
		// Create the adapter that will return a fragment for each of the three
		// primary sections of the activity.
		mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());

		// Set up the ViewPager with the sections adapter.
		mViewPager = (ViewPager) findViewById(R.id.container);
		mViewPager.setAdapter(mSectionsPagerAdapter);

		TabLayout tabLayout = (TabLayout) findViewById(R.id.tabs);
		tabLayout.setupWithViewPager(mViewPager);
	}

	@Override
	protected void onResume() {
		super.onResume();
		mSectionsPagerAdapter.notifyDataSetChanged();
	}

	public static class NewGameFragment extends Fragment {
		public NewGameFragment() {
		}

		public static NewGameFragment newInstance() {
			NewGameFragment fragment = new NewGameFragment();
			return fragment;
		}

		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
			final View rootView = inflater.inflate(R.layout.fragment_new_game, container, false);
			ListView mapsList = rootView.findViewById(R.id.mapsList);
			mapsList.setAdapter(new ArrayAdapter<MapInfo>(rootView.getContext(), R.layout.map_list_item_layout, Arrays.asList(mapInfoList)){
				@NonNull
				@Override
				public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
					View row = convertView;

					if(row == null)	{
						//LayoutInflater inflater = (LayoutInflater) JoinActivity.this.getSystemService( JoinActivity.this.LAYOUT_INFLATER_SERVICE );
						LayoutInflater inflater = getActivity().getLayoutInflater();
						row = inflater.inflate(R.layout.map_list_item_layout, parent, false);
					}

					final MapInfo mi = mapInfoList[position];
					SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
					((TextView)row.findViewById(R.id.mapName)).setText(mi.mapName);
					((TextView)row.findViewById(R.id.mapPlayers)).setText(Integer.toString(mi.numberOfPlayers)+" "+getString(R.string.players));
					((ImageView)row.findViewById(R.id.mapThumb)).setImageResource(mi.thumbId);
					((TextView)row.findViewById(R.id.gamesWon)).setText(String.format(getString(R.string.games_won), prefs.getInt("map_"+Integer.toString(mi.resId)+"_win", 0)));
					((TextView)row.findViewById(R.id.gamesLost)).setText(String.format(getString(R.string.games_lost), prefs.getInt("map_"+Integer.toString(mi.resId)+"_lost", 0)));

					return row;
				}
			});
			mapsList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
				@Override
				public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
					Intent intent = new Intent(getActivity(), GameActivity.class);
					intent.putExtra("type", "server");
					intent.putExtra("map", mapInfoList[i].resId);
					startActivity(intent);
				}
			});
			return rootView;
		}
	}

	public static class LoadGameFragment extends Fragment {
		private List<String> savedGamesList;
		private ArrayAdapter<String> adapter;

		public LoadGameFragment() {
		}

		public static LoadGameFragment newInstance() {
			LoadGameFragment fragment = new LoadGameFragment();
			return fragment;
		}

		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
			final View rootView = inflater.inflate(R.layout.fragment_load_game, container, false);

			savedGamesList = new LinkedList<>();
			File saveFolder = getActivity().getFilesDir();
			for(File f : saveFolder.listFiles()) {
				savedGamesList.add(f.getName());
			}

			ListView mapsList = rootView.findViewById(R.id.saveList);
			adapter = new ArrayAdapter<String>(rootView.getContext(), R.layout.save_list_item_layout, savedGamesList){
				@NonNull
				@Override
				public View getView(final int position, @Nullable View convertView, @NonNull ViewGroup parent) {
					View row = convertView;

					if(row == null)	{
						LayoutInflater inflater = getActivity().getLayoutInflater();
						row = inflater.inflate(R.layout.save_list_item_layout, parent, false);
					}

					final String saveName = savedGamesList.get(position);
					((TextView)row.findViewById(R.id.saveName)).setText(saveName);
					((ImageButton)row.findViewById(R.id.deleteButton)).setOnClickListener(new View.OnClickListener() {
						@Override
						public void onClick(View view) {
							new File(getActivity().getFilesDir(), saveName).delete();
							savedGamesList.remove(position);
							adapter.notifyDataSetChanged();
						}
					});
					row.setOnClickListener(new View.OnClickListener() {
						@Override
						public void onClick(View view) {
							Intent intent = new Intent(getActivity(), GameActivity.class);
							intent.putExtra("type", "server");
							intent.putExtra("load", savedGamesList.get(position));
							startActivity(intent);
						}
					});

					return row;
				}
			};
			mapsList.setAdapter(adapter);
			return rootView;

		}
	}

	/**
	 * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
	 * one of the sections/tabs/pages.
	 */
	public class SectionsPagerAdapter extends FragmentPagerAdapter {

		public SectionsPagerAdapter(FragmentManager fm) {
			super(fm);
		}

		@Override
		public Fragment getItem(int position) {
			// getItem is called to instantiate the fragment for the given page.
			switch (position){
				case 0:
					return NewGameFragment.newInstance();
				case 1:
					return LoadGameFragment.newInstance();
			}
			return null;
		}

		@Override
		public int getCount() {
			return 2;
		}

		@Override
		public CharSequence getPageTitle(int position) {
			switch (position) {
				case 0:
					return getString(R.string.new_game);
				case 1:
					return getString(R.string.load_game);
			}
			return null;
		}
	}
}
