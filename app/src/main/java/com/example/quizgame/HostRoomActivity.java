package com.example.quizgame;

import android.os.Bundle;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.Request;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.example.quizgame.adapter.CategoryAdapter;
import com.example.quizgame.model.TriviaCategory;
import com.google.android.material.appbar.MaterialToolbar;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class HostRoomActivity extends AppCompatActivity {

    RecyclerView rvRow1, rvRow2,rvRow3,rvRow4,rvRow5;
    CategoryAdapter adapterRow1, adapterRow2,adapterRow3,adapterRow4,adapterRow5;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        setContentView(R.layout.activity_host_room);
        MaterialToolbar toolbar = findViewById(R.id.topAppBar);
        setSupportActionBar(toolbar); // only ONCE

        // remove default app title
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }
        String mode = getIntent().getStringExtra("mode");
        toolbar.setTitle("Host This Pack");

        toolbar.setNavigationOnClickListener(v -> finish());
        // ================== RECYCLER VIEWS ==================
        rvRow1 = findViewById(R.id.rvRow1);
        rvRow2 = findViewById(R.id.rvRow2);
        rvRow3 = findViewById(R.id.rvRow3);
        rvRow4 = findViewById(R.id.rvRow4);
        rvRow5 = findViewById(R.id.rvRow5);

        rvRow1.setLayoutManager(new LinearLayoutManager(this, RecyclerView.HORIZONTAL, false));
        rvRow2.setLayoutManager(new LinearLayoutManager(this, RecyclerView.HORIZONTAL, false));
        rvRow3.setLayoutManager(new LinearLayoutManager(this, RecyclerView.HORIZONTAL, false));
        rvRow4.setLayoutManager(new LinearLayoutManager(this, RecyclerView.HORIZONTAL, false));
        rvRow5.setLayoutManager(new LinearLayoutManager(this, RecyclerView.HORIZONTAL, false));

        adapterRow1 = new CategoryAdapter(this, mode);
        adapterRow2 = new CategoryAdapter(this, mode);
        adapterRow3 = new CategoryAdapter(this, mode);
        adapterRow4 = new CategoryAdapter(this, mode);
        adapterRow5 = new CategoryAdapter(this, mode);

        rvRow1.setAdapter(adapterRow1);
        rvRow2.setAdapter(adapterRow2);
        rvRow3.setAdapter(adapterRow3);
        rvRow4.setAdapter(adapterRow4);
        rvRow5.setAdapter(adapterRow5);

        // ================== API CALL ==================
        String url = "https://opentdb.com/api_category.php";

        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.GET, url, null,
                response -> {
                    try {
                        JSONArray categoriesJson = response.getJSONArray("trivia_categories");

                        ArrayList<TriviaCategory> list = new ArrayList<>();

                        for (int i = 0; i < categoriesJson.length(); i++) {
                            JSONObject obj = categoriesJson.getJSONObject(i);

                            TriviaCategory cat = new TriviaCategory();
                            cat.id = obj.getInt("id");
                            cat.name = obj.getString("name");
                            cat.imageResId = getImageForCategory(cat.name);

                            list.add(cat);
                        }

                        Log.d("API", "LOADED CATEGORIES = " + list.size());

                        adapterRow1.setItems(safeSubList(list, 0, 5));
                        adapterRow2.setItems(safeSubList(list, 5, 10));
                        adapterRow3.setItems(safeSubList(list, 10, 15));
                        adapterRow4.setItems(safeSubList(list, 15, 20));
                        adapterRow5.setItems(list.subList(20, list.size()));


                    } catch (Exception e) {
                        Log.e("API", "JSON error: " + e.getMessage());
                    }
                },
                error -> Log.e("API", "Volley error: " + error.getMessage())
        );

        Volley.newRequestQueue(this).add(request);
    }
    private List<TriviaCategory> safeSubList(ArrayList<TriviaCategory> list, int start, int end) {
        if (start >= list.size()) return new ArrayList<>();
        end = Math.min(end, list.size());
        return list.subList(start, end);
    }
    private int getImageForCategory(String name) {
        switch (name) {
            case "General Knowledge":
                return R.drawable.knowledge;

            case "Entertainment: Books":
                return R.drawable.books;

            case "Entertainment: Film":
                return R.drawable.movie;

            case "Entertainment: Music":
                return R.drawable.musicalnotes;

            case "Entertainment: Musicals & Theatres":
                return R.drawable.movietheater;

            case "Entertainment: Television":
                return R.drawable.smarttv;

            case "Entertainment: Video Games":
                return R.drawable.gameconsole;

            case "Entertainment: Board Games":
                return R.drawable.chessboard;

            case "Science & Nature":
                return R.drawable.science;

            case "Science: Computers":
                return R.drawable.ux;

            case "Science: Mathematics":
                return R.drawable.mathematics;

            case "Mythology":
                return R.drawable.magicbook;

            case "Sports":
                return R.drawable.physical;

            case "Geography":
                return R.drawable.zone;

            case "History":
                return R.drawable.history;

            case "Politics":
                return R.drawable.politics;

            case "Art":
                return R.drawable.monalisa;

            case "Celebrities":
                return R.drawable.celebrity;

            case "Animals":
                return R.drawable.habitat;

            case "Vehicles":
                return R.drawable.vehicles;

            case "Entertainment: Comics":
                return R.drawable.comicbook;

            case "Science: Gadgets":
                return R.drawable.gadget;

            case "Entertainment: Japanese Anime & Manga":
                return R.drawable.avatar;

            case "Entertainment: Cartoon & Animations":
                return R.drawable.comic;

            default:
                return R.drawable.knowledge;
        }
    }

}
