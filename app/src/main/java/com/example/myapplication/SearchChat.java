package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class SearchChat extends AppCompatActivity {

    EditText etSearch;
    RecyclerView recyclerView;
    FirebaseFirestore db;
    FacultyAdapter adapter;

    List<Map<String, Object>> allFacultyList      = new ArrayList<>();
    List<Map<String, Object>> filteredFacultyList = new ArrayList<>();

    String currentUid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search_chat);

        if (getSupportActionBar() != null)
            getSupportActionBar().hide();

        db           = FirebaseFirestore.getInstance();
        etSearch     = findViewById(R.id.etSearch);
        recyclerView = findViewById(R.id.recyclerView);
        currentUid   = FirebaseAuth.getInstance().getCurrentUser().getUid();

        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        adapter = new FacultyAdapter(this, filteredFacultyList, (otherUid, fullName, department) -> {

            if (otherUid == null || otherUid.isEmpty()) {
                Toast.makeText(this, "User ID not found!", Toast.LENGTH_SHORT).show();
                return;
            }

            // ── chatId — alphabetically sorted ───────────────
            // Chat_Screen mein bhi same logic hai
            // Isse dono phones pe same chatId banega
            String[] uids = {currentUid, otherUid};
            Arrays.sort(uids);
            String chatId = uids[0] + "_" + uids[1];

            Intent intent = new Intent(SearchChat.this, Chat_Screen.class);
            intent.putExtra("facultyUid",        otherUid);
            intent.putExtra("facultyName",       fullName);
            intent.putExtra("facultyDepartment", department);
            intent.putExtra("chatId",            chatId);
            startActivity(intent);
        });

        recyclerView.setAdapter(adapter);
        fetchAllUsers();

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            @Override public void onTextChanged(CharSequence s, int st, int b, int c) {}
            @Override
            public void afterTextChanged(Editable s) {
                filterAndShow(s.toString().trim().toLowerCase());
            }
        });
    }

    // ── Faculty + Students dono fetch karo ───────────────────
    private void fetchAllUsers() {
        allFacultyList.clear();

        db.collection("faculty_user")
                .get()
                .addOnSuccessListener(facultySnapshot -> {
                    for (QueryDocumentSnapshot doc : facultySnapshot) {
                        Map<String, Object> data = doc.getData();
                        if (!data.containsKey("uid") || data.get("uid") == null)
                            data.put("uid", doc.getId());
                        data.put("userType", "Faculty");
                        allFacultyList.add(data);
                    }

                    db.collection("users")
                            .get()
                            .addOnSuccessListener(studentSnapshot -> {
                                for (QueryDocumentSnapshot doc : studentSnapshot) {
                                    Map<String, Object> data = doc.getData();
                                    if (!data.containsKey("uid") || data.get("uid") == null)
                                        data.put("uid", doc.getId());
                                    data.put("userType", "Student");
                                    allFacultyList.add(data);
                                }
                                filteredFacultyList.clear();
                                adapter.updateList(filteredFacultyList);
                            })
                            .addOnFailureListener(e ->
                                    Toast.makeText(this,
                                            "Fetch failed: " + e.getMessage(),
                                            Toast.LENGTH_SHORT).show());
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this,
                                "Fetch failed: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show());
    }

    // ── Local filter ──────────────────────────────────────────
    private void filterAndShow(String query) {
        filteredFacultyList.clear();

        if (query.isEmpty()) {
            adapter.updateList(filteredFacultyList);
            return;
        }

        for (Map<String, Object> user : allFacultyList) {
            String firstName = getStr(user, "firstName").toLowerCase();
            String surname   = getStr(user, "surname").toLowerCase();
            String fullName  = getStr(user, "fullName").toLowerCase();
            String uid       = getStr(user, "uid").toLowerCase();

            if (firstName.contains(query) ||
                    surname.contains(query)   ||
                    fullName.contains(query)  ||
                    uid.contains(query)) {
                filteredFacultyList.add(user);
            }
        }

        adapter.updateList(filteredFacultyList);
    }

    private String getStr(Map<String, Object> map, String key) {
        Object val = map.get(key);
        return val != null ? val.toString() : "";
    }
}