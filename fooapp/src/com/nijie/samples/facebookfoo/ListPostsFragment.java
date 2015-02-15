package com.nijie.samples.facebookfoo;

/**
 * Created by Ni Jie on 2/13/2015.
 */
import android.os.Bundle;
        import android.support.v4.app.Fragment;
        import android.view.LayoutInflater;
        import android.view.View;
        import android.view.ViewGroup;

public class ListPostsFragment extends Fragment {
    @Override
    public View onCreateView(LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        // Inflate the layout for this fragment
        View view =  inflater.inflate(R.layout.listpost_fragment,
                container, false);
        return view;
    }
}