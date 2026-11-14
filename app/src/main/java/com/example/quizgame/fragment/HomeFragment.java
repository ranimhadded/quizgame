package com.example.quizgame.fragment;


import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import androidx.fragment.app.Fragment;

import com.example.quizgame.EnterCodeActivity;
import com.example.quizgame.HostRoomActivity;
import com.example.quizgame.JoinRoomActivity;
import com.example.quizgame.R;

public class HomeFragment extends Fragment {

    private Button solo, join, host;

    public HomeFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        solo = view.findViewById(R.id.solo);
        join = view.findViewById(R.id.join);
        host = view.findViewById(R.id.host);

        setupClickListeners();

        return view;
    }

    private void setupClickListeners() {
        host.setOnClickListener(v -> {
            Intent intent=new Intent(getActivity(), HostRoomActivity.class);
            intent.putExtra("mode", "host");
            startActivity(intent);

        });

        solo.setOnClickListener(v -> {
            Intent intent=new Intent(getActivity(), HostRoomActivity.class);
            intent.putExtra("mode", "solo");
            startActivity(intent);

        });

        join.setOnClickListener(v -> {
            Intent intent=new Intent(getActivity(), EnterCodeActivity.class);
            startActivity(intent);
        });
    }
}