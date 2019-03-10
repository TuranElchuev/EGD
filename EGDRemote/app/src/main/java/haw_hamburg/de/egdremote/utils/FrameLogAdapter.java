package haw_hamburg.de.egdremote.utils;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;

import haw_hamburg.de.egdremote.R;

public class FrameLogAdapter extends ArrayAdapter {

    private ArrayList<IRxFrame> frames = new ArrayList<>();
    private SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss.SSS");
    private ListView list;
    private boolean autoScroll = true;

    public FrameLogAdapter(ListView list, Context context, ArrayList data) {
        super(context, 0, data);
        this.list = list;
        this.list.setAdapter(this);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.frame_log_item, parent, false);
        }

        ((TextView)convertView.findViewById(R.id.text)).
                setText(sdf.format(new Timestamp(frames.get(position).getTimestamp()))
                        + ": " + frames.get(position).toString());

        return convertView;
    }

    @Override
    public int getCount() {
        return frames.size();
    }

    public void addFrame(IRxFrame frame){
        frames.add(frame);
        notifyDataSetChanged();
        if(autoScroll){
            list.setSelection(getCount() - 1);
        }
    }

    public void setAutoscroll(boolean autoScroll) {
        this.autoScroll = autoScroll;
    }

    public boolean isAutoscroll() {
        return autoScroll;
    }

    public void clear(){
        frames.clear();
        notifyDataSetChanged();
    }

    public String dataToString(){
        StringBuilder sb = new StringBuilder();
        for(IRxFrame frame : frames){
            sb.append(sdf.format(new Timestamp(frame.getTimestamp())))
                    .append(":\t\t")
                    .append(frame.toString().trim())
                    .append("\n");
        }
        return sb.toString();
    }
}