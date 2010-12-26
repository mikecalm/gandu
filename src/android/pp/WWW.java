package android.pp;

import android.app.Activity;
import android.os.Bundle;
import android.webkit.WebView;

public class WWW extends Activity{
	
	WebView mWebView;
	String url;
	protected void onCreate(Bundle savedInstanceState) {
		  super.onCreate(savedInstanceState);
		  setContentView(R.layout.webview);
		  url = getIntent().getStringExtra("adres");
		  mWebView = (WebView) findViewById(R.id.www);
		  mWebView.getSettings().setJavaScriptEnabled(true);
		  mWebView.loadUrl(url);
	}

	
}
