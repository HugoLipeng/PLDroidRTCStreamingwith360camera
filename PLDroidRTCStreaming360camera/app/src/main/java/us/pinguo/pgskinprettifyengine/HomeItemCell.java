package us.pinguo.pgskinprettifyengine;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.qiniu.pili.droid.rtcstreaming.demo.R;


/**
 * Created by PC-Xu on 2016/6/24.
 */
public class HomeItemCell extends IRecycleCell<ItemData> implements View.OnClickListener{
    private Activity mContext;
    private MyItemClickListener myItemClickListener;
    public HomeItemCell(Activity activity, ItemData data, MyItemClickListener listener)
    {
        SetData(data);
        mContext =activity;
        myItemClickListener= listener;
    }

    @Override
    public int getViewType()
    {
        return RecycleType.TYPE_NORMAL;
    }

    @Override
    public MyViewHolder onCreateCellView(ViewGroup viewGroup)
    {
           View view= LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.listview_item,null);

          return   new MyViewHolder(view);
    }

    @Override
    public void onBindViewHolder(MyViewHolder viewHolder)
    {
        View itemView = viewHolder.itemView;
        InitView(itemView);
    }


    private  void  InitView(View view)
    {
        TextView textView = (TextView)view.findViewById(R.id.tv_name);
        if (textView!=null)
        {
            ItemData itemData = getData();
            textView.setText(itemData.filterName);
            if (myItemClickListener !=null&&itemData.filterType.equals(myItemClickListener.getCurFilterType()))
            {
              textView.setBackgroundColor(mContext.getResources().getColor(R.color.color_selectedlitem));
            }
            else
            {
                textView.setBackgroundColor(mContext.getResources().getColor(R.color.color_normalitem));
            }

        }

        textView.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                if (myItemClickListener ==null)
                    return;
                myItemClickListener.onItemClick(getData().filterType);
            }
        });

    }


    @Override
    public void onClick(View v) {
        switch (v.getId()){

        }
    }

}
