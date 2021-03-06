package com.kartikhariharan.prayerbookapp.activities;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import android.app.ActionBar;
import android.app.Activity;
import android.app.SearchManager;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.EditText;
import android.widget.ExpandableListView;
import android.widget.ListAdapter;
import android.widget.SearchView;
import android.widget.TextView;

import com.kartikhariharan.prayerbookapp.Category;
import com.kartikhariharan.prayerbookapp.DataBaseHelper;
import com.kartikhariharan.prayerbookapp.Prayer;
import com.kartikhariharan.prayerbookapp.R;
import com.kartikhariharan.prayerbookapp.R.id;
import com.kartikhariharan.prayerbookapp.R.layout;
import com.kartikhariharan.prayerbookapp.R.menu;
import com.kartikhariharan.prayerbookapp.R.string;
import com.kartikhariharan.prayerbookapp.adapters.PrayerListAdapter;

public class HomeActivity extends Activity {
	
	ExpandableListView exlvHomeListView;
	
	static final String DB_NAME = "prayers.db";
    
    //A good practice is to define database field names as constants
	static final String CATEGORY_TABLE_NAME = "CATEGORY";	
	static final String CATEGORY_ID = "CATEGORY_id";
	static final String CATEGORY_TITLE = "TITLE";
	
	private static final String PRAYER_TABLE_NAME = "PRAYER";
	private static final String PRAYER_ID = "PRAYER_id";
	static final String PRAYER_TITLE = "TITLE";
	static final String PRAYER_CONTENT = "CONTENT";
	private static final String IS_FAVORITE = "IS_FAVORITE";
	
	static final String CATEGORY_PRAYER_MAP_TABLE_NAME = "CATEGORY_PRAYER_MAP";
	static final String CATEGORY_MAP_ID = "CATEGORY_ID";
	static final String PRAYER_MAP_ID = "PRAYER_ID";
	
	private SQLiteDatabase database;
	
	List<List<Prayer>> prayerList;
	List<Category> categoryList;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		
		super.onCreate(savedInstanceState);
		
		//requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);
		setContentView(R.layout.home);
		
		//getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE, R.layout.actionbar);
		
		ActionBar actionBar = getActionBar();
	    actionBar.setDisplayHomeAsUpEnabled(false);
		
		exlvHomeListView = (ExpandableListView) findViewById(R.id.exlvHomeListView);
		
		//Our key helper
        DataBaseHelper dbOpenHelper = new DataBaseHelper(this, DB_NAME);
        setDatabase(dbOpenHelper.openDataBase());
        //That�s it, the database is open!
		
		prayerList = new ArrayList<List<Prayer>>();
		categoryList = new ArrayList<Category>();

		categoryList = populateCategoryList(categoryList);
		prayerList = populatePrayerList(prayerList, categoryList);
		
		// Initialize favorites category
		int arbitraryId = -10;
		String favTitle = "Favorite Prayers";
		
		if (categoryList.get(categoryList.size()-1).getId() != arbitraryId) {
			categoryList.add(new Category(arbitraryId, favTitle));
		}
		
		populateFavorites(prayerList);
		
		exlvHomeListView.setAdapter(new PrayerListAdapter(this, categoryList, prayerList));
		
		exlvHomeListView.setOnGroupClickListener(new ExpandableListView.OnGroupClickListener() {

			@Override
			public boolean onGroupClick(ExpandableListView parent, View v,
					int groupPosition, long id) {
				// Do nothing when header is clicked
				
				if (groupPosition == 0) {
					
					return true;
					
				} else {
					
					return false;
					
				}
				
			}
		});
		
