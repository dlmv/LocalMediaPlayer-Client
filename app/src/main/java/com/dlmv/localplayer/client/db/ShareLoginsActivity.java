package com.dlmv.localplayer.client.db;

import java.io.*;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import com.dlmv.localplayer.client.util.ApplicationUtil;
import com.dlmv.localplayer.client.network.*;

import android.app.Activity;
import android.content.Intent;
import android.util.Log;
import android.view.*;
import android.widget.*;

public class ShareLoginsActivity extends DBListActivity<String> {
	
	public static final String LOGINS = "logins";
	
	private static class Response {
		private boolean myValid;
		private String myCause;
		private ArrayList<String> myContent;
	}
	
	static public void callMe(final Response res, final Activity caller) {
		if (!res.myValid) {
			caller.runOnUiThread(new Runnable() {
				public void run() {
					Toast.makeText(caller, res.myCause, Toast.LENGTH_SHORT).show();
				}
			});
			return;
		}
		Intent i = new Intent(caller, ShareLoginsActivity.class);
		i.putStringArrayListExtra(LOGINS , res.myContent);
		caller.startActivity(i);
	}
	
	public static class Parser {
		public Response parse(InputStream s) throws ParserConfigurationException, SAXException, IOException {
			Response res = new Response();
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			DocumentBuilder db = dbf.newDocumentBuilder();
			Document doc = db.parse(new InputSource(s));
			Element root = doc.getDocumentElement();
			if (!"response".equals(root.getTagName())) {
				Log.e("testmpclient", "wrong tag!!!");
			}
			res.myValid = root.getAttribute("valid").equals("true");
			res.myCause = root.getAttribute("reason");
			if (res.myValid) {
				NodeList list = root.getElementsByTagName("loginlist");
				if (list.getLength() == 1) {
					Element dir = (Element)list.item(0);
					NodeList list1 = dir.getElementsByTagName("share");
					res.myContent = new ArrayList<>();
					for (int i = 0; i < list1.getLength(); ++i) {
						Element e = (Element)list1.item(i);
						String share = URLDecoder.decode(e.getAttribute("name"), "UTF-8");
						res.myContent.add(share);
					}
				}
			}
			return res;
		}
	}
	
	@Override
	protected View setAdapterView(String b, View convertView, ViewGroup parent, LayoutInflater inflater) {
		if (convertView == null) {
			convertView = inflater.inflate(android.R.layout.simple_expandable_list_item_2, parent, false);
		}
		TextView title = convertView.findViewById(android.R.id.text1);
		TextView subtitle = convertView.findViewById(android.R.id.text2);
		int divider = b.lastIndexOf("/");
		title.setText(b.substring(divider+1));
		subtitle.setText(b);
		return convertView;
	}

	@Override
	protected List<String> getList() {
		return getIntent().getStringArrayListExtra(LOGINS);
	}

	@Override
	protected void fillResultIntent(String s, Intent data) {
	}

	@Override
	protected void delete(String b) {
		final NetworkRequest request = new NetworkRequest(ApplicationUtil.Data.serverUri + "forgetlogin") {
			@Override
			public void handleStream(InputStream inputStream) throws NetworkException {
				//TODO: show something
			}
		};
		request.addPostParameter("share", b);
		new Thread() {
			@Override
			public void run() {
				try {
					NetworkManager.Instance().perform(request);
				} catch (NetworkException e)  {
					e.printStackTrace();
				}
			}
		}.start();
	}

	@Override
	protected boolean openable() {
		return false;
	}

}
