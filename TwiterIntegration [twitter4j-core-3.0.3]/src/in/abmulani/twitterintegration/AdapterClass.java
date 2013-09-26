package in.abmulani.twitterintegration;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

public class AdapterClass extends BaseAdapter {
	Context mContext; // ADD THIS to keep a context
	private LayoutInflater inflater;

	public AdapterClass(Context context) {
		this.mContext = context;
		this.inflater = (LayoutInflater) context
				.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	}

	@Override
	public int getCount() {
		return BaseActivity.SearchedStatus.size();
	}

	@Override
	public Object getItem(int position) {
		return position;
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	public static class ViewHolder {
		TextView name;
		TextView text;
	}

	@Override
	public View getView(final int position, View convertView, ViewGroup parent) {
		ViewHolder holder = null;
		try {
			if (convertView == null) {
				holder = new ViewHolder();
				convertView = inflater.inflate(R.layout.list_inflater, null);
				holder.name = (TextView) convertView.findViewById(R.id.textView1);
				holder.text = (TextView) convertView.findViewById(R.id.textView2);
				convertView.setTag(holder);
			} else
				holder = (ViewHolder) convertView.getTag();
			holder.name.setText(BaseActivity.SearchedStatus.get(position).getUser().getScreenName());
			holder.text.setText(BaseActivity.SearchedStatus.get(position).getText());
				
		} catch (Exception e) {
			Log.e("StakeHolders/getView()", e.toString());
			e.printStackTrace();
		}
		return convertView;
	}

}
