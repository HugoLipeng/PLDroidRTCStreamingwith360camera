package us.pinguo.pgskinprettifyengine;

import android.view.ViewGroup;

/**
 * Created by PC-Xu on 2016/6/24.
 */
public abstract class IRecycleCell<T>
{
    private  T mData;
    public  abstract int getViewType();
    public  abstract MyViewHolder onCreateCellView(ViewGroup viewGroup);
    public  abstract  void  onBindViewHolder(MyViewHolder viewHolder);
    public    T getData()
    {
        return mData;
    }
    public    void  SetData(T data)
    {
        mData =data;
    }

}
