package com.dlmv.localplayer.client.main;

import java.io.*;
import java.util.*;

import javax.xml.parsers.*;

import org.w3c.dom.*;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import com.dlmv.localmediaplayer.client.R;
import com.dlmv.localplayer.*;
import com.dlmv.localplayer.PlayerStatus.PlaylistItem;
import com.dlmv.localplayer.client.network.*;
import com.dlmv.localplayer.client.db.*;
import com.dlmv.localplayer.client.util.AbsFile;
import com.dlmv.localplayer.client.util.ApplicationUtil;

import android.os.Bundle;
import android.app.*;
import android.content.*;
import android.content.DialogInterface.OnCancelListener;
import android.util.Log;
import android.view.*;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.*;
import android.widget.AdapterView.OnItemClickListener;

import static com.dlmv.localplayer.client.util.ApplicationUtil.showLoginDialog;


public class MainActivity extends Activity implements OnItemClickListener {
	
	private final int PLAY_OPTION = 0;
	private final int OPEN_OPTION = 1;
	private final int REMOVE_OPTION = 2;
	private final int CLEAR_OPTION = 3;
	private final int STOPAFTER_OPTION = 4;
	private final int PAUSEAFTER_OPTION = 5;
	private final int NOAFTER_OPTION = 6;
	private final int REMOVE_BEFORE_OPTION = 7;
	private final int REMOVE_AFTER_OPTION = 8;

	private PlayListAdapter myAdapter;
	private ArrayList<PlayerStatus.PlaylistItem> myPlayList = new ArrayList<>();

	boolean myLazyStatusInProgress = false;

	private ImageButton myPlayButton;
	private ImageButton myPlaytypeButton;
	private SeekBar myVolumeBar;
	private SeekBar myPlayBar;
	private TextView myCurrent;
	private TextView myTotal;
	private int myLastPlayingPosition;
	private Date myLastUpdateTime;

	private boolean myVolumeBarIsAdjusting = false;
	private boolean myPlayBarIsAdjusting = false;
	private boolean myMpVolumeBarIsAdjusting = false;
	private boolean myBackMpVolumeBarIsAdjusting = false;

	private PlayerStatus myStatus = new PlayerStatus();

