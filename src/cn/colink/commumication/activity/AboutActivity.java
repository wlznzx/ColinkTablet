package cn.colink.commumication.activity;

import android.os.Bundle;
import android.text.util.Linkify;
import android.widget.TextView;
import cn.colink.commumication.R;
import cn.colink.commumication.swipeback.SwipeBackActivity;


public class AboutActivity extends SwipeBackActivity {
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.about);
		TextView tv = (TextView) findViewById(R.id.app_information);
		Linkify.addLinks(tv, Linkify.ALL);
	}
}
