package com.example.mealsync.data.repository;

import com.example.mealsync.data.models.InvitationCode;
import com.example.mealsync.data.models.Member;
import com.example.mealsync.data.models.Mess;
import com.example.mealsync.data.models.UserProfile;
import com.example.mealsync.util.InvitationCodeGenerator;
import com.example.mealsync.util.InvitationCodeValidator;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldPath;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.WriteBatch;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public class MessRepository {

    public interface ActionCallback {
        void onSuccess();
        void onFailure(Exception error);
    }

    public interface CreateMessCallback {
        void onSuccess(Mess mess, String code);
        void onFailure(Exception error);
    }

    public interface JoinMessCallback {
        void onSuccess(String messId);
        void onFailure(Exception error);
    }

    public interface RegenerateCodeCallback {
        void onSuccess(String newCode);
        void onFailure(Exception error);
    }

    public interface UserProfileCallback {
        void onSuccess(UserProfile profile);
        void onFailure(Exception error);
    }

    public interface MessCallback {
        void onSuccess(Mess mess);
        void onFailure(Exception error);
    }

    public interface MessListener {
        void onMessUpdated(Mess mess);
        void onError(Exception error);
    }

    public interface MembersListener {
        void onMembersUpdated(List<Member> members);
        void onError(Exception error);
    }

    private static final long SEVEN_DAYS_MS = 7L * 24 * 60 * 60 * 1000;
    private static final int DELETE_BATCH_SIZE = 400;

    private final FirebaseFirestore db;
    private final FirebaseAuth auth;

    public MessRepository() {
        this.db = FirebaseFirestore.getInstance();
        this.auth = FirebaseAuth.getInstance();
    }

    public MessRepository(FirebaseFirestore db, FirebaseAuth auth) {
        this.db = db;
        this.auth = auth;
    }

    public void getUserProfile(String uid, UserProfileCallback callback) {
        db.collection("users").document(uid).get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null && task.getResult().exists()) {
                UserProfile profile = task.getResult().toObject(UserProfile.class);
                callback.onSuccess(profile);
            } else if (task.isSuccessful()) {
                callback.onSuccess(null);
            } else {
                callback.onFailure(task.getException());
            }
        });
    }

    public void createMess(String messName, String managerName, String timezone, String cutoffTime, CreateMessCallback callback) {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            callback.onFailure(new Exception("User not signed in"));
            return;
        }

        String uid = currentUser.getUid();
        String email = currentUser.getEmail() != null ? currentUser.getEmail() : "";
        DocumentReference messRef = db.collection("messes").document();
        String messId = messRef.getId();
        String code = InvitationCodeGenerator.generateCode();

        long now = System.currentTimeMillis();
        long expiresAt = now + SEVEN_DAYS_MS;
        int utcOffsetMinutes = Mess.calculateUtcOffsetMinutes(timezone);

        ZoneId zoneId;
        try {
            zoneId = ZoneId.of(timezone != null ? timezone.trim() : "Asia/Dhaka");
        } catch (Exception e) {
            zoneId = ZoneId.of("Asia/Dhaka");
        }

        ZonedDateTime zdt = ZonedDateTime.now(zoneId);
        String periodId = zdt.format(DateTimeFormatter.ofPattern("yyyy-MM"));
        String startDate = zdt.withDayOfMonth(1).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        String endDate = zdt.withDayOfMonth(zdt.toLocalDate().lengthOfMonth()).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        Mess mess = new Mess(messId, messName, uid, managerName, timezone, cutoffTime, code, utcOffsetMinutes, periodId, 100, "equal", now);
        Member member = new Member(uid, managerName, email, "manager", now);
        InvitationCode invitationCode = new InvitationCode(code, messId, uid, now, expiresAt, true);

        Map<String, Object> userUpdate = new HashMap<>();
        userUpdate.put("uid", uid);
        userUpdate.put("email", email);
        userUpdate.put("displayName", managerName);
        userUpdate.put("messId", messId);
        userUpdate.put("role", "manager");
        userUpdate.put("createdAt", now);

        Map<String, Object> periodData = new HashMap<>();
        periodData.put("periodId", periodId);
        periodData.put("status", "open");
        periodData.put("startDate", startDate);
        periodData.put("endDate", endDate);
        periodData.put("openingBalances", Collections.emptyMap());
        periodData.put("createdAt", FieldValue.serverTimestamp());

        WriteBatch batch = db.batch();
        batch.set(db.collection("users").document(uid), userUpdate);
        batch.set(messRef, mess);
        batch.set(messRef.collection("members").document(uid), member);
        batch.set(db.collection("invitationCodes").document(code), invitationCode);
        batch.set(messRef.collection("periods").document(periodId), periodData);

        batch.commit().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                callback.onSuccess(mess, code);
            } else {
                callback.onFailure(task.getException());
            }
        });
    }

    public void updateMessSettings(String messId, String name, String timezone, String cutoffTime,
                                   long mealRatePaisa, ActionCallback callback) {
        int utcOffsetMinutes = Mess.calculateUtcOffsetMinutes(timezone);

        Map<String, Object> updates = new HashMap<>();
        updates.put("name", name);
        updates.put("timezone", timezone);
        updates.put("cutoffTime", cutoffTime);
        updates.put("utcOffsetMinutes", utcOffsetMinutes);
        updates.put("mealRatePaisa", mealRatePaisa);

        db.collection("messes").document(messId)
                .update(updates)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        callback.onSuccess();
                    } else {
                        callback.onFailure(task.getException());
                    }
                });
    }

    public void deleteMess(String messId, ActionCallback callback) {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            callback.onFailure(new Exception("User not signed in"));
            return;
        }

        String managerUid = currentUser.getUid();
        DocumentReference messRef = db.collection("messes").document(messId);
        Task<Void> deletion = messRef.get().continueWithTask(task -> {
            if (!task.isSuccessful()) {
                return Tasks.forException(task.getException());
            }
            DocumentSnapshot snapshot = task.getResult();
            if (snapshot == null || !snapshot.exists()) {
                return Tasks.forException(new IllegalStateException("Mess no longer exists."));
            }
            if (!managerUid.equals(snapshot.getString("managerUid"))) {
                return Tasks.forException(new SecurityException("Only the mess manager can delete this mess."));
            }
            if (Boolean.TRUE.equals(snapshot.getBoolean("deleting"))) {
                return Tasks.forResult(null);
            }
            return messRef.update("deleting", true);
        });

        deletion = continueIfSuccessful(deletion, ignored ->
                deleteCollection(db.collection("invitationCodes").whereEqualTo("messId", messId)));
        deletion = continueIfSuccessful(deletion, ignored ->
                resetMemberProfiles(db.collection("users").whereEqualTo("messId", messId)
                        .orderBy(FieldPath.documentId()).limit(DELETE_BATCH_SIZE), null, managerUid));
        deletion = continueIfSuccessful(deletion, ignored -> deleteCollection(messRef.collection("adjustments")));
        deletion = continueIfSuccessful(deletion, ignored -> deleteCollection(messRef.collection("expenses")));
        deletion = continueIfSuccessful(deletion, ignored -> deleteCollection(messRef.collection("deposits")));
        deletion = continueIfSuccessful(deletion, ignored -> deleteCollection(messRef.collection("meals")));
        deletion = continueIfSuccessful(deletion, ignored -> deletePeriodData(messRef));
        deletion = continueIfSuccessful(deletion, ignored ->
                deleteCollection(messRef.collection("members").whereNotEqualTo(FieldPath.documentId(), managerUid)));
        deletion = continueIfSuccessful(deletion, ignored -> {
            WriteBatch batch = db.batch();
            Map<String, Object> userReset = new HashMap<>();
            userReset.put("messId", null);
            userReset.put("role", null);
            batch.update(db.collection("users").document(managerUid), userReset);
            batch.delete(messRef.collection("members").document(managerUid));
            batch.delete(messRef);
            return batch.commit();
        });

        deletion.addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                callback.onSuccess();
            } else {
                callback.onFailure(task.getException());
            }
        });
    }

    private Task<Void> deletePeriodData(DocumentReference messRef) {
        return messRef.collection("periods").get().continueWithTask(task -> {
            if (!task.isSuccessful()) {
                return Tasks.forException(task.getException());
            }
            Task<Void> deletion = Tasks.forResult(null);
            for (DocumentSnapshot period : task.getResult().getDocuments()) {
                DocumentReference periodRef = period.getReference();
                deletion = continueIfSuccessful(deletion, ignored ->
                        deleteCollection(periodRef.collection("results")));
            }
            return continueIfSuccessful(deletion, ignored -> deleteCollection(messRef.collection("periods")));
        });
    }

    private Task<Void> resetMemberProfiles(Query pageQuery, DocumentSnapshot lastDocument, String managerUid) {
        Query query = pageQuery;
        if (lastDocument != null) {
            query = query.startAfter(lastDocument);
        }
        return query.get().continueWithTask(task -> {
            if (!task.isSuccessful()) {
                return Tasks.forException(task.getException());
            }

            QuerySnapshot snapshot = task.getResult();
            WriteBatch batch = db.batch();
            int updates = 0;
            for (DocumentSnapshot member : snapshot.getDocuments()) {
                if (!managerUid.equals(member.getId())) {
                    Map<String, Object> userReset = new HashMap<>();
                    userReset.put("messId", null);
                    userReset.put("role", null);
                    batch.update(db.collection("users").document(member.getId()), userReset);
                    updates++;
                }
            }

            Task<Void> updateTask = updates == 0 ? Tasks.forResult(null) : batch.commit();
            return continueIfSuccessful(updateTask, ignored -> {
                if (snapshot.size() < DELETE_BATCH_SIZE) {
                    return Tasks.forResult(null);
                }
                return resetMemberProfiles(pageQuery, snapshot.getDocuments().get(snapshot.size() - 1), managerUid);
            });
        });
    }

    private Task<Void> deleteCollection(Query query) {
        return query.orderBy(FieldPath.documentId()).limit(DELETE_BATCH_SIZE).get()
                .continueWithTask(task -> {
                    if (!task.isSuccessful()) {
                        return Tasks.forException(task.getException());
                    }

                    List<DocumentSnapshot> documents = task.getResult().getDocuments();
                    if (documents.isEmpty()) {
                        return Tasks.forResult(null);
                    }

                    WriteBatch batch = db.batch();
                    for (DocumentSnapshot document : documents) {
                        batch.delete(document.getReference());
                    }
                    return continueIfSuccessful(batch.commit(), ignored -> deleteCollection(query));
                });
    }

    private <T> Task<Void> continueIfSuccessful(Task<T> task, Function<T, Task<Void>> next) {
        return task.continueWithTask(result -> {
            if (!result.isSuccessful()) {
                return Tasks.forException(result.getException());
            }
            return next.apply(result.getResult());
        });
    }

    public void ensureMessSetup(Mess mess, ActionCallback callback) {
        if (mess == null || mess.getMessId() == null) {
            return;
        }
        int offset = Mess.calculateUtcOffsetMinutes(mess.getTimezone());
        db.collection("messes").document(mess.getMessId())
                .update("utcOffsetMinutes", offset)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        if (callback != null) callback.onSuccess();
                    } else {
                        if (callback != null) callback.onFailure(task.getException());
                    }
                });
    }

    public void joinMess(String code, String displayName, JoinMessCallback callback) {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            callback.onFailure(new Exception("User not signed in"));
            return;
        }

        db.collection("invitationCodes").document(code).get().addOnCompleteListener(task -> {
            if (!task.isSuccessful() || task.getResult() == null || !task.getResult().exists()) {
                callback.onFailure(new Exception("Invalid invitation code"));
                return;
            }

            InvitationCode invitationCode = task.getResult().toObject(InvitationCode.class);
            if (!InvitationCodeValidator.isValidAndActive(invitationCode, System.currentTimeMillis())) {
                callback.onFailure(new Exception("Invalid or expired invitation code"));
                return;
            }

            String uid = currentUser.getUid();
            String email = currentUser.getEmail() != null ? currentUser.getEmail() : "";
            String messId = invitationCode.getMessId();
            long now = System.currentTimeMillis();

            Member member = new Member(uid, displayName, email, "member", now);

            Map<String, Object> userUpdate = new HashMap<>();
            userUpdate.put("uid", uid);
            userUpdate.put("email", email);
            userUpdate.put("displayName", displayName);
            userUpdate.put("messId", messId);
            userUpdate.put("role", "member");

            WriteBatch batch = db.batch();
            batch.set(db.collection("users").document(uid), userUpdate);
            batch.set(db.collection("messes").document(messId).collection("members").document(uid), member);

            batch.commit().addOnCompleteListener(batchTask -> {
                if (batchTask.isSuccessful()) {
                    callback.onSuccess(messId);
                } else {
                    callback.onFailure(batchTask.getException());
                }
            });
        });
    }

    public void revokeCode(String code, ActionCallback callback) {
        db.collection("invitationCodes").document(code)
                .update("active", false)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        callback.onSuccess();
                    } else {
                        callback.onFailure(task.getException());
                    }
                });
    }

    public void regenerateCode(String messId, String oldCode, RegenerateCodeCallback callback) {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            callback.onFailure(new Exception("User not signed in"));
            return;
        }

        String newCode = InvitationCodeGenerator.generateCode();
        long now = System.currentTimeMillis();
        long expiresAt = now + SEVEN_DAYS_MS;

        InvitationCode invitationCode = new InvitationCode(newCode, messId, currentUser.getUid(), now, expiresAt, true);

        WriteBatch batch = db.batch();
        if (oldCode != null && !oldCode.trim().isEmpty()) {
            batch.update(db.collection("invitationCodes").document(oldCode), "active", false);
        }
        batch.set(db.collection("invitationCodes").document(newCode), invitationCode);
        batch.update(db.collection("messes").document(messId), "invitationCode", newCode);

        batch.commit().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                callback.onSuccess(newCode);
            } else {
                callback.onFailure(task.getException());
            }
        });
    }

    public ListenerRegistration observeMess(String messId, MessListener listener) {
        return db.collection("messes").document(messId).addSnapshotListener((snapshot, error) -> {
            if (error != null) {
                listener.onError(error);
                return;
            }
            if (snapshot != null) {
                listener.onMessUpdated(snapshot.exists() ? snapshot.toObject(Mess.class) : null);
            }
        });
    }

    public ListenerRegistration observeMembers(String messId, MembersListener listener) {
        return db.collection("messes").document(messId).collection("members")
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null) {
                        listener.onError(error);
                        return;
                    }
                    if (snapshot != null) {
                        List<Member> members = new ArrayList<>();
                        for (DocumentSnapshot doc : snapshot.getDocuments()) {
                            Member member = doc.toObject(Member.class);
                            if (member != null) {
                                members.add(member);
                            }
                        }
                        listener.onMembersUpdated(members);
                    }
                });
    }

    public void removeMember(String messId, String memberUid, ActionCallback callback) {
        WriteBatch batch = db.batch();
        batch.delete(db.collection("messes").document(messId).collection("members").document(memberUid));

        Map<String, Object> userReset = new HashMap<>();
        userReset.put("messId", null);
        userReset.put("role", null);
        batch.update(db.collection("users").document(memberUid), userReset);

        batch.commit().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                callback.onSuccess();
            } else {
                callback.onFailure(task.getException());
            }
        });
    }

    public void updateIncludeInFixed(String messId, String memberUid, boolean includeInFixed, ActionCallback callback) {
        db.collection("messes").document(messId).collection("members").document(memberUid)
                .update("includeInFixed", includeInFixed)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        if (callback != null) callback.onSuccess();
                    } else {
                        if (callback != null) callback.onFailure(task.getException());
                    }
                });
    }
}
