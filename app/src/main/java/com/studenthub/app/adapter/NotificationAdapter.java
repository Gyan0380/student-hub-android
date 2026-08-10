package com.studenthub.app.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.studenthub.app.R;
import com.studenthub.app.model.AppNotification;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.List;

public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.Holder> {
    private final List<AppNotification> items = new ArrayList<>();
    public void setItems(List<AppNotification> value){items.clear(); items.addAll(value); notifyDataSetChanged();}
    @NonNull @Override public Holder onCreateViewHolder(@NonNull ViewGroup p,int t){return new Holder(LayoutInflater.from(p.getContext()).inflate(R.layout.item_notification,p,false));}
    @Override public void onBindViewHolder(@NonNull Holder h,int pos){
        AppNotification n=items.get(pos);
        h.title.setText(n.getTitle()!=null?n.getTitle():"Notification");
        h.body.setText(n.getBody()!=null?n.getBody():(n.getText()!=null?n.getText():""));
        h.time.setText(n.getCreatedAt()!=null?DateFormat.getDateTimeInstance(DateFormat.SHORT,DateFormat.SHORT).format(n.getCreatedAt()):"");
    }
    @Override public int getItemCount(){return items.size();}
    static class Holder extends RecyclerView.ViewHolder{
        TextView title,body,time; Holder(View v){super(v);title=v.findViewById(R.id.notificationTitle);body=v.findViewById(R.id.notificationBody);time=v.findViewById(R.id.notificationTime);}
    }
}
