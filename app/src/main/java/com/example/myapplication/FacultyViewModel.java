package com.example.myapplication;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.Query;
import java.util.List;

public class FacultyViewModel extends ViewModel {

    private final MutableLiveData<DocumentSnapshot> facultyData = new MutableLiveData<>();
    private final MutableLiveData<List<DocumentSnapshot>> ticketsData = new MutableLiveData<>();
    private final MutableLiveData<List<DocumentSnapshot>> allFacultiesData = new MutableLiveData<>();
    private boolean facultyFetched = false;

    public LiveData<DocumentSnapshot> getFacultyData() {
        return facultyData;
    }

    public LiveData<List<DocumentSnapshot>> getTicketsData() {
        return ticketsData;
    }

    public LiveData<List<DocumentSnapshot>> getAllFacultiesData() {
        return allFacultiesData;
    }

    public void fetchIfNeeded() {
        fetchFacultyData();
        fetchTickets();
        fetchAllFaculties();
    }

    private void fetchFacultyData() {
        if (facultyFetched) return;
        facultyFetched = true;

        String email = FirebaseAuth.getInstance().getCurrentUser().getEmail();
        if (email == null) return;

        FirebaseFirestore.getInstance()
                .collection("faculty_user")
                .whereEqualTo("email", email)
                .get()
                .addOnSuccessListener(query -> {
                    if (!query.isEmpty()) {
                        facultyData.setValue(query.getDocuments().get(0));
                    }
                });
    }

    public void fetchTickets() {
        FirebaseFirestore.getInstance()
                .collection("tickets")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .addSnapshotListener((value, error) -> {
                    if (error != null) return;
                    if (value != null) {
                        ticketsData.setValue(value.getDocuments());
                    }
                });
    }

    private void fetchAllFaculties() {
        FirebaseFirestore.getInstance()
                .collection("faculty_user")
                .addSnapshotListener((value, error) -> {
                    if (error != null) return;
                    if (value != null) {
                        allFacultiesData.setValue(value.getDocuments());
                    }
                });
    }
}