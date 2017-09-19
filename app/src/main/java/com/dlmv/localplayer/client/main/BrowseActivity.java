package com.dlmv.localplayer.client.main;

import java.io.*;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.*;

import javax.xml.parsers.*;

import org.w3c.dom.*;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import com.dlmv.localmediaplayer.client.R;
import com.dlmv.localplayer.client.util.AbsFile;
import com.dlmv.localplayer.client.util.ApplicationUtil;
import com.dlmv.localplayer.client.util.ApplicationUtil.Location;
import com.dlmv.localplayer.client.db.*;
import com.dlmv.localplayer.client.image.ImageViewActivity;
import com.dlmv.localplayer.client.network.*;
import com.dlmv.localplayer.client.util.RootApplication;
import com.dlmv.localplayer.client.util.UpFile;

import android.app.*;
import android.content.*;
import android.content.DialogInterface.OnCancelListener;
import android.os.Bundle;
import android.util.Log;
import android.view.*;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.*;

import static com.dlmv.localplayer.client.util.ApplicationUtil.showLoginDialog;

public class BrowseActivity extends Activity implements AdapterView.OnItemClickListener {

	static class Response {
		boolean myValid;
		String myCause;
		private Location myLocation;
		private ArrayList<AbsFile> myContent;
	}

	private enum Mode {
		LOCAL,
		SMB,
	}

	private Mode myMode;

