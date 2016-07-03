package fr.virtualabs.btlejuice;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.TextView;

import java.util.List;

/**
 * Created by virtualabs on 29/01/16.
 */
public class DeviceAdapter extends ArrayAdapter<DeviceInfo> {

    public class Holder
    {
        TextView details;
        TextView name;
        ImageView img;
    }

    public DeviceAdapter(Context context, int textViewResourceId, List<DeviceInfo> collection) {
        super(context, textViewResourceId, collection);
    }



    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        Holder h;
        DeviceInfo p = getItem(position);
        if (p != null) {
            if (convertView == null) {
                LayoutInflater inflater = (LayoutInflater)getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                convertView = inflater.inflate(R.layout.device_info, null);
                h = new Holder();
                h.name = (TextView) convertView.findViewById(R.id.deviceName);
                h.details = (TextView) convertView.findViewById(R.id.deviceDetails);
                h.img  = (ImageView) convertView.findViewById(R.id.iconType);
                convertView.setTag(h);
            } else {
                h = (Holder)convertView.getTag();
            }

            h.name.setText(p.getDevice());
            h.details.setText(p.getAddress()+String.format(" | RSSI: %d", p.getRSSI()));
            switch(p.getType()) {
                case FITBIT:
                    h.img.setImageDrawable(getContext().getDrawable(R.drawable.fitbit));
                    break;
                case WRISTBAND:
                    h.img.setImageDrawable(getContext().getDrawable(R.drawable.wrist));
                    break;
                case KEYFOB:
                    h.img.setImageDrawable(getContext().getDrawable(R.drawable.tracker));
                    break;
                case SMARTWATCH:
                    h.img.setImageDrawable(getContext().getDrawable(R.drawable.watch));
                    break;
                case SOUND:
                    h.img.setImageDrawable(getContext().getDrawable(R.drawable.audio));
                    break;
                case PHONE:
                    h.img.setImageDrawable(getContext().getDrawable(R.drawable.phone));
                    break;
                default:
                    h.img.setImageDrawable(getContext().getDrawable(R.drawable.unknown));
                    break;
            }
        } else {
            Log.e("DeviceAdapter", "item is null");
        }

        return convertView;
    }
}
