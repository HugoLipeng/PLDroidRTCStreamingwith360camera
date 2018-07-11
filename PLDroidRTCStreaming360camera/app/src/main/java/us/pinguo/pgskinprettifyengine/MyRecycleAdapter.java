package us.pinguo.pgskinprettifyengine;

import android.app.Activity;
import android.support.v7.widget.RecyclerView;
import android.view.ViewGroup;

import java.util.List;

/**
 * Created by PC-Xu on 2016/6/24.
 */
public class MyRecycleAdapter extends  RecyclerView.Adapter
{
    private static final String TAG = MyRecycleAdapter.class.getSimpleName();
    private List<IRecycleCell> mListData;
    private Activity mActivity;

    public MyRecycleAdapter(Activity activity, List<IRecycleCell> mListData)
    {
        this.mListData = mListData;
        mActivity = activity;
    }

    @Override
    public int getItemViewType(int position)
    {
        return mListData.get(position).getViewType();
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position)
    {
         mListData.get(position).onBindViewHolder((MyViewHolder)holder);
    }

    @Override
    public int getItemCount()
    {
        return mListData.size();
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType)
    {
       int pos= getFirstPositionByType(viewType);
        if (pos ==-1 || pos >=getItemCount())
            return  null;
        IRecycleCell cell = mListData.get(pos);
        return cell == null ? null : cell.onCreateCellView(parent);
    }

    public int getFirstPositionByType(int viewType) {
        int size = getItemCount();
        for (int i = 0; i < size; i++) {
            if (getItemViewType(i) == viewType) {
                return i;
            }
        }
        return -1;
    }
}
