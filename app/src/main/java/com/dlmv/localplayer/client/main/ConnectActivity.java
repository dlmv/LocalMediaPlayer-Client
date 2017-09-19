package com.dlmv.localplayer.client.main;

import java.io.InputStream;
import java.util.Locale;

import com.dlmv.localmediaplayer.client.R;
import com.dlmv.localplayer.client.db.ServersDB.ServerBookmark;
import com.dlmv.localplayer.client.db.ServerActivity;
import com.dlmv.localplayer.client.network.*;
import com.dlmv.localplayer.client.util.ApplicationUtil;
import com.dlmv.localplayer.client.util.RootApplication;
import com.dlmv.localplayer.client.util.ServerPath;

import android.content.Context;
import android.os.Bundle;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.view.*;
import android.view.ViewGroup.LayoutParams;
import android.widget.*;

public class ConnectActivity extends Activity {

	private EditText myServer;
	private EditText myPort;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_connect);
		LayoutParams params = getWindow().getAttributes();
		params.width = LayoutParams.MATCH_PARENT;
		getWindow().setAttributes((android.view.WindowManager.LayoutParams) params);
		myServer = findViewById(R.id.server);
		myServer.setText("http://");
		myPort = findViewById(R.id.port);
		if (ApplicationUtil.Data.serverUri != null) {
			int index = ApplicationUtil.Data.serverUri.lastIndexOf(":");
			String ip = ApplicationUtil.Data.serverUri.substring(0, index);
			String port = ApplicationUtil.Data.serverUri.substring(index + 1, ApplicationUtil.Data.serverUri.length() - 1);
			myServer.setText(ip);
			myPort.setText(port);
			Button disconnect = findViewById(R.id.disconnect);
			disconnect.setVisibility(View.VISIBLE);
			disconnect.setText(getResources().getString(R.string.menu_disconnect));
			disconnect.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					Intent data = new Intent();
					data.putExtra("URI", (String)null);
					setResult(RESULT_OK, data);
					finish();
				}
			});
		}
		Button connect = findViewById(R.id.connect);
		connect.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				String server = myServer.getText().toString();
				if (!server.startsWith("http")) {
					server = "http://" + server;
				}
				if (!"".equals(myPort.getText().toString())) {
					checkUri(server, Integer.parseInt(myPort.getText().toString()));
				}
			}
		});
		((TextView)findViewById(R.id.serverText)).setText(getResources().getString(R.string.address));
		((TextView)findViewById(R.id.portText)).setText(getResources().getString(R.string.port));
		connect.setText(getResources().getString(R.string.menu_connect));
		Button bookmarks = findViewById(R.id.bookmarks);
		bookmarks.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent i = new Intent(ConnectActivity.this, ServerActivity.class);
				startActivityForResult(i, 0);
			}
		});
		bookmarks.setText(getResources().getString(R.string.servers));
	}

	synchronized void checkUri(final String server, final int port) {
		final String uri = myServer.getText().toString() + ":" + myPort.getText() + "/";
		tryLogin(ConnectActivity.this, uri, new ServerLoginRunnable() {
			@Override
			public void run(String password) {
				onDialogResult(true, server, port);
			}
		}, new Runnable() {
			@Override
			public void run() {
				onDialogResult(false,  server, port);
			}
		}, false);

	}

	protected void onDialogResult(boolean success, final String server, final int port) {
		if (success) {
			Intent data = new Intent();
			data.putExtra("URI", server + ":" + port + "/");
			setResult(RESULT_OK, data);
			runOnUiThread(new Runnable() {
				@Override
				public void run() {
					ServerBookmark b = new ServerBookmark();
					b.server = server;
					b.port = port;
					if (((RootApplication)getApplication()).ServersDB().exists(b)) {
						finish();
						return;
					}
					View dialogView = View.inflate(ConnectActivity.this, R.layout.enter_value, null);
					final EditText input = dialogView.findViewById(R.id.name);
					final AlertDialog.Builder d = new AlertDialog.Builder(ConnectActivity.this)
					.setMessage(getResources().getString(R.string.saveServer))
					.setPositiveButton(getResources().getString(R.string.yes), new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog1, int which) {
							String name = input.getText().toString();
							ServerBookmark b = new ServerBookmark();
							b.server = server;
							b.port = port;
							b.name = name;
							if (!name.equals("")) {
								((RootApplication)getApplication()).ServersDB().saveServer(b);
							}
							dialog1.dismiss();
							finish();
						}
					})
					.setNegativeButton(getResources().getString(R.string.no), new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog1, int which) {
							dialog1.dismiss();
							finish();
						}
					});
					d.setView(dialogView);
					d.setOnCancelListener(new DialogInterface.OnCancelListener() {
						@Override
						public void onCancel(DialogInterface dialog) {
							finish();
							
						}
					});
					d.show();
				}
			});
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == 0 && resultCode == RESULT_OK) {
			myServer.setText(data.getStringExtra("SERVER"));
			myPort.setText(String.format(Locale.getDefault(), "%d", data.getIntExtra("PORT", 8123)));
		}
	}

	public interface ServerLoginRunnable {
        void run(String password);
    }

    public static void tryLogin(final Activity a, final String uri, final ServerLoginRunnable okRunnable, final Runnable cancelRunnable, final boolean quiet) {
		a.runOnUiThread(new Runnable() {
			@Override
			public void run() {
				final ProgressDialog dialog = quiet ? null : ProgressDialog.show(a, a.getResources().getString(R.string.checking), "");

				final NetworkRequest request = new NetworkRequest(uri + ServerPath.LOGIN) {
					@Override
					public void handleStream(InputStream inputStream) throws NetworkException {
					}
				};
				new Thread() {
					@Override
					public void run() {
						try {
							NetworkManager.Instance().perform(request);
							a.runOnUiThread(new Runnable() {
								@Override
								public void run() {
									if (!quiet) {
										dialog.dismiss();
									}
									okRunnable.run("");
								}
							});
						} catch (final NetworkException e)  {
							a.runOnUiThread(new Runnable() {
								@Override
								public void run() {
									if (!quiet) {
										dialog.dismiss();
									}

									if (e.isUnauthorized()) {
										login(a, uri, okRunnable, cancelRunnable);
									} else {
										Toast.makeText(a, a.getResources().getString(R.string.networkError) + "\n" + e.getLocalizedMessage(a), Toast.LENGTH_LONG).show();
									}
								}
							});
						}
					}

				}.start();
			}
		});
	}

	public static void login(final Activity a, final String uri, final ServerLoginRunnable okRunnable, final Runnable cancelRunnable) {
        a.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                View dialogView = View.inflate(a, R.layout.enter_value, null);
                ((TextView)dialogView.findViewById(R.id.textView1)).setText(a.getResources().getString(R.string.enterPassword));
                final EditText input = dialogView.findViewById(R.id.name);
                final AlertDialog.Builder d = new AlertDialog.Builder(a)
                        .setMessage(a.getResources().getString(R.string.passwordRequired))
                        .setPositiveButton(a.getResources().getString(R.string.ok), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(final DialogInterface dialog1, int which) {
                                final String pass = input.getText().toString();
								final ProgressDialog dialog = ProgressDialog.show(a, a.getResources().getString(R.string.checking), "");
                                new Thread() {
                                    @Override
                                    public void run() {
                                        final NetworkRequest request = new NetworkRequest(uri + ServerPath.LOGIN) {
                                            @Override
                                            public void handleStream(InputStream inputStream) throws NetworkException {
                                            }
                                        };
                                        request.addPostParameter(ServerPath.PASSWORD, pass);
										try {
                                            NetworkManager.Instance().perform(request);
											a.runOnUiThread(new Runnable() {
												@Override
												public void run() {
													dialog.dismiss();
													okRunnable.run(pass);
												}
											});
                                        } catch (final NetworkException e) {
                                            a.runOnUiThread(new Runnable() {
                                                @Override
                                                public void run() {
													dialog.dismiss();
													if (e.isUnauthorized()) {
                                                        login(a, uri, okRunnable, cancelRunnable);
                                                    } else {
                                                        Toast.makeText(a, a.getResources().getString(R.string.networkError) + "\n" + e.getLocalizedMessage(a), Toast.LENGTH_LONG).show();
                                                    }
                                                }
                                                });
                                            }

                                        }
                                }.start();
                            }
                        }).setNegativeButton(a.getResources().getString(R.string.cancel), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(final DialogInterface dialog1, int which) {
                                cancelRunnable.run();
                            }
                        });

                d.setView(dialogView);
                d.show();
            }

        });
    }

}