	PlayerStatus getStatus() {
		return myStatus;
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_VOLUME_UP && event.getAction() == KeyEvent.ACTION_DOWN) {
			if (myStatus.myVolume < myStatus.myMaxVolume) {
				NetworkRequest request = getRequest("volumeup");
				performRequest(request, false);
			}
			return true;
		}
		if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN && event.getAction() == KeyEvent.ACTION_DOWN ) {
			if (myStatus.myVolume > 0) {
				NetworkRequest request = getRequest("volumedown");
				performRequest(request, false);
			}
			return true;
		}
		return super.onKeyDown(keyCode, event);
	}

	private boolean myNeedToCheck = false;


	@Override
	protected void onResume() {
		super.onResume();
		if (myNeedToCheck) {
			checkOnStart();
			myNeedToCheck = false;
		}
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_PROGRESS);
		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
		setContentView(R.layout.main);

		new ServersDB(this);
		new BookmarksDB(this);

		setProgressBarVisibility(true);

		ImageButton b = findViewById(R.id.local_button);
		b.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				ApplicationUtil.browseLocal(MainActivity.this);
			}
		});
		b.setEnabled(ApplicationUtil.Data.serverUri != null);

		ImageButton b1 = findViewById(R.id.shared_button);
		b1.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				ApplicationUtil.browseSmb(MainActivity.this);
			}
		});
		b1.setEnabled(ApplicationUtil.Data.serverUri != null);

		ListView l = findViewById(R.id.list);
		myAdapter = new PlayListAdapter(this, myPlayList);
		l.setAdapter(myAdapter);
		l.setOnItemClickListener(this);
		registerForContextMenu(l);
		myVolumeBar = findViewById(R.id.volumebar);
		myVolumeBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
			private int myValueToSet = -1;
			@Override
			public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
				if (fromUser) {
					myValueToSet = progress;
				}
			}
			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {
				myVolumeBarIsAdjusting = true;
			}
			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {
				myVolumeBarIsAdjusting = false;
				if (myValueToSet != -1) {
					final NetworkRequest request = getRequest("setvolume");
					request.addPostParameter("volume", Integer.toString(myValueToSet));
					performRequest(request, false);
				}
			}
		});

		myPlayBar = findViewById(R.id.playbar);
		myPlayBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
			private int myValueToSet = -1;
			private int myTrackNum;
			@Override
			public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
				if (fromUser) {
					myValueToSet = progress;
					myCurrent.setText(ApplicationUtil.timeFormat(progress));
				}
			}
			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {
				myPlayBarIsAdjusting = true;
				myTrackNum = myStatus.myCurrentTrackNo;
			}
			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {
				myPlayBarIsAdjusting = false;
				if (myValueToSet != -1) {
					final NetworkRequest request = getRequest("seekto");
					request.addPostParameter("num", Integer.toString(myTrackNum));
					request.addPostParameter("position", Integer.toString(myValueToSet * 1000));
					performRequest(request, false);
				}
			}
		});

		setTitle(getResources().getString(R.string.playlist));

		myTotal = findViewById(R.id.total);
		myCurrent = findViewById(R.id.current);

		myPlayButton = findViewById(R.id.play);
		ImageButton stopButton = findViewById(R.id.stop);
		ImageButton nextButton = findViewById(R.id.next);
		ImageButton prevButton = findViewById(R.id.prev);
		myPlaytypeButton = findViewById(R.id.playtype);
		myPlayButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (myStatus.myState.equals(PlayerStatus.State.STOPPED) || myStatus.myState.equals(PlayerStatus.State.PAUSED)) {
					NetworkRequest request = getRequest("play");
					performRequest(request, false);
				}
				if (myStatus.myState.equals(PlayerStatus.State.PLAYING)) {
					NetworkRequest request = getRequest("pause");
					performRequest(request, false);
				}
			}
		});
		stopButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				//if (myStatus.myState.equals(PlayerStatus.State.PLAYING) || myStatus.myState.equals(PlayerStatus.State.PAUSED)) {
				NetworkRequest request = getRequest("stop");
				performRequest(request, false);
				//}
			}
		});
		nextButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if ((myStatus.myState.equals(PlayerStatus.State.PLAYING) || myStatus.myState.equals(PlayerStatus.State.PAUSED)) && (myStatus.myCurrentTrackNo + 1) < myPlayList.size()) {
					NetworkRequest request = getRequest("playnum");
					request.addPostParameter("num", Integer.toString(myStatus.myCurrentTrackNo + 1));
					performRequest(request, false);
				}
			}
		});
		prevButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if ((myStatus.myState.equals(PlayerStatus.State.PLAYING) || myStatus.myState.equals(PlayerStatus.State.PAUSED)) && (myStatus.myCurrentTrackNo - 1) >= 0) {
					NetworkRequest request = getRequest("playnum");
					request.addPostParameter("num", Integer.toString(myStatus.myCurrentTrackNo - 1));
					performRequest(request, false);
				}
			}
		});
		myPlaytypeButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				NetworkRequest request = getRequest("setplaytype");
				request.addPostParameter("type", myStatus.getNextType().name());
				performRequest(request, false);
			}
		});
		Timer tickTimer = new Timer();
		tickTimer.schedule(new TickTimerTask(), 500, 500);
		Timer connectTimer = new Timer();
		connectTimer.schedule(new ConnectTimerTask(), 10000, 10000);

		myNeedToCheck = true;
	}

	private void setUri(String uri) {
		ApplicationUtil.Data.serverUri = uri;
		ApplicationUtil.Data.cache.clear();
		ApplicationUtil.Data.lastLocalLocation = new ApplicationUtil.Location("/", "");
		ApplicationUtil.Data.lastSmbLocation = new ApplicationUtil.Location("smb://", "");
		Response r = new Response();
		r.myStatus = new PlayerStatus();
		r.myValid = uri != null;
		r.myCause = uri != null ? "" : getResources().getString(R.string.disconnected);
		init(r);
		getStatus(false);
		SharedPreferences settings = getSharedPreferences(ApplicationUtil.PREFS_NAME, 0);
		SharedPreferences.Editor editor = settings.edit();
		editor.putString(ApplicationUtil.LAST_URI, uri);
		editor.apply();

		invalidateOptionsMenu();
		ImageButton b = findViewById(R.id.local_button);
		b.setEnabled(ApplicationUtil.Data.serverUri != null);
		ImageButton b1 = findViewById(R.id.shared_button);
		b1.setEnabled(ApplicationUtil.Data.serverUri != null);

	}

	private void checkOnStart() {
		SharedPreferences settings = getSharedPreferences(ApplicationUtil.PREFS_NAME, 0);
		final String uri = settings.getString(ApplicationUtil.LAST_URI, null);
		if (uri == null) {
			setProgressBarVisibility(false);
			return;
		}
		final NetworkRequest request = new NetworkRequest(uri + "status") {
			@Override
			public void handleStream(InputStream inputStream) throws NetworkException {
				try {
					Response r = new Parser().parse(inputStream);
					init(r);
				} catch (Exception e) {
					throw new NetworkException(e);
				}
			}
		};
		setProgressBarVisibility(true);
		new Thread() {
			@Override
			public void run() {
				try {
					NetworkManager.Instance().perform(request);
					runOnUiThread(new Runnable() {
						@Override
						public void run() {
							setUri(uri);
						}
					});

				} catch (NetworkException e)  {
					if (e.isUnauthorized()) {
						//TODO: ask password
					}
					e.printStackTrace();
					onNetworkError(e);
				}
			}
		}.start();
	}

	private void connect() {
		Intent i = new Intent(this, ConnectActivity.class);
		startActivityForResult(i, ApplicationUtil.CONNECT_CODE);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == ApplicationUtil.CONNECT_CODE && resultCode == RESULT_OK) {
			String uri = data.getStringExtra("URI");
			setUri(uri);
		}
		if (requestCode == ApplicationUtil.BOOKMARKS_CODE && resultCode == RESULT_OK) {
			String path = data.getStringExtra("BOOKMARK");
			ApplicationUtil.open(this, path);
		}
		if (requestCode == ApplicationUtil.BROWSE_CODE && resultCode == RESULT_FIRST_USER) {
			setProgressBarVisibility(true);
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.activity_main, menu);
		return true;
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		super.onPrepareOptionsMenu(menu);
		menu.findItem(R.id.menu_connect).setVisible(ApplicationUtil.Data.serverUri == null);
		menu.findItem(R.id.menu_connected).setVisible(ApplicationUtil.Data.serverUri != null);
		menu.findItem(R.id.menu_reload).setVisible(ApplicationUtil.Data.serverUri != null);
		menu.findItem(R.id.menu_open).setVisible(ApplicationUtil.Data.serverUri != null);
		menu.findItem(R.id.menu_volume).setVisible(ApplicationUtil.Data.serverUri != null);
		menu.findItem(R.id.menu_stop_background).setVisible(ApplicationUtil.Data.serverUri != null && myStatus.myBackItem != null);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == R.id.menu_connect || item.getItemId() == R.id.menu_connected) {
			connect();
		}
		if (item.getItemId() == R.id.menu_reload) {
			reload();
		}
		if (item.getItemId() == R.id.menu_open) {
			showOpenDialog();
		}
		if (item.getItemId() == R.id.menu_volume) {
			showVolumeDialog();
		}
		if (item.getItemId() == R.id.menu_stop_background) {
			final NetworkRequest request = getRequest("stopbackground");
			performRequest(request, false);
		}
		return true;
	}

	private AlertDialog myVolumeDialog = null;

	private void showVolumeDialog() {
		View dialogView = View.inflate(this, R.layout.precisevolume, null);
		SeekBar vol = dialogView.findViewById(R.id.volumebar);
		vol.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
			private int myValueToSet = -1;
			@Override
			public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
				if (fromUser) {
					myValueToSet = progress;
				}
			}
			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {
				myMpVolumeBarIsAdjusting = true;
			}
			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {
				myMpVolumeBarIsAdjusting = false;
				if (myValueToSet != -1) {
					final NetworkRequest request = getRequest("setmpvolume");
					request.addPostParameter("volume", Integer.toString(myValueToSet));
					performRequest(request, false);
				}
			}
		});
		vol.setMax(myStatus.myMpMaxVolume);
		vol.setProgress(myStatus.myMpVolume);

		SeekBar volb = dialogView.findViewById(R.id.backvolumebar);
		volb.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
			private int myValueToSet = -1;
			@Override
			public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
				if (fromUser) {
					myValueToSet = progress;
				}
			}
			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {
				myBackMpVolumeBarIsAdjusting = true;
			}
			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {
				myBackMpVolumeBarIsAdjusting = false;
				if (myValueToSet != -1) {
					final NetworkRequest request = getRequest("setbackmpvolume");
					request.addPostParameter("volume", Integer.toString(myValueToSet));
					performRequest(request, false);
				}
			}
		});
		volb.setMax(myStatus.myBackMpMaxVolume);
		volb.setProgress(myStatus.myBackMpVolume);
		final AlertDialog.Builder d = new AlertDialog.Builder(this);
		d.setView(dialogView);
		d.setOnCancelListener(new OnCancelListener() {
			@Override
			public void onCancel(DialogInterface dialog) {
				myVolumeDialog = null;
			}
		});
		myVolumeDialog = d.show();
	}

	private void showOpenDialog() {
		View dialogView = View.inflate(this, R.layout.enter_value, null);
		((TextView)dialogView.findViewById(R.id.textView1)).setText(getResources().getString(R.string.path));
		final EditText input = dialogView.findViewById(R.id.name);
		input.setText("");
		final AlertDialog.Builder d = new AlertDialog.Builder(this)
		.setMessage(getResources().getString(R.string.enterPath))
		.setNeutralButton(getResources().getString(R.string.menu_bookmarks), new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog1, int which) {
				ApplicationUtil.showBookmarks(MainActivity.this);
			}
		})
		.setPositiveButton(getResources().getString(R.string.ok), new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog1, int which) {
				final String uri = input.getText().toString();
				dialog1.dismiss();
				ApplicationUtil.open(MainActivity.this, uri);
			}
		});
		d.setView(dialogView);
		d.show();
	}

	public void reload() {
		getStatus(false);
	}



	private static class Response {
		private boolean myValid;
		private String myCause;
		private PlayerStatus myStatus;
	}

	private static class Parser {
		Response parse(InputStream s) throws ParserConfigurationException, SAXException, IOException {
			Response res = new Response();
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			DocumentBuilder db = dbf.newDocumentBuilder();
			Document doc = db.parse(new InputSource(s));
			Element root = doc.getDocumentElement();
			if (!"response".equals(root.getTagName())) {
				Log.e("testmpclient", "wrong tag!!!");
			}
			res.myValid = root.getAttribute("valid").equals("true");
			res.myCause= root.getAttribute("reason");
			NodeList list = root.getElementsByTagName("status");
			if (list.getLength() == 1) {
				Element e = (Element)list.item(0);
				res.myStatus = PlayerStatus.fromDom(e);
			}
			return res;
		}
	}

	private NetworkRequest getRequest(String uri) {
		return getRequest(uri, false, false);
	}

	private NetworkRequest getRequest(String uri, final boolean lazy, final boolean doInit) {
		return new NetworkRequest(ApplicationUtil.Data.serverUri + uri) {
			@Override
			public void handleStream(InputStream inputStream) throws NetworkException {
				try {
					if (lazy) {
						myLazyStatusInProgress = false;
					}
					if (doInit) {
						if (ApplicationUtil.Data.serverUri != null) {
							init(new Parser().parse(inputStream));
						}
					}
				} catch (Exception e) {
					throw new NetworkException(e);
				}
			}
		};
	}

	private void performRequest(final NetworkRequest request, final boolean lazy) {
		if (!lazy) {
			setProgressBarVisibility(true);
		} else {
			myLazyStatusInProgress = true;
		}
		new Thread() {
			@Override
			public void run() {
				try {
					NetworkManager.Instance().perform(request);
				} catch (NetworkException e)  {
					if (lazy) {
						myLazyStatusInProgress = false;
					} else {
						onNetworkError(e);
					}
					e.printStackTrace();
				}
			}
		}.start();
	}

	void getStatus(final boolean lazy) {
		if (ApplicationUtil.Data.serverUri == null) {
			return;
		}
		if (lazy && myLazyStatusInProgress) {
			return;
		}
		String uri = lazy ? "lazystatus" : "status";
		final NetworkRequest request = getRequest(uri, lazy, true);
		performRequest(request, lazy);
	}

	void onNetworkError(final NetworkException e) {
		runOnUiThread(new Runnable() {
			public void run() {
				Toast.makeText(MainActivity.this, getResources().getString(R.string.networkError) + "\n" + e.getLocalizedMessage(MainActivity.this), Toast.LENGTH_LONG).show();
				setProgressBarVisibility(false);
			}
		});	
	}

	private void test(String share, String login, String password) {
		if (!share.endsWith("/")) {
			share +=  "/";
		}
		final NetworkRequest request = geTestRequest();
		request.addPostParameter("path", share);
		request.addPostParameter("login", login);
		request.addPostParameter("password", password);
		performRequest(request, false);
	}

	private NetworkRequest geTestRequest() {
		return new NetworkRequest(ApplicationUtil.Data.serverUri +"testShare") {
			@Override
			public void handleStream(InputStream inputStream) throws NetworkException {
				try {
					initTest(new BrowseActivity.Parser().parse(inputStream));
				} catch (Exception e) {
					throw new NetworkException(e);
				}
			}
		};
	}

	void initTest(final BrowseActivity.Response res) {
		if (!res.myValid) {
			if (res.myCause.startsWith("loginNeeded:")) {
				final String share = res.myCause.substring("loginNeeded:".length() + 1).trim();
				runOnUiThread(new Runnable() {
					public void run() {
						showLoginDialog(MainActivity.this, share, new ApplicationUtil.LoginRunnable() {
							@Override
							public  void  run(String login, String password) {
								test(share, login, password);
							}
						});
					}
				});
			} else {
				runOnUiThread(new Runnable() {
					public void run() {
						Toast.makeText(MainActivity.this, res.myCause, Toast.LENGTH_SHORT).show();
					}
				});
			}
		}
	}


	void init(final Response r) {
		getStatus(true);
		if (!r.myValid) {
			if (r.myCause.startsWith("loginNeeded:")) {
				final String share =r.myCause.substring("loginNeeded:".length() + 1).trim();
				runOnUiThread(new Runnable() {
					public void run() {
						showLoginDialog(MainActivity.this, share, new ApplicationUtil.LoginRunnable() {
							@Override
							public  void  run(String login, String password) {
								test(share, login, password);
							}
						});
					}
				});
			} else {
				runOnUiThread(new Runnable() {
					public void run() {
						Toast.makeText(MainActivity.this, r.myCause, Toast.LENGTH_SHORT).show();
						setProgressBarVisibility(false);
					}
				});
			}
		}
		myStatus = r.myStatus;
		setPosition(myStatus.myCurrentPosition, true);
		runOnUiThread(new Runnable() {
			public void run() {
				invalidateOptionsMenu();
				myPlayList.clear();
					myPlayList.addAll(myStatus.myPlaylist);
				myAdapter.notifyDataSetChanged();
				if (myStatus.myState.equals(PlayerStatus.State.PLAYING)) {
					myPlayButton.setImageResource(R.drawable.pause);
				} else {
					myPlayButton.setImageResource(R.drawable.play);
				}
				if (myStatus.myType.equals(PlayerStatus.PlaylistType.CYCLIC)) {
					myPlaytypeButton.setImageResource(R.drawable.cycle);
				} else {
					myPlaytypeButton.setImageResource(R.drawable.linear);
				}
				setPlayBar();
				setVolumeBar();
				setMpVolumeBar();
				setProgressBarVisibility(false);
				getContentView().postInvalidate();
			}
		});
	}

	protected View getContentView() {
		return findViewById(android.R.id.content);
	}

	private void setPlayBar() {
		if (myPlayBarIsAdjusting) {
			return;
		}
		if (myStatus.myCurrentDuration > 0 && myStatus.myCurrentPosition >= 0 && myStatus.myCurrentPosition <= myStatus.myCurrentDuration) {
			myPlayBar.setMax(myStatus.myCurrentDuration / 1000);
			myPlayBar.setProgress(myStatus.myCurrentPosition / 1000);
		} else {
			myPlayBar.setMax(0);
			myPlayBar.setProgress(0);
		}
		myTotal.setText(ApplicationUtil.timeFormat(myStatus.myCurrentDuration / 1000));
		myCurrent.setText(ApplicationUtil.timeFormat(myStatus.myCurrentPosition / 1000));
	}

	private void setVolumeBar() {
		if (myVolumeBarIsAdjusting) {
			return;
		}
		if (myStatus.myMaxVolume > 0 && myStatus.myVolume >= 0 && myStatus.myVolume <= myStatus.myMaxVolume) {
			myVolumeBar.setMax(myStatus.myMaxVolume);
			myVolumeBar.setProgress(myStatus.myVolume);
		} else {
			myVolumeBar.setMax(0);
			myVolumeBar.setProgress(0);
		}
	}

	private void setMpVolumeBar() {
		if (myVolumeDialog == null) {
			return;
		}
		if (!myMpVolumeBarIsAdjusting){
			SeekBar s = myVolumeDialog.findViewById(R.id.volumebar);
			if (myStatus.myMpMaxVolume > 0 && myStatus.myMpVolume >= 0 && myStatus.myMpVolume <= myStatus.myMpMaxVolume) {
				s.setMax(myStatus.myMpMaxVolume);
				s.setProgress(myStatus.myMpVolume);
			} else {
				s.setMax(0);
				s.setProgress(0);
			}
		}
		if (!myBackMpVolumeBarIsAdjusting){
			SeekBar s = myVolumeDialog.findViewById(R.id.backvolumebar);
			if (myStatus.myBackMpMaxVolume > 0 && myStatus.myBackMpVolume >= 0 && myStatus.myBackMpVolume <= myStatus.myBackMpMaxVolume) {
				s.setMax(myStatus.myBackMpMaxVolume);
				s.setProgress(myStatus.myBackMpVolume);
			} else {
				s.setMax(0);
				s.setProgress(0);
			}
		}
	}


	private class TickTimerTask extends TimerTask {
		@Override
		public void run() {
			if (myStatus.myState.equals(PlayerStatus.State.PLAYING)) {
				setPosition(0, false);
				runOnUiThread(new Runnable() {
					public void run() {
						setPlayBar();
						getContentView().postInvalidate();
					}
				});
			}
		}
	}

	private class ConnectTimerTask extends TimerTask {
		@Override
		public void run() {
			getStatus(true);
		}
	}

	private synchronized void setPosition(int ms, boolean sure) {
		if (sure) {
			myStatus.myCurrentPosition = ms;
			myLastPlayingPosition = ms;
			myLastUpdateTime = new Date();
		} else {
			myStatus.myCurrentPosition =  (int) (myLastPlayingPosition + new Date().getTime() - myLastUpdateTime.getTime());
		}
	}

	@Override
	public void onItemClick(AdapterView<?> p, View v, int position, long id) {
		NetworkRequest request = getRequest("playnum");
		request.addPostParameter("num", Integer.toString(position));
		performRequest(request, false);
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
		AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
		int pos = info.position;
		PlaylistItem i = myPlayList.get(pos);
		menu.setHeaderTitle(i.getName());
		menu.add(Menu.NONE, PLAY_OPTION, Menu.NONE, getResources().getString(R.string.play));
		menu.add(Menu.NONE, OPEN_OPTION, Menu.NONE, getResources().getString(R.string.openFolder));
		menu.add(Menu.NONE, REMOVE_OPTION, Menu.NONE, getResources().getString(R.string.remove));
		if (pos > 0) {
			menu.add(Menu.NONE, REMOVE_BEFORE_OPTION, Menu.NONE, getResources().getString(R.string.removeBefore));
		}
		if (pos < myPlayList.size() - 1) {
			menu.add(Menu.NONE, REMOVE_AFTER_OPTION, Menu.NONE, getResources().getString(R.string.removeAfter));
		}
		menu.add(Menu.NONE, CLEAR_OPTION, Menu.NONE, getResources().getString(R.string.clear));
		menu.add(Menu.NONE, STOPAFTER_OPTION, Menu.NONE, getResources().getString(R.string.stopAfter));
		menu.add(Menu.NONE, PAUSEAFTER_OPTION, Menu.NONE, getResources().getString(R.string.pauseAfter));
		if (myStatus.myStopAfter == pos) {
			menu.add(Menu.NONE, NOAFTER_OPTION, Menu.NONE, getResources().getString(R.string.clearStopAfter));
		}
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
		int position = info.position;
		PlaylistItem i = myPlayList.get(position);
		if (item.getItemId() == PLAY_OPTION) {
			NetworkRequest request = getRequest("playnum");
			request.addPostParameter("num", Integer.toString(position));
			performRequest(request, false);
		}
		if (item.getItemId() == OPEN_OPTION) {
			ApplicationUtil.open(this, AbsFile.parent(i.Path));
		}
		if (item.getItemId() == REMOVE_OPTION) {
			NetworkRequest request = getRequest("remove");
			request.addPostParameter("start", Integer.toString(position));
			request.addPostParameter("finish", Integer.toString(position));
			performRequest(request, false);
			myPlayList.remove(position);
			myAdapter.notifyDataSetChanged();
		}
		if (item.getItemId() == REMOVE_BEFORE_OPTION) {
			NetworkRequest request = getRequest("remove");
			request.addPostParameter("start", Integer.toString(0));
			request.addPostParameter("finish", Integer.toString(position - 1));
			performRequest(request, false);
			myPlayList.remove(position);
			myAdapter.notifyDataSetChanged();
		}
		if (item.getItemId() == REMOVE_AFTER_OPTION) {
			NetworkRequest request = getRequest("remove");
			request.addPostParameter("start", Integer.toString(position + 1));
			request.addPostParameter("finish", Integer.toString(myPlayList.size() - 1));
			performRequest(request, false);
			myPlayList.remove(position);
			myAdapter.notifyDataSetChanged();
		}
		if (item.getItemId() == CLEAR_OPTION) {
			NetworkRequest request = getRequest("clearplaylist");
			performRequest(request, false);
			myPlayList.clear();
			myAdapter.notifyDataSetChanged();
		}
		if (item.getItemId() == STOPAFTER_OPTION) {
			NetworkRequest request = getRequest("stopafter");
			request.addPostParameter("num", Integer.toString(position));
			request.addPostParameter("type", Integer.toString(PlayerStatus.STOP));
			performRequest(request, false);
			myAdapter.notifyDataSetChanged();
		}
		if (item.getItemId() == PAUSEAFTER_OPTION) {
			NetworkRequest request = getRequest("stopafter");
			request.addPostParameter("num", Integer.toString(position));
			request.addPostParameter("type", Integer.toString(PlayerStatus.PAUSE));
			performRequest(request, false);
			myAdapter.notifyDataSetChanged();
		}
		if (item.getItemId() == NOAFTER_OPTION) {
			NetworkRequest request = getRequest("stopafter");
			request.addPostParameter("num", Integer.toString(-1));
			request.addPostParameter("type", Integer.toString(PlayerStatus.STOP));
			performRequest(request, false);
			myAdapter.notifyDataSetChanged();
		}
		return true;
	}



}
