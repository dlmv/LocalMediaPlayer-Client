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
import com.dlmv.localplayer.client.util.ServerPath;

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
				NetworkRequest request = getRequest(ServerPath.VOLUME_UP);
				performRequest(request, false);
			}
			return true;
		}
		if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN && event.getAction() == KeyEvent.ACTION_DOWN ) {
			if (myStatus.myVolume > 0) {
				NetworkRequest request = getRequest(ServerPath.VOLUME_DOWN);
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
					final NetworkRequest request = getRequest(ServerPath.SET_VOLUME);
					request.addPostParameter(ServerPath.VOLUME, Integer.toString(myValueToSet));
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
					final NetworkRequest request = getRequest(ServerPath.SEEK_TO);
					request.addPostParameter(ServerPath.NUM, Integer.toString(myTrackNum));
					request.addPostParameter(ServerPath.POSITION, Integer.toString(myValueToSet * 1000));
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
					NetworkRequest request = getRequest(ServerPath.PLAY);
					performRequest(request, false);
				} else if (myStatus.myState.equals(PlayerStatus.State.PLAYING)) {
					NetworkRequest request = getRequest(ServerPath.PAUSE);
					performRequest(request, false);
				}
			}
		});
		stopButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				//if (myStatus.myState.equals(PlayerStatus.State.PLAYING) || myStatus.myState.equals(PlayerStatus.State.PAUSED)) {
				NetworkRequest request = getRequest(ServerPath.STOP);
				performRequest(request, false);
				//}
			}
		});
		nextButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if ((myStatus.myState.equals(PlayerStatus.State.PLAYING) || myStatus.myState.equals(PlayerStatus.State.PAUSED)) && (myStatus.myCurrentTrackNo + 1) < myPlayList.size()) {
					NetworkRequest request = getRequest(ServerPath.PLAY_NUM);
					request.addPostParameter(ServerPath.NUM, Integer.toString(myStatus.myCurrentTrackNo + 1));
					performRequest(request, false);
				}
			}
		});
		prevButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if ((myStatus.myState.equals(PlayerStatus.State.PLAYING) || myStatus.myState.equals(PlayerStatus.State.PAUSED)) && (myStatus.myCurrentTrackNo - 1) >= 0) {
					NetworkRequest request = getRequest(ServerPath.PLAY_NUM);
					request.addPostParameter(ServerPath.NUM, Integer.toString(myStatus.myCurrentTrackNo - 1));
					performRequest(request, false);
				}
			}
		});
		myPlaytypeButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				NetworkRequest request = getRequest(ServerPath.SET_PLAYTYPE);
				request.addPostParameter(ServerPath.TYPE, myStatus.getNextType().name());
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
		myLoginErrorHandled = false;
		ApplicationUtil.Data.setUri(uri);
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

	}

	private void checkOnStart() {
		SharedPreferences settings = getSharedPreferences(ApplicationUtil.PREFS_NAME, 0);
		final String uri = settings.getString(ApplicationUtil.LAST_URI, null);
		if (uri == null) {
			setProgressBarVisibility(false);
			return;
		}
		final NetworkRequest request = new NetworkRequest(uri + ServerPath.STATUS) {
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
						ConnectActivity.tryLogin(MainActivity.this, uri, new ConnectActivity.ServerLoginRunnable() {
							@Override
							public void run(String password) {
								setUri(uri);
							}
						}, new Runnable() {
							@Override
							public void run() {
								setProgressBarVisibility(false);
							}
						}, true);
					} else {
						onNetworkError(e);
					}
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
		menu.findItem(R.id.menu_browse).setVisible(ApplicationUtil.Data.serverUri != null);
		menu.findItem(R.id.menu_volume).setVisible(ApplicationUtil.Data.serverUri != null);
		menu.findItem(R.id.menu_settings).setVisible(ApplicationUtil.Data.serverUri != null);
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
		if (item.getItemId() == R.id.menu_browse) {
			ApplicationUtil.browse(this);
		}
		if (item.getItemId() == R.id.menu_volume) {
			showVolumeDialog();
		}
		if (item.getItemId() == R.id.menu_settings) {
			showSettingsDialog();
		}
		return true;
	}

	private void showSettingsDialog() {
		View dialogView = View.inflate(this, R.layout.settingsdialog, null);
		final AlertDialog.Builder d = new AlertDialog.Builder(this).setTitle(getResources().getString(R.string.menu_settings));
		Button passwordButton = dialogView.findViewById(R.id.setPassword);
		passwordButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				setPassword();
			}
		});
		Button loginsButton = dialogView.findViewById(R.id.editLogins);
		loginsButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				showLogins();
			}
		});
		d.setView(dialogView);
		d.show();
	}

	private void setPassword() {
		View dialogView = View.inflate(this, R.layout.login_dialog, null);
		((TextView) dialogView.findViewById(R.id.loginText)).setText(getResources().getString(R.string.master_password));
		((TextView) dialogView.findViewById(R.id.passwordText)).setText(getResources().getString(R.string.server_password));
		final EditText inputL = dialogView.findViewById(R.id.login);
		final EditText inputP = dialogView.findViewById(R.id.password);
		inputL.setText("");
		inputP.setText("");
		final AlertDialog.Builder d = new AlertDialog.Builder(this)
				.setPositiveButton(getResources().getString(R.string.ok), new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog1, int which) {
						final String login = inputL.getText().toString();
						final String password = inputP.getText().toString();
						final NetworkRequest request = getRequest(ServerPath.SET_PASSWORD);
						request.addPostParameter(ServerPath.MASTER_PASSWORD, login);
						request.addPostParameter(ServerPath.PASSWORD, password);
						new Thread() {
							@Override
							public void run() {
								try {
									NetworkManager.Instance().perform(request);
								} catch (NetworkException e)  {
									if (e.isUnauthorized()) {
										runOnUiThread(new Runnable() {
											@Override
											public void run() {
												setPassword();
											}
										});
									}  else {
										onNetworkError(e);
									}
								}
							}
						}.start();
						dialog1.dismiss();
					}
				}).setNegativeButton(getResources().getString(R.string.cancel), new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog1, int which) {
						dialog1.dismiss();
					}
				});
		d.setView(dialogView);
		d.show();
	}

	private void showLogins() {
		final NetworkRequest request = new NetworkRequest(ApplicationUtil.Data.serverUri + ServerPath.LOGIN_LIST) {
			@Override
			public void handleStream(InputStream inputStream) throws NetworkException {
				try {
					ShareLoginsActivity.callMe(new ShareLoginsActivity.Parser().parse(inputStream), MainActivity.this);
				} catch (Exception e) {
					throw  new NetworkException(e);
				}
			}
		};
		performRequest(request, false);
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
					final NetworkRequest request = getRequest(ServerPath.SET_MP_VOLUME);
					request.addPostParameter(ServerPath.VOLUME, Integer.toString(myValueToSet));
					performRequest(request, false);
				}
			}
		});

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
					final NetworkRequest request = getRequest(ServerPath.SET_BACKMP_VOLUME);
					request.addPostParameter(ServerPath.VOLUME, Integer.toString(myValueToSet));
					performRequest(request, false);
				}
			}
		});

		ImageButton playButton = dialogView.findViewById(R.id.playbackground);
		playButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				if (myStatus.myBackState.equals(PlayerStatus.State.PAUSED)) {
					NetworkRequest request = getRequest(ServerPath.RESUME_BACKGROUND);
					performRequest(request, false);
				} else if (myStatus.myBackState.equals(PlayerStatus.State.PLAYING)) {
					NetworkRequest request = getRequest(ServerPath.PAUSE_BACKGROUND);
					performRequest(request, false);
				}
			}
		});
		ImageButton stopButton = dialogView.findViewById(R.id.stopbackground);
		stopButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				NetworkRequest request = getRequest(ServerPath.STOP_BACKGROUND);
				performRequest(request, false);
			}
		});

		final AlertDialog.Builder d = new AlertDialog.Builder(this);
		d.setView(dialogView);
		d.setOnCancelListener(new OnCancelListener() {
			@Override
			public void onCancel(DialogInterface dialog) {
				myVolumeDialog = null;
			}
		});
		myVolumeDialog = d.show();
		setupVolumeDialog();
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
			res.myValid = Boolean.parseBoolean(root.getAttribute("valid"));
			res.myCause = root.getAttribute("reason");
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
					} else {
						runOnUiThread(new Runnable() {
							@Override
							public void run() {
								setProgressBarVisibility(false);
							}
						});
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
		if (ApplicationUtil.Data.serverUri == null) {
			return;
		}
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
						if (e.isUnauthorized()) {
							onNetworkError(e);
						}
					} else {
						runOnUiThread(new Runnable() {
							@Override
							public void run() {
								setProgressBarVisibility(false);
							}
						});
						onNetworkError(e);
					}
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
		String uri = lazy ? ServerPath.LAZY_STATUS : ServerPath.STATUS;
		final NetworkRequest request = getRequest(uri, lazy, true);
		performRequest(request, lazy);
	}

	private boolean myLoginErrorHandled = false;

	void onNetworkError(final NetworkException e) {
		runOnUiThread(new Runnable() {
			public void run() {
				setProgressBarVisibility(false);
				if (e.isUnauthorized()) {
					if (!myLoginErrorHandled) {
						myLoginErrorHandled = true;
						if (!isFinishing()) {
							ConnectActivity.tryLogin(MainActivity.this, ApplicationUtil.Data.serverUri, new ConnectActivity.ServerLoginRunnable() {
								@Override
								public void run(String password) {
									setUri(ApplicationUtil.Data.serverUri);
								}
							}, new Runnable() {
								@Override
								public void run() {
									setUri(null);
								}
							}, true);
						}
					}
				} else {
					Toast.makeText(MainActivity.this, getResources().getString(R.string.networkError) + "\n" + e.getLocalizedMessage(MainActivity.this), Toast.LENGTH_LONG).show();
				}
			}
		});	
	}

	private void test(String share, String login, String password) {
		if (!share.endsWith("/")) {
			share +=  "/";
		}
		final NetworkRequest request = getTestRequest();
		request.addPostParameter(ServerPath.PATH, share);
		request.addPostParameter(ServerPath.LOGIN, login);
		request.addPostParameter(ServerPath.PASSWORD, password);
		performRequest(request, false);
	}

	private NetworkRequest getTestRequest() {
		return new NetworkRequest(ApplicationUtil.Data.serverUri + ServerPath.CHECK_SHARE) {
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
		Log.d("WTF", "init");
		getStatus(true);
		runOnUiThread(new Runnable() {
			public void run() {
				if (!r.myValid) {
					if (r.myCause.startsWith("loginNeeded:")) {
						final String share = r.myCause.substring("loginNeeded:".length() + 1).trim();
						showLoginDialog(MainActivity.this, share, new ApplicationUtil.LoginRunnable() {
							@Override
							public void run(String login, String password) {
								test(share, login, password);
							}
						});
					} else {
						Toast.makeText(MainActivity.this, r.myCause, Toast.LENGTH_SHORT).show();
					}
				}
				myStatus = r.myStatus;
				setPosition(myStatus.myCurrentPosition, true);
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
				setupVolumeDialog();
				getContentView().postInvalidate();
				setProgressBarVisibility(false);
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

	private void setupVolumeDialog() {
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
		ImageButton playButton = myVolumeDialog.findViewById(R.id.playbackground);
		ImageButton stopButton = myVolumeDialog.findViewById(R.id.stopbackground);
		TextView backTitle = myVolumeDialog.findViewById(R.id.backTitle);
		if (myStatus.myBackState.equals(PlayerStatus.State.PLAYING)) {
			playButton.setVisibility(View.VISIBLE);
			stopButton.setVisibility(View.VISIBLE);
			playButton.setImageResource(R.drawable.pause);
		} else if (myStatus.myBackState.equals(PlayerStatus.State.PAUSED)) {
			playButton.setVisibility(View.VISIBLE);
			stopButton.setVisibility(View.VISIBLE);
			playButton.setImageResource(R.drawable.play);
		} else {
			playButton.setVisibility(View.GONE);
			stopButton.setVisibility(View.GONE);
		}
		if (myStatus.myBackItem != null) {
			backTitle.setText(myStatus.myBackItem.getName());
		} else {
			backTitle.setText(R.string.empty);
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
		NetworkRequest request = getRequest(ServerPath.PLAY_NUM);
		request.addPostParameter(ServerPath.NUM, Integer.toString(position));
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
			NetworkRequest request = getRequest(ServerPath.PLAY_NUM);
			request.addPostParameter(ServerPath.NUM, Integer.toString(position));
			performRequest(request, false);
		}
		if (item.getItemId() == OPEN_OPTION) {
			ApplicationUtil.open(this, AbsFile.parent(i.Path));
		}
		if (item.getItemId() == REMOVE_OPTION) {
			NetworkRequest request = getRequest(ServerPath.REMOVE);
			request.addPostParameter(ServerPath.START, Integer.toString(position));
			request.addPostParameter(ServerPath.FINISH, Integer.toString(position));
			performRequest(request, false);
			myPlayList.remove(position);
			myAdapter.notifyDataSetChanged();
		}
		if (item.getItemId() == REMOVE_BEFORE_OPTION) {
			NetworkRequest request = getRequest(ServerPath.REMOVE);
			request.addPostParameter(ServerPath.START, Integer.toString(0));
			request.addPostParameter(ServerPath.FINISH, Integer.toString(position - 1));
			performRequest(request, false);
			myPlayList.remove(position);
			myAdapter.notifyDataSetChanged();
		}
		if (item.getItemId() == REMOVE_AFTER_OPTION) {
			NetworkRequest request = getRequest(ServerPath.REMOVE);
			request.addPostParameter(ServerPath.START, Integer.toString(position + 1));
			request.addPostParameter(ServerPath.FINISH, Integer.toString(myPlayList.size() - 1));
			performRequest(request, false);
			myPlayList.remove(position);
			myAdapter.notifyDataSetChanged();
		}
		if (item.getItemId() == CLEAR_OPTION) {
			NetworkRequest request = getRequest(ServerPath.CLEAR);
			performRequest(request, false);
			myPlayList.clear();
			myAdapter.notifyDataSetChanged();
		}
		if (item.getItemId() == STOPAFTER_OPTION) {
			NetworkRequest request = getRequest(ServerPath.STOP_AFTER);
			request.addPostParameter(ServerPath.NUM, Integer.toString(position));
			request.addPostParameter(ServerPath.TYPE, Integer.toString(PlayerStatus.STOP));
			performRequest(request, false);
			myAdapter.notifyDataSetChanged();
		}
		if (item.getItemId() == PAUSEAFTER_OPTION) {
			NetworkRequest request = getRequest(ServerPath.STOP_AFTER);
			request.addPostParameter(ServerPath.NUM, Integer.toString(position));
			request.addPostParameter(ServerPath.TYPE, Integer.toString(PlayerStatus.PAUSE));
			performRequest(request, false);
			myAdapter.notifyDataSetChanged();
		}
		if (item.getItemId() == NOAFTER_OPTION) {
			NetworkRequest request = getRequest(ServerPath.STOP_AFTER);
			request.addPostParameter(ServerPath.NUM, Integer.toString(-1));
			request.addPostParameter(ServerPath.TYPE, Integer.toString(PlayerStatus.STOP));
			performRequest(request, false);
			myAdapter.notifyDataSetChanged();
		}
		return true;
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
	}



}