	static class Parser {
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
			if (res.myValid) {
				NodeList list = root.getElementsByTagName("result");
				if (list.getLength() == 1) {
					Element dir = (Element)list.item(0);
					String path = URLDecoder.decode(dir.getAttribute("path"), "UTF-8");
					String request = URLDecoder.decode(dir.getAttribute("request"), "UTF-8");
					res.myLocation = new Location(path, request);
					NodeList list1 = dir.getElementsByTagName("absfile");
					res.myContent = new ArrayList<>();
					for (int i = 0; i < list1.getLength(); ++i) {
						Element e = (Element)list1.item(i);
						AbsFile f = AbsFile.fromDom(e);
						res.myContent.add(f);
					}
				}
			}
			return res;
		}
	}



	private Location myLocation;
	private Location myLocationToOpen;
	private Location myTempLocation;

	private boolean iamDying = false;
	public void prepareToDie() {
		iamDying = true;
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.browse);
		mySearchDialog = new ProgressDialog(this);

		ApplicationUtil.Data.history.clear();
		String path = (getIntent().getStringExtra(ApplicationUtil.LOCATION_PATH));
		String request = (getIntent().getStringExtra(ApplicationUtil.LOCATION_REQUEST));
		myLocation = new Location(path, request);

		Button exit = findViewById(R.id.exit);

		exit.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				prepareToDie();
				finish();
			}
		});
		exit.setText(getResources().getString(R.string.exit));

		setTitle(getResources().getString(R.string.browse));


		myAdapter = new DirAdapter(this, myCurrentContent);
		getListView().setAdapter(myAdapter);
		getListView().setOnItemClickListener(this);

		registerForContextMenu(getListView());

		open(myLocation);

	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_DOWN) {
			if (!back()) {
				prepareToDie();
				finish();
			}
			return true;
		}
		return super.onKeyDown(keyCode, event);
	}

	private void open(String path) {
		filter(null);
		open(new Location(path, ""));
	}

	private void search(String request) {
		open(new Location(myLocation.Path, request));
	}

	private NetworkRequest getRequest(String uri, final boolean init) {
		return new NetworkRequest(ApplicationUtil.Data.serverUri + uri) {
			@Override
			public void handleStream(InputStream inputStream) throws NetworkException {
				try {
					if (init) {
						init(new Parser().parse(inputStream));
					}
				} catch (Exception e) {
					throw new NetworkException(e);
				}
			}
		};
	}

    private NetworkRequest getRequest(String uri)  {
        return  getRequest(uri, true);
    }


	private NetworkRequest geTestRequest(final String path, final Runnable doAfter) {
		return new NetworkRequest(ApplicationUtil.Data.serverUri +"test") {
			@Override
			public void handleStream(InputStream inputStream) throws NetworkException {
				try {
					initTest(new Parser().parse(inputStream), path, doAfter);
				} catch (Exception e) {
					throw new NetworkException(e);
				}
			}
		};
	}

	private void performRequest(final NetworkRequest request, final boolean quiet) {
		if (!quiet) {
			setMyProgressBarVisibility(true);
		}
		new Thread() {
			@Override
			public void run() {
				try {
					NetworkManager.Instance().perform(request);
				} catch (NetworkException e)  {
					if (!quiet) {
						onNetworkError(e);
					}
					e.printStackTrace();
				}
			}
		}.start();
	}

	private void open(Location l) {
		open(l, false);
	}

	private void open(Location l, boolean force) {
		if (myLocation != null) {
			myTempLocation = myLocation;
		}
		myLocationToOpen = l;
		if (!force && ApplicationUtil.Data.cache.containsKey(l)) {
			Response r = new Response();
			r.myValid = true;
			r.myLocation = myLocationToOpen;
			r.myContent = ApplicationUtil.Data.cache.get(l);
			init(r);
			return;
		}
		String uri = l.Request.equals("") ? "browse" : "search";

		final NetworkRequest request = getRequest(uri);
		request.addPostParameter("path", l.Path);
		request.addPostParameter("request", l.Request);
		performRequest(request, false);
	}
	
	private void open(Location l, String login, String password) {
		if (myLocation != null) {
			myTempLocation = myLocation;
		}
		myLocationToOpen = l;
		String uri = l.Request.equals("") ? "browse" : "search";

		final NetworkRequest request = getRequest(uri);
		request.addPostParameter("path", l.Path);
		request.addPostParameter("login", login);
		request.addPostParameter("password", password);
		request.addPostParameter("request", l.Request);
		performRequest(request, false);
	}

	private void test(String path, Runnable doAfter) {
		final NetworkRequest request = geTestRequest(path, doAfter);
		request.addPostParameter("path", path);
		performRequest(request, false);
	}

	private void test(String path, Runnable doAfter, String login, String password) {
		final NetworkRequest request = geTestRequest(path, doAfter);
		request.addPostParameter("path", path);
		request.addPostParameter("login", login);
		request.addPostParameter("password", password);
		performRequest(request, false);
	}

	private void stopSearch() {
		performRequest(getRequest("stopsearch"), true);
	}

	private boolean back() {
		if (myFilter != null) {
			filter(null);
			return true;
		}
		if (ApplicationUtil.Data.history.empty()) {
			return false;
		}
		if (ApplicationUtil.Data.history.peek().equals(myLocation)) {
			return false;
		}
		myTempLocation = myLocation;
		myLocation = null;
		Location l = ApplicationUtil.Data.history.pop();
		open(l);
		return true;
	}

	public ListView getListView() {
		return (ListView)findViewById(R.id.dir_list);
	}

	void onNetworkError(final NetworkException e) {
		runOnUiThread(new Runnable() {
			public void run() {
				Toast.makeText(BrowseActivity.this, getResources().getString(R.string.networkError) + ": " + e.getLocalizedMessage(BrowseActivity.this), Toast.LENGTH_SHORT).show();
				setMyProgressBarVisibility(false);
			}
		});	
	}

	private ArrayList<AbsFile> myCurrentContent = new ArrayList<>();
	private ArrayList<AbsFile> myFullContent = new ArrayList<>();
	private DirAdapter myAdapter;

	void initTest(final Response res, final String path, final Runnable doAfter) {
		if (iamDying) {
			return;
		}
		if (!res.myValid) {
			if (res.myCause.startsWith("loginNeeded:")) {
				final String share = res.myCause.substring("loginNeeded:".length() + 1).trim();
				runOnUiThread(new Runnable() {
					public void run() {
						showLoginDialog(BrowseActivity.this, share, new ApplicationUtil.LoginRunnable() {
							@Override
							public  void  run(String login, String password) {
								test(path, doAfter, login, password);
							}
						});
					}
				});
			} else {
				runOnUiThread(new Runnable() {
					public void run() {
						Toast.makeText(BrowseActivity.this, res.myCause, Toast.LENGTH_SHORT).show();
						setMyProgressBarVisibility(false);
					}
				});
			}
		} else {
			runOnUiThread(new Runnable() {
				public void run() {
					setMyProgressBarVisibility(false);
					doAfter.run();
				}
			});
		}
	}

	void init(final Response res) {
		if (iamDying) {
			return;
		}
		mySearchDialog.dismiss();
		if (!res.myValid) {
			if (res.myCause.startsWith("loginNeeded:")) {
				final String share = res.myCause.substring("loginNeeded:".length() + 1).trim();
				runOnUiThread(new Runnable() {
					public void run() {
						showLoginDialog(BrowseActivity.this, share, new ApplicationUtil.LoginRunnable() {
							@Override
							public  void  run(String login, String password) {
								open(myLocationToOpen, login, password);
							}
						});
					}
				});
				return;
			} else {
				runOnUiThread(new Runnable() {
					public void run() {
						Toast.makeText(BrowseActivity.this, res.myCause, Toast.LENGTH_SHORT).show();
						setMyProgressBarVisibility(false);
					}
				});
				return;
			}
		}
		if (!res.myLocation.equals(myLocationToOpen)) {
			return;
		}
		if (myLocationToOpen.Path.startsWith("smb://")) {
			myMode = Mode.SMB;
		} else if (myLocationToOpen.Path.startsWith("/")) {
			myMode = Mode.LOCAL;
		} else {
			prepareToDie();
			finish();
		}
		myLocationToOpen = null;
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				TextView t = findViewById(R.id.dir_header);
				if (!res.myLocation.Request.equals("")) {
					t.setText(getResources().getString(R.string.searchResult));
				} else if (res.myLocation.Path.equals("/") || res.myLocation.Path.equals("smb://")) {
					t.setText(res.myLocation.Path);
				} else {
					String path = res.myLocation.Path.substring(0, res.myLocation.Path.length() - 1);
					int divider = path.lastIndexOf("/");
					t.setText(path.substring(divider+1));
				}
				myFullContent.clear();
				if (AbsFile.parent(res.myLocation.Path) != null) {
					String parent = AbsFile.parent(res.myLocation.Path);
					AbsFile f = new UpFile(parent);
					myFullContent.add(f);
				}
				myFullContent.addAll(res.myContent);
				myCurrentContent.clear();
				myCurrentContent.addAll(myFullContent);
				myAdapter.notifyDataSetChanged();
				int sel = 0;
				if (myTempLocation != null) {
					for (int i = 0; i < myCurrentContent.size(); ++i) {
						if ((myCurrentContent.get(i).getPath()).equals(myTempLocation.Path)) {
							sel = i;
							break;
						}
					}
					myTempLocation = null;
				}
				final int sel1 = sel;
				getListView().post(new Runnable(){
					@Override
					public void run() {
						getListView().setSelection(sel1);
					}
				}
				);
				setMyProgressBarVisibility(false);
				if (myLocation != null && (ApplicationUtil.Data.history.empty() || !ApplicationUtil.Data.history.peek().equals(myLocation))) {
					ApplicationUtil.Data.history.push(myLocation);
				}
				String p = res.myLocation.Path;
				if (!p.endsWith("/")) {
					p +=  "/";
				}
				myLocation = new Location(p, res.myLocation.Request);
				ApplicationUtil.Data.cache.put(myLocation, res.myContent);
				if (myMode.equals(Mode.LOCAL)) {
					ApplicationUtil.Data.lastLocalLocation = myLocation;
				}
				if (myMode.equals(Mode.SMB)) {
					ApplicationUtil.Data.lastSmbLocation = myLocation;
				}
				initStar();
			}
		});
	}

	public void setMyProgressBarVisibility(boolean visible) {
		ProgressBar b = findViewById(R.id.progress);
		ImageView i = findViewById(R.id.star);
		if (visible) {
			b.setVisibility(View.VISIBLE);
			i.setVisibility(View.GONE);
		} else {
			b.setVisibility(View.GONE);
			i.setVisibility(View.VISIBLE);
		}
	}

	@Override
	public void onItemClick(AdapterView<?> p, View v, int position, long id) {
		AbsFile f = (AbsFile) p.getItemAtPosition(position);
		if ((f.Type.equals(AbsFile.MediaType.DIR) || f.Type.equals(AbsFile.MediaType.UP)) && f.Readable) {
			open(f.getPath());
		}
		if (f.Type.equals(AbsFile.MediaType.IMAGE) && f.Readable) { 
			view(f.getPath());
		}
	}
	
	private final static int OPEN_OPTION = 0;
	private final static int ENQUEUE_OPTION =1;
	private final static int PLAY_OPTION = 2;
	private final static int VIEW_OPTION = 3;
	private final static int OPEN_FOLDER_OPTION = 4;
	private final static int PLAY_BACKGROUND_OPTION = 5;

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
		AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
		int pos = info.position;
		AbsFile f = myCurrentContent.get(pos);
		menu.setHeaderTitle(f.Path);
		if (f.Readable) {
			if (f.Type.equals(AbsFile.MediaType.DIR)) {
				menu.add(Menu.NONE, OPEN_OPTION, Menu.NONE, getResources().getString(R.string.open));
			}
			if (f.Type.equals(AbsFile.MediaType.UP)) {
				menu.add(Menu.NONE, OPEN_OPTION, Menu.NONE, getResources().getString(R.string.up));
			}
			if (f.Type.equals(AbsFile.MediaType.DIR) || f.Type.equals(AbsFile.MediaType.AUDIO)) {
				menu.add(Menu.NONE, ENQUEUE_OPTION, Menu.NONE, getResources().getString(R.string.enqueue));
				menu.add(Menu.NONE, PLAY_OPTION, Menu.NONE, getResources().getString(R.string.play));
			}
			if (f.Type.equals(AbsFile.MediaType.IMAGE)) {
				menu.add(Menu.NONE, VIEW_OPTION, Menu.NONE, getResources().getString(R.string.view));
			}
			if (!myLocation.Request.equals("")) {
				menu.add(Menu.NONE, OPEN_FOLDER_OPTION, Menu.NONE, getResources().getString(R.string.openFolder));
			}
			if (f.Type.equals(AbsFile.MediaType.AUDIO)) {
				menu.add(Menu.NONE, PLAY_BACKGROUND_OPTION, Menu.NONE, getResources().getString(R.string.playBackground));
			}
		}
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
		int pos = info.position;
		final AbsFile f = myCurrentContent.get(pos);
		if (item.getItemId() == OPEN_OPTION) {
			open(f.getPath());
		}
		if (item.getItemId() == ENQUEUE_OPTION) {
			test(f.getPath(), new Runnable() {
				@Override
				public void run() {
					NetworkRequest request = getRequest("enqueue", false);
					request.addPostParameter("path", f.getPath());
					performRequest(request, true);
				}
			});
		}
		if (item.getItemId() == PLAY_OPTION) {
			test(f.getPath(), new Runnable() {
				@Override
				public void run() {
					NetworkRequest request = getRequest("enqueueandplay", false);
					request.addPostParameter("path", f.getPath());
					performRequest(request, true);
					setResult(RESULT_FIRST_USER);
					prepareToDie();
					finish();
				}
			});
		}
		if (item.getItemId() == VIEW_OPTION) {
			view(f.getPath());
		}
		if (item.getItemId() == OPEN_FOLDER_OPTION) {
			open(AbsFile.parent(f.getPath()));
		}
		if (item.getItemId() == PLAY_BACKGROUND_OPTION) {
			test(f.getPath(), new Runnable() {
				@Override
				public void run() {
					NetworkRequest request = getRequest("playbackground");
					request.addPostParameter("path", f.getPath());
					performRequest(request, true);
				}
			});
		}
		return true;
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.activity_browse, menu);

		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == R.id.menu_reload) {
			reload();
		}
		if (item.getItemId() == R.id.menu_open) {
			showOpenDialog();
		}
		if (item.getItemId() == R.id.menu_bookmarks) {
			ApplicationUtil.showBookmarks(this);
		}
		if (item.getItemId() == R.id.menu_search) {
			showSearchDialog();
		}
		if (item.getItemId() == R.id.menu_filter) {
			showFilterDialog();
		}
		return true;
	}

	public void reload() {
		open(myLocation, true);
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
				ApplicationUtil.showBookmarks(BrowseActivity.this);
			}
		})
		.setPositiveButton(getResources().getString(R.string.ok), new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog1, int which) {
				final String uri = input.getText().toString();
				dialog1.dismiss();
				open(uri);
			}
		});
		d.setView(dialogView);
		d.show();
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == ApplicationUtil.BOOKMARKS_CODE && resultCode == RESULT_OK) {
			String path = data.getStringExtra("BOOKMARK");
			open(path);
		}
	}

	private ProgressDialog mySearchDialog;

	private void showSearchDialog() {
		View dialogView = View.inflate(this, R.layout.enter_value, null);
		((TextView)dialogView.findViewById(R.id.textView1)).setText(getResources().getString(R.string.enterRequest));
		final EditText input = dialogView.findViewById(R.id.name);
		input.setText("");
		final AlertDialog.Builder d = new AlertDialog.Builder(this)
		.setMessage(getResources().getString(R.string.menu_search))
		.setPositiveButton(getResources().getString(R.string.ok), new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog1, int which) {
				final String request = input.getText().toString();
				dialog1.dismiss();
				mySearchDialog.setTitle(getResources().getString(R.string.menu_search));
				mySearchDialog.setMessage(getResources().getString(R.string.wait));
				mySearchDialog.setCancelable(true);
				mySearchDialog.setOnCancelListener(new OnCancelListener() {
					@Override
					public void onCancel(DialogInterface dialog) {
						stopSearch();
					}
				});
				mySearchDialog.show();
				search(request);
			}
		});
		d.setView(dialogView);
		d.show();
	}
	
	private String myFilter = null;
	
	private void filter(String filter) {
		myCurrentContent.clear();
		myFilter = filter;
		if (myFilter == null) {
			myCurrentContent.addAll(myFullContent);
		} else {
			for (AbsFile f : myFullContent) {
				if (f.getName().toLowerCase().contains(myFilter.toLowerCase())) {
					myCurrentContent.add(f);
				}
			}
		}
		myAdapter.notifyDataSetChanged();
		setMyProgressBarVisibility(false);
	}
	
	private void showFilterDialog() {
		View dialogView = View.inflate(this, R.layout.enter_value, null);
		((TextView)dialogView.findViewById(R.id.textView1)).setText(getResources().getString(R.string.enterRequest));
		final EditText input = dialogView.findViewById(R.id.name);
		input.setText("");
		final AlertDialog.Builder d = new AlertDialog.Builder(this)
		.setMessage(getResources().getString(R.string.menu_filter))
		.setPositiveButton(getResources().getString(R.string.ok), new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog1, int which) {
				final String request = input.getText().toString();
				dialog1.dismiss();
				filter(request);
			}
		});
		d.setView(dialogView);
		d.show();
	}

	void view(String uri) {
		String uri1 = uri; 
		try {
			uri1 = URLEncoder.encode(uri1, "UTF-8");
		} catch (UnsupportedEncodingException e1) {
			e1.printStackTrace();
		}
		Intent i = new Intent(this, ImageViewActivity.class);
		i.putExtra(ImageViewActivity.PATH , uri1);
		startActivity(i);
	}	

	void initStar() {
		BookmarksDB db = ((RootApplication)getApplication()).BookmarksDB();
		final BookmarksDB.Bookmark b = new BookmarksDB.Bookmark(myLocation.Path, -1);
		ImageView star = findViewById(R.id.star);
		if (myLocation.Path.equals("/") || myLocation.Path.equals("smb://") || !myLocation.Request.equals("")) {
			star.setImageResource(android.R.drawable.btn_star_big_off);
			star.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
				}
			});
			return;
		}
		if (db.exists(b)) {
			star.setImageResource(android.R.drawable.btn_star_big_on);
			star.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					((RootApplication)getApplication()).BookmarksDB().deleteBookmark(b.Path);
					initStar();
					Toast.makeText(BrowseActivity.this, b.getName() + getResources().getString(R.string.bookmarkRemoved), Toast.LENGTH_SHORT).show();
				}
			});
		} else {
			star.setImageResource(android.R.drawable.btn_star_big_off);
			star.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					((RootApplication)getApplication()).BookmarksDB().saveBookmark(b);
					initStar();
					Toast.makeText(BrowseActivity.this, b.getName() + getResources().getString(R.string.bookmarkAdded), Toast.LENGTH_SHORT).show();
				}
			});
		}
	}

}
