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
import android.os.Bundle;
import android.util.Log;
import android.view.*;
import android.widget.*;

class ShareDescription {
	final String Share;
	final String Login;

	ShareDescription(String share, String login) {
		Share = share;
		Login = login;
	}
}

public class ShareLoginsActivity extends DBListActivity<ShareDescription> {
	
	public static final String LOGINS = "logins";
	public static final String SHARES = "shares";


	private static class Response {
		private boolean myValid;
		private String myCause;
		private ArrayList<String> myShares;
		private ArrayList<String> myLogins;
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
		i.putStringArrayListExtra(LOGINS , res.myLogins);
		i.putStringArrayListExtra(SHARES , res.myShares);

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
					res.myLogins = new ArrayList<>();
					res.myShares = new ArrayList<>();
					for (int i = 0; i < list1.getLength(); ++i) {
						Element e = (Element)list1.item(i);
						String share = URLDecoder.decode(e.getAttribute("name"), "UTF-8");
						String login = URLDecoder.decode(e.getAttribute("login"), "UTF-8");
						res.myShares.add(share);
						res.myLogins.add(login);
					}
				}
			}
			return res;
		}
	}
	
	@Override
	protected View setAdapterView(ShareDescription b, View convertView, ViewGroup parent, LayoutInflater inflater) {
		if (convertView == null) {
			convertView = inflater.inflate(android.R.layout.simple_expandable_list_item_2, parent, false);
		}
		TextView title = convertView.findViewById(android.R.id.text1);
		TextView subtitle = convertView.findViewById(android.R.id.text2);
		title.setText(b.Login);
		subtitle.setText(b.Share);
		return convertView;
	}

	@Override
	protected List<ShareDescription> getList() {
		ArrayList<ShareDescription> list = new ArrayList<>();
		for (int i = 0; i < getIntent().getStringArrayListExtra(SHARES).size(); ++i) {
			String share = getIntent().getStringArrayListExtra(SHARES).get(i);
			String login = getIntent().getStringArrayListExtra(LOGINS).get(i);
			list.add(new ShareDescription(share, login));
		}
		return  list;
	}

	@Override
	protected void fillResultIntent(ShareDescription s, Intent data) {
	}

	@Override
	protected void delete(ShareDescription b) {
		final NetworkRequest request = new NetworkRequest(ApplicationUtil.Data.serverUri + "forgetlogin") {
			@Override
			public void handleStream(InputStream inputStream) throws NetworkException {
				//TODO: show something
			}
		};
		request.addPostParameter("share", b.Share);
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
