package in.abmulani.twitterintegration;

import java.util.ArrayList;

import twitter4j.Query;
import twitter4j.QueryResult;
import twitter4j.Status;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.User;
import twitter4j.auth.AccessToken;
import twitter4j.auth.RequestToken;
import twitter4j.conf.ConfigurationBuilder;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Html;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class BaseActivity extends Activity {
	//String constant for shared pref and Authentication
	static String TWITTER_CONSUMER_KEY = "marlVCZaLYAG52rVvholRw";
	static String TWITTER_CONSUMER_SECRET = "5uZZFboHSK9psBPsqJUDSb1GuC36Fy83cYornOPu9A";
	static String PREFERENCE_NAME = "twitter_oauth";
	static final String PREF_KEY_OAUTH_TOKEN = "oauth_token";
	static final String PREF_KEY_OAUTH_SECRET = "oauth_token_secret";
	static final String PREF_KEY_TWITTER_LOGIN = "isTwitterLogedIn";
	static final String TWITTER_CALLBACK_URL = "oauth://t4jsample";
	static final String URL_TWITTER_AUTH = "auth_url";
	static final String URL_TWITTER_OAUTH_VERIFIER = "oauth_verifier";
	static final String URL_TWITTER_OAUTH_TOKEN = "oauth_token";

	//
	private static final int NO_INTERNET = 0;
	private static final String NO_INTERNET_MSG = "No Internet connection,plz check your connection and restart the application..\nThe App will close now.. ";
	private static final String NO_INTERNET_TITLE = "No Internet:";
	//
	private static final int LOG_IN_DONE = 1;
	private static final String LOG_IN_DONE_MSG = "Welcome :\n";
	private static final String LOG_IN_DONE_TITLE = "Log In Done:";
	//
	private static final int LOG_IN = 2;
	private static final String LOG_IN_MSG = "Please login into your Twitter account..";
	private static final String LOG_IN_TITLE = "LOGIN:";
	private static final int SELECTED_SEARCH_RESULT = 3;

	//
	public static final int RESULTSET_MAX_COUNT = 25;

	//
	private SharedPreferences mSharedPreferences;
	private static Twitter twitter;
	private static RequestToken requestToken;
	//
	private String UserName = null;
	private long UserId;
	private User CURRENT_USER = null;
	
	// Used in Adaptor class
	public static ArrayList<Status> SearchedStatus = new ArrayList<Status>();

	// UI 
	ImageButton searchBtn;
	EditText searchEditTxt;
	TextView noDataLabel;
	ListView searchList;
	TextView searchCount;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mSharedPreferences = getApplicationContext().getSharedPreferences(
				"MyTwitte", 0);
		if (checkInternetConnection()) {
			new getAccessTokensAsync().execute();
			InitSearchPage();
		} else {
			showThisDialog(NO_INTERNET, NO_INTERNET_MSG, NO_INTERNET_TITLE);
			Toast.makeText(getApplicationContext(), "No Internet Connection..",
					Toast.LENGTH_LONG).show();
		}

	}
	
	/** Clear all the shared preference if needed */
	@Override
	protected void onDestroy() {
		super.onDestroy();
		getSharedPreferences("MyTwitte", 0).edit().clear().commit();
	}

	/**
	 * Called every time when onCreate is called. It retrieves the AccessToken
	 * verifiers
	 * [Need to be called in async task to be execute in Android v3.0 and above]
	 */
	class getAccessTokensAsync extends AsyncTask<String, String, Boolean> {
		ProgressDialog pDialog;

		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			pDialog = new ProgressDialog(BaseActivity.this);
			pDialog.setMessage("Validating..");
			pDialog.setIndeterminate(true);
			pDialog.setCancelable(false);
			pDialog.show();
		}
		@Override
		protected Boolean doInBackground(String... args) {
			Uri uri = getIntent().getData();
			if (uri != null && uri.toString().startsWith(TWITTER_CALLBACK_URL)) {
				String verifier = uri.getQueryParameter(URL_TWITTER_OAUTH_VERIFIER);
				try {
					AccessToken accessToken = twitter.getOAuthAccessToken(
							requestToken, verifier);
					// Shared Preferences
					Editor e = mSharedPreferences.edit();
					Log.e("getToken ", accessToken.getToken());
					e.putString(PREF_KEY_OAUTH_TOKEN, accessToken.getToken());
					Log.e("getTokenSecret ", accessToken.getTokenSecret());
					e.putString(PREF_KEY_OAUTH_SECRET, accessToken.getTokenSecret());
					e.putBoolean(PREF_KEY_TWITTER_LOGIN, true);
					e.commit();

					Log.e("Twitter OAuth Token", "> " + accessToken.getToken());

					UserId = accessToken.getUserId();
					CURRENT_USER = twitter.showUser(UserId);
					UserName = CURRENT_USER.getName();
					Log.e("UserID: ", "userID: " + UserId + "" + UserName);
					Log.v("Welcome:",
							"Thanks:"
									+ Html.fromHtml("<b>Welcome " + UserName
											+ "</b>"));
					
				} catch (Exception e) {
					e.printStackTrace();
					Log.e("Twitter Login Error", "> " + e.getMessage());
				}
			} else {
				Log.e("No Tokens", "No Tokens Retrieved...");
			}
			return false;
		}
		
		@Override
		protected void onPostExecute(Boolean result) {
			super.onPostExecute(result);
			pDialog.cancel();
			if (isTwitterLoggedInAlready()) {
				if(UserName==null)
					showThisDialog(LOG_IN, LOG_IN_MSG, LOG_IN_TITLE);
				else
				showThisDialog(LOG_IN_DONE, LOG_IN_DONE_MSG + UserName,
						LOG_IN_DONE_TITLE);
			} else {
				showThisDialog(LOG_IN, LOG_IN_MSG, LOG_IN_TITLE);
			}
		}
	}

	
	/** Initialize the UI components and add listeners */
	private void InitSearchPage() {
		setContentView(R.layout.activity_search_layout);
		searchBtn = (ImageButton) findViewById(R.id.imageButton1);
		searchEditTxt = (EditText) findViewById(R.id.search_edittext);
		noDataLabel = (TextView) findViewById(R.id.no_data_textView);
		searchList = (ListView) findViewById(R.id.listView_search);
		searchCount= (TextView) findViewById(R.id.searchCount_textView);
		searchBtn.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				InputMethodManager imm = (InputMethodManager) getSystemService(Activity.INPUT_METHOD_SERVICE);
			    imm.toggleSoftInput(InputMethodManager.HIDE_IMPLICIT_ONLY, 0);
				if (searchEditTxt.getText().toString().trim().length() == 0) {
					searchEditTxt.setError("Please enter a keyword..");
					searchEditTxt.requestFocus();
				} else {
					new GetSearchedData(searchEditTxt.getText().toString()
							.trim()).execute();
				}
			}
		});
	}

	/** Hits the Json and get Back the result [MAX 25] for now
	 * and stores the result in the ArrayList<Status>
	 */
	class GetSearchedData extends AsyncTask<String, String, Boolean> {
		ProgressDialog pDialog;
		String KeyWord;

		public GetSearchedData(String KeyWord) {
			this.KeyWord = KeyWord;
		}

		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			pDialog = new ProgressDialog(BaseActivity.this);
			pDialog.setMessage("Sending Request...");
			pDialog.setIndeterminate(false);
			pDialog.setCancelable(false);
			pDialog.show();
		}
		
		@Override
		protected Boolean doInBackground(String... args) {
			try {
				ConfigurationBuilder builder = new ConfigurationBuilder();
				builder.setOAuthConsumerKey(TWITTER_CONSUMER_KEY);
				builder.setOAuthConsumerSecret(TWITTER_CONSUMER_SECRET);
				// Access Token
				String access_token = mSharedPreferences.getString(
						PREF_KEY_OAUTH_TOKEN, "");
				// Access Token Secret
				String access_token_secret = mSharedPreferences.getString(
						PREF_KEY_OAUTH_SECRET, "");

				AccessToken accessToken = new AccessToken(access_token,
						access_token_secret);
				Twitter twitter = new TwitterFactory(builder.build())
						.getInstance(accessToken);

				Query querryy = new Query();
				querryy.setCount(RESULTSET_MAX_COUNT);
				querryy.setQuery(KeyWord);

				Log.d("REQUEST>>", querryy.toString());
				QueryResult response = twitter.search(querryy);
				publishProgress("");
				// clear prev list
				SearchedStatus.clear();
				// store newly obtained list
				for (twitter4j.Status resultStatus : response.getTweets()) {
					SearchedStatus.add(resultStatus);
				}
				return true;
			} catch (TwitterException e) {
				Log.d("Twitter Update Error", e.getMessage());

			}
			return false;
		}

		@Override
		protected void onProgressUpdate(String... values) {
			super.onProgressUpdate(values);
			pDialog.setMessage("Retrieving Data...");
		}
		@Override
		protected void onPostExecute(Boolean result) {
			pDialog.dismiss();
			if (result) {
				RefreshListView();
			} else {
				Toast.makeText(getApplicationContext(), "Unexpected Error", Toast.LENGTH_LONG).show();
				searchList.setVisibility(View.GONE);
				searchCount.setVisibility(View.GONE);
				noDataLabel.setVisibility(View.VISIBLE);
			}
		}
	}

	/**
	 * Refreshes the list with newly obtained Values and onSelecting any Item it
	 * calls onSelectingSearchItem()
	 */
	public void RefreshListView() {
		if (SearchedStatus.size() > 0) {
			searchList.setVisibility(View.VISIBLE);
			searchCount.setVisibility(View.VISIBLE);
			noDataLabel.setVisibility(View.GONE);
			AdapterClass listadapter = new AdapterClass(BaseActivity.this);
			if (searchList.getCount() != 0)
				searchList.setAdapter(null);
			searchList.setAdapter(listadapter);
			searchList.setOnItemClickListener(new OnItemClickListener() {

				@Override
				public void onItemClick(AdapterView<?> parent, View v,
						int position, long id) {
					Log.v("position", position + "");
					onSelectingSearchItem(position);
				}
			});
		} else {
			searchList.setVisibility(View.GONE);
			noDataLabel.setVisibility(View.VISIBLE);
			searchCount.setVisibility(View.GONE);
		}
	}

	/**
	 * Parse the value from the Status using the ArrayList at specified position
	 * and display it in a dialog box
	 */
	protected void onSelectingSearchItem(int position) {
		Status TempStatus = SearchedStatus.get(position);
		String strMessage = "-------[TWEET]--------\n " + TempStatus.getText();
		;
		strMessage += "\n\n-------Created At-------\n"
				+ TempStatus.getCreatedAt().toString();
		showThisDialog(SELECTED_SEARCH_RESULT, strMessage, TempStatus.getUser()
				.getScreenName());
	}

	/**
	 * Calls the browser login to authenticate the twitter account and givving
	 * access to our application to read user data
	 */
	protected void loginToTwitter() {
		ConfigurationBuilder builder = new ConfigurationBuilder();
		builder.setOAuthConsumerKey(TWITTER_CONSUMER_KEY);
		builder.setOAuthConsumerSecret(TWITTER_CONSUMER_SECRET);
		twitter4j.conf.Configuration configuration = builder.build();

		TwitterFactory factory = new TwitterFactory(configuration);
		twitter = factory.getInstance();
		try {
			new getOAuthRequestTokenAsync().execute().get();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/** get the Tokens before hitting the intent to the browser*/
	class getOAuthRequestTokenAsync extends AsyncTask<String, String, Boolean> {
		ProgressDialog pDialog;

		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			pDialog = new ProgressDialog(BaseActivity.this);
			pDialog.setMessage("Processing");
			pDialog.setIndeterminate(true);
			pDialog.setCancelable(false);
			pDialog.show();
		}
		@Override
		protected Boolean doInBackground(String... args) {
			try {
					requestToken = twitter.getOAuthRequestToken(TWITTER_CALLBACK_URL);
					publishProgress("");
					BaseActivity.this.startActivity(new Intent(Intent.ACTION_VIEW, Uri
							.parse(requestToken.getAuthenticationURL())));
			} catch (TwitterException e) {
				Log.d("Twitter Update Error", e.getMessage());
			}
			return false;
		}
		@Override
		protected void onProgressUpdate(String... values) {
			super.onProgressUpdate(values);
			pDialog.dismiss();
		}
	}
	
	
	/**
	 * Checks preferences and returns true if we have the twitter login details
	 * else return false [by default]
	 */
	private boolean isTwitterLoggedInAlready() {
		return mSharedPreferences.getBoolean(PREF_KEY_TWITTER_LOGIN, false);
	}

	/**
	 * Shows the alert box with specified message and on proceed button follows
	 * the switch case to do the corresponding function
	 */
	public void showThisDialog(final int type, String msg, String title) {
		AlertDialog.Builder builder = new AlertDialog.Builder(BaseActivity.this);
		builder.setCancelable(true);
		builder.setMessage(msg);
		builder.setTitle(title);
		builder.setPositiveButton("Proceed",
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						dialog.dismiss();
						switch (type) {
						case NO_INTERNET:
							finish();
							break;
						case LOG_IN_DONE:
							Toast.makeText(getApplicationContext(),
									"Make a Search", Toast.LENGTH_LONG).show();
							break;
						case LOG_IN:
							loginToTwitter();
							break;
						case SELECTED_SEARCH_RESULT:
							// Do something after showing the selected search
							// result..
							break;
						}
					}
				});
		builder.setOnCancelListener(new OnCancelListener() {

			@Override
			public void onCancel(DialogInterface arg0) {
				switch (type) {
				case NO_INTERNET:
					finish();
					break;
				case LOG_IN_DONE:
					Toast.makeText(getApplicationContext(), "Make a Search",
							Toast.LENGTH_LONG).show();
					break;
				case LOG_IN:
					Toast.makeText(getApplicationContext(),
							"Exiting the application..", Toast.LENGTH_LONG)
							.show();
					finish();
					break;
				case SELECTED_SEARCH_RESULT:
					// Do something after showing the selected search result..
					break;
				}
			}
		});
		builder.show();
	}

	/** Check Internet Availibility */
	public boolean checkInternetConnection() {
		ConnectivityManager cm = (ConnectivityManager) BaseActivity.this
				.getSystemService(Context.CONNECTIVITY_SERVICE);

		NetworkInfo wifiNetwork = cm
				.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
		if (wifiNetwork != null && wifiNetwork.isConnected()) {
			return true;
		}

		NetworkInfo mobileNetwork = cm
				.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
		if (mobileNetwork != null && mobileNetwork.isConnected()) {
			return true;
		}

		NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
		if (activeNetwork != null && activeNetwork.isConnected()) {
			return true;
		}
		return false;
	}

}