		exlvHomeListView.setOnChildClickListener(new ExpandableListView.OnChildClickListener() {

			@Override
			public boolean onChildClick(ExpandableListView parent, View v,
					int groupPosition, int childPosition, long id) {
				
				((PrayerListAdapter) parent.getExpandableListAdapter()).clickPrayer(groupPosition, childPosition);
				return false;
				
			}			
			
		});
				
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.main, menu);
		
		// Get the SearchView and set the searchable configuration
	    SearchView searchView = (SearchView) menu.findItem(R.id.action_search).getActionView();
	    SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
		// Assumes current activity is the searchable activity
	    searchView.setSearchableInfo(searchManager.getSearchableInfo( getComponentName() ));
	    searchView.setIconifiedByDefault(false);
	    searchView.setQueryHint(getResources().getText(R.string.search_hint));
	    
	    searchView.setFocusable(false);
	    
	    int searchSrcTextId = getResources().getIdentifier("android:id/search_src_text", null, null);  
	    EditText searchEditText = (EditText) searchView.findViewById(searchSrcTextId);  
	    searchEditText.setTextColor(Color.DKGRAY);
		
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		
		switch (item.getItemId()) {
		
		case R.id.menu_item_about:
			Intent aboutIntent = new Intent(this, AboutActivity.class);
			startActivity(aboutIntent);
			break;
		
		case R.id.menu_item_rate:
			Uri uri = Uri.parse("market://details?id=" + this.getPackageName());
			Intent goToMarket = new Intent(Intent.ACTION_VIEW, uri);
			try {
			  startActivity(goToMarket);
			} catch (ActivityNotFoundException e) {
			  startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("http://play.google.com/store/apps/details?id=" + this.getPackageName())));
			}
			
		case R.id.menu_item_share:
			String appUri = "http://play.google.com/store/apps/details?id=" + this.getPackageName();
			String textToShare = "Check out the Prayer Book App at " + appUri + "! I use it and I love it.";
			Intent sendIntent = new Intent();
			sendIntent.setAction(Intent.ACTION_SEND);
			sendIntent.putExtra(Intent.EXTRA_TEXT, textToShare);
			sendIntent.setType("text/plain");
			this.startActivity(Intent.createChooser(sendIntent, "Share this app..."));
			
		default:
			return super.onOptionsItemSelected(item);
		
		}
		
		return super.onOptionsItemSelected(item);
		
	}
	
	public List<Category> populateCategoryList (List<Category> categoryList) {
		// Method to populate the category list from the data source
		
		Cursor categoryCursor = getDatabase().query(CATEGORY_TABLE_NAME,
				new String[] {CATEGORY_ID, CATEGORY_TITLE},
				null, null, null, null, CATEGORY_ID);
		
		categoryCursor.moveToFirst();
		
		if (!categoryCursor.isAfterLast()) {
			
			do {
				
				int id = categoryCursor.getInt(0);
				String title = categoryCursor.getString(1);
				
				categoryList.add(new Category(id, title));
				
			} while (categoryCursor.moveToNext());
			
		}
		
		return categoryList;
	}
	
	public List<List<Prayer>> populatePrayerList (List<List<Prayer>> prayerList, List<Category> categoryList) {
		// Method to populate the prayer list from the data source
		
		Cursor prayerCursor;	
		
		Cursor categoryCursor = getDatabase().query(CATEGORY_TABLE_NAME,
				new String[] {CATEGORY_ID, CATEGORY_TITLE},
				null, null, null, null, CATEGORY_ID);
		
		Cursor mapCursor;
		
		categoryCursor.moveToFirst();
		if (!categoryCursor.isAfterLast()) {
			
			int i = 0;
			
			do {
					int category_id = categoryCursor.getInt(0);
					
					mapCursor = getDatabase().query(CATEGORY_PRAYER_MAP_TABLE_NAME,
							new String[] {CATEGORY_MAP_ID, PRAYER_MAP_ID},
							"CATEGORY_ID="+category_id, null, null, null, CATEGORY_MAP_ID);
				
					mapCursor.moveToFirst();
					if (!mapCursor.isAfterLast()) {						
						
						do {
							
							prayerCursor = getDatabase().query(getPrayerTableName(),
										new String[] {getPrayerId(), PRAYER_TITLE, PRAYER_CONTENT, getIsFavorite()},
										"PRAYER_id="+mapCursor.getInt(1), null, null, null, getPrayerId());	
							
							
							prayerCursor.moveToFirst();
							if (!prayerCursor.isAfterLast()) {
								
								prayerList.add(new ArrayList<Prayer>());
								
								do {
									
									int id = prayerCursor.getInt(0);
									String title = prayerCursor.getString(1);
									String content = prayerCursor.getString(2);
									boolean favoriteState = prayerCursor.getInt(3) > 0 ? true : false;								
									
									prayerList.get(i).add(new Prayer(id, title,	content, false, favoriteState));									
									
								} while (prayerCursor.moveToNext());
								
							}
							
						} while (mapCursor.moveToNext());
						
					}
					
					i++;
				
			} while (categoryCursor.moveToNext());
			
		}
		
		return prayerList;
	}
	
	public void populateFavorites (List<List<Prayer>> prayerList) {
		
		int categoryListSize = prayerList.size();	
		
		categoryListSize = categoryList.size();
		
		int i = categoryListSize - 1;
		
		prayerList.get(i).clear();
		
		Cursor prayerCursor = getDatabase().query(getPrayerTableName(),
						new String[] {getPrayerId(), PRAYER_TITLE, PRAYER_CONTENT, getIsFavorite()},
						"IS_FAVORITE="+1, null, null, null, getPrayerId());
	
				
		prayerCursor.moveToFirst();
		if (!prayerCursor.isAfterLast()) {		
			
				do {
					
					int id = prayerCursor.getInt(0);
					String title = prayerCursor.getString(1);
					String content = prayerCursor.getString(2);
					boolean favoriteState = prayerCursor.getInt(3) > 0 ? true : false;								
					
					prayerList.get(i).add(new Prayer(id, title,	content, false, favoriteState));									
					
				} while (prayerCursor.moveToNext());
				
			}
	}


	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {
		super.onRestoreInstanceState(savedInstanceState);
		int lastExpandedGroup = savedInstanceState.getInt("LAST_EXPANDED_GROUP");
		int[] lastExpandedPrayer = savedInstanceState.getIntArray("LAST_EXPANDED_PRAYER");
		((PrayerListAdapter) exlvHomeListView.getExpandableListAdapter()).setLastExpandedGroupPosition(lastExpandedGroup);
		((PrayerListAdapter) exlvHomeListView.getExpandableListAdapter()).setLastExpandedPrayer(lastExpandedPrayer);
		if (lastExpandedPrayer[0] != -1 && lastExpandedPrayer[1] != -1) {
			
			((PrayerListAdapter) exlvHomeListView.getExpandableListAdapter()).clickPrayer(lastExpandedPrayer[0], lastExpandedPrayer[1]);
			
		}
		
	}


	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putInt("LAST_EXPANDED_GROUP", ((PrayerListAdapter) exlvHomeListView.getExpandableListAdapter()).getLastExpandedGroupPosition());
		outState.putIntArray("LAST_EXPANDED_PRAYER", ((PrayerListAdapter) exlvHomeListView.getExpandableListAdapter()).getLastExpandedPrayer());
	}

	@Override
	protected void onRestart() {
		super.onRestart();
		
		int lastExpandedGroup = ((PrayerListAdapter) exlvHomeListView.getExpandableListAdapter()).getLastExpandedGroupPosition();
		int[] lastExpandedPrayer = ((PrayerListAdapter) exlvHomeListView.getExpandableListAdapter()).getLastExpandedPrayer();
		
		prayerList = new ArrayList<List<Prayer>>();
		categoryList = new ArrayList<Category>();

		categoryList = populateCategoryList(categoryList);
		prayerList = populatePrayerList(prayerList, categoryList);
		
		// Initialize favorites category
		int arbitraryId = -10;
		String favTitle = "Favorite Prayers";
		
		if (categoryList.get(categoryList.size()-1).getId() != arbitraryId) {
			categoryList.add(new Category(arbitraryId, favTitle));
		}
		
		populateFavorites(prayerList);
		
		exlvHomeListView.setAdapter(new PrayerListAdapter(this, categoryList, prayerList));
		
		((PrayerListAdapter) exlvHomeListView.getExpandableListAdapter()).setLastExpandedGroupPosition(lastExpandedGroup);
		((PrayerListAdapter) exlvHomeListView.getExpandableListAdapter()).setLastExpandedPrayer(lastExpandedPrayer);
		
		if (lastExpandedGroup != -1) {
			exlvHomeListView.expandGroup(lastExpandedGroup);
		}
		
		if (lastExpandedPrayer[0] != -1 && lastExpandedPrayer[1] != -1) {
			
			((PrayerListAdapter) exlvHomeListView.getExpandableListAdapter()).clickPrayer(lastExpandedPrayer[0], lastExpandedPrayer[1]);
			
		}
		
	}

	public SQLiteDatabase getDatabase() {
		return database;
	}

	public void setDatabase(SQLiteDatabase database) {
		this.database = database;
	}

	public static String getIsFavorite() {
		return IS_FAVORITE;
	}

	public static String getPrayerTableName() {
		return PRAYER_TABLE_NAME;
	}

	public static String getPrayerId() {
		return PRAYER_ID;
	}

}
